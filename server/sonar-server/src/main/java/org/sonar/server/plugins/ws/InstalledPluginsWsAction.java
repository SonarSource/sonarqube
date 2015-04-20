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
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.io.Resources;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.SortedSet;

import static com.google.common.collect.Iterables.filter;

/**
 * Implementation of the {@code installed} action for the Plugins WebService.
 */
public class InstalledPluginsWsAction implements PluginsWsAction {
  private static final String PROPERTY_KEY = "key";
  private static final String PROPERTY_NAME = "name";
  private static final String PROPERTY_DESCRIPTION = "description";
  private static final String PROPERTY_LICENSE = "license";
  private static final String PROPERTY_VERSION = "version";
  private static final String ARRAY_PLUGINS = "plugins";
  private static final String PROPERTY_ORGANIZATION_NAME = "organizationName";
  private static final String PROPERTY_ORGANIZATION_URL = "organizationUrl";
  private static final String OBJECT_URLS = "urls";
  private static final String PROPERTY_HOMEPAGE = "homepage";
  private static final String PROPERTY_ISSUE_TRACKER = "issueTracker";
  private static final String OBJECT_ARTIFACT = "artifact";

  private final PluginRepository pluginRepository;

  public static final Ordering<PluginMetadata> NAME_KEY_PLUGIN_METADATA_COMPARATOR = Ordering.natural()
    .onResultOf(PluginMetadataToName.INSTANCE)
    .compound(Ordering.natural().onResultOf(PluginMetadataToKey.INSTANCE));

  public InstalledPluginsWsAction(PluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("installed")
      .setDescription("Get the list of all the plugins installed on the SonarQube instance, sorted by name")
      .setSince("5.2")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-installed_plugins.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Collection<PluginMetadata> pluginMetadatas = retrieveAndSortPluginMetadata();

    JsonWriter jsonWriter = response.newJsonWriter();
    jsonWriter.setSerializeEmptys(false);
    jsonWriter.beginObject();

    writeMetadataList(jsonWriter, pluginMetadatas);

    jsonWriter.endObject();
    jsonWriter.close();
  }

  private SortedSet<PluginMetadata> retrieveAndSortPluginMetadata() {
    return ImmutableSortedSet.copyOf(
      NAME_KEY_PLUGIN_METADATA_COMPARATOR,
      filter(pluginRepository.getMetadata(), NotCorePluginsPredicate.INSTANCE)
      );
  }

  private void writeMetadataList(JsonWriter jsonWriter, Collection<PluginMetadata> pluginMetadatas) {
      jsonWriter.name(ARRAY_PLUGINS);
      jsonWriter.beginArray();
      for (PluginMetadata pluginMetadata : pluginMetadatas) {
        writePluginMetadata(jsonWriter, pluginMetadata);
      }
      jsonWriter.endArray();
  }

  private void writePluginMetadata(JsonWriter jsonWriter, PluginMetadata pluginMetadata) {
    jsonWriter.beginObject();

    writeMetadata(jsonWriter, pluginMetadata);

    writeUrls(jsonWriter, pluginMetadata);

    writeArtifact(jsonWriter, pluginMetadata);

    jsonWriter.endObject();
  }

  private void writeMetadata(JsonWriter jsonWriter, PluginMetadata pluginMetadata) {
    jsonWriter.prop(PROPERTY_KEY, pluginMetadata.getKey());
    jsonWriter.prop(PROPERTY_NAME, pluginMetadata.getName());
    jsonWriter.prop(PROPERTY_DESCRIPTION, pluginMetadata.getDescription());
    jsonWriter.prop(PROPERTY_VERSION, pluginMetadata.getVersion());
    jsonWriter.prop(PROPERTY_LICENSE, pluginMetadata.getLicense());
    jsonWriter.prop(PROPERTY_ORGANIZATION_NAME, pluginMetadata.getOrganization());
    jsonWriter.prop(PROPERTY_ORGANIZATION_URL, pluginMetadata.getOrganizationUrl());
  }

  private void writeUrls(JsonWriter jsonWriter, PluginMetadata pluginMetadata) {
    jsonWriter.name(OBJECT_URLS);
    jsonWriter.beginObject();
    jsonWriter.prop(PROPERTY_HOMEPAGE, pluginMetadata.getHomepage());
    jsonWriter.prop(PROPERTY_ISSUE_TRACKER, pluginMetadata.getIssueTrackerUrl());
    jsonWriter.endObject();
  }

  private void writeArtifact(JsonWriter jsonWriter, PluginMetadata pluginMetadata) {
    if (pluginMetadata.getFile() == null) {
      return;
    }

    jsonWriter.name(OBJECT_ARTIFACT);
    jsonWriter.beginObject();
    jsonWriter.prop(PROPERTY_NAME, pluginMetadata.getFile().getName());
    jsonWriter.endObject();
  }

  private enum NotCorePluginsPredicate implements Predicate<PluginMetadata> {
    INSTANCE;

    @Override
    public boolean apply(@Nullable PluginMetadata input) {
      return input != null && !input.isCore();
    }
  }

  private enum PluginMetadataToName implements Function<PluginMetadata, String> {
    INSTANCE;

    @Override
    public String apply(@Nullable PluginMetadata input) {
      if (input == null) {
        return null;
      }
      return input.getName();
    }
  }

  private enum PluginMetadataToKey implements Function<PluginMetadata, String> {
    INSTANCE;

    @Override
    public String apply(@Nullable PluginMetadata input) {
      if (input == null) {
        return null;
      }
      return input.getKey();
    }
  }
}
