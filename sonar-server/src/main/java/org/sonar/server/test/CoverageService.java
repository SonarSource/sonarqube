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

package org.sonar.server.test;

import org.sonar.api.ServerComponent;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.Testable;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.core.measure.db.MeasureDataDao;
import org.sonar.core.measure.db.MeasureDataDto;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.Map;

public class CoverageService implements ServerComponent {

  private final MeasureDataDao measureDataDao;
  private final SnapshotPerspectives snapshotPerspectives;

  public CoverageService(MeasureDataDao measureDataDao, SnapshotPerspectives snapshotPerspectives) {
    this.measureDataDao = measureDataDao;
    this.snapshotPerspectives = snapshotPerspectives;
  }

  public void checkPermission(String fileKey) {
    UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);
  }

  /**
   * Warning - does not check permission
   */
  @CheckForNull
  public String getHitsData(String fileKey) {
    return findDataFromComponent(fileKey, CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY);
  }

  /**
   * Warning - does not check permission
   */
  @CheckForNull
  public String getConditionsData(String fileKey) {
    return findDataFromComponent(fileKey, CoreMetrics.CONDITIONS_BY_LINE_KEY);
  }

  /**
   * Warning - does not check permission
   */
  @CheckForNull
  public String getCoveredConditionsData(String fileKey) {
    return findDataFromComponent(fileKey, CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY);
  }

  /**
   * Warning - does not check permission
   */
  @CheckForNull
  public Map<Integer, Integer> getTestCasesByLines(String fileKey) {
    Testable testable = snapshotPerspectives.as(MutableTestable.class, fileKey);
    if (testable != null) {
      return testable.testCasesByLines();
    }
    return null;
  }

  @CheckForNull
  private String findDataFromComponent(String fileKey, String metricKey) {
    MeasureDataDto data = measureDataDao.findByComponentKeyAndMetricKey(fileKey, metricKey);
    if (data != null) {
      return data.getData();
    }
    return null;
  }
}
