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

import com.google.common.collect.Maps;
import org.sonar.api.ServerSide;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.Testable;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.Map;

@ServerSide
public class CoverageService {

  public enum TYPE {
    UT, IT, OVERALL
  }

  private final MyBatis myBatis;
  private final MeasureDao measureDao;
  private final SnapshotPerspectives snapshotPerspectives;

  public CoverageService(MyBatis myBatis, MeasureDao measureDao, SnapshotPerspectives snapshotPerspectives) {
    this.myBatis = myBatis;
    this.measureDao = measureDao;
    this.snapshotPerspectives = snapshotPerspectives;
  }

  public void checkPermission(String fileKey) {
    UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);
  }

  public Map<Integer, Integer> getHits(String fileKey, CoverageService.TYPE type) {
    switch (type) {
      case IT:
        return findDataFromComponent(fileKey, CoreMetrics.IT_COVERAGE_LINE_HITS_DATA_KEY);
      case OVERALL:
        return findDataFromComponent(fileKey, CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA_KEY);
      default:
        return findDataFromComponent(fileKey, CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY);
    }
  }

  public Map<Integer, Integer> getConditions(String fileKey, CoverageService.TYPE type) {
    switch (type) {
      case IT:
        return findDataFromComponent(fileKey, CoreMetrics.IT_CONDITIONS_BY_LINE_KEY);
      case OVERALL:
        return findDataFromComponent(fileKey, CoreMetrics.OVERALL_CONDITIONS_BY_LINE_KEY);
      default:
        return findDataFromComponent(fileKey, CoreMetrics.CONDITIONS_BY_LINE_KEY);
    }
  }

  public Map<Integer, Integer> getCoveredConditions(String fileKey, CoverageService.TYPE type) {
    switch (type) {
      case IT:
        return findDataFromComponent(fileKey, CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE_KEY);
      case OVERALL:
        return findDataFromComponent(fileKey, CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE_KEY);
      default:
        return findDataFromComponent(fileKey, CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY);
    }
  }

  /**
   * Test cases is only return for unit tests
   */
  public Map<Integer, Integer> getTestCases(String fileKey, CoverageService.TYPE type) {
    if (TYPE.UT.equals(type)) {
      Testable testable = snapshotPerspectives.as(MutableTestable.class, fileKey);
      if (testable != null) {
        return testable.testCasesByLines();
      }
    }
    return Maps.newHashMap();
  }

  @CheckForNull
  private Map<Integer, Integer> findDataFromComponent(String fileKey, String metricKey) {
    DbSession session = myBatis.openSession(false);
    try {
      MeasureDto data = measureDao.findByComponentKeyAndMetricKey(session, fileKey, metricKey);
      String dataValue = data != null ? data.getData() : null;
      if (dataValue != null) {
        return KeyValueFormat.parseIntInt(dataValue);
      }
      return Maps.newHashMap();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
