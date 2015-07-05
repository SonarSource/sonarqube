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
package org.sonar.db.component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.AbstractDaoTestCase;
import org.sonar.db.DbSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ResourceIndexerDaoTest extends AbstractDaoTestCase {

  private static ResourceIndexerDao dao;

  @Before
  public void createDao() {
    dao = new ResourceIndexerDao(getMyBatis(), mock(System2.class));
  }

  @Test
  public void shouldIndexResource() {
    setupData("shouldIndexResource");

    dao.indexResource(10, "ZipUtils", "FIL", 8);

    checkTables("shouldIndexResource", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldIndexProjects() {
    setupData("shouldIndexProjects");

    dao.indexProjects();

    checkTables("shouldIndexProjects", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldIndexMultiModulesProject() {
    setupData("shouldIndexMultiModulesProject");

    dao.indexProject(1);

    checkTables("shouldIndexMultiModulesProject", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldReindexProjectAfterRenaming() {
    setupData("shouldReindexProjectAfterRenaming");

    dao.indexProject(1);

    checkTables("shouldReindexProjectAfterRenaming", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldNotIndexPackages() throws SQLException {
    setupData("shouldNotIndexPackages");

    dao.indexProject(1);

    Connection connection = getConnection();
    ResultSet rs = null;
    try {
      // project
      rs = connection.createStatement().executeQuery("select count(resource_id) from resource_index where resource_id=1");
      rs.next();
      assertThat(rs.getInt(1)).isGreaterThan(0);

      // directory
      rs = connection.createStatement().executeQuery("select count(resource_id) from resource_index where resource_id=2");
      rs.next();
      assertThat(rs.getInt(1)).isEqualTo(0);

      // file
      rs = connection.createStatement().executeQuery("select count(resource_id) from resource_index where resource_id=3");
      rs.next();
      assertThat(rs.getInt(1)).isGreaterThan(0);
    } finally {
      if (null != rs) {
        rs.close();
      }
    }
  }

  @Test
  public void shouldIndexTwoLettersLongResources() {
    setupData("shouldIndexTwoLettersLongResource");

    dao.indexResource(10, "AB", Qualifiers.PROJECT, 3);

    checkTables("shouldIndexTwoLettersLongResource", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldReIndexTwoLettersLongResources() {
    setupData("shouldReIndexTwoLettersLongResource");

    dao.indexResource(1, "AS", Qualifiers.PROJECT, 1);

    checkTables("shouldReIndexTwoLettersLongResource", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldReIndexNewTwoLettersLongResource() {
    setupData("shouldReIndexNewTwoLettersLongResource");

    dao.indexResource(1, "AS", Qualifiers.PROJECT, 1);

    checkTables("shouldReIndexNewTwoLettersLongResource", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldReindexResource() {
    setupData("shouldReindexResource");

    dao.indexResource(1, "New Struts", Qualifiers.PROJECT, 1);

    checkTables("shouldReindexResource", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldNotReindexUnchangedResource() {
    setupData("shouldNotReindexUnchangedResource");

    dao.indexResource(1, "Struts", Qualifiers.PROJECT, 1);

    checkTables("shouldNotReindexUnchangedResource", new String[] {"id"}, "resource_index");
  }

  @Test
  public void select_project_ids_from_query_and_view_or_sub_view_uuid() {
    setupData("select_project_ids_from_query_and_view_or_sub_view_uuid");
    String viewUuid = "EFGH";

    DbSession session = getMyBatis().openSession(false);
    assertThat(dao.selectProjectIdsFromQueryAndViewOrSubViewUuid(session, "project", viewUuid)).containsOnly(1L, 2L);
    assertThat(dao.selectProjectIdsFromQueryAndViewOrSubViewUuid(session, "one", viewUuid)).containsOnly(1L);
    assertThat(dao.selectProjectIdsFromQueryAndViewOrSubViewUuid(session, "two", viewUuid)).containsOnly(2L);
    assertThat(dao.selectProjectIdsFromQueryAndViewOrSubViewUuid(session, "unknown", viewUuid)).isEmpty();
  }
}
