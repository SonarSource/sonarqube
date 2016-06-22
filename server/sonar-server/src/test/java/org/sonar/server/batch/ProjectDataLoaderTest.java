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
package org.sonar.server.batch;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class ProjectDataLoaderTest {
  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();

  private ProjectDataLoader underTest = new ProjectDataLoader(dbClient, mock(UserSession.class));

  @After
  public void tearDown() throws Exception {
    dbSession.close();
  }

  @Test
  public void load_fails_with_NPE_if_query_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.load(null);
  }

  @Test
  public void load_fails_with_NFE_if_query_is_empty() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project or module with key 'null' is not found");

    underTest.load(ProjectDataQuery.create());
  }

  @Test
  public void load_fails_with_NFE_if_component_does_not_exist() {
    String key = "theKey";

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project or module with key '" + key + "' is not found");

    underTest.load(ProjectDataQuery.create().setModuleKey(key));
  }

  private int uuidCounter = 0;
  @Test
  public void load_fails_with_BRE_if_component_is_neither_a_project_or_a_module() {
    String[][] allScopesAndQualifierButProjectAndModule = {
      {Scopes.PROJECT, Qualifiers.VIEW},
      {Scopes.PROJECT, Qualifiers.SUBVIEW},
      {Scopes.FILE, Qualifiers.PROJECT},
      {Scopes.DIRECTORY, Qualifiers.DIRECTORY},
      {Scopes.FILE, Qualifiers.UNIT_TEST_FILE},
      {Scopes.PROJECT, "DEV"},
      {Scopes.PROJECT, "DEV_PRJ"}
    };

    for (String[] scopeAndQualifier : allScopesAndQualifierButProjectAndModule) {
      String scope = scopeAndQualifier[0];
      String qualifier = scopeAndQualifier[1];
      String key = "theKey_" + scope + "_" + qualifier;
      String uuid = "uuid_" + uuidCounter++;
      dbClient.componentDao().insert(dbSession, new ComponentDto()
        .setUuid(uuid)
        .setUuidPath(uuid + ".")
        .setRootUuid(uuid)
        .setScope(scope)
        .setQualifier(qualifier)
        .setKey(key));
      dbSession.commit();

      try {
        underTest.load(ProjectDataQuery.create().setModuleKey(key));
        fail(format("A NotFoundException should have been raised because scope (%s) or qualifier (%s) is not project", scope, qualifier));
      } catch (BadRequestException e) {
        Assertions.assertThat(e).hasMessage("Key '" + key + "' belongs to a component which is not a Project");
      }
    }
  }
}
