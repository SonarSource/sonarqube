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
package org.sonar.server.plugins.ws;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.plugins.ws.PluginUpdateAggregator.PluginUpdateAggregate;
import org.sonar.server.user.UserSession;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonarqube.ws.Plugins.AvailableUpdate;
import org.sonarqube.ws.Plugins.UpdatablePlugin;
import org.sonarqube.ws.Plugins.UpdatesPluginsWsResponse;
import org.sonarqube.ws.Plugins.UpdatesPluginsWsResponse.Builder;

import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.plugins.edition.EditionBundledPlugins.isEditionBundled;
import static org.sonar.server.plugins.ws.PluginWSCommons.buildRelease;
import static org.sonar.server.plugins.ws.PluginWSCommons.buildRequires;
import static org.sonar.server.plugins.ws.PluginWSCommons.convertUpdateCenterStatus;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

/**
 * Implementation of the {@code updates} action for the Plugins WebService.
 */
public class UpdatesAction implements PluginsWsAction {

  private static final boolean DO_NOT_FORCE_REFRESH = false;
  private static final String HTML_TAG_BR = "<br/>";

  private static final Ordering<PluginUpdateAggregate> NAME_KEY_PLUGIN_UPGRADE_AGGREGATE_ORDERING = Ordering.from(PluginWSCommons.NAME_KEY_PLUGIN_ORDERING)
    .onResultOf(PluginUpdateAggregate::getPlugin);
  private static final Ordering<PluginUpdate> PLUGIN_UPDATE_BY_VERSION_ORDERING = Ordering.natural()
    .onResultOf(input -> Objects.requireNonNull(input).getRelease().getVersion().toString());

  private final UserSession userSession;
  private final UpdateCenterMatrixFactory updateCenterMatrixFactory;
  private final PluginUpdateAggregator aggregator;

  public UpdatesAction(UserSession userSession, UpdateCenterMatrixFactory updateCenterMatrixFactory,
    PluginUpdateAggregator aggregator) {
    this.userSession = userSession;
    this.updateCenterMatrixFactory = updateCenterMatrixFactory;
    this.aggregator = aggregator;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("updates")
      .setDescription("Lists plugins installed on the SonarQube instance for which at least one newer version is available, sorted by plugin name." +
        HTML_TAG_BR +
        "Each newer version is listed, ordered from the oldest to the newest, with its own update/compatibility status." +
        HTML_TAG_BR +
        "Plugin information is retrieved from Update Center. Date and time at which Update Center was last refreshed is provided in the response." +
        HTML_TAG_BR +
        "Update status values are: [COMPATIBLE, INCOMPATIBLE, REQUIRES_UPGRADE, DEPS_REQUIRE_UPGRADE]." +
        HTML_TAG_BR +
        "Require 'Administer System' permission.")
      .setSince("5.2")
      .setHandler(this)
      .setResponseExample(this.getClass().getResource("example-updates_plugins.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    Optional<UpdateCenter> updateCenter = updateCenterMatrixFactory.getUpdateCenter(DO_NOT_FORCE_REFRESH);

    Collection<UpdatablePlugin> plugins = updateCenter.isPresent() ? getPluginUpdates(updateCenter.get()) : Collections.emptyList();
    Builder responseBuilder = UpdatesPluginsWsResponse.newBuilder().addAllPlugins(plugins);
    updateCenter.ifPresent(u -> responseBuilder.setUpdateCenterRefresh(formatDateTime(u.getDate().getTime())));

    writeProtobuf(responseBuilder.build(), request, response);
  }

  private List<UpdatablePlugin> getPluginUpdates(UpdateCenter updateCenter) {
    return retrieveUpdatablePlugins(updateCenter)
      .stream()
      .map(pluginUpdateAggregate -> {
        Plugin plugin = pluginUpdateAggregate.getPlugin();
        UpdatablePlugin.Builder builder = UpdatablePlugin.newBuilder()
          .setKey(plugin.getKey())
          .setEditionBundled(isEditionBundled(plugin))
          .addAllUpdates(buildUpdates(pluginUpdateAggregate));
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

  private static Collection<AvailableUpdate> buildUpdates(PluginUpdateAggregate pluginUpdateAggregate) {
    return ImmutableSortedSet.copyOf(PLUGIN_UPDATE_BY_VERSION_ORDERING, pluginUpdateAggregate.getUpdates()).stream()
      .map(pluginUpdate -> AvailableUpdate.newBuilder()
        .setRelease(buildRelease(pluginUpdate.getRelease()))
        .setStatus(convertUpdateCenterStatus(pluginUpdate.getStatus()))
        .addAllRequires(buildRequires(pluginUpdate))
        .build())
      .toList();
  }

  private Collection<PluginUpdateAggregate> retrieveUpdatablePlugins(UpdateCenter updateCenter) {
    List<PluginUpdate> pluginUpdates = updateCenter.findPluginUpdates();
    // aggregates updates of the same plugin to a single object and sort these objects by plugin name then key
    return ImmutableSortedSet.copyOf(
      NAME_KEY_PLUGIN_UPGRADE_AGGREGATE_ORDERING,
      aggregator.aggregate(pluginUpdates));
  }
}
