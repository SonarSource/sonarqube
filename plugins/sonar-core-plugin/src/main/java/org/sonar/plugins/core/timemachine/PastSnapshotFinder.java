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
package org.sonar.plugins.core.timemachine;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.database.model.Snapshot;

public class PastSnapshotFinder implements BatchExtension {

  private PastSnapshotFinderByDays finderByDays;
  private PastSnapshotFinderByVersion finderByVersion;

  public PastSnapshotFinder(PastSnapshotFinderByDays finderByDays, PastSnapshotFinderByVersion finderByVersion) {
    this.finderByDays = finderByDays;
    this.finderByVersion = finderByVersion;
  }

  public PastSnapshot find(Configuration conf, int index) {
    return find(index, conf.getString("sonar.timemachine.variation" + index));
  }

  public PastSnapshot find(int index, String property) {
    if (StringUtils.isBlank(property)) {
      return null;
    }

    PastSnapshot result = null;
    Integer days = getValueInDays(property);
    if (days != null) {
      result = findSnapshotInDays(index, days);
    } else {
      String version = getValueVersion(property);
      if (StringUtils.isNotBlank(version)) {
        result = findSnapshotByVersion(index, version);
      }
    }

    return result;
  }

  private PastSnapshot findSnapshotByVersion(int index, String version) {
    Snapshot projectSnapshot = finderByVersion.findVersion(version);
    if (projectSnapshot != null) {
      return new PastSnapshot(index, "version", projectSnapshot).setConfigurationModeParameter(version);
    }
    return null;
  }

  private String getValueVersion(String property) {
    // todo check if it's a version with a regexp
    return null;
  }

  private PastSnapshot findSnapshotInDays(int index, Integer days) {
    Snapshot projectSnapshot = finderByDays.findInDays(days);
    if (projectSnapshot != null) {
      return new PastSnapshot(index, "days", projectSnapshot).setConfigurationModeParameter(String.valueOf(days));
    }
    return null;
  }

  private Integer getValueInDays(String property) {
    try {
      return Integer.parseInt(property);

    } catch (NumberFormatException e) {
      return null;
    }
  }
}
