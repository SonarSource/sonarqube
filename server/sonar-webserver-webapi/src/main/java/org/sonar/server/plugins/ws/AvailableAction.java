/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.plugins.ws;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.user.UserSession;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonarqube.ws.Plugins.AvailablePlugin;
import org.sonarqube.ws.Plugins.AvailablePluginsWsResponse;
import org.sonarqube.ws.Plugins.AvailablePluginsWsResponse.Builder;
import org.sonarqube.ws.Plugins.Update;

import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.plugins.edition.EditionBundledPlugins.isEditionBundled;
import static org.sonar.server.plugins.ws.PluginWSCommons.NAME_KEY_PLUGIN_UPDATE_ORDERING;
import static org.sonar.server.plugins.ws.PluginWSCommons.buildRelease;
import static org.sonar.server.plugins.ws.PluginWSCommons.buildRequires;
import static org.sonar.server.plugins.ws.PluginWSCommons.convertUpdateCenterStatus;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class AvailableAction implements PluginsWsAction {

  private static final boolean DO_NOT_FORCE_REFRESH = false;

  private final UserSession userSession;
  private final UpdateCenterMatrixFactory updateCenterFactory;

  public AvailableAction(UserSession userSession, UpdateCenterMatrixFactory updateCenterFactory) {
    this.userSession = userSession;
    this.updateCenterFactory = updateCenterFactory;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("available")
      .setDescription("Get the list of all the plugins available for installation on the SonarQube instance, sorted by plugin name." +
        "<br/>" +
        "Plugin information is retrieved from Update Center. Date and time at which Update Center was last refreshed is provided in the response." +
        "<br/>" +
        "Update status values are: " +
        "<ul>" +
        "<li>COMPATIBLE: plugin is compatible with current SonarQube instance.</li>" +
        "<li>INCOMPATIBLE: plugin is not compatible with current SonarQube instance.</li>" +
        "<li>REQUIRES_SYSTEM_UPGRADE: plugin requires SonarQube to be upgraded before being installed.</li>" +
        "<li>DEPS_REQUIRE_SYSTEM_UPGRADE: at least one plugin on which the plugin is dependent requires SonarQube to be upgraded.</li>" +
        "</ul>" +
        "Require 'Administer System' permission.")
      .setSince("5.2")
      .setHandler(this)
      .setResponseExample(this.getClass().getResource("example-available_plugins.json"));
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkIsSystemAdministrator();

    Optional<UpdateCenter> updateCenter = updateCenterFactory.getUpdateCenter(DO_NOT_FORCE_REFRESH);
    Collection<AvailablePlugin> plugins = updateCenter.isPresent() ? getPlugins(updateCenter.get()) : Collections.emptyList();
    Builder responseBuilder = AvailablePluginsWsResponse.newBuilder().addAllPlugins(plugins);
    updateCenter.ifPresent(u -> responseBuilder.setUpdateCenterRefresh(formatDateTime(u.getDate().getTime())));

    writeProtobuf(responseBuilder.build(), request, response);
  }

  private static List<AvailablePlugin> getPlugins(UpdateCenter updateCenter) {
    return retrieveAvailablePlugins(updateCenter)
      .stream()
      .map(pluginUpdate -> {
        Plugin plugin = pluginUpdate.getPlugin();
        AvailablePlugin.Builder builder = AvailablePlugin.newBuilder()
          .setKey(plugin.getKey())
          .setEditionBundled(isEditionBundled(plugin))
          .setRelease(buildRelease(pluginUpdate.getRelease()))
          .setUpdate(buildUpdate(pluginUpdate));
        ofNullable(plugin.getName()).ifPresent(builder::setName);
        ofNullable(plugin.getCategory()).ifPresent(builder::setCategory);
        ofNullable(plugin.getDescription()).ifPresent(builder::setDescription);
        ofNullable(plugin.getLicense()).ifPresent(builder::setLicense);
        ofNullable(plugin.getTermsConditionsUrl()).ifPresent(builder::setTermsAndConditionsUrl);
        ofNullable(plugin.getOrganization()).ifPresent(builder::setOrganizationName);
        ofNullable(plugin.getOrganizationUrl()).ifPresent(builder::setOrganizationUrl);
        ofNullable(plugin.getIssueTrackerUrl()).ifPresent(builder::setIssueTrackerUrl);
        ofNullable(plugin.getHomepageUrl()).ifPresent(builder::setHomepageUrl);
        return builder.build();
      }).toList();
  }

  private static Update buildUpdate(PluginUpdate pluginUpdate) {
    return Update.newBuilder()
      .setStatus(convertUpdateCenterStatus(pluginUpdate.getStatus()))
      .addAllRequires(buildRequires(pluginUpdate))
      .build();
  }

  private static Collection<PluginUpdate> retrieveAvailablePlugins(UpdateCenter updateCenter) {
    return updateCenter.findAvailablePlugins().stream().sorted(NAME_KEY_PLUGIN_UPDATE_ORDERING).toList();
  }

}
