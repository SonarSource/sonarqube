/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.ui.ws;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.page.Page;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.dialect.H2;
import org.sonar.server.authentication.DefaultAdminCredentialsVerifier;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.ui.VersionFormatter;
import org.sonar.server.ui.WebAnalyticsLoader;
import org.sonar.server.user.UserSession;

import static org.sonar.api.CoreProperties.DEVELOPER_AGGREGATED_INFO_DISABLED;
import static org.sonar.api.CoreProperties.RATING_GRID;
import static org.sonar.api.internal.MetadataLoader.loadSqVersionEol;
import static org.sonar.core.config.WebConstants.SONAR_LF_ENABLE_GRAVATAR;
import static org.sonar.core.config.WebConstants.SONAR_LF_GRAVATAR_SERVER_URL;
import static org.sonar.core.config.WebConstants.SONAR_LF_LOGO_URL;
import static org.sonar.core.config.WebConstants.SONAR_LF_LOGO_WIDTH_PX;
import static org.sonar.process.ProcessProperties.Property.SONAR_UPDATECENTER_ACTIVATE;

public class GlobalAction implements NavigationWsAction, Startable {

  private static final Set<String> DYNAMIC_SETTING_KEYS = Set.of(
    SONAR_LF_LOGO_URL,
    SONAR_LF_LOGO_WIDTH_PX,
    SONAR_LF_ENABLE_GRAVATAR,
    SONAR_LF_GRAVATAR_SERVER_URL,
    RATING_GRID,
    DEVELOPER_AGGREGATED_INFO_DISABLED);

  private final Map<String, String> systemSettingValuesByKey;

  private final PageRepository pageRepository;
  private final Configuration config;
  private final ResourceTypes resourceTypes;
  private final Server server;
  private final NodeInformation nodeInformation;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final PlatformEditionProvider editionProvider;
  private final WebAnalyticsLoader webAnalyticsLoader;
  private final IssueIndexSyncProgressChecker issueIndexSyncChecker;
  private final DefaultAdminCredentialsVerifier defaultAdminCredentialsVerifier;
  private final DocumentationLinkGenerator documentationLinkGenerator;

  public GlobalAction(PageRepository pageRepository, Configuration config, ResourceTypes resourceTypes, Server server,
    NodeInformation nodeInformation, DbClient dbClient, UserSession userSession, PlatformEditionProvider editionProvider,
    WebAnalyticsLoader webAnalyticsLoader, IssueIndexSyncProgressChecker issueIndexSyncChecker,
    DefaultAdminCredentialsVerifier defaultAdminCredentialsVerifier, DocumentationLinkGenerator documentationLinkGenerator) {
    this.pageRepository = pageRepository;
    this.config = config;
    this.resourceTypes = resourceTypes;
    this.server = server;
    this.nodeInformation = nodeInformation;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.editionProvider = editionProvider;
    this.webAnalyticsLoader = webAnalyticsLoader;
    this.systemSettingValuesByKey = new HashMap<>();
    this.issueIndexSyncChecker = issueIndexSyncChecker;
    this.defaultAdminCredentialsVerifier = defaultAdminCredentialsVerifier;
    this.documentationLinkGenerator = documentationLinkGenerator;
  }

  @Override
  public void start() {
    this.systemSettingValuesByKey.put(SONAR_UPDATECENTER_ACTIVATE.getKey(), config.get(SONAR_UPDATECENTER_ACTIVATE.getKey()).orElse(null));
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  @Override
  public void define(NewController context) {
    context.createAction("global")
      .setDescription("Get information concerning global navigation for the current user.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("global-example.json"))
      .setSince("5.2")
      .setChangelog(new Change("10.5", "Field 'versionEOL' added, to indicate the end of support of installed version."));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject();
      writeActions(json);
      writePages(json);
      writeSettings(json);
      writeDeprecatedLogoProperties(json);
      writeQualifiers(json);
      writeVersion(json);
      writeVersionEol(json);
      writeDatabaseProduction(json);
      writeInstanceUsesDefaultAdminCredentials(json);
      editionProvider.get().ifPresent(e -> json.prop("edition", e.name().toLowerCase(Locale.ENGLISH)));
      writeNeedIssueSync(json);
      json.prop("standalone", nodeInformation.isStandalone());
      writeWebAnalytics(json);
      writeDocumentationUrl(json);
      json.endObject();
    }
  }

  private void writeActions(JsonWriter json) {
    json.prop("canAdmin", userSession.isSystemAdministrator());
  }

  private void writePages(JsonWriter json) {
    json.name("globalPages").beginArray();
    for (Page page : pageRepository.getGlobalPages(false)) {
      json.beginObject()
        .prop("key", page.getKey())
        .prop("name", page.getName())
        .endObject();
    }
    json.endArray();
  }

  private void writeSettings(JsonWriter json) {
    json.name("settings").beginObject();
    DYNAMIC_SETTING_KEYS.forEach(key -> json.prop(key, config.get(key).orElse(null)));
    systemSettingValuesByKey.forEach(json::prop);
    json.endObject();
  }

  private void writeDeprecatedLogoProperties(JsonWriter json) {
    json.prop("logoUrl", config.get(SONAR_LF_LOGO_URL).orElse(null));
    json.prop("logoWidth", config.get(SONAR_LF_LOGO_WIDTH_PX).orElse(null));
  }

  private void writeQualifiers(JsonWriter json) {
    json.name("qualifiers").beginArray();
    for (ResourceType rootType : resourceTypes.getRoots()) {
      json.value(rootType.getQualifier());
    }
    json.endArray();
  }

  private void writeVersion(JsonWriter json) {
    String displayVersion = VersionFormatter.format(server.getVersion());
    json.prop("version", displayVersion);
  }

  private void writeVersionEol(JsonWriter json) {
    json.prop("versionEOL", loadSqVersionEol(System2.INSTANCE));
  }

  private void writeDatabaseProduction(JsonWriter json) {
    json.prop("productionDatabase", !dbClient.getDatabase().getDialect().getId().equals(H2.ID));
  }

  private void writeInstanceUsesDefaultAdminCredentials(JsonWriter json) {
    if (userSession.isSystemAdministrator()) {
      json.prop("instanceUsesDefaultAdminCredentials", defaultAdminCredentialsVerifier.hasDefaultCredentialUser());
    }
  }

  private void writeNeedIssueSync(JsonWriter json) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      json.prop("needIssueSync", issueIndexSyncChecker.isIssueSyncInProgress(dbSession));
    }
  }

  private void writeWebAnalytics(JsonWriter json) {
    webAnalyticsLoader.getUrlPathToJs().ifPresent(p -> json.prop("webAnalyticsJsPath", p));
  }

  private void writeDocumentationUrl(JsonWriter json) {
    json.prop("documentationUrl", documentationLinkGenerator.getDocumentationLink(null));
  }
}
