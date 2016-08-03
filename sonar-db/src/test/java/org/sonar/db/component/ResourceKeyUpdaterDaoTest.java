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

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;

public class ResourceKeyUpdaterDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbSession dbSession = db.getSession();
  ComponentDbTester componentDb = new ComponentDbTester(db);

  ResourceKeyUpdaterDao underTest = db.getDbClient().resourceKeyUpdaterDao();

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

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Impossible to update key: a resource with \"foo:struts-core\" key already exists.");

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
    componentDb.insertComponent(newFileDto(project).setKey("old-project-key:file"));
    String newLongProjectKey = Strings.repeat("a", 400);
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Component key length (405) is longer than the maximum authorized (400). '" + newLongProjectKey + ":file' was provided.");

    underTest.updateKey(project.uuid(), newLongProjectKey);
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

}
