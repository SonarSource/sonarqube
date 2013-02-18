/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.plugins;

import org.sonar.api.ServerComponent;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.api.platform.Server;
import org.sonar.updatecenter.common.PluginCenter;
import org.sonar.updatecenter.common.PluginManifest;
import org.sonar.updatecenter.common.PluginReferential;
import org.sonar.updatecenter.common.PluginReferentialManifestConverter;
import org.sonar.updatecenter.common.Version;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class InstalledPluginReferentialFactory implements ServerComponent {

  private final PluginDeployer pluginDeployer;
  private PluginRepository pluginRepository;
  private Version sonarVersion;

  public InstalledPluginReferentialFactory(PluginRepository pluginRepository, Server server, PluginDeployer pluginDeployer) {
    this.pluginRepository = pluginRepository;
    this.pluginDeployer = pluginDeployer;
    this.sonarVersion = Version.create(server.getVersion());
  }

  public PluginCenter getPluginCenter() {
    PluginReferential installedPluginReferential = getInstalledPluginReferential();
    return PluginCenter.createForInstalledPlugins(installedPluginReferential, sonarVersion)
        .setDate(new Date());
  }

  public void uninstall(String pluginKey) {
    for (String key : getPluginCenter().findRemovableReleases(pluginKey)) {
      pluginDeployer.uninstall(key);
    }
  }

  public PluginReferential getInstalledPluginReferential() {
    List<PluginManifest> pluginManifestList = getPluginManifestList();
    return PluginReferentialManifestConverter.fromPluginManifests(pluginManifestList, sonarVersion);
  }

  private List<PluginManifest> getPluginManifestList() {
    List<PluginManifest> pluginManifestList = newArrayList();
    for (PluginMetadata metadata : pluginRepository.getMetadata()) {
      if (!metadata.isCore()) {
        pluginManifestList.add(toPluginManifest(metadata));
      }
    }
    return pluginManifestList;
  }

  private PluginManifest toPluginManifest(PluginMetadata metadata) {
    PluginManifest pluginManifest = new PluginManifest();
    pluginManifest.setKey(metadata.getKey());
    pluginManifest.setName(metadata.getName());
    pluginManifest.setVersion(metadata.getVersion());
    pluginManifest.setDescription(metadata.getDescription());
    pluginManifest.setMainClass(metadata.getMainClass());
    pluginManifest.setOrganization(metadata.getOrganization());
    pluginManifest.setOrganizationUrl(metadata.getOrganizationUrl());
    pluginManifest.setLicense(metadata.getLicense());
    pluginManifest.setHomepage(metadata.getHomepage());
    pluginManifest.setBasePlugin(metadata.getBasePlugin());
    pluginManifest.setParent(metadata.getParent());
    pluginManifest.setRequiresPlugins(metadata.getRequiredPlugins());
    return pluginManifest;
  }

}
