/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;

public class PortfolioDaoTest {
  private final System2 system2 = new AlwaysIncreasingSystem2(1L, 1);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final PortfolioDao portfolioDao = new PortfolioDao(system2, UuidFactoryFast.getInstance());

  @Test
  public void selectByKey_returns_empty_if_no_match() {
    assertThat(portfolioDao.selectByKey(db.getSession(), "nonexisting")).isEmpty();
  }

  @Test
  public void selectAllRoots() {
    createPortfolio("p1", null, "p1");
    createPortfolio("p11", "p1", "p1");
    createPortfolio("p111", "p11", "p1");
    createPortfolio("p2", null, "p2");

    assertThat(portfolioDao.selectAllRoots(db.getSession())).extracting("uuid")
      .containsExactlyInAnyOrder("p1", "p2");
  }

  @Test
  public void selectByKey_returns_match() {
    createPortfolio("name");
    assertThat(portfolioDao.selectByKey(db.getSession(), "key_name")).isNotEmpty();
  }

  @Test
  public void selectByUuid_returns_empty_if_no_match() {
    assertThat(portfolioDao.selectByUuid(db.getSession(), "nonexisting")).isEmpty();
  }

  @Test
  public void selectByUuid_returns_match() {
    createPortfolio("name");
    assertThat(portfolioDao.selectByUuid(db.getSession(), "name")).isNotEmpty();
    assertThat(portfolioDao.selectByUuid(db.getSession(), "name").get())
      .extracting("name", "key", "uuid", "description", "rootUuid", "parentUuid", "selectionMode", "selectionExpression", "createdAt", "updatedAt")
      .containsExactly("name_name", "key_name", "name", "desc_name", "root", "parent", "mode", "exp", 1000L, 2000L);
  }

  @Test
  public void update_portfolio() {
    createPortfolio("name");
    PortfolioDto dto = portfolioDao.selectByUuid(db.getSession(), "name").get();
    dto.setSelectionMode("newMode");
    dto.setSelectionExpression("newExp");
    dto.setDescription("newDesc");
    dto.setName("newName");
    portfolioDao.update(db.getSession(), dto);

    assertThat(portfolioDao.selectByUuid(db.getSession(), "name").get())
      .extracting("name", "key", "uuid", "description", "private", "rootUuid", "parentUuid", "selectionMode", "selectionExpression", "createdAt", "updatedAt")
      .containsExactly("newName", "key_name", "name", "newDesc", true, "root", "parent", "newMode", "newExp", 1000L, 1L);
  }

  @Test
  public void selectTree() {
    createPortfolio("p1", null, "p1");
    createPortfolio("p11", "p1", "p1");
    createPortfolio("p111", "p11", "p1");
    createPortfolio("p12", "p1", "p1");

    createPortfolio("p2", null, "p2");

    assertThat(portfolioDao.selectTree(db.getSession(), "p1")).extracting("uuid").containsOnly("p1", "p11", "p111", "p12");
    assertThat(portfolioDao.selectTree(db.getSession(), "p11")).extracting("uuid").containsOnly("p1", "p11", "p111", "p12");
    assertThat(portfolioDao.selectTree(db.getSession(), "p111")).extracting("uuid").containsOnly("p1", "p11", "p111", "p12");
    assertThat(portfolioDao.selectTree(db.getSession(), "p2")).extracting("uuid").containsOnly("p2");
  }

  @Test
  public void deleteByUuids() {
    createPortfolio("p1");
    createPortfolio("p2");
    createPortfolio("p3");
    createPortfolio("p4");

    portfolioDao.addProject(db.getSession(), "p1", "proj1");
    portfolioDao.addProject(db.getSession(), "p2", "proj1");

    portfolioDao.addReference(db.getSession(), "p1", "app1");
    portfolioDao.addReference(db.getSession(), "p2", "app1");
    portfolioDao.addReference(db.getSession(), "p4", "p1");

    portfolioDao.deleteByUuids(db.getSession(), Set.of("p1", "p3"));
    assertThat(db.select(db.getSession(), "select uuid from portfolios")).extracting(m -> m.values().iterator().next())
      .containsOnly("p2", "p4");
    assertThat(db.select(db.getSession(), "select portfolio_uuid from portfolio_references")).extracting(m -> m.values().iterator().next())
      .containsOnly("p2");
    assertThat(db.select(db.getSession(), "select portfolio_uuid from portfolio_projects")).extracting(m -> m.values().iterator().next())
      .containsOnly("p2");

  }

  @Test
  public void selectReferencersByKey() {
    createPortfolio("portfolio1");
    createPortfolio("portfolio2");
    ProjectDto app1 = db.components().insertPrivateApplicationDto(p -> p.setDbKey("app1"));
    portfolioDao.addReference(db.getSession(), "portfolio1", "portfolio2");
    portfolioDao.addReference(db.getSession(), "portfolio1", app1.getUuid());
    portfolioDao.addReference(db.getSession(), "portfolio2", app1.getUuid());

    assertThat(portfolioDao.selectReferencersByKey(db.getSession(), "key_portfolio2"))
      .extracting("uuid").containsOnly("portfolio1");

    assertThat(portfolioDao.selectReferencersByKey(db.getSession(), "app1"))
      .extracting("uuid").containsOnly("portfolio1", "portfolio2");

  }

