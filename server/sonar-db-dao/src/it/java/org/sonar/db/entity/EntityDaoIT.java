/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class EntityDaoIT {
  private final System2 system2 = new AlwaysIncreasingSystem2(1000L);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final EntityDao entityDao = new EntityDao();

  @Test
  void selectEntityByComponentUuid_shouldReturnProjectEntityBasedOnComponent() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    BranchDto branchDto = db.components().insertProjectBranch(project);
    ComponentDto fileInBranch = db.components().insertFile(branchDto);

    assertThat(entityDao.selectByComponentUuid(db.getSession(), fileInBranch.uuid()).get())
      .extracting(EntityDto::getUuid, EntityDto::getKey)
      .containsOnly(project.getUuid(), project.getKey());
  }

  @Test
  void selectEntityByComponentUuid_shouldReturnPortfolioEntityBasedOnComponent() {
    PortfolioDto portfolio = db.components().insertPublicPortfolioDto();
    assertThat(entityDao.selectByComponentUuid(db.getSession(), portfolio.getUuid()).get())
      .extracting(EntityDto::getUuid, EntityDto::getKey)
      .containsOnly(portfolio.getUuid(), portfolio.getKey());
  }

  @Test
  void selectEntityByComponentUuid_whenPortfolioWithHierarchy_shouldReturnPortfolioEntityBasedOnComponent() {
    ComponentDto projectBranch = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto portfolio = db.components().insertPublicPortfolio();
    ComponentDto subPortfolio = db.components().insertSubportfolio(portfolio);
    ComponentDto projectCopy = db.components().insertComponent(ComponentTesting.newProjectCopy(projectBranch, subPortfolio));

    assertThat(entityDao.selectByComponentUuid(db.getSession(), projectCopy.uuid()).get())
      .extracting(EntityDto::getUuid, EntityDto::getKey)
      .containsOnly(portfolio.uuid(), portfolio.getKey());
  }

  @Test
  void selectEntityByComponentUuid_whenUnknown_shouldReturnEmpty() {
    assertThat(entityDao.selectByComponentUuid(db.getSession(), "unknown")).isEmpty();
  }

  @Test
  void selectEntitiesByKeys_shouldReturnAllEntities() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    assertThat(entityDao.selectByKeys(db.getSession(), List.of(application.projectKey(), project.projectKey(), portfolio.getKey(),
      "unknown")))
      .extracting(EntityDto::getUuid)
      .containsOnly(application.projectUuid(), project.projectUuid(), portfolio.getUuid());
  }

  @Test
  void selectEntitiesByKeys_whenEmptyInput_shouldReturnEmptyList() {
    assertThat(entityDao.selectByKeys(db.getSession(), emptyList())).isEmpty();
  }

  @Test
  void selectEntitiesByUuids_shouldReturnAllEntities() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    assertThat(entityDao.selectByUuids(db.getSession(), List.of(application.projectUuid(), project.projectUuid(), portfolio.getUuid(),
      "unknown")))
      .extracting(EntityDto::getKey)
      .containsOnly(application.projectKey(), project.projectKey(), portfolio.getKey());
  }

  @Test
  void selectEntitiesByUuids_whenEmptyInput_shouldReturnEmptyList() {
    assertThat(entityDao.selectByUuids(db.getSession(), emptyList())).isEmpty();
  }

  @Test
  void selectEntityByUuid_shouldReturnAllEntities() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    assertThat(entityDao.selectByUuid(db.getSession(), application.projectUuid()).get().getKey()).isEqualTo(application.projectKey());
    assertThat(entityDao.selectByUuid(db.getSession(), project.projectUuid()).get().getKey()).isEqualTo(project.projectKey());
    assertThat(entityDao.selectByUuid(db.getSession(), portfolio.getUuid()).get().getKey()).isEqualTo(portfolio.getKey());
  }

  @Test
  void getDescription_shouldNotReturnNull() {
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    assertThat(entityDao.selectByUuid(db.getSession(), project.projectUuid()).get().getDescription()).isNotNull();
    assertThat(entityDao.selectByUuid(db.getSession(), portfolio.getUuid()).get().getDescription()).isNotNull();
  }

  @Test
  void selectEntityByUuid_whenNoMatch_shouldReturnEmpty() {
    assertThat(entityDao.selectByUuid(db.getSession(), "unknown")).isEmpty();
  }

  @Test
  void selectEntityByKey_shouldReturnAllEntities() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    assertThat(entityDao.selectByKey(db.getSession(), application.projectKey()).get().getUuid()).isEqualTo(application.projectUuid());
    assertThat(entityDao.selectByKey(db.getSession(), project.projectKey()).get().getUuid()).isEqualTo(project.projectUuid());
    assertThat(entityDao.selectByKey(db.getSession(), portfolio.getKey()).get().getUuid()).isEqualTo(portfolio.getUuid());
  }

  @Test
  void selectEntityByKey_whenNoMatch_shouldReturnEmpty() {
    assertThat(entityDao.selectByKey(db.getSession(), "unknown")).isEmpty();
  }

  @Test
  void scrollEntitiesForIndexing_shouldReturnAllEntities() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();

    List<EntityDto> result = new LinkedList<>();
    ResultHandler<EntityDto> handler = resultContext -> result.add(resultContext.getResultObject());
    entityDao.scrollForIndexing(db.getSession(), handler);

    assertThat(result).extracting(EntityDto::getUuid)
      .containsOnly(project.projectUuid(), application.projectUuid(), portfolio.getUuid());
  }
}
