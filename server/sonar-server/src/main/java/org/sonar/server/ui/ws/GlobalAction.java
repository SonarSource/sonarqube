/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.page.Page;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.dialect.H2;
import org.sonar.server.branch.BranchFeatureProxy;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.platform.WebServer;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.ui.VersionFormatter;
import org.sonar.server.user.UserSession;

import static org.sonar.api.CoreProperties.RATING_GRID;
import static org.sonar.core.config.WebConstants.SONAR_LF_ENABLE_GRAVATAR;
import static org.sonar.core.config.WebConstants.SONAR_LF_GRAVATAR_SERVER_URL;
import static org.sonar.core.config.WebConstants.SONAR_LF_LOGO_URL;
import static org.sonar.core.config.WebConstants.SONAR_LF_LOGO_WIDTH_PX;
import static org.sonar.process.ProcessProperties.Property.SONARCLOUD_ENABLED;
import static org.sonar.process.ProcessProperties.Property.SONARCLOUD_HOMEPAGE_URL;
import static org.sonar.process.ProcessProperties.Property.SONAR_ANALYTICS_TRACKING_ID;
import static org.sonar.process.ProcessProperties.Property.SONAR_PRISMIC_ACCESS_TOKEN;
import static org.sonar.process.ProcessProperties.Property.SONAR_UPDATECENTER_ACTIVATE;

public class GlobalAction implements NavigationWsAction, Startable {

  private static final Set<String> DYNAMIC_SETTING_KEYS = ImmutableSet.of(
    SONAR_LF_LOGO_URL,
    SONAR_LF_LOGO_WIDTH_PX,
    SONAR_LF_ENABLE_GRAVATAR,
    SONAR_LF_GRAVATAR_SERVER_URL,
    RATING_GRID);

  private final Map<String, String> systemSettingValuesByKey;

  private final PageRepository pageRepository;
  private final Configuration config;
  private final ResourceTypes resourceTypes;
  private final Server server;
  private final WebServer webServer;
  private final DbClient dbClient;
  private final OrganizationFlags organizationFlags;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final BranchFeatureProxy branchFeature;
  private final UserSession userSession;
  private final PlatformEditionProvider editionProvider;

  public GlobalAction(PageRepository pageRepository, Configuration config, ResourceTypes resourceTypes, Server server,
    WebServer webServer, DbClient dbClient, OrganizationFlags organizationFlags,
    DefaultOrganizationProvider defaultOrganizationProvider, BranchFeatureProxy branchFeature, UserSession userSession, PlatformEditionProvider editionProvider) {
    this.pageRepository = pageRepository;
    this.config = config;
    this.resourceTypes = resourceTypes;
    this.server = server;
    this.webServer = webServer;
    this.dbClient = dbClient;
    this.organizationFlags = organizationFlags;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.branchFeature = branchFeature;
    this.userSession = userSession;
    this.editionProvider = editionProvider;
    this.systemSettingValuesByKey = new HashMap<>();
  }

  @Override
  public void start() {
    this.systemSettingValuesByKey.put(SONAR_UPDATECENTER_ACTIVATE.getKey(), config.get(SONAR_UPDATECENTER_ACTIVATE.getKey()).orElse(null));
    boolean isOnSonarCloud = config.getBoolean(SONARCLOUD_ENABLED.getKey()).orElse(false);
    if (isOnSonarCloud) {
      this.systemSettingValuesByKey.put(SONAR_PRISMIC_ACCESS_TOKEN.getKey(), config.get(SONAR_PRISMIC_ACCESS_TOKEN.getKey()).orElse(null));
      this.systemSettingValuesByKey.put(SONAR_ANALYTICS_TRACKING_ID.getKey(), config.get(SONAR_ANALYTICS_TRACKING_ID.getKey()).orElse(null));
      this.systemSettingValuesByKey.put(SONARCLOUD_HOMEPAGE_URL.getKey(), config.get(SONARCLOUD_HOMEPAGE_URL.getKey()).orElse(null));
    }
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
      .setSince("5.2");
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
      writeDatabaseProduction(json);
      writeOrganizationSupport(json);
      writeBranchSupport(json);
      editionProvider.get().ifPresent(e -> json.prop("edition", e.name().toLowerCase(Locale.ENGLISH)));
      json.prop("standalone", webServer.isStandalone());
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

  private void writeDatabaseProduction(JsonWriter json) {
    json.prop("productionDatabase", !dbClient.getDatabase().getDialect().getId().equals(H2.ID));
  }

  private void writeOrganizationSupport(JsonWriter json) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      json.prop("organizationsEnabled", organizationFlags.isEnabled(dbSession));
      json.prop("defaultOrganization", defaultOrganizationProvider.get().getKey());
    }
  }

  private void writeBranchSupport(JsonWriter json) {
    json.prop("branchesEnabled", branchFeature.isEnabled());
  }

}
