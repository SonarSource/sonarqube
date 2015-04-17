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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.updatecenter.common.Artifact;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.Release;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

public class PluginWSCommons {
  static final String PROPERTY_KEY = "key";
  static final String PROPERTY_NAME = "name";
  static final String PROPERTY_DESCRIPTION = "description";
  static final String PROPERTY_LICENSE = "license";
  static final String PROPERTY_VERSION = "version";
  static final String PROPERTY_CATEGORY = "category";
  static final String PROPERTY_ORGANIZATION_NAME = "organizationName";
  static final String PROPERTY_ORGANIZATION_URL = "organizationUrl";
  static final String PROPERTY_DATE = "date";
  static final String PROPERTY_STATUS = "status";
  static final String PROPERTY_HOMEPAGE = "homepage";
  static final String PROPERTY_ISSUE_TRACKER_URL = "issueTrackerUrl";
  static final String OBJECT_ARTIFACT = "artifact";
  static final String PROPERTY_URL = "url";
  static final String PROPERTY_TERMS_AND_CONDITIONS_URL = "termsAndConditionsUrl";
  static final String OBJECT_UPDATE = "update";
  static final String OBJECT_RELEASE = "release";
  static final String ARRAY_REQUIRES = "requires";

  public static final Ordering<PluginMetadata> NAME_KEY_PLUGIN_METADATA_COMPARATOR = Ordering.natural()
    .onResultOf(PluginMetadataToName.INSTANCE)
    .compound(Ordering.natural().onResultOf(PluginMetadataToKey.INSTANCE));

  public void writePluginMetadata(JsonWriter jsonWriter, PluginMetadata pluginMetadata) {
    jsonWriter.beginObject();

    writeMetadata(jsonWriter, pluginMetadata);

    writeArtifact(jsonWriter, pluginMetadata);

    jsonWriter.endObject();
  }

  public void writeMetadata(JsonWriter jsonWriter, PluginMetadata pluginMetadata) {
    jsonWriter.prop(PROPERTY_KEY, pluginMetadata.getKey());
    jsonWriter.prop(PROPERTY_NAME, pluginMetadata.getName());
    jsonWriter.prop(PROPERTY_DESCRIPTION, pluginMetadata.getDescription());
    jsonWriter.prop(PROPERTY_VERSION, pluginMetadata.getVersion());
    jsonWriter.prop(PROPERTY_LICENSE, pluginMetadata.getLicense());
    jsonWriter.prop(PROPERTY_ORGANIZATION_NAME, pluginMetadata.getOrganization());
    jsonWriter.prop(PROPERTY_ORGANIZATION_URL, pluginMetadata.getOrganizationUrl());
    jsonWriter.prop(PROPERTY_HOMEPAGE, pluginMetadata.getHomepage());
    jsonWriter.prop(PROPERTY_ISSUE_TRACKER_URL, pluginMetadata.getIssueTrackerUrl());
  }

  public void writeArtifact(JsonWriter jsonWriter, PluginMetadata pluginMetadata) {
    if (pluginMetadata.getFile() == null) {
      return;
    }

    jsonWriter.name(OBJECT_ARTIFACT);
    jsonWriter.beginObject();
    jsonWriter.prop(PROPERTY_NAME, pluginMetadata.getFile().getName());
    jsonWriter.endObject();
  }

  public void writePluginUpdate(JsonWriter jsonWriter, PluginUpdate pluginUpdate) {
    jsonWriter.beginObject();
    Plugin plugin = pluginUpdate.getPlugin();

    writeMetadata(jsonWriter, plugin);

    writeRelease(jsonWriter, pluginUpdate.getRelease());

    writeUpdate(jsonWriter, pluginUpdate);

    jsonWriter.endObject();
  }

  public void writeMetadata(JsonWriter jsonWriter, Plugin plugin) {
    jsonWriter.prop(PROPERTY_KEY, plugin.getKey());
    jsonWriter.prop(PROPERTY_NAME, plugin.getName());
    jsonWriter.prop(PROPERTY_CATEGORY, plugin.getCategory());
    jsonWriter.prop(PROPERTY_DESCRIPTION, plugin.getDescription());
    jsonWriter.prop(PROPERTY_LICENSE, plugin.getLicense());
    jsonWriter.prop(PROPERTY_TERMS_AND_CONDITIONS_URL, plugin.getTermsConditionsUrl());
    jsonWriter.prop(PROPERTY_ORGANIZATION_NAME, plugin.getOrganization());
    jsonWriter.prop(PROPERTY_ORGANIZATION_URL, plugin.getOrganizationUrl());
  }

  public void writeRelease(JsonWriter jsonWriter, Release release) {
    jsonWriter.name(OBJECT_RELEASE);
    jsonWriter.beginObject();
    jsonWriter.prop(PROPERTY_VERSION, release.getVersion().toString());
    jsonWriter.propDate(PROPERTY_DATE, release.getDate());

    writeArtifact(jsonWriter, release);

    jsonWriter.endObject();
  }

  public void writeArtifact(JsonWriter jsonWriter, Release release) {
    jsonWriter.name(OBJECT_ARTIFACT);
    jsonWriter.beginObject();
    jsonWriter.prop(PROPERTY_NAME, release.getFilename());
    jsonWriter.prop(PROPERTY_URL, release.getDownloadUrl());
    jsonWriter.endObject();
  }

  public void writeUpdate(JsonWriter jsonWriter, PluginUpdate pluginUpdate) {
    jsonWriter.name(OBJECT_UPDATE);
    jsonWriter.beginObject();
    jsonWriter.prop(PROPERTY_STATUS, toJSon(pluginUpdate.getStatus()));

    jsonWriter.name(ARRAY_REQUIRES);
    jsonWriter.beginArray();
    Release release = pluginUpdate.getRelease();
    for (Plugin child : filter(transform(release.getOutgoingDependencies(), ReleaseToArtifact.INSTANCE), Plugin.class)) {
      jsonWriter.beginObject();
      jsonWriter.prop(PROPERTY_KEY, child.getKey());
      jsonWriter.prop(PROPERTY_NAME, child.getName());
      jsonWriter.prop(PROPERTY_DESCRIPTION, child.getDescription());
      jsonWriter.endObject();
    }
    jsonWriter.endArray();

    jsonWriter.endObject();
  }

  @VisibleForTesting
  static String toJSon(PluginUpdate.Status status) {
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
    public Artifact apply(@Nullable Release input) {
      if (input == null) {
        return null;
      }
      return input.getArtifact();
    }
  }

  private enum PluginMetadataToName implements Function<PluginMetadata, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull PluginMetadata input) {
      return input.getName();
    }
  }

  private enum PluginMetadataToKey implements Function<PluginMetadata, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull PluginMetadata input) {
      return input.getKey();
    }
  }
}
