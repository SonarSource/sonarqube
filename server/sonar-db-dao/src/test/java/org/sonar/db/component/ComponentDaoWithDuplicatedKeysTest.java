/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.component;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * On H2, the index on PROJECTS.KEE is unique. In order to simulate the MySQL behaviour where the index is not unique,
 * we need to create a schema where there's no unique index on PROJECTS.KEE
 */

public class ComponentDaoWithDuplicatedKeysTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, ComponentDaoWithDuplicatedKeysTest.class, "schema.sql");

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private ComponentDao underTest = new ComponentDao();

  @Test
  public void select_components_having_same_key() {
    OrganizationDto organizationDto = db.organizations().insert();
    insertProject(ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey(PROJECT_KEY));
    insertProject(ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey(PROJECT_KEY));
    insertProject(ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey(PROJECT_KEY));
    insertProject(ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey("ANOTHER_PROJECT_KEY"));

    assertThat(underTest.selectComponentsHavingSameKeyOrderedById(db.getSession(), PROJECT_KEY)).hasSize(3);
  }

  @Test
  public void return_nothing()  {
    assertThat(underTest.selectComponentsHavingSameKeyOrderedById(db.getSession(), PROJECT_KEY)).isEmpty();
  }

  private ComponentDto insertProject(ComponentDto project) {
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();
    return project;
  }
}
