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
package org.sonar.server.plugins.ws;

import com.google.common.base.Function;
import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.updatecenter.common.Artifact;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.Release;

import java.util.List;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

public class AvailablePluginsWsAction implements PluginsWsAction {

  private static final boolean DO_NOT_FORCE_REFRESH = false;
  private static final String PROPERTY_KEY = "key";
  private static final String PROPERTY_NAME = "name";
  private static final String PROPERTY_DESCRIPTION = "description";
  private static final String PROPERTY_ORGANIZATION_NAME = "organizationName";
  private static final String PROPERTY_ORGANIZATION_URL = "organizationUrl";
  private static final String PROPERTY_URL = "url";
  private static final String PROPERTY_TERMS_AND_CONDITIONS_URL = "termsAndConditionsUrl";
  private static final String OBJECT_UPDATE = "update";
  private static final String OBJECT_ARTIFACT = "artifact";
  private static final String OBJECT_RELEASE = "release";
  private static final String PROPERTY_VERSION = "version";
  private static final String PROPERTY_DATE = "date";
  private static final String PROPERTY_STATUS = "status";
  private static final String ARRAY_REQUIRES = "requires";
  private static final String ARRAY_PLUGINS = "plugins";
  private static final String PROPERTY_CATEGORY = "category";
  private static final String PROPERTY_LICENSE = "license";

  private final UpdateCenterMatrixFactory updateCenterFactory;

  public AvailablePluginsWsAction(UpdateCenterMatrixFactory updateCenterFactory) {
    this.updateCenterFactory = updateCenterFactory;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("available")
      .setDescription("Get the list of all the plugins available for installation on the SonarQube instance, sorted by name." +
        "<br/>" +
        "Update status values are: [COMPATIBLE, INCOMPATIBLE, REQUIRES_UPGRADE, DEPS_REQUIRE_UPGRADE]")
      .setSince("5.2")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-available_plugins.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    JsonWriter jsonWriter = response.newJsonWriter();
    jsonWriter.beginObject();

    writePlugins(jsonWriter);

    jsonWriter.close();
  }

  private void writePlugins(JsonWriter jsonWriter) {
    jsonWriter.name(ARRAY_PLUGINS);
    jsonWriter.beginArray();
    for (PluginUpdate pluginUpdate : retrieveAvailablePlugins()) {
      writePluginUpdate(jsonWriter, pluginUpdate);
    }
    jsonWriter.endArray();
    jsonWriter.endObject();
  }

  private void writePluginUpdate(JsonWriter jsonWriter, PluginUpdate pluginUpdate) {
    jsonWriter.beginObject();
    Plugin plugin = pluginUpdate.getPlugin();

    jsonWriter.prop(PROPERTY_KEY, plugin.getKey());
    jsonWriter.prop(PROPERTY_NAME, plugin.getName());
    jsonWriter.prop(PROPERTY_CATEGORY, plugin.getCategory());
    jsonWriter.prop(PROPERTY_DESCRIPTION, plugin.getDescription());
    jsonWriter.prop(PROPERTY_LICENSE, plugin.getLicense());
    jsonWriter.prop(PROPERTY_TERMS_AND_CONDITIONS_URL, plugin.getTermsConditionsUrl());
    jsonWriter.prop(PROPERTY_ORGANIZATION_NAME, plugin.getOrganization());
    jsonWriter.prop(PROPERTY_ORGANIZATION_URL, plugin.getOrganizationUrl());

    writeRelease(jsonWriter, pluginUpdate.getRelease());

    writeUpdate(jsonWriter, pluginUpdate);

    jsonWriter.endObject();
  }

  private List<PluginUpdate> retrieveAvailablePlugins() {
    return updateCenterFactory.getUpdateCenter(DO_NOT_FORCE_REFRESH).findAvailablePlugins();
  }

  private void writeRelease(JsonWriter jsonWriter, Release release) {
    jsonWriter.name(OBJECT_RELEASE);
    jsonWriter.beginObject();
    jsonWriter.prop(PROPERTY_VERSION, release.getVersion().toString());
    jsonWriter.propDate(PROPERTY_DATE, release.getDate());
    writeArchive(jsonWriter, release);
    jsonWriter.endObject();
  }

  private void writeArchive(JsonWriter jsonWriter, Release release) {
    jsonWriter.name(OBJECT_ARTIFACT);
    jsonWriter.beginObject();
    jsonWriter.prop(PROPERTY_NAME, release.getFilename());
    jsonWriter.prop(PROPERTY_URL, release.getDownloadUrl());
    jsonWriter.endObject();
  }

  private void writeUpdate(JsonWriter jsonWriter, PluginUpdate pluginUpdate) {
    jsonWriter.name(OBJECT_UPDATE);
    jsonWriter.beginObject();
    jsonWriter.prop(PROPERTY_STATUS, toJSon(pluginUpdate.getStatus()));

    jsonWriter.name(ARRAY_REQUIRES);
    jsonWriter.beginArray();
    Release release = pluginUpdate.getRelease();
    for (Plugin child : filter(transform(release.getOutgoingDependencies(), ReleaseToArtifact.INSTANCE), Plugin.class)) {
      jsonWriter.beginObject();
      jsonWriter.prop(PROPERTY_KEY, child.getKey());
      jsonWriter.prop(AvailablePluginsWsAction.PROPERTY_NAME, child.getName());
      jsonWriter.prop(PROPERTY_DESCRIPTION, child.getDescription());
      jsonWriter.endObject();
    }
    jsonWriter.endArray();

    jsonWriter.endObject();
  }

  private static String toJSon(PluginUpdate.Status status) {
    switch (status) {
      case COMPATIBLE:
        return "COMPATIBLE";
      case INCOMPATIBLE:
        return "INCOMPATIBLE";
      case REQUIRE_SONAR_UPGRADE:
        return "REQUIRES_UPGRADE";
      case DEPENDENCIES_REQUIRE_SONAR_UPGRADE:
        return "DEPS_REQUIRE_UPGRADE";
      default:
        throw new IllegalArgumentException("Unsupported value of PluginUpdate.Status " + status);
    }
  }

  private enum ReleaseToArtifact implements Function<Release, Artifact> {
    INSTANCE;

    @Override
    public Artifact apply(Release input) {
      return input.getArtifact();
    }
  }
}
