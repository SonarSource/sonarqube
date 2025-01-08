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
package org.sonar.ce.task.projectanalysis.component;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.portfolio.PortfolioDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;

public class ComponentUuidFactoryImplIT {
  private final Branch mainBranch = new DefaultBranchImpl(DEFAULT_MAIN_BRANCH_NAME);
  private final Branch mockedBranch = mock(Branch.class);

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Test
  public void getOrCreateForKey_when_existingComponentsInDbForMainBranch_should_load() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    ComponentUuidFactory underTest = new ComponentUuidFactoryImpl(db.getDbClient(), db.getSession(), project.getKey(), mainBranch);

    assertThat(underTest.getOrCreateForKey(project.getKey())).isEqualTo(project.uuid());
  }

  @Test
  public void getOrCreateForKey_when_existingComponentsInDbForNonMainBranch_should_load() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("b1"));
    when(mockedBranch.getType()).thenReturn(BranchType.BRANCH);
    when(mockedBranch.isMain()).thenReturn(false);
    when(mockedBranch.getName()).thenReturn("b1");

    ComponentUuidFactory underTest = new ComponentUuidFactoryImpl(db.getDbClient(), db.getSession(), project.getKey(), mockedBranch);

    assertThat(underTest.getOrCreateForKey(project.getKey())).isEqualTo(branch.uuid());
  }

  @Test
  public void getOrCreateForKey_when_existingComponentsInDbForPr_should_load() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto pr = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setKey("pr1"));
    when(mockedBranch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(mockedBranch.isMain()).thenReturn(false);
    when(mockedBranch.getPullRequestKey()).thenReturn("pr1");

    ComponentUuidFactory underTest = new ComponentUuidFactoryImpl(db.getDbClient(), db.getSession(), project.getKey(), mockedBranch);

    assertThat(underTest.getOrCreateForKey(project.getKey())).isEqualTo(pr.uuid());
  }

  @Test
  public void getOrCreateForKey_when_componentsNotInDb_should_generate() {
    ComponentUuidFactory underTest = new ComponentUuidFactoryImpl(db.getDbClient(), db.getSession(), "theProjectKey", mainBranch);

    String generatedKey = underTest.getOrCreateForKey("foo");
    assertThat(generatedKey).isNotEmpty();

    // uuid is kept in memory for further calls with same key
    assertThat(underTest.getOrCreateForKey("foo")).isEqualTo(generatedKey);
  }

  @Test
  public void getOrCreateForKey_whenExistingComponentsInDbForPortfolioAndSubPortfolio_shouldLoadUuidsFromComponentTable() {
    ComponentDto portfolioDto = db.components().insertPublicPortfolio("pft1", p -> p.setKey("root_portfolio"));
    ComponentDto subView = db.components().insertSubView(portfolioDto, s -> s.setKey("sub_portfolio").setUuid("subPtf1"));
    ComponentUuidFactory underTest = new ComponentUuidFactoryImpl(db.getDbClient(), db.getSession(), portfolioDto.getKey(), mockedBranch);

    assertThat(underTest.getOrCreateForKey("root_portfolio")).isEqualTo(portfolioDto.uuid());
    assertThat(underTest.getOrCreateForKey("sub_portfolio")).isEqualTo(subView.uuid());
  }

  @Test
  public void getOrCreateForKey_whenNoExistingComponentsInDbForPortfolioAndSubPortfolio_shouldLoadUuidFromPortfolioTable() {
    PortfolioDto portfolioDto = ComponentTesting.newPortfolioDto("uuid_ptf1", "ptf1", "Portfolio1", null);
    db.getDbClient().portfolioDao().insertWithAudit(db.getSession(), portfolioDto);
    PortfolioDto subPortfolio = ComponentTesting.newPortfolioDto("subPtf1", "sub_ptf_1", "portfolio", portfolioDto);
    db.getDbClient().portfolioDao().insertWithAudit(db.getSession(), subPortfolio);

    ComponentUuidFactory underTest = new ComponentUuidFactoryImpl(db.getDbClient(), db.getSession(), portfolioDto.getKey());

    assertThat(underTest.getOrCreateForKey("ptf1")).isEqualTo(portfolioDto.getUuid());
    assertThat(underTest.getOrCreateForKey("sub_ptf_1")).isEqualTo(subPortfolio.getUuid());
  }

}
