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
package org.sonar.db.portfolio;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.KeyWithUuidDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ApplicationProjectDto;
import org.sonar.db.project.ProjectDto;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.db.portfolio.PortfolioDto.SelectionMode.MANUAL;
import static org.sonar.db.portfolio.PortfolioDto.SelectionMode.REGEXP;
import static org.sonar.db.portfolio.PortfolioDto.SelectionMode.REST;
import static org.sonar.db.portfolio.PortfolioDto.SelectionMode.TAGS;

class PortfolioDaoIT {
  private final System2 system2 = new AlwaysIncreasingSystem2(1L, 1);
  private final SequenceUuidFactory uuidFactory = new SequenceUuidFactory();

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);
  private final AuditPersister audit = mock(AuditPersister.class);
  private final PortfolioDao portfolioDao = new PortfolioDao(system2, UuidFactoryFast.getInstance(), audit);
  private final DbSession session = db.getSession();

  @Test
  void selectAllRoots() {
    PortfolioDto p1 = db.components().insertPrivatePortfolioDto("p1");
    PortfolioDto p11 = addPortfolio(p1, "p11");
    PortfolioDto p111 = addPortfolio(p11, "p111");
    PortfolioDto p2 = db.components().insertPrivatePortfolioDto("p2");

    assertThat(portfolioDao.selectAllRoots(session)).extracting("uuid")
      .containsExactlyInAnyOrder("p1", "p2");
  }

  @Test
  void selectAll() {
    PortfolioDto p1 = db.components().insertPrivatePortfolioDto("p1");
    PortfolioDto p11 = addPortfolio(p1, "p11");
    PortfolioDto p111 = addPortfolio(p11, "p111");
    PortfolioDto p2 = db.components().insertPrivatePortfolioDto("p2");

    assertThat(portfolioDao.selectAll(session)).extracting("uuid")
      .containsExactlyInAnyOrder("p1", "p2", "p11", "p111");
  }

  @Test
  void selectTree() {
    PortfolioDto p1 = db.components().insertPrivatePortfolioDto("p1");
    PortfolioDto p11 = addPortfolio(p1, "p11");
    PortfolioDto p111 = addPortfolio(p11, "p111");
    PortfolioDto p12 = addPortfolio(p1, "p12");
    PortfolioDto p2 = db.components().insertPrivatePortfolioDto("p2");

    assertThat(portfolioDao.selectTree(session, "p1")).extracting("uuid").containsOnly("p1", "p11", "p111", "p12");
    assertThat(portfolioDao.selectTree(session, "p11")).extracting("uuid").containsOnly("p1", "p11", "p111", "p12");
    assertThat(portfolioDao.selectTree(session, "p111")).extracting("uuid").containsOnly("p1", "p11", "p111", "p12");
    assertThat(portfolioDao.selectTree(session, "p2")).extracting("uuid").containsOnly("p2");
  }

  @Test
  void selectByKey_returns_empty_if_no_match() {
    assertThat(portfolioDao.selectByKey(session, "nonexisting")).isEmpty();
  }

  @Test
  void selectByKey_returns_match() {
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();
    assertThat(portfolioDao.selectByKey(session, portfolio.getKey())).isNotEmpty();
  }

  @Test
  void selectByUuid_returns_empty_if_no_match() {
    assertThat(portfolioDao.selectByUuid(session, "nonexisting")).isEmpty();
  }

  @Test
  void selectByUuid_returns_match() {
    db.components().insertPrivatePortfolioDto("name");
    assertThat(portfolioDao.selectByUuid(session, "name")).isNotEmpty();
    assertThat(portfolioDao.selectByUuid(session, "name").get())
      .extracting("name", "key", "uuid", "description", "rootUuid", "parentUuid", "selectionMode", "selectionExpression")
      .containsExactly("NAME_name", "KEY_name", "name", "DESCRIPTION_name", "name", null, "NONE", null);
  }

  @Test
  void selectByUuids_returns_empty_if_no_match() {
    db.components().insertPrivatePortfolioDto("name1");
    assertThat(portfolioDao.selectByUuids(session, Set.of("name2"))).isEmpty();
  }

  @Test
  void selectByUuids_returns_empty_if_uuids_is_empty() {
    db.components().insertPrivatePortfolioDto("name1");
    assertThat(portfolioDao.selectByUuids(session, emptySet())).isEmpty();
  }

  @Test
  void selectByUuids_returns_matches() {
    db.components().insertPrivatePortfolioDto("name1");
    db.components().insertPrivatePortfolioDto("name2");
    db.components().insertPrivatePortfolioDto("name3");

    assertThat(portfolioDao.selectByUuids(session, Set.of("name1", "name2")))
      .extracting("name", "key", "uuid", "description", "rootUuid", "parentUuid", "selectionMode", "selectionExpression")
      .containsOnly(
        tuple("NAME_name1", "KEY_name1", "name1", "DESCRIPTION_name1", "name1", null, "NONE", null),
        tuple("NAME_name2", "KEY_name2", "name2", "DESCRIPTION_name2", "name2", null, "NONE", null));
  }

  @Test
  void selectUuidsByKeyFromPortfolioKey_returns_all_uuidByKeyForPortfoliosLinkedToRootKey() {
    PortfolioDto portfolio1 = ComponentTesting.newPortfolioDto("uuid1", "ptf1", "Portfolio 1", null);
    PortfolioDto subPortfolio1 = ComponentTesting.newPortfolioDto("sub_uuid11", "sub_ptf1", "SubPortfolio 1", portfolio1);
    PortfolioDto subSubPortfolio1 = ComponentTesting.newPortfolioDto("sub_uuid12", "sub_sub_ptf1", "SubSubPortfolio 1", portfolio1);
    PortfolioDto portfolio2 = ComponentTesting.newPortfolioDto("uuid2", "ptf2", "Portfolio 2", null);
    PortfolioDto subPortfolio2 = ComponentTesting.newPortfolioDto("sub_uuid21", "sub_ptd2", "SubPortfolio 2", portfolio2);
    Arrays.asList(portfolio1, subPortfolio1, subSubPortfolio1, portfolio2, subPortfolio2)
      .forEach(portfolio -> portfolioDao.insertWithAudit(db.getSession(), portfolio));

    List<KeyWithUuidDto> keyWithUuidDtos = portfolioDao.selectUuidsByKey(db.getSession(), portfolio1.getKey());

    KeyWithUuidDto[] expectedKey = {
      new KeyWithUuidDto(portfolio1.getKee(), portfolio1.getUuid()),
      new KeyWithUuidDto(subPortfolio1.getKee(), subPortfolio1.getUuid()),
      new KeyWithUuidDto(subSubPortfolio1.getKee(), subSubPortfolio1.getUuid())
    };

    assertThat(keyWithUuidDtos).containsExactlyInAnyOrder(expectedKey);
  }

  @Test
  void insert_fails_if_root_is_inconsistent_with_parent() {
    PortfolioDto portfolio = new PortfolioDto()
      .setUuid("uuid")
      .setParentUuid(null)
      .setRootUuid("root");
    assertThatThrownBy(() -> portfolioDao.insertWithAudit(session, portfolio))
      .isInstanceOf(IllegalArgumentException.class);

    PortfolioDto portfolio2 = new PortfolioDto()
      .setUuid("uuid")
      .setParentUuid("parent")
      .setRootUuid("uuid");
    assertThatThrownBy(() -> portfolioDao.insertWithAudit(session, portfolio2))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void delete() {
    ProjectDto proj1 = db.components().insertPrivateProject("proj1").getProjectDto();
    ProjectDto app1 = db.components().insertPrivateApplication().getProjectDto();

    PortfolioDto p1 = db.components().insertPrivatePortfolioDto("p1");
    PortfolioDto p2 = db.components().insertPrivatePortfolioDto("p2");
    PortfolioDto p3 = db.components().insertPrivatePortfolioDto("p3");
    PortfolioDto p4 = db.components().insertPrivatePortfolioDto("p4");

    db.components().addPortfolioProject(p1, proj1);
    db.components().addPortfolioProject(p2, proj1);
    db.components().addPortfolioProjectBranch(p1, proj1, "branch1");
    db.components().addPortfolioProjectBranch(p2, proj1, "branch2");

    portfolioDao.addReferenceBranch(session, "p1", "app1", "main1");
    portfolioDao.addReferenceBranch(session, "p2", "app1", "main2");
    portfolioDao.addReference(session, "p4", "p1");

    portfolioDao.delete(session, p1);
    portfolioDao.delete(session, p3);

    assertThat(db.select(session, "select branch_uuid from portfolio_proj_branches")).extracting(m -> m.values().iterator().next())
      .containsOnly("branch2");
    assertThat(db.select(session, "select uuid from portfolios")).extracting(m -> m.values().iterator().next())
      .containsOnly("p2", "p4");
    assertThat(db.select(session, "select portfolio_uuid from portfolio_references")).extracting(m -> m.values().iterator().next())
      .containsOnly("p2");
    assertThat(db.select(session, "select portfolio_uuid from portfolio_projects")).extracting(m -> m.values().iterator().next())
      .containsOnly("p2");

    verify(audit, times(2)).deleteComponent(any(), any());
  }

  @Test
  void deleteAllDescendantPortfolios() {
    PortfolioDto root = db.components().insertPrivatePortfolioDto();
    PortfolioDto child1 = addPortfolio(root);
    PortfolioDto child11 = addPortfolio(child1);
    PortfolioDto root2 = db.components().insertPrivatePortfolioDto();

    portfolioDao.deleteAllDescendantPortfolios(session, root.getUuid());
    portfolioDao.deleteAllDescendantPortfolios(session, root2.getUuid());

    assertThat(db.countSql(session, "select count(*) from portfolios where parent_uuid is not null")).isZero();
  }

  @Test
  void update_portfolio() {
    db.components().insertPrivatePortfolioDto("name");
    PortfolioDto dto = portfolioDao.selectByUuid(session, "name").get();
    dto.setSelectionMode("newMode");
    dto.setSelectionExpression("newExp");
    dto.setDescription("newDesc");
    dto.setName("newName");
    dto.setRootUuid("root");
    dto.setParentUuid("parent");
    portfolioDao.update(session, dto);

    assertThat(portfolioDao.selectByUuid(session, "name").get())
      .extracting("name", "key", "uuid", "description", "private", "rootUuid", "parentUuid", "selectionMode", "selectionExpression")
      .containsExactly("newName", "KEY_name", "name", "newDesc", true, "root", "parent", "newMode", "newExp");
    verify(audit).updateComponent(any(), any());
  }

  @Test
  void selectAllReferencesToPortfolios() {
    db.components().insertPrivatePortfolioDto("portfolio1");
    db.components().insertPrivatePortfolioDto("portfolio2");
    db.components().insertPrivatePortfolioDto("portfolio3");
    ProjectDto app1 = db.components().insertPrivateApplication(p -> p.setKey("app1")).getProjectDto();

    portfolioDao.addReference(session, "portfolio1", "portfolio2");
    portfolioDao.addReference(session, "portfolio2", "portfolio3");
    portfolioDao.addReferenceBranch(session, "portfolio3", "app1", "main1");

    assertThat(portfolioDao.selectAllReferencesToPortfolios(session))
      .extracting(ReferenceDto::getSourceUuid, ReferenceDto::getTargetUuid, ReferenceDto::getBranchUuids)
      .containsOnly(tuple("portfolio1", "portfolio2", emptySet()), tuple("portfolio2", "portfolio3", emptySet()));
  }

  @Test
  void selectAllReferencesToApplications() {
    db.components().insertPrivatePortfolioDto("portfolio1");
    db.components().insertPrivatePortfolioDto("portfolio2");
    db.components().insertPrivatePortfolioDto("portfolio3");
    ProjectData appData1 = db.components().insertPrivateApplication(p -> p.setKey("app1"));
    ProjectDto app1 = appData1.getProjectDto();

    portfolioDao.addReference(session, "portfolio1", "portfolio2");
    portfolioDao.addReference(session, "portfolio2", "portfolio3");
    portfolioDao.addReferenceBranch(session, "portfolio3", app1.getUuid(), "branch1");
    portfolioDao.addReferenceBranch(session, "portfolio2", app1.getUuid(), appData1.getMainBranchDto().getUuid());

    assertThat(portfolioDao.selectAllReferencesToApplications(session))
      .extracting(ReferenceDto::getSourceUuid, ReferenceDto::getTargetUuid, ReferenceDto::getTargetRootUuid, ReferenceDto::getBranchUuids)
      .containsOnly(
        tuple("portfolio3", app1.getUuid(), app1.getUuid(), singleton("branch1")),
        tuple("portfolio2", app1.getUuid(), app1.getUuid(), singleton(appData1.getMainBranchDto().getUuid())));
  }

  @Test
  void selectAllDirectChildApplications() {
    var p1 = db.components().insertPrivatePortfolioDto("portfolio1");
    var p2 = db.components().insertPrivatePortfolioDto("portfolio2", p -> p.setRootUuid(p1.getUuid()).setParentUuid(p1.getUuid()));
    var p3 = db.components().insertPrivatePortfolioDto("portfolio3", p -> p.setRootUuid(p1.getUuid()).setParentUuid(p1.getUuid()));
    ProjectData appData1 = db.components().insertPrivateApplication(p -> p.setKey("app1"));
    ProjectDto app1 = appData1.getProjectDto();
    ProjectData appData2 = db.components().insertPrivateApplication(p -> p.setKey("app2"));
    ProjectDto app2 = appData2.getProjectDto();
    ProjectData appData3 = db.components().insertPrivateApplication(p -> p.setKey("app3"));
    ProjectDto app3 = appData3.getProjectDto();

    portfolioDao.addReferenceBranch(session, "portfolio1", app1.getUuid(), appData1.getMainBranchDto().getUuid());
    portfolioDao.addReferenceBranch(session, "portfolio2", app2.getUuid(), appData2.getMainBranchDto().getUuid());
    portfolioDao.addReferenceBranch(session, "portfolio3", app3.getUuid(), appData3.getMainBranchDto().getUuid());

    assertThat(portfolioDao.selectApplicationReferenceUuids(session, p1.getUuid()))
      .containsOnly(app1.getUuid());

    assertThat(portfolioDao.selectApplicationReferenceUuids(session, p2.getUuid()))
      .containsOnly(app2.getUuid());

    assertThat(portfolioDao.selectApplicationReferenceUuids(session, p3.getUuid()))
      .containsOnly(app3.getUuid());
  }

  @Test
  void selectAllReferencesToApplicationsInHierarchy() {
    var p1 = db.components().insertPrivatePortfolioDto("portfolio1");
    var p2 = db.components().insertPrivatePortfolioDto("portfolio2", p -> p.setRootUuid(p1.getUuid()).setParentUuid(p1.getUuid()));
    var p3 = db.components().insertPrivatePortfolioDto("portfolio3", p -> p.setRootUuid(p1.getUuid()).setParentUuid(p1.getUuid()));
    ProjectData appData1 = db.components().insertPrivateApplication(p -> p.setKey("app1"));
    ProjectDto app1 = appData1.getProjectDto();
    ProjectData appData2 = db.components().insertPrivateApplication(p -> p.setKey("app2"));
    ProjectDto app2 = appData2.getProjectDto();
    ProjectData appData3 = db.components().insertPrivateApplication(p -> p.setKey("app3"));
    ProjectDto app3 = appData3.getProjectDto();

    portfolioDao.addReferenceBranch(session, "portfolio1", app1.getUuid(), appData1.getMainBranchDto().getUuid());
    portfolioDao.addReferenceBranch(session, "portfolio2", app2.getUuid(), appData2.getMainBranchDto().getUuid());
    portfolioDao.addReferenceBranch(session, "portfolio3", app3.getUuid(), appData3.getMainBranchDto().getUuid());

    assertThat(portfolioDao.selectAllReferencesToApplicationsInHierarchy(session, p1.getUuid()))
      .extracting(ReferenceDto::getTargetUuid)
      .containsExactlyInAnyOrder(app1.getUuid(), app2.getUuid(), app3.getUuid());
  }

  @Test
  void selectAllReferencesToPortfoliosInHierarchy() {
    var p1 = db.components().insertPrivatePortfolioDto("portfolio1");
    var p2 = db.components().insertPrivatePortfolioDto("portfolio2", p -> p.setRootUuid(p1.getUuid()).setParentUuid(p1.getUuid()));
    var p3 = db.components().insertPrivatePortfolioDto("portfolio3", p -> p.setRootUuid(p1.getUuid()).setParentUuid(p1.getUuid()));
    var p4 = db.components().insertPrivatePortfolioDto("portfolio4");
    var p5 = db.components().insertPrivatePortfolioDto("portfolio5");
    var p6 = db.components().insertPrivatePortfolioDto("portfolio6");

    portfolioDao.addReference(session, "portfolio1", p4.getUuid());
    portfolioDao.addReference(session, "portfolio2", p5.getUuid());
    portfolioDao.addReference(session, "portfolio3", p6.getUuid());

    assertThat(portfolioDao.selectAllReferencesToPortfoliosInHierarchy(session, p1.getUuid()))
      .extracting(ReferenceDto::getTargetUuid)
      .containsExactlyInAnyOrder(p4.getUuid(), p5.getUuid(), p6.getUuid());
  }

  @Test
  void selectAllApplicationProjectsBelongToTheSamePortfolio() {
    var portfolio = db.components().insertPrivatePortfolioDto("portfolio1");
    ProjectData appData1 = db.components().insertPrivateApplication(p -> p.setKey("app1"));
    var app1 = appData1.getProjectDto();
    ProjectData appData2 = db.components().insertPrivateApplication(p -> p.setKey("app2"));
    var app2 = appData2.getProjectDto();
    var project1 = db.components().insertPrivateProject(p -> p.setKey("project:one").setName("Projet Un")).getProjectDto();
    var project2 = db.components().insertPrivateProject(p -> p.setKey("project:two").setName("Projet Deux")).getProjectDto();

    db.components().addApplicationProject(app1, project1);
    db.components().addApplicationProject(app2, project2);
    db.components().addPortfolioApplicationBranch(portfolio.getUuid(), app1.getUuid(), appData1.getMainBranchDto().getUuid());
    db.components().addPortfolioApplicationBranch(portfolio.getUuid(), app2.getUuid(), appData2.getMainBranchDto().getUuid());

    assertThat(portfolioDao.selectAllApplicationProjects(session, portfolio.getRootUuid()))
      .extracting(ApplicationProjectDto::getAppUuid, ApplicationProjectDto::getAppKey, ApplicationProjectDto::getProjectUuid)
      .containsOnly(
        tuple(app1.getUuid(), "app1", project1.getUuid()),
        tuple(app2.getUuid(), "app2", project2.getUuid()));
  }

  @Test
  void add_and_select_references_by_uuid() {
    assertThat(portfolioDao.selectPortfolioProjects(session, "portfolio1")).isEmpty();
    portfolioDao.addReferenceBranch(session, "portfolio1", "app1", "main1");
    portfolioDao.addReferenceBranch(session, "portfolio1", "app2", "main2");
    portfolioDao.addReferenceBranch(session, "portfolio2", "app3", "main3");
    db.commit();
    assertThat(portfolioDao.selectReferenceUuids(session, "portfolio1")).containsExactlyInAnyOrder("app1", "app2");
    assertThat(portfolioDao.selectReferenceUuids(session, "portfolio2")).containsExactlyInAnyOrder("app3");
    assertThat(portfolioDao.selectReferenceUuids(session, "portfolio3")).isEmpty();

    assertThat(db.countRowsOfTable("portfolio_references")).isEqualTo(3);
    assertThat(db.select(session, "select created_at from portfolio_references"))
      .extracting(m -> m.values().iterator().next())
      .containsExactlyInAnyOrder(1L, 2L, 3L);
  }

  @Test
  void select_reference_to_app_by_key() {
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto("portfolio1");
    ProjectData projectData = db.components().insertPrivateApplication(p -> p.setKey("app1"));
    ProjectDto app1 = projectData.getProjectDto();
    db.components().addPortfolioApplicationBranch(portfolio.getUuid(), app1.getUuid(), projectData.getMainBranchDto().getUuid());

    assertThat(portfolioDao.selectReferenceToApp(db.getSession(), portfolio.getUuid(), app1.getKey()))
      .get()
      .extracting(ReferenceDto::getTargetUuid)
      .isEqualTo(app1.getUuid());

    assertThat(portfolioDao.selectReference(db.getSession(), portfolio.getUuid(), app1.getKey()))
      .extracting(ReferenceDto::getTargetUuid)
      .isEqualTo(app1.getUuid());

    assertThat(portfolioDao.selectReferenceToPortfolio(db.getSession(), portfolio.getUuid(), app1.getKey())).isEmpty();
  }

  @Test
  void select_reference_to_app_with_branches() {
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto("portfolio1");
    ProjectData projectData = db.components().insertPrivateApplication(p -> p.setKey("app").setName("app"));
    ProjectDto app = projectData.getProjectDto();
    BranchDto branch1 = db.components().insertProjectBranch(app, b -> b.setExcludeFromPurge(true));
    BranchDto branch2 = db.components().insertProjectBranch(app, b -> b.setExcludeFromPurge(true));

    db.components().addPortfolioApplicationBranch(portfolio.getUuid(), app.getUuid(), projectData.getMainBranchDto().getUuid());
    db.components().addPortfolioApplicationBranch(portfolio.getUuid(), app.getUuid(), branch1.getUuid());
    db.components().addPortfolioApplicationBranch(portfolio.getUuid(), app.getUuid(), branch2.getUuid());

    var appFromDb = portfolioDao.selectReferenceToApp(db.getSession(), portfolio.getUuid(), app.getKey());
    assertThat(appFromDb).isPresent();

    assertThat(appFromDb.get())
      .extracting(ReferenceDto::getTargetKey, ReferenceDto::getTargetName, ReferenceDto::getBranchUuids)
      .containsExactly("app", "app", Set.of(branch1.getUuid(), branch2.getUuid(), projectData.getMainBranchDto().getUuid()));

  }

  @Test
  void select_root_reference_to_app_with_branches() {
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto("portfolio1");
    ProjectDto app = db.components().insertPrivateApplication(p -> p.setKey("app").setName("app")).getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(app, b -> b.setExcludeFromPurge(true));

    db.components().addPortfolioApplicationBranch(portfolio.getUuid(), app.getUuid(), branch.getUuid());

    assertThat(portfolioDao.selectRootOfReferencersToAppBranch(db.getSession(), branch.getUuid()))
      .extracting(PortfolioDto::getKey)
      .containsExactly(portfolio.getKey());
  }

  @Test
  void select_reference_to_portfolio_by_key() {
    PortfolioDto portfolio1 = db.components().insertPrivatePortfolioDto("portfolio1");
    PortfolioDto portfolio2 = db.components().insertPrivatePortfolioDto("portfolio2");
    db.components().addPortfolioReference(portfolio1, portfolio2);

    assertThat(portfolioDao.selectReferenceToPortfolio(db.getSession(), portfolio1.getUuid(), portfolio2.getKey()))
      .get()
      .extracting(ReferenceDto::getTargetUuid)
      .isEqualTo(portfolio2.getUuid());

    assertThat(portfolioDao.selectReference(db.getSession(), portfolio1.getUuid(), portfolio2.getKey()))
      .extracting(ReferenceDto::getTargetUuid)
      .isEqualTo(portfolio2.getUuid());

    assertThat(portfolioDao.selectReferenceToApp(db.getSession(), portfolio1.getUuid(), portfolio2.getKey())).isEmpty();
  }

  @Test
  void selectReferencers() {
    PortfolioDto portfolio1 = db.components().insertPrivatePortfolioDto("portfolio1");
    PortfolioDto portfolio2 = db.components().insertPrivatePortfolioDto("portfolio2");

    ProjectData appData1 = db.components().insertPrivateApplication("app1");
    ProjectDto app1 = appData1.getProjectDto();
    portfolioDao.addReference(session, "portfolio1", "portfolio2");
    portfolioDao.addReferenceBranch(session, "portfolio1", app1.getUuid(), appData1.getMainBranchDto().getUuid());
    portfolioDao.addReferenceBranch(session, "portfolio2", app1.getUuid(), appData1.getMainBranchDto().getUuid());

    assertThat(portfolioDao.selectReferencers(session, portfolio2.getUuid()))
      .extracting("uuid").containsOnly("portfolio1");

    assertThat(portfolioDao.selectReferencers(session, "app1"))
      .extracting("uuid").containsOnly("portfolio1", "portfolio2");
  }

  @Test
  void selectReferencers_for_non_existing_reference() {
    assertThat(portfolioDao.selectReferencers(session, "unknown")).isEmpty();
  }

  @Test
  void selectRootOfReferencers_returns_root() {
    PortfolioDto portfolio1 = db.components().insertPrivatePortfolioDto("name1");
    PortfolioDto sub = addPortfolio(portfolio1, "sub1");
    PortfolioDto portfolio2 = db.components().insertPrivatePortfolioDto("name2");
    db.components().addPortfolioReference(sub, portfolio2.getUuid());

    assertThat(portfolioDao.selectRootOfReferencers(session, portfolio2.getUuid()))
      .extracting("uuid")
      .containsOnly(portfolio1.getUuid());
  }

  @Test
  void selectRootOfReferencers_for_non_existing_reference() {
    assertThat(portfolioDao.selectRootOfReferencers(session, "nonexisting")).isEmpty();
  }

  @Test
  void deleteReferencesTo() {
    db.components().insertPrivatePortfolioDto("portfolio1");
    db.components().insertPrivatePortfolioDto("portfolio2");
    db.components().insertPrivatePortfolioDto("portfolio3");

    db.components().addPortfolioReference("portfolio1", "portfolio3");
    db.components().addPortfolioReference("portfolio2", "portfolio3");
    portfolioDao.deleteReferencesTo(session, "portfolio3");
    session.commit();
    assertThat(db.countRowsOfTable("portfolio_references")).isZero();
  }

  @Test
  void deleteReferencesTo_with_non_existing_reference_doesnt_fail() {
    assertThatCode(() -> portfolioDao.deleteReferencesTo(session, "portfolio3"))
      .doesNotThrowAnyException();
  }

  @Test
  void deleteAllReferences() {
    PortfolioDto root = db.components().insertPrivatePortfolioDto();
    PortfolioDto child1 = addPortfolio(root);
    PortfolioDto child11 = addPortfolio(child1);
    PortfolioDto root2 = db.components().insertPrivatePortfolioDto();
    PortfolioDto root3 = db.components().insertPrivatePortfolioDto();
    PortfolioDto root4 = db.components().insertPrivatePortfolioDto();

    db.components().addPortfolioReference(child1.getUuid(), root2.getUuid());
    db.components().addPortfolioReference(root3.getUuid(), root4.getUuid());
    assertThat(db.countRowsOfTable("portfolio_references")).isEqualTo(2);

    portfolioDao.deleteAllReferences(session);
    session.commit();
    assertThat(db.countRowsOfTable("portfolio_references")).isZero();
  }

  @Test
  void deleteReference() {
    db.components().insertPrivatePortfolioDto("portfolio1");
    db.components().insertPrivatePortfolioDto("portfolio2");
    db.components().insertPrivatePortfolioDto("portfolio3");

    db.components().addPortfolioReference("portfolio1", "portfolio3");
    db.components().addPortfolioReference("portfolio2", "portfolio3");
    portfolioDao.deleteReference(session, "portfolio1", "portfolio3");
    assertThat(portfolioDao.selectAllReferencesToPortfolios(session))
      .extracting(ReferenceDto::getSourceUuid, ReferenceDto::getTargetUuid)
      .containsOnly(tuple("portfolio2", "portfolio3"));
  }

  @Test
  void deleteReferenceBranch() {
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto("portfolio1");
    ProjectData appData = db.components().insertPrivateApplication(p -> p.setKey("app").setName("app"));
    ProjectDto app = appData.getProjectDto();
    BranchDto branch1 = db.components().insertProjectBranch(app, b -> b.setExcludeFromPurge(true));
    BranchDto branch2 = db.components().insertProjectBranch(app, b -> b.setExcludeFromPurge(true));

    db.components().addPortfolioApplicationBranch(portfolio.getUuid(), app.getUuid(), appData.getMainBranchDto().getUuid());
    db.components().addPortfolioApplicationBranch(portfolio.getUuid(), app.getUuid(), branch1.getUuid());
    db.components().addPortfolioApplicationBranch(portfolio.getUuid(), app.getUuid(), branch2.getUuid());

    assertThat(portfolioDao.selectReferenceToApp(db.getSession(), portfolio.getUuid(), app.getKey()))
      .isPresent()
      .map(ReferenceDto::getBranchUuids)
      .contains(Set.of(branch1.getUuid(), branch2.getUuid(), appData.getMainBranchDto().getUuid()));

    portfolioDao.deleteReferenceBranch(db.getSession(), portfolio.getUuid(), app.getUuid(), branch1.getUuid());

    assertThat(portfolioDao.selectReferenceToApp(db.getSession(), portfolio.getUuid(), app.getKey()))
      .isPresent()
      .map(ReferenceDto::getBranchUuids)
      .contains(Set.of(branch2.getUuid(), appData.getMainBranchDto().getUuid()));

  }

  @Test
  void insert_and_select_projects() {
    PortfolioDto portfolio1 = db.components().insertPublicPortfolioDto();
    PortfolioDto portfolio2 = db.components().insertPublicPortfolioDto();
    PortfolioDto portfolio3 = db.components().insertPublicPortfolioDto();

    ProjectDto project1 = db.components().insertPrivateProject("project1").getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject("project2").getProjectDto();

    assertThat(portfolioDao.selectPortfolioProjects(session, portfolio1.getUuid())).isEmpty();
    db.components().addPortfolioProject(portfolio1, project1);
    db.components().addPortfolioProject(portfolio1, project2);
    db.components().addPortfolioProject(portfolio2, project2);
    db.commit();
    assertThat(portfolioDao.selectPortfolioProjects(session, portfolio1.getUuid())).extracting(PortfolioProjectDto::getProjectUuid).containsOnly("project1", "project2");
    assertThat(portfolioDao.selectPortfolioProjects(session, portfolio2.getUuid())).extracting(PortfolioProjectDto::getProjectUuid).containsOnly("project2");
    assertThat(portfolioDao.selectPortfolioProjects(session, portfolio3.getUuid())).isEmpty();

    assertThat(db.countRowsOfTable("portfolio_projects")).isEqualTo(3);
    assertThat(db.select(session, "select created_at from portfolio_projects"))
      .extracting(m -> m.values().iterator().next())
      .containsOnly(3L, 4L, 5L);
  }

  @Test
  void deleteProjects_deletes_selected_projects_and_branches() {
    db.components().insertPrivatePortfolioDto("portfolio1");
    db.components().insertPrivatePortfolioDto("portfolio2");

    db.components().insertPrivateProject("project1").getProjectDto();
    db.components().insertPrivateProject("project2").getProjectDto();

    assertThat(portfolioDao.selectPortfolioProjects(session, "portfolio1")).isEmpty();
    String uuid = portfolioDao.addProject(session, "portfolio1", "project1");
    portfolioDao.addBranch(session, uuid, "project1Branch");
    portfolioDao.addProject(session, "portfolio1", "project2");
    portfolioDao.addProject(session, "portfolio2", "project2");

    session.commit();
    assertThat(portfolioDao.selectPortfolioProjects(session, "portfolio1")).isNotEmpty();
    assertThat(db.countRowsOfTable("portfolio_proj_branches")).isOne();

    portfolioDao.deleteProjects(session, "portfolio1");
    session.commit();
    assertThat(portfolioDao.selectPortfolioProjects(session, "portfolio1")).isEmpty();
    assertThat(portfolioDao.selectPortfolioProjects(session, "portfolio2")).extracting(PortfolioProjectDto::getProjectUuid).containsOnly(
      "project2");
    assertThat(db.countRowsOfTable("portfolio_proj_branches")).isZero();
  }

  @Test
  void deleteProject_deletes_selected_branches() {
    db.components().insertPrivatePortfolioDto("portfolio1");

    db.components().insertPrivateProject("project1").getProjectDto();
    db.components().insertPrivateProject("project2").getProjectDto();

    String uuid1 = portfolioDao.addProject(session, "portfolio1", "project1");
    portfolioDao.addBranch(session, uuid1, "project1Branch");
    String uuid2 = portfolioDao.addProject(session, "portfolio1", "project2");
    portfolioDao.addBranch(session, uuid2, "project2Branch");
    session.commit();
    assertThat(db.countRowsOfTable("portfolio_proj_branches")).isEqualTo(2);

    portfolioDao.deleteProject(session, "portfolio1", "project2");
    assertThat(portfolioDao.selectPortfolioProjects(session, "portfolio1"))
      .extracting(PortfolioProjectDto::getProjectUuid, PortfolioProjectDto::getBranchUuids)
      .containsOnly(tuple("project1", Set.of("project1Branch")));
  }

  @Test
  void add_and_delete_selected_branches() {
    PortfolioDto portfolio1 = db.components().insertPrivatePortfolioDto("portfolio1");
    ProjectDto project1 = db.components().insertPrivateProject("project1").getProjectDto();
    db.components().addPortfolioProject(portfolio1, project1);

    assertThat(db.countRowsOfTable(db.getSession(), "portfolio_proj_branches")).isZero();
    assertThat(portfolioDao.selectPortfolioProjectOrFail(db.getSession(), portfolio1.getUuid(), project1.getUuid()).getBranchUuids()).isEmpty();

    db.components().addPortfolioProjectBranch(portfolio1, project1, "branch1");
    assertThat(db.countRowsOfTable(db.getSession(), "portfolio_proj_branches")).isOne();
    PortfolioProjectDto portfolioProject = portfolioDao.selectPortfolioProjectOrFail(db.getSession(), portfolio1.getUuid(),
      project1.getUuid());
    assertThat(portfolioProject.getBranchUuids()).containsOnly("branch1");

    portfolioDao.deleteBranch(db.getSession(), portfolio1.getUuid(), project1.getUuid(), "branch1");
    assertThat(db.countRowsOfTable(db.getSession(), "portfolio_proj_branches")).isZero();
    assertThat(portfolioDao.selectPortfolioProjectOrFail(db.getSession(), portfolio1.getUuid(), project1.getUuid()).getBranchUuids()).isEmpty();
  }

  @Test
  void delete_nonexisting_branch_doesnt_fail() {
    DbSession session = db.getSession();
    assertThatCode(() -> portfolioDao.deleteBranch(session, "nonexisting1", "nonexisting2", "branch1"))
      .doesNotThrowAnyException();
  }

  @Test
  void selectAllProjectsInHierarchy() {
    ProjectData p1 = db.components().insertPrivateProject("p1");
    ProjectData p2 = db.components().insertPrivateProject("p2");
    ProjectData p3 = db.components().insertPrivateProject("p3");
    ProjectData p4 = db.components().insertPrivateProject("p4");

    PortfolioDto root = db.components().insertPrivatePortfolioDto("root");
    PortfolioDto child1 = addPortfolio(root, "child1");
    PortfolioDto child11 = addPortfolio(child1, "child11");
    PortfolioDto root2 = db.components().insertPrivatePortfolioDto("root2");

    db.components().addPortfolioProject(root, p1.getProjectDto());
    db.components().addPortfolioProject(child1, p2.getProjectDto());
    db.components().addPortfolioProject(child11, p3.getProjectDto());
    db.components().addPortfolioProject(root2, p4.getProjectDto());

    db.components().addPortfolioProjectBranch(root, p1.getProjectDto(), "branch1");
    session.commit();

    assertThat(portfolioDao.selectAllProjectsInHierarchy(session, root.getUuid()))
      .extracting(PortfolioProjectDto::getProjectUuid, PortfolioProjectDto::getBranchUuids, PortfolioProjectDto::getMainBranchUuid)
      .containsExactlyInAnyOrder(
        tuple("p1", Set.of("branch1"), p1.getMainBranchDto().getUuid()),
        tuple("p2", emptySet(), p2.getMainBranchDto().getUuid()),
        tuple("p3", emptySet(), p3.getMainBranchDto().getUuid()));
    assertThat(portfolioDao.selectAllProjectsInHierarchy(session, "nonexisting")).isEmpty();
  }

  @Test
  void deleteAllProjects() {
    db.components().insertPrivateProject("p1").getProjectDto();
    db.components().insertPrivateProject("p2").getMainBranchComponent();
    db.components().insertPrivateProject("p3").getMainBranchComponent();
    db.components().insertPrivateProject("p4").getMainBranchComponent();

    PortfolioDto root = db.components().insertPrivatePortfolioDto();
    PortfolioDto child1 = addPortfolio(root);
    PortfolioDto child11 = addPortfolio(child1);
    PortfolioDto root2 = db.components().insertPrivatePortfolioDto();

    String portfolioProjectUuid = portfolioDao.addProject(session, root.getUuid(), "p1");
    portfolioDao.addProject(session, child1.getUuid(), "p2");
    portfolioDao.addProject(session, child11.getUuid(), "p3");
    portfolioDao.addProject(session, root2.getUuid(), "p4");
    portfolioDao.addBranch(session, portfolioProjectUuid, "branch1");

    assertThat(db.countRowsOfTable(session, "portfolio_projects")).isEqualTo(4);
    assertThat(db.countRowsOfTable(session, "portfolio_proj_branches")).isOne();

    portfolioDao.deleteAllProjects(session);
    assertThat(db.countRowsOfTable(session, "portfolio_projects")).isZero();
    assertThat(db.countRowsOfTable(session, "portfolio_proj_branches")).isZero();
  }

  @Test
  void countPortfoliosByMode_shouldReturnCorrectData() {
    PortfolioDto portfolio1 = ComponentTesting.newPortfolioDto("uuid1", "ptf1", "Portfolio 1", null)
      .setSelectionMode(REGEXP);
    PortfolioDto subPortfolio1 = ComponentTesting.newPortfolioDto("sub_uuid11", "sub_ptf1", "SubPortfolio 1", portfolio1)
      .setSelectionMode(MANUAL);
    PortfolioDto subSubPortfolio1 = ComponentTesting.newPortfolioDto("sub_uuid12", "sub_sub_ptf1", "SubSubPortfolio 1", portfolio1)
      .setSelectionMode(REST);
    PortfolioDto portfolio2 = ComponentTesting.newPortfolioDto("uuid2", "ptf2", "Portfolio 2", null)
      .setSelectionMode(REGEXP);
    PortfolioDto subPortfolio2 = ComponentTesting.newPortfolioDto("sub_uuid21", "sub_ptd2", "SubPortfolio 2", portfolio2)
      .setSelectionMode(TAGS);
    Arrays.asList(portfolio1, subPortfolio1, subSubPortfolio1, portfolio2, subPortfolio2)
      .forEach(portfolio -> portfolioDao.insertWithAudit(db.getSession(), portfolio));

    Map<String, Integer> expectedForRoot = Map.of(REGEXP.name(), 2);
    assertThat(portfolioDao.countPortfoliosByMode(db.getSession()))
      .containsExactlyInAnyOrderEntriesOf(expectedForRoot);
    Map<String, Integer> expectedForSub = Map.of(MANUAL.name(), 1, REST.name(), 1, TAGS.name(), 1);
    assertThat(portfolioDao.countSubportfoliosByMode(db.getSession()))
      .containsExactlyInAnyOrderEntriesOf(expectedForSub);
  }

  private PortfolioDto addPortfolio(PortfolioDto parent) {
    return addPortfolio(parent, uuidFactory.create());
  }

  private PortfolioDto addPortfolio(PortfolioDto parent, String uuid) {
    PortfolioDto portfolio = new PortfolioDto()
      .setKey("key_" + uuid)
      .setName("name_" + uuid)
      .setSelectionMode(PortfolioDto.SelectionMode.NONE)
      .setRootUuid(parent.getRootUuid())
      .setParentUuid(parent.getUuid())
      .setUuid(uuid);
    db.getDbClient().portfolioDao().insertWithAudit(session, portfolio);
    session.commit();
    return portfolio;
  }
}
