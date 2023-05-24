/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.entity;

import java.util.LinkedList;
import java.util.List;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.portfolio.PortfolioDto;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class EntityDaoIT {
  private final System2 system2 = new AlwaysIncreasingSystem2(1000L);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final EntityDao entityDao = new EntityDao();

  @Test
  public void selectEntitiesByKeys_shouldReturnAllEntities() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    assertThat(entityDao.selectByKeys(db.getSession(), List.of(application.projectKey(), project.projectKey(), portfolio.getKey(), "unknown")))
      .extracting(EntityDto::getUuid)
      .containsOnly(application.projectUuid(), project.projectUuid(), portfolio.getUuid());
  }

  @Test
  public void selectEntitiesByKeys_whenEmptyInput_shouldReturnEmptyList() {
    assertThat(entityDao.selectByKeys(db.getSession(), emptyList())).isEmpty();
  }

  @Test
  public void selectEntitiesByUuids_shouldReturnAllEntities() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    assertThat(entityDao.selectByUuids(db.getSession(), List.of(application.projectUuid(), project.projectUuid(), portfolio.getUuid(), "unknown")))
      .extracting(EntityDto::getKey)
      .containsOnly(application.projectKey(), project.projectKey(), portfolio.getKey());
  }

  @Test
  public void selectEntitiesByUuids_whenEmptyInput_shouldReturnEmptyList() {
    assertThat(entityDao.selectByUuids(db.getSession(), emptyList())).isEmpty();
  }

  @Test
  public void selectEntityByUuid_shouldReturnAllEntities() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    assertThat(entityDao.selectByUuid(db.getSession(), application.projectUuid()).get().getKey()).isEqualTo(application.projectKey());
    assertThat(entityDao.selectByUuid(db.getSession(), project.projectUuid()).get().getKey()).isEqualTo(project.projectKey());
    assertThat(entityDao.selectByUuid(db.getSession(), portfolio.getUuid()).get().getKey()).isEqualTo(portfolio.getKey());
  }

  @Test
  public void selectEntityByUuid_whenNoMatch_shouldReturnEmpty() {
    assertThat(entityDao.selectByUuid(db.getSession(), "unknown")).isEmpty();
  }

  @Test
  public void selectEntityByKey_shouldReturnAllEntities() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    assertThat(entityDao.selectByKey(db.getSession(), application.projectKey()).get().getUuid()).isEqualTo(application.projectUuid());
    assertThat(entityDao.selectByKey(db.getSession(), project.projectKey()).get().getUuid()).isEqualTo(project.projectUuid());
    assertThat(entityDao.selectByKey(db.getSession(), portfolio.getKey()).get().getUuid()).isEqualTo(portfolio.getUuid());
  }

  @Test
  public void selectEntityByKey_whenNoMatch_shouldReturnEmpty() {
    assertThat(entityDao.selectByKey(db.getSession(), "unknown")).isEmpty();
  }

  @Test
  public void scrollEntitiesForIndexing_shouldReturnAllEntities() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    List<EntityDto> result = new LinkedList<>();
    ResultHandler<EntityDto> handler = resultContext -> result.add(resultContext.getResultObject());
    entityDao.scrollForIndexing(db.getSession(), null, handler);

    assertThat(result).extracting(EntityDto::getUuid)
      .containsOnly(project.projectUuid(), application.projectUuid(), portfolio.getUuid());
  }

  @Test
  public void scrollEntitiesForIndexing_whenEntityUuidSpecified_shouldReturnSpecificEntity() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    List<EntityDto> result = new LinkedList<>();
    ResultHandler<EntityDto> handler = resultContext -> result.add(resultContext.getResultObject());
    entityDao.scrollForIndexing(db.getSession(), project.projectUuid(), handler);

    assertThat(result).extracting(EntityDto::getUuid)
      .containsOnly(project.projectUuid());
  }

  @Test
  public void scrollEntitiesForIndexing_whenNonExistingUuidSpecified_shouldReturnEmpty() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    List<EntityDto> result = new LinkedList<>();
    ResultHandler<EntityDto> handler = resultContext -> result.add(resultContext.getResultObject());
    entityDao.scrollForIndexing(db.getSession(), "unknown", handler);

    assertThat(result).isEmpty();
  }
}
