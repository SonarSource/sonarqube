/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.sonar.updatecenter.common.*;

import java.util.*;

public final class UpdateFinder {

  private UpdateCenter center;
  private Version installedSonarVersion;
  private Map<Plugin, Version> installedPlugins = new HashMap<Plugin, Version>();
  private List<String> pendingPluginFilenames = new ArrayList<String>();

  public UpdateFinder(UpdateCenter center, Version installedSonarVersion) {
    this.center = center;
    this.installedSonarVersion = installedSonarVersion;
  }

  public UpdateFinder(UpdateCenter center, String installedSonarVersion) {
    this(center, Version.create(installedSonarVersion));
  }

  public UpdateCenter getCenter() {
    return center;
  }

  public Version getInstalledSonarVersion() {
    return installedSonarVersion;
  }

  public UpdateFinder registerInstalledPlugin(String pluginKey, Version pluginVersion) {
    Plugin plugin = center.getPlugin(pluginKey);
    if (plugin != null) {
      installedPlugins.put(plugin, pluginVersion);
    }
    return this;
  }

  public UpdateFinder registerPendingPluginsByFilename(String filename) {
    pendingPluginFilenames.add(filename);
    return this;
  }

  public List<PluginUpdate> findAvailablePlugins() {
    List<PluginUpdate> availables = new ArrayList<PluginUpdate>();
    for (Plugin plugin : center.getPlugins()) {
      if (!installedPlugins.containsKey(plugin) && !isAlreadyDownloaded(plugin)) {
        Release release = plugin.getLastCompatibleRelease(installedSonarVersion);
        if (release != null) {
          availables.add(PluginUpdate.createWithStatus(release, PluginUpdate.Status.COMPATIBLE));

        } else {
          release = plugin.getLastCompatibleReleaseIfUpgrade(installedSonarVersion);
          if (release != null) {
            availables.add(PluginUpdate.createWithStatus(release, PluginUpdate.Status.REQUIRE_SONAR_UPGRADE));
          }
        }
      }
    }
    return availables;
  }

  private boolean isAlreadyDownloaded(Artifact artifact) {
    for (Release r : artifact.getReleases()) {
      if (pendingPluginFilenames.contains(r.getFilename())) {
        // already downloaded
        return true;
      }
    }
    return false;
  }

  public List<PluginUpdate> findPluginUpdates() {
    List<PluginUpdate> updates = new ArrayList<PluginUpdate>();
    for (Map.Entry<Plugin, Version> entry : installedPlugins.entrySet()) {
      Plugin plugin = entry.getKey();
      if (!isAlreadyDownloaded(plugin)) {
        Version pluginVersion = entry.getValue();
        for (Release release : plugin.getReleasesGreaterThan(pluginVersion)) {
          updates.add(PluginUpdate.createForPluginRelease(release, installedSonarVersion));
        }
      }
    }
    return updates;
  }

  public List<SonarUpdate> findSonarUpdates() {
    List<SonarUpdate> updates = new ArrayList<SonarUpdate>();
    SortedSet<Release> releases = center.getSonar().getReleasesGreaterThan(installedSonarVersion);
    for (Release release : releases) {
      updates.add(createSonarUpdate(release));
    }
    return updates;
  }

  SonarUpdate createSonarUpdate(Release sonarRelease) {
    SonarUpdate update = new SonarUpdate(sonarRelease);
    for (Map.Entry<Plugin, Version> entry : installedPlugins.entrySet()) {
      Plugin plugin = entry.getKey();
      Version pluginVersion = entry.getValue();
      Release pluginRelease = plugin.getRelease(pluginVersion);

      if (pluginRelease == null) {
        update.addIncompatiblePlugin(plugin);

      } else if (pluginRelease.supportSonarVersion(sonarRelease.getVersion())) {
        update.addCompatiblePlugin(plugin);

      } else {
        // search for a compatible plugin upgrade
        boolean ok = false;
        for (Release greaterPluginRelease : plugin.getReleasesGreaterThan(pluginVersion)) {
          if (greaterPluginRelease.supportSonarVersion(sonarRelease.getVersion())) {
            ok = true;
          }
        }
        if (ok) {
          update.addPluginToUpgrade(plugin);
        } else {
          update.addIncompatiblePlugin(plugin);
        }
      }
    }
    return update;
  }
}
