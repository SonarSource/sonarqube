/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.ws;

import java.util.List;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.SonarUpdate;
import org.sonar.updatecenter.common.UpdateCenter;

import com.google.common.io.Resources;

/**
 * Implementation of the {@code upgrades} action for the System WebService.
 */
public class UpgradesSystemWsAction implements SystemWsAction {

  private static final boolean DO_NOT_FORCE_REFRESH = false;
  private static final String ARRAY_UPGRADES = "upgrades";
  private static final String PROPERTY_NAME = "name";
  private static final String PROPERTY_UPDATE_CENTER_REFRESH = "updateCenterRefresh";
  private static final String PROPERTY_VERSION = "version";
  private static final String PROPERTY_DESCRIPTION = "description";
  private static final String PROPERTY_RELEASE_DATE = "releaseDate";
  private static final String PROPERTY_CHANGE_LOG_URL = "changeLogUrl";
  private static final String PROPERTY_DOWNLOAD_URL = "downloadUrl";
  private static final String OBJECT_PLUGINS = "plugins";
  private static final String ARRAY_REQUIRE_UPDATE = "requireUpdate";
  private static final String PROPERTY_KEY = "key";
  private static final String ARRAY_INCOMPATIBLE = "incompatible";

  private final UpdateCenterMatrixFactory updateCenterFactory;

  public UpgradesSystemWsAction(UpdateCenterMatrixFactory updateCenterFactory) {
    this.updateCenterFactory = updateCenterFactory;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("upgrades")
      .setDescription("Lists available upgrades for the SonarQube instance (if any) and for each one, " +
        "lists incompatible plugins and plugins requiring upgrade." +
        "<br/>" +
        "Plugin information is retrieved from Update Center. Date and time at which Update Center was last refreshed " +
        "is provided in the response.")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-updates_plugins.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    JsonWriter jsonWriter = response.newJsonWriter().setSerializeEmptys(false);

    jsonWriter.beginObject();
    UpdateCenter updateCenter = updateCenterFactory.getUpdateCenter(DO_NOT_FORCE_REFRESH);
    writeUpgrades(jsonWriter, updateCenter);
    jsonWriter.propDateTime(PROPERTY_UPDATE_CENTER_REFRESH, updateCenter.getDate());
    jsonWriter.endObject();

    jsonWriter.close();
  }

  private void writeUpgrades(JsonWriter jsonWriter, UpdateCenter updateCenter) {
    jsonWriter.name(ARRAY_UPGRADES).beginArray();

    for (SonarUpdate sonarUpdate : updateCenter.findSonarUpdates()) {
      writeUpgrade(jsonWriter, sonarUpdate);
    }

    jsonWriter.endArray();
  }

  private void writeUpgrade(JsonWriter jsonWriter, SonarUpdate sonarUpdate) {
    jsonWriter.beginObject();

    writeMetadata(jsonWriter, sonarUpdate.getRelease());

    writePlugins(jsonWriter, sonarUpdate);

    jsonWriter.endObject();
  }

  private void writeMetadata(JsonWriter jsonWriter, Release release) {
    jsonWriter.prop(PROPERTY_VERSION, release.getVersion().getName());
    jsonWriter.prop(PROPERTY_DESCRIPTION, release.getDescription());
    jsonWriter.propDate(PROPERTY_RELEASE_DATE, release.getDate());
    jsonWriter.prop(PROPERTY_CHANGE_LOG_URL, release.getChangelogUrl());
    jsonWriter.prop(PROPERTY_DOWNLOAD_URL, release.getDownloadUrl());
  }

  private void writePlugins(JsonWriter jsonWriter, SonarUpdate sonarUpdate) {
    jsonWriter.name(OBJECT_PLUGINS).beginObject();

    writePluginsToUpdate(jsonWriter, sonarUpdate.getPluginsToUpgrade());

    writeIncompatiblePlugins(jsonWriter, sonarUpdate.getIncompatiblePlugins());

    jsonWriter.endObject();
  }

  private void writePluginsToUpdate(JsonWriter jsonWriter, List<Release> pluginsToUpgrade) {
    jsonWriter.name(ARRAY_REQUIRE_UPDATE).beginArray();
    for (Release release : pluginsToUpgrade) {
      jsonWriter.beginObject();

      Plugin plugin = (Plugin) release.getArtifact();
      writePlugin(jsonWriter, plugin);
      jsonWriter.prop(PROPERTY_VERSION, release.getVersion().toString());

      jsonWriter.endObject();
    }

    jsonWriter.endArray();
  }

  private void writePlugin(JsonWriter jsonWriter, Plugin plugin) {
    jsonWriter.prop(PROPERTY_KEY, plugin.getKey());
    jsonWriter.prop(PROPERTY_NAME, plugin.getName());
    jsonWriter.prop(PROPERTY_DESCRIPTION, plugin.getDescription());
  }

  private void writeIncompatiblePlugins(JsonWriter jsonWriter, List<Plugin> incompatiblePlugins) {
    jsonWriter.name(ARRAY_INCOMPATIBLE).beginArray();

    for (Plugin incompatiblePlugin : incompatiblePlugins) {
      jsonWriter.beginObject();
      writePlugin(jsonWriter, incompatiblePlugin);
      jsonWriter.endObject();
    }

    jsonWriter.endArray();
  }
}
