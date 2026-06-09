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

import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.Strings.CS;
import static org.sonar.db.alm.setting.ALM.GITHUB;
import static org.sonar.db.permission.ProjectPermission.ADMIN;

@ServerSide
public class AlmSettingsSupport {

  private static final Pattern WORKSPACE_ID_PATTERN = Pattern.compile("^[a-z0-9\\-_]+$");

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final MultipleAlmFeature multipleAlmFeature;

  public AlmSettingsSupport(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder,
    MultipleAlmFeature multipleAlmFeature) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.multipleAlmFeature = multipleAlmFeature;
  }

  public void checkAlmSettingDoesNotAlreadyExist(DbSession dbSession, String almSetting) {
    dbClient.almSettingDao().selectByKey(dbSession, almSetting)
      .ifPresent(a -> {
        throw new IllegalArgumentException(format("An DevOps Platform setting with key '%s' already exists", a.getKey()));
      });
  }

  public void checkAlmMultipleFeatureEnabled(ALM alm) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (!multipleAlmFeature.isAvailable() && !dbClient.almSettingDao().selectByAlm(dbSession, alm).isEmpty()) {
        throw BadRequestException.create("A " + alm + " setting is already defined");
      }
    }
  }

  public void checkBitbucketCloudWorkspaceIDFormat(String workspaceId) {
    if (!WORKSPACE_ID_PATTERN.matcher(workspaceId).matches()) {
      throw BadRequestException.create(String.format(
        "Workspace ID '%s' has an incorrect format. Should only contain lowercase letters, numbers, dashes, and underscores.",
        workspaceId
      ));
    }
  }

  public ProjectDto getProjectAsAdmin(DbSession dbSession, String projectKey) {
    return getProject(dbSession, projectKey, ADMIN);
  }

  public ProjectDto getProject(DbSession dbSession, String projectKey, ProjectPermission projectPermission) {
    ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
    userSession.checkEntityPermission(projectPermission, project);
    return project;
  }

  /**
   * Values needed to persist a GitHub ALM setting, bundled so the same insertion logic can be shared by
   * the manual {@code create_github} web service and the GitHub App Manifest flow.
   */
  public record NewGithubSetting(String key, String url, String appId, String privateKey, String clientId,
    String clientSecret, @Nullable String webhookSecret) {
  }

  /**
   * Inserts a new GitHub ALM setting. Shared by the manual {@code create_github} web service and the
   * GitHub App Manifest flow so both persist credentials identically (encryption and auditing are
   * handled by the DAO).
   */
  public void createGithubSetting(DbSession dbSession, NewGithubSetting setting) {
    dbClient.almSettingDao().insert(dbSession, new AlmSettingDto()
      .setAlm(GITHUB)
      .setKey(setting.key())
      .setUrl(CS.removeEnd(setting.url(), "/"))
      .setAppId(setting.appId())
      .setPrivateKey(setting.privateKey())
      .setClientId(setting.clientId())
      .setClientSecret(setting.clientSecret())
      .setWebhookSecret(setting.webhookSecret()));
  }

  public AlmSettingDto getAlmSetting(DbSession dbSession, String almSetting) {
    return dbClient.almSettingDao().selectByKey(dbSession, almSetting)
      .orElseThrow(() -> new NotFoundException(format("DevOps Platform setting with key '%s' cannot be found", almSetting)));
  }

  public void checkPrivateKeyOnUrlUpdate(AlmSettingDto almSettingDto, String url, @Nullable String privateKey) {
    checkCredentialArtifactOnUrlUpdate(url, almSettingDto, privateKey, "Please provide the Private Key to update the URL.");
  }

  public void checkPatOnUrlUpdate(AlmSettingDto almSettingDto, String url, @Nullable String pat) {
    checkCredentialArtifactOnUrlUpdate(url, almSettingDto, pat, "Please provide the Personal Access Token to update the URL.");
  }

  private static void checkCredentialArtifactOnUrlUpdate(String url, AlmSettingDto almSettingDto, @Nullable String credentialArtifact, String errorMessage) {
    if (!url.equals(almSettingDto.getUrl()) && isEmpty(credentialArtifact)) {
      throw new IllegalArgumentException(errorMessage);
    }
  }
}
