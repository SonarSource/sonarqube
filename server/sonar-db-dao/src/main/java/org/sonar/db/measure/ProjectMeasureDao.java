/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.measure;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class ProjectMeasureDao implements Dao {

  private final UuidFactory uuidFactory;

  public ProjectMeasureDao(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  public Optional<ProjectMeasureDto> selectLastMeasure(DbSession dbSession, String componentUuid, String metricKey) {
    return Optional.ofNullable(mapper(dbSession).selectLastMeasure(componentUuid, metricKey));
  }

  public Optional<ProjectMeasureDto> selectMeasure(DbSession dbSession, String analysisUuid, String componentUuid, String metricKey) {
    return Optional.ofNullable(mapper(dbSession).selectMeasure(analysisUuid, componentUuid, metricKey));
  }

  /**
   * Select measures of:
   * - one component
   * - for a list of metrics
   * - with analysis from a date (inclusive) - optional
   * - with analysis to a date (exclusive) - optional
   *
   * If no constraints on dates, all the history is returned
   */
  public List<ProjectMeasureDto> selectPastMeasures(DbSession dbSession, PastMeasureQuery query) {
    return mapper(dbSession).selectPastMeasuresOnSeveralAnalyses(query);
  }

  public void insert(DbSession session, ProjectMeasureDto projectMeasureDto) {
    projectMeasureDto.setUuid(uuidFactory.create());
    mapper(session).insert(projectMeasureDto);
  }

  public void insert(DbSession session, Collection<ProjectMeasureDto> items) {
    for (ProjectMeasureDto item : items) {
      item.setUuid(uuidFactory.create());
      insert(session, item);
    }
  }

  public void insert(DbSession session, ProjectMeasureDto item, ProjectMeasureDto... others) {
    insert(session, Lists.asList(item, others));
  }

  private static ProjectMeasureMapper mapper(DbSession session) {
    return session.getMapper(ProjectMeasureMapper.class);
  }

}
