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

package org.sonar.server.computation.db;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.utils.System2;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.computation.db.AnalysisReportMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;

import java.util.Date;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class AnalysisReportDao extends BaseDao<AnalysisReportMapper, AnalysisReportDto, String> implements DaoComponent {

  public AnalysisReportDao() {
    this(System2.INSTANCE);
  }

  @VisibleForTesting
  public AnalysisReportDao(System2 system) {
    super(AnalysisReportMapper.class, system);
  }

  @Override
  protected AnalysisReportDto doGetNullableByKey(DbSession session, String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected AnalysisReportDto doUpdate(DbSession session, AnalysisReportDto issue) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected AnalysisReportDto doInsert(DbSession session, AnalysisReportDto report) {
    checkNotNull(report.getProjectKey(), "Cannot insert Report with no project key!");
    mapper(session).insert(report);
    return report;
  }

  @Override
  protected String getSynchronizationStatementName() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Map getSynchronizationParams(Date date, Map<String, String> params) {
    throw new UnsupportedOperationException();
  }
}
