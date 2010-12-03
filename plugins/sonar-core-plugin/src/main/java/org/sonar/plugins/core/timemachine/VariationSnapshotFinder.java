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

public class VariationSnapshotFinder implements BatchExtension {

  private PastSnapshotFinderByDays finderInDays;

  public VariationSnapshotFinder(PastSnapshotFinderByDays finderInDays) {
    this.finderInDays = finderInDays;
  }

  public VariationSnapshot find(Configuration conf, int index) {
    return find(index, conf.getString("sonar.timemachine.variation" + index));
  }
  
  public VariationSnapshot find(int index, String property) {
    if (StringUtils.isNotBlank(property)) {
      // todo manage non-integer values
      int days = Integer.parseInt(property);
      Snapshot projectSnapshot = finderInDays.findInDays(days);
      if (projectSnapshot != null) {
        return new VariationSnapshot(index, "days", projectSnapshot).setModeParameter(String.valueOf(days));
      }
    }
    return null;
  }
}