  @Test
  public void insert_and_select_references() {
    assertThat(portfolioDao.getProjects(db.getSession(), "portfolio1")).isEmpty();
    portfolioDao.addReference(db.getSession(), "portfolio1", "app1");
    portfolioDao.addReference(db.getSession(), "portfolio1", "app2");
    portfolioDao.addReference(db.getSession(), "portfolio2", "app3");
    db.commit();
    assertThat(portfolioDao.getReferences(db.getSession(), "portfolio1")).containsExactlyInAnyOrder("app1", "app2");
    assertThat(portfolioDao.getReferences(db.getSession(), "portfolio2")).containsExactlyInAnyOrder("app3");
    assertThat(portfolioDao.getReferences(db.getSession(), "portfolio3")).isEmpty();

    assertThat(db.countRowsOfTable("portfolio_references")).isEqualTo(3);
    assertThat(db.select(db.getSession(), "select created_at from portfolio_references"))
      .extracting(m -> m.values().iterator().next())
      .containsExactlyInAnyOrder(1L, 2L, 3L);
  }

  @Test
  public void insert_and_select_projects() {
    db.components().insertPrivateProject("project1");
    db.components().insertPrivateProject("project2");

    assertThat(portfolioDao.getProjects(db.getSession(), "portfolio1")).isEmpty();
    portfolioDao.addProject(db.getSession(), "portfolio1", "project1");
    portfolioDao.addProject(db.getSession(), "portfolio1", "project2");
    portfolioDao.addProject(db.getSession(), "portfolio2", "project2");
    db.commit();
    assertThat(portfolioDao.getProjects(db.getSession(), "portfolio1")).extracting(ProjectDto::getUuid).containsExactlyInAnyOrder("project1", "project2");
    assertThat(portfolioDao.getProjects(db.getSession(), "portfolio2")).extracting(ProjectDto::getUuid).containsExactlyInAnyOrder("project2");
    assertThat(portfolioDao.getProjects(db.getSession(), "portfolio3")).isEmpty();

    assertThat(db.countRowsOfTable("portfolio_projects")).isEqualTo(3);
    assertThat(db.select(db.getSession(), "select created_at from portfolio_projects"))
      .extracting(m -> m.values().iterator().next())
      .containsExactlyInAnyOrder(3L, 4L, 5L);
  }

  @Test
  public void delete_projects() {
    db.components().insertPrivateProject("project1");
    db.components().insertPrivateProject("project2");

    assertThat(portfolioDao.getProjects(db.getSession(), "portfolio1")).isEmpty();
    portfolioDao.addProject(db.getSession(), "portfolio1", "project1");
    portfolioDao.addProject(db.getSession(), "portfolio1", "project2");
    portfolioDao.addProject(db.getSession(), "portfolio2", "project2");

    portfolioDao.deleteProjects(db.getSession(), "portfolio1");
    assertThat(portfolioDao.getProjects(db.getSession(), "portfolio1")).isEmpty();
    assertThat(portfolioDao.getProjects(db.getSession(), "portfolio2")).extracting(ProjectDto::getUuid)
        .containsExactlyInAnyOrder("project2");
  }

  @Test
  public void getAllProjectsInHierarchy() {
    db.components().insertPrivateProject("p1");
    db.components().insertPrivateProject("p2");
    db.components().insertPrivateProject("p3");
    db.components().insertPrivateProject("p4");

    createPortfolio("root", null, "root");
    createPortfolio("child1", null, "root");
    createPortfolio("child11", "child1", "root");

    createPortfolio("root2", null, "root2");

    portfolioDao.addProject(db.getSession(), "root", "p1");
    portfolioDao.addProject(db.getSession(), "child1", "p2");
    portfolioDao.addProject(db.getSession(), "child11", "p3");
    portfolioDao.addProject(db.getSession(), "root2", "p4");

    assertThat(portfolioDao.getAllProjectsInHierarchy(db.getSession(), "root").keySet()).containsExactly("p1", "p2", "p3");
    assertThat(portfolioDao.getAllProjectsInHierarchy(db.getSession(), "nonexisting")).isEmpty();
  }

  private PortfolioDto createPortfolio(String uuid, @Nullable String parentUuid, String rootUuid) {
    PortfolioDto p = new PortfolioDto()
      .setName("name_" + uuid)
      .setKey("key_" + uuid)
      .setUuid(uuid)
      .setDescription("desc_" + uuid)
      .setRootUuid(rootUuid)
      .setParentUuid(parentUuid)
      .setPrivate(true)
      .setSelectionExpression("exp")
      .setSelectionMode("mode")
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);

    portfolioDao.insert(db.getSession(), p);
    return p;
  }

  private PortfolioDto createPortfolio(String name) {
    return createPortfolio(name, "parent", "root");
  }
}
