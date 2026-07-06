/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.almsettings.ws;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.MediaTypes;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sonar.db.alm.setting.ALM.GITHUB;

/**
 * Starts the GitHub App Manifest flow: validates the request, generates the manifest plus a
 * single-use {@code state} token, and returns everything the browser needs to POST the manifest to
 * GitHub. GitHub then redirects back to {@code GithubManifestCallbackFilter}.
 */
public class CreateGithubFromManifestAction implements AlmSettingsWsAction {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_ORGANIZATION = "organization";
  private static final String PARAM_NAME = "name";
  private static final String PARAM_DEVOPS = "devops";
  private static final String PARAM_AUTH = "auth";
  private static final String DEFAULT_APP_NAME = "SonarQube - <add_unique_name>";
  private static final Gson GSON = new Gson();

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AlmSettingsSupport almSettingsSupport;
  private final GithubAppManifestGenerator manifestGenerator;
  private final GithubManifestStateStore stateStore;

  public CreateGithubFromManifestAction(DbClient dbClient, UserSession userSession, AlmSettingsSupport almSettingsSupport,
    GithubAppManifestGenerator manifestGenerator, GithubManifestStateStore stateStore) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.almSettingsSupport = almSettingsSupport;
    this.manifestGenerator = manifestGenerator;
    this.stateStore = stateStore;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("create_github_from_manifest")
      .setDescription("Initiate the creation of a GitHub instance Setting using the GitHub App Manifest flow. "
        + "Returns the manifest and a single-use state to be POSTed to GitHub by the browser. <br/>"
        + "Requires the 'Administer System' permission")
      .setPost(true)
      .setInternal(true)
      .setSince("2026.4")
      .setResponseExample(getClass().getResource("example-create_github_from_manifest.json"))
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(false)
      .setMaximumLength(200)
      .setDescription("Unique key of the GitHub instance setting that will be created. Required when '" + PARAM_DEVOPS + "' is true.");
    action.createParam(PARAM_ORGANIZATION)
      .setRequired(false)
      .setMaximumLength(200)
      .setDescription("GitHub organization the App should be created under. Leave empty to create it under the user's personal account.");
    action.createParam(PARAM_NAME)
      .setRequired(false)
      .setMaximumLength(200)
      .setDescription("Suggested name for the GitHub App (the user can change it on GitHub). Defaults to 'SonarQube - <add_unique_name>'.");
    action.createParam(PARAM_DEVOPS)
      .setRequired(false)
      .setBooleanPossibleValues()
      .setDefaultValue(true)
      .setDescription("Whether to create the DevOps Platform integration (project import / PR analysis) for this App.");
    action.createParam(PARAM_AUTH)
      .setRequired(false)
      .setBooleanPossibleValues()
      .setDefaultValue(false)
      .setDescription("Whether to also set up GitHub authentication (sign-in) using this App.");
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkIsSystemAdministrator();

    boolean setupDevops = request.mandatoryParamAsBoolean(PARAM_DEVOPS);
    boolean setupAuth = request.mandatoryParamAsBoolean(PARAM_AUTH);
    if (!setupDevops && !setupAuth) {
      throw BadRequestException.create("At least one of '" + PARAM_DEVOPS + "' or '" + PARAM_AUTH + "' must be true.");
    }

    String key = request.param(PARAM_KEY);
    String organization = request.param(PARAM_ORGANIZATION);
    String appName = requireNonNull(request.getParam(PARAM_NAME).emptyAsNull().or(() -> DEFAULT_APP_NAME));

    if (setupDevops) {
      almSettingsSupport.checkAlmMultipleFeatureEnabled(GITHUB);
      if (isBlank(key)) {
        throw BadRequestException.create("Parameter '" + PARAM_KEY + "' is required to create the DevOps Platform integration.");
      }
      try (DbSession dbSession = dbClient.openSession(false)) {
        almSettingsSupport.checkAlmSettingDoesNotAlreadyExist(dbSession, key);
      }
    }

    if (isBlank(manifestGenerator.baseUrl())) {
      throw BadRequestException.create("The server base URL must be configured (Administration > Configuration > Server base URL) "
        + "before creating a GitHub App, so that GitHub can reach this SonarQube instance.");
    }

    String setupPath = setupDevops ? GithubAppManifestGenerator.SETTINGS_PATH : GithubAppManifestGenerator.AUTH_SETTINGS_PATH;
    String manifest = manifestGenerator.generateManifest(appName, setupPath);
    String githubAppUrl = manifestGenerator.githubAppCreationUrl(organization);
    String state = stateStore.create(key, organization, requireNonNull(userSession.getUuid()), setupDevops, setupAuth);

    Map<String, String> body = new LinkedHashMap<>();
    body.put("githubAppUrl", githubAppUrl);
    body.put("manifest", manifest);
    body.put("state", state);
    writeJson(response, GSON.toJson(body));
  }

  private static void writeJson(Response response, String json) {
    response.stream().setMediaType(MediaTypes.JSON);
    try {
      response.stream().output().write(json.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write the GitHub App Manifest response", e);
    }
  }
}
