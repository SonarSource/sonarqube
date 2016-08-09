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

import com.google.common.base.Strings;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;

public class ComponentKeyUpdaterDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbSession dbSession = db.getSession();
  ComponentDbTester componentDb = new ComponentDbTester(db);

  ComponentKeyUpdaterDao underTest = db.getDbClient().componentKeyUpdaterDao();

  @Test
  public void shouldUpdateKey() {
    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.updateKey("B", "struts:core");

    db.assertDbUnit(getClass(), "shouldUpdateKey-result.xml", "projects");
  }

  @Test
  public void shouldNotUpdateKey() {
    db.prepareDbUnit(getClass(), "shared.xml");

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Impossible to update key: a component with key \"org.struts:struts-ui\" already exists.");

    underTest.updateKey("B", "org.struts:struts-ui");
  }

  @Test
  public void shouldBulkUpdateKey() {
    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.bulkUpdateKey(dbSession, "A", "org.struts", "org.apache.struts");
    dbSession.commit();

    db.assertDbUnit(getClass(), "shouldBulkUpdateKey-result.xml", "projects");
  }

  @Test
  public void shouldBulkUpdateKeyOnOnlyOneSubmodule() {
    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.bulkUpdateKey(dbSession, "A", "struts-ui", "struts-web");
    dbSession.commit();

    db.assertDbUnit(getClass(), "shouldBulkUpdateKeyOnOnlyOneSubmodule-result.xml", "projects");
  }

  @Test
  public void shouldFailBulkUpdateKeyIfKeyAlreadyExist() {
    db.prepareDbUnit(getClass(), "shared.xml");

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Impossible to update key: a component with key \"foo:struts-core\" already exists.");

    underTest.bulkUpdateKey(dbSession, "A", "org.struts", "foo");
    dbSession.commit();
  }

  @Test
  public void shouldNotUpdateAllSubmodules() {
    db.prepareDbUnit(getClass(), "shouldNotUpdateAllSubmodules.xml");

    underTest.bulkUpdateKey(dbSession, "A", "org.struts", "org.apache.struts");
    dbSession.commit();

    db.assertDbUnit(getClass(), "shouldNotUpdateAllSubmodules-result.xml", "projects");
  }

  @Test
  public void fail_with_functional_exception_when_sub_component_key_is_longer_than_authorized() {
    ComponentDto project = newProjectDto("project-uuid").setKey("old-project-key");
    componentDb.insertComponent(project);
    componentDb.insertComponent(newFileDto(project, null).setKey("old-project-key:file"));
    String newLongProjectKey = Strings.repeat("a", 400);
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Component key length (405) is longer than the maximum authorized (400). '" + newLongProjectKey + ":file' was provided.");

    underTest.updateKey(project.uuid(), newLongProjectKey);
  }

  @Test
  public void fail_when_new_key_is_invalid() {
    ComponentDto project = componentDb.insertProject();

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Malformed key for 'my?project?key'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");

    underTest.bulkUpdateKey(dbSession, project.uuid(), project.key(), "my?project?key");
  }

  @Test
  public void shouldCheckModuleKeysBeforeRenaming() {
    db.prepareDbUnit(getClass(), "shared.xml");

    Map<String, String> checkResults = underTest.checkModuleKeysBeforeRenaming("A", "org.struts", "foo");
    assertThat(checkResults.size()).isEqualTo(3);
    assertThat(checkResults.get("org.struts:struts")).isEqualTo("foo:struts");
    assertThat(checkResults.get("org.struts:struts-core")).isEqualTo("#duplicate_key#");
    assertThat(checkResults.get("org.struts:struts-ui")).isEqualTo("foo:struts-ui");
  }

  @Test
  public void check_component_keys() {
    db.prepareDbUnit(getClass(), "shared.xml");

    Map<String, Boolean> result = underTest.checkComponentKeys(dbSession, newArrayList("foo:struts", "foo:struts-core", "foo:struts-ui"));

    assertThat(result)
      .hasSize(3)
      .containsOnly(entry("foo:struts", false), entry("foo:struts-core", true), entry("foo:struts-ui", false));
  }

  @Test
  public void simulate_bulk_update_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    Map<String, String> result = underTest.simulateBulkUpdateKey(dbSession, "A", "org.struts", "foo");

    assertThat(result)
      .hasSize(3)
      .containsOnly(entry("org.struts:struts", "foo:struts"), entry("org.struts:struts-core", "foo:struts-core"), entry("org.struts:struts-ui", "foo:struts-ui"));
  }
}
