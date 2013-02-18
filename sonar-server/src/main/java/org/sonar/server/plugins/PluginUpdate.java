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

import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Version;

@Deprecated
public final class PluginUpdate {

  public enum Status {
    COMPATIBLE, INCOMPATIBLE, REQUIRE_SONAR_UPGRADE
  }

  private Status status = Status.INCOMPATIBLE;
  private Release release;

  public Status getStatus() {
    return status;
  }

  public boolean isCompatible() {
    return Status.COMPATIBLE.equals(status);
  }

  public boolean isIncompatible() {
    return Status.INCOMPATIBLE.equals(status);
  }

  public boolean requiresSonarUpgrade() {
    return Status.REQUIRE_SONAR_UPGRADE.equals(status);
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Plugin getPlugin() {
    return (Plugin)release.getArtifact();
  }

  public Release getRelease() {
    return release;
  }

  public void setRelease(Release release) {
    this.release = release;
  }

  public static PluginUpdate createWithStatus(Release pluginRelease, Status status) {
    PluginUpdate update = new PluginUpdate();
    update.setRelease(pluginRelease);
    update.setStatus(status);
    return update;
  }

  public static PluginUpdate createForPluginRelease(Release pluginRelease, Version sonarVersion) {
    PluginUpdate update = new PluginUpdate();
    update.setRelease(pluginRelease);

    if (pluginRelease.supportSonarVersion(sonarVersion)) {
      update.setStatus(Status.COMPATIBLE);

    } else {
      for (Version requiredSonarVersion : pluginRelease.getRequiredSonarVersions()) {
        if (requiredSonarVersion.compareTo(sonarVersion)>0) {
          update.setStatus(Status.REQUIRE_SONAR_UPGRADE);
          break;
        }
      }
    }
    return update;
  }
}
