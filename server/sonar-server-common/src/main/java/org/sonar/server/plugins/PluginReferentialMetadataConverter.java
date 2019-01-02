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
package org.sonar.server.plugins;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.sonar.core.platform.PluginInfo;
import org.sonar.updatecenter.common.PluginManifest;
import org.sonar.updatecenter.common.PluginReferential;
import org.sonar.updatecenter.common.PluginReferentialManifestConverter;
import org.sonar.updatecenter.common.Version;

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class PluginReferentialMetadataConverter {

  private PluginReferentialMetadataConverter() {
    // Only static call
  }

  public static PluginReferential getInstalledPluginReferential(Collection<PluginInfo> infos) {
    List<PluginManifest> pluginManifestList = getPluginManifestList(infos);
    return PluginReferentialManifestConverter.fromPluginManifests(pluginManifestList);
  }

  private static List<PluginManifest> getPluginManifestList(Collection<PluginInfo> metadata) {
    List<PluginManifest> pluginManifestList = newArrayList();
    for (PluginInfo plugin : metadata) {
      pluginManifestList.add(toPluginManifest(plugin));
    }
    return pluginManifestList;
  }

  private static PluginManifest toPluginManifest(PluginInfo metadata) {
    PluginManifest pluginManifest = new PluginManifest();
    pluginManifest.setKey(metadata.getKey());
    pluginManifest.setName(metadata.getName());
    Version version = metadata.getVersion();
    if (version != null) {
      pluginManifest.setVersion(version.getName());
    }
    pluginManifest.setDescription(metadata.getDescription());
    pluginManifest.setMainClass(metadata.getMainClass());
    pluginManifest.setOrganization(metadata.getOrganizationName());
    pluginManifest.setOrganizationUrl(metadata.getOrganizationUrl());
    pluginManifest.setLicense(metadata.getLicense());
    pluginManifest.setHomepage(metadata.getHomepageUrl());
    pluginManifest.setIssueTrackerUrl(metadata.getIssueTrackerUrl());
    pluginManifest.setBasePlugin(metadata.getBasePlugin());
    pluginManifest.setRequirePlugins(Collections2.transform(metadata.getRequiredPlugins(), RequiredPluginToString.INSTANCE).toArray(
      new String[metadata.getRequiredPlugins().size()]));
    return pluginManifest;
  }

  private enum RequiredPluginToString implements Function<PluginInfo.RequiredPlugin, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull PluginInfo.RequiredPlugin requiredPlugin) {
      return requiredPlugin.toString();
    }
  }
}
