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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PastSnapshotFinder implements BatchExtension {

  public static final String LAST__ANALYSIS_MODE = "last_analysis";
  public static final String DATE_MODE = "date";
  public static final String VERSION_MODE = "version";
  public static final String DAYS_MODE = "days";
  
  private PastSnapshotFinderByDays finderByDays;
  private PastSnapshotFinderByVersion finderByVersion;
  private PastSnapshotFinderByDate finderByDate;
  private PastSnapshotFinderByLastAnalysis finderByLastAnalysis;

  public PastSnapshotFinder(PastSnapshotFinderByDays finderByDays, PastSnapshotFinderByVersion finderByVersion,
                            PastSnapshotFinderByDate finderByDate, PastSnapshotFinderByLastAnalysis finderByLastAnalysis) {
    this.finderByDays = finderByDays;
    this.finderByVersion = finderByVersion;
    this.finderByDate = finderByDate;
    this.finderByLastAnalysis = finderByLastAnalysis;
  }

  public PastSnapshot find(Configuration conf, int index) {
    return find(index, conf.getString("sonar.timemachine.variation" + index));
  }

  public PastSnapshot find(int index, String property) {
    if (StringUtils.isBlank(property)) {
      return null;
    }

    PastSnapshot result = findByDays(index, property);
    if (result == null) {
      result = findByDate(index, property);
      if (result == null) {
        result = findByLastAnalysis(index, property);
        if (result == null) {
          result = findByVersion(index, property);
        }
      }
    }
    return result;
  }

  private PastSnapshot findByLastAnalysis(int index, String property) {
    if (StringUtils.equals(LAST__ANALYSIS_MODE, property)) {
      Snapshot projectSnapshot = finderByLastAnalysis.findLastAnalysis();
      if (projectSnapshot != null) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String date = format.format(projectSnapshot.getCreatedAt());
        return new PastSnapshot(index, LAST__ANALYSIS_MODE, projectSnapshot).setModeParameter(date);
      }
    }
    return null;
  }

  private PastSnapshot findByDate(int index, String property) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    try {
      Date date = format.parse(property);
      Snapshot projectSnapshot = finderByDate.findByDate(date);
      if (projectSnapshot != null) {
        return new PastSnapshot(index, DATE_MODE, projectSnapshot).setModeParameter(property);
      }
      return null;

    } catch (ParseException e) {
      return null;
    }
  }

  private PastSnapshot findByVersion(int index, String property) {
    Snapshot projectSnapshot = finderByVersion.findVersion(property);
    if (projectSnapshot != null) {
      return new PastSnapshot(index, VERSION_MODE, projectSnapshot).setModeParameter(property);
    }
    return null;
  }

  private PastSnapshot findByDays(int index, String property) {
    try {
      int days = Integer.parseInt(property);
      Snapshot projectSnapshot = finderByDays.findInDays(days);
      if (projectSnapshot != null) {
        return new PastSnapshot(index, DAYS_MODE, projectSnapshot).setModeParameter(String.valueOf(days));
      }
      return null;

    } catch (NumberFormatException e) {
      return null;
    }
  }

}
