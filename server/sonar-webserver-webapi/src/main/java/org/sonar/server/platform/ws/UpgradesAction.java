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
package org.sonar.server.platform.ws;

import com.google.common.base.Optional;
import com.google.common.io.Resources;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.plugins.ws.PluginWSCommons;
import org.sonar.server.ui.VersionFormatter;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.SonarUpdate;
import org.sonar.updatecenter.common.UpdateCenter;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Implementation of the {@code upgrades} action for the System WebService.
 */
public class UpgradesAction implements SystemWsAction {

  private static final boolean DO_NOT_FORCE_REFRESH = false;

  private static final String ARRAY_UPGRADES = "upgrades";
  private static final String PROPERTY_UPDATE_CENTER_REFRESH = "updateCenterRefresh";
  private static final String PROPERTY_VERSION = "version";
  private static final String PROPERTY_DESCRIPTION = "description";
  private static final String PROPERTY_RELEASE_DATE = "releaseDate";
  private static final String PROPERTY_CHANGE_LOG_URL = "changeLogUrl";
  private static final String PROPERTY_DOWNLOAD_URL = "downloadUrl";
  private static final String OBJECT_PLUGINS = "plugins";
  private static final String ARRAY_REQUIRE_UPDATE = "requireUpdate";
  private static final String ARRAY_INCOMPATIBLE = "incompatible";

  private final UpdateCenterMatrixFactory updateCenterFactory;

  public UpgradesAction(UpdateCenterMatrixFactory updateCenterFactory) {
    this.updateCenterFactory = updateCenterFactory;
  }

  private static void writeMetadata(JsonWriter jsonWriter, Release release) {
    jsonWriter.prop(PROPERTY_VERSION, VersionFormatter.format(release.getVersion().getName()));
    jsonWriter.prop(PROPERTY_DESCRIPTION, release.getDescription());
    jsonWriter.propDate(PROPERTY_RELEASE_DATE, release.getDate());
    jsonWriter.prop(PROPERTY_CHANGE_LOG_URL, release.getChangelogUrl());
    jsonWriter.prop(PROPERTY_DOWNLOAD_URL, release.getDownloadUrl());
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("upgrades")
      .setDescription("Lists available upgrades for the SonarQube instance (if any) and for each one, " +
        "lists incompatible plugins and plugins requiring upgrade." +
        "<br/>" +
        "Plugin information is retrieved from Update Center. Date and time at which Update Center was last refreshed " +
        "is provided in the response.")
      .setSince("5.2")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-upgrades_plugins.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (JsonWriter jsonWriter = response.newJsonWriter()) {
      jsonWriter.setSerializeEmptys(false);
      writeResponse(jsonWriter);
    }
  }

  private void writeResponse(JsonWriter jsonWriter) {
    jsonWriter.beginObject();

    Optional<UpdateCenter> updateCenter = updateCenterFactory.getUpdateCenter(DO_NOT_FORCE_REFRESH);
    writeUpgrades(jsonWriter, updateCenter);
    if (updateCenter.isPresent()) {
      jsonWriter.propDateTime(PROPERTY_UPDATE_CENTER_REFRESH, updateCenter.get().getDate());
    }

    jsonWriter.endObject();
  }

  private static void writeUpgrades(JsonWriter jsonWriter, Optional<UpdateCenter> updateCenter) {
    jsonWriter.name(ARRAY_UPGRADES).beginArray();

    if (updateCenter.isPresent()) {
      for (SonarUpdate sonarUpdate : updateCenter.get().findSonarUpdates()) {
        writeUpgrade(jsonWriter, sonarUpdate);
      }
    }

    jsonWriter.endArray();
  }

  private static void writeUpgrade(JsonWriter jsonWriter, SonarUpdate sonarUpdate) {
    jsonWriter.beginObject();

    writeMetadata(jsonWriter, sonarUpdate.getRelease());

    writePlugins(jsonWriter, sonarUpdate);

    jsonWriter.endObject();
  }

  private static void writePlugins(JsonWriter jsonWriter, SonarUpdate sonarUpdate) {
    jsonWriter.name(OBJECT_PLUGINS).beginObject();

    writePluginsToUpdate(jsonWriter, sonarUpdate.getPluginsToUpgrade());

    writeIncompatiblePlugins(jsonWriter, sonarUpdate.getIncompatiblePlugins());

    jsonWriter.endObject();
  }

  private static void writePluginsToUpdate(JsonWriter jsonWriter, List<Release> pluginsToUpgrade) {
    jsonWriter.name(ARRAY_REQUIRE_UPDATE).beginArray();
    for (Release release : pluginsToUpgrade) {
      jsonWriter.beginObject();

      PluginWSCommons.writePlugin(jsonWriter, (Plugin) release.getArtifact());
      String version = isNotBlank(release.getDisplayVersion()) ? release.getDisplayVersion() : release.getVersion().toString();
      jsonWriter.prop(PROPERTY_VERSION, version);

      jsonWriter.endObject();
    }

    jsonWriter.endArray();
  }

  private static void writeIncompatiblePlugins(JsonWriter jsonWriter, List<Plugin> incompatiblePlugins) {
    jsonWriter.name(ARRAY_INCOMPATIBLE).beginArray();

    for (Plugin incompatiblePlugin : incompatiblePlugins) {
      jsonWriter.beginObject();
      PluginWSCommons.writePlugin(jsonWriter, incompatiblePlugin);
      jsonWriter.endObject();
    }

    jsonWriter.endArray();
  }
}
