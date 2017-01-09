/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.component;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;

public class ResourceIndexDaoTest {

  private static final String[] EXCLUDED_ID_COLUMN = new String[]{"id"};
  private static final String CPT_UUID = "cpt_uuid";
  private static final String ROOT_UUID = "ABCD";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  ResourceIndexDao underTest = dbTester.getDbClient().componentIndexDao();

  @Test
  public void shouldIndexResource() {
    dbTester.prepareDbUnit(getClass(), "shouldIndexResource.xml");

    underTest.indexResource(CPT_UUID, "ZipUtils", "FIL", ROOT_UUID);

    dbTester.assertDbUnit(getClass(), "shouldIndexResource-result.xml", EXCLUDED_ID_COLUMN, "resource_index");
  }

  @Test
  public void shouldIndexMultiModulesProject() {
    dbTester.prepareDbUnit(getClass(), "shouldIndexMultiModulesProject.xml");

    underTest.indexProject("ABCD");

    dbTester.assertDbUnit(getClass(), "shouldIndexMultiModulesProject-result.xml", EXCLUDED_ID_COLUMN, "resource_index");
  }

  @Test
  public void shouldReindexProjectAfterRenaming() {
    dbTester.prepareDbUnit(getClass(), "shouldReindexProjectAfterRenaming.xml");

    underTest.indexProject("ABCD");

    dbTester.assertDbUnit(getClass(), "shouldReindexProjectAfterRenaming-result.xml", EXCLUDED_ID_COLUMN, "resource_index");
  }

  @Test
  public void shouldNotIndexDirectories() {
    dbTester.prepareDbUnit(getClass(), "shouldNotIndexPackages.xml");

    underTest.indexProject("ABCD");
    // project
    assertThat(dbTester.countSql("select count(1) from resource_index where component_uuid='ABCD'")).isGreaterThan(0);
    // directory
    assertThat(dbTester.countSql("select count(1) from resource_index where component_uuid='BCDE'")).isEqualTo(0);
    // file
    assertThat(dbTester.countSql("select count(1) from resource_index where component_uuid='CDEF'")).isGreaterThan(0);
  }

  @Test
  public void shouldIndexTwoLettersLongResources() {
    dbTester.prepareDbUnit(getClass(), "shouldIndexTwoLettersLongResource.xml");

    underTest.indexResource(CPT_UUID, "AB", Qualifiers.PROJECT, ROOT_UUID);

    dbTester.assertDbUnit(getClass(), "shouldIndexTwoLettersLongResource-result.xml", EXCLUDED_ID_COLUMN, "resource_index");
  }

  @Test
  public void shouldReIndexTwoLettersLongResources() {
    dbTester.prepareDbUnit(getClass(), "shouldReIndexTwoLettersLongResource.xml");

    underTest.indexResource(ROOT_UUID, "AS", Qualifiers.PROJECT, ROOT_UUID);

    dbTester.assertDbUnit(getClass(), "shouldReIndexTwoLettersLongResource-result.xml", EXCLUDED_ID_COLUMN, "resource_index");
  }

  @Test
  public void shouldReIndexNewTwoLettersLongResource() {
    dbTester.prepareDbUnit(getClass(), "shouldReIndexNewTwoLettersLongResource.xml");

    underTest.indexResource(ROOT_UUID, "AS", Qualifiers.PROJECT, ROOT_UUID);

    dbTester.assertDbUnit(getClass(), "shouldReIndexNewTwoLettersLongResource-result.xml", EXCLUDED_ID_COLUMN, "resource_index");
  }

  @Test
  public void shouldReindexResource() {
    dbTester.prepareDbUnit(getClass(), "shouldReindexResource.xml");

    underTest.indexResource(ROOT_UUID, "New Struts", Qualifiers.PROJECT, ROOT_UUID);

    dbTester.assertDbUnit(getClass(), "shouldReindexResource-result.xml", EXCLUDED_ID_COLUMN, "resource_index");
  }

  @Test
  public void shouldNotReindexUnchangedResource() {
    dbTester.prepareDbUnit(getClass(), "shouldNotReindexUnchangedResource.xml");

    underTest.indexResource(ROOT_UUID, "Struts", Qualifiers.PROJECT, ROOT_UUID);

    dbTester.assertDbUnit(getClass(), "shouldNotReindexUnchangedResource-result.xml", EXCLUDED_ID_COLUMN, "resource_index");
  }

  @Test
  public void select_project_ids_from_query_and_view_or_sub_view_uuid() {
    dbTester.prepareDbUnit(getClass(), "select_project_ids_from_query_and_view_or_sub_view_uuid.xml");

    String viewUuid = "EFGH";

    DbSession session = dbTester.getSession();
    assertThat(underTest.selectProjectIdsFromQueryAndViewOrSubViewUuid(session, "project", viewUuid)).containsOnly(1L, 2L);
    assertThat(underTest.selectProjectIdsFromQueryAndViewOrSubViewUuid(session, "one", viewUuid)).containsOnly(1L);
    assertThat(underTest.selectProjectIdsFromQueryAndViewOrSubViewUuid(session, "two", viewUuid)).containsOnly(2L);
    assertThat(underTest.selectProjectIdsFromQueryAndViewOrSubViewUuid(session, "unknown", viewUuid)).isEmpty();
  }

  /**
   * SONAR-7594
   * PROJECTS.NAME is 2'000 characters long whereas RESOURCE_INDEX.KEE is only 400 characters. As there's no functional need
   * to increase size of the latter column, indexed values must be truncated.
   */
  @Test
  public void restrict_indexed_combinations_to_400_characters() {
    String longName = repeat("a", 2_000);
    ComponentDto project = ComponentTesting.newProjectDto(ROOT_UUID)
      .setProjectUuid(ROOT_UUID)
      .setName(longName);
    DbSession session = dbTester.getSession();
    dbTester.getDbClient().componentDao().insert(session, project);
    dbTester.getDbClient().snapshotDao().insert(session, new SnapshotDto()
      .setUuid("u1")
      .setComponentUuid(project.uuid())
      .setLast(true));

    underTest.indexProject(session, project.uuid());
    session.commit();

    assertThat(dbTester.countRowsOfTable("resource_index")).isEqualTo(longName.length() - ResourceIndexDao.MINIMUM_KEY_SIZE + 1);
  }
}
