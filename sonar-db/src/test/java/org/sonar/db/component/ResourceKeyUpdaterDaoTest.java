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

import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class ResourceKeyUpdaterDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  ResourceKeyUpdaterDao dao = dbTester.getDbClient().resourceKeyUpdaterDao();

  @Test
  public void shouldUpdateKey() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    dao.updateKey(2, "struts:core");

    dbTester.assertDbUnit(getClass(), "shouldUpdateKey-result.xml", "projects");
  }

  @Test
  public void shouldNotUpdateKey() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Impossible to update key: a resource with \"org.struts:struts-ui\" key already exists.");

    dao.updateKey(2, "org.struts:struts-ui");
  }

  @Test
  public void shouldBulkUpdateKey() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    dao.bulkUpdateKey(1, "org.struts", "org.apache.struts");

    dbTester.assertDbUnit(getClass(), "shouldBulkUpdateKey-result.xml", "projects");
  }

  @Test
  public void shouldBulkUpdateKeyOnOnlyOneSubmodule() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    dao.bulkUpdateKey(1, "struts-ui", "struts-web");

    dbTester.assertDbUnit(getClass(), "shouldBulkUpdateKeyOnOnlyOneSubmodule-result.xml", "projects");
  }

  @Test
  public void shouldFailBulkUpdateKeyIfKeyAlreadyExist() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Impossible to update key: a resource with \"foo:struts-core\" key already exists.");

    dao.bulkUpdateKey(1, "org.struts", "foo");
  }

  @Test
  public void shouldNotUpdateAllSubmodules() {
    dbTester.prepareDbUnit(getClass(), "shouldNotUpdateAllSubmodules.xml");

    dao.bulkUpdateKey(1, "org.struts", "org.apache.struts");

    dbTester.assertDbUnit(getClass(), "shouldNotUpdateAllSubmodules-result.xml", "projects");
  }

  @Test
  public void shouldCheckModuleKeysBeforeRenaming() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    Map<String, String> checkResults = dao.checkModuleKeysBeforeRenaming(1, "org.struts", "foo");
    assertThat(checkResults.size()).isEqualTo(3);
    assertThat(checkResults.get("org.struts:struts")).isEqualTo("foo:struts");
    assertThat(checkResults.get("org.struts:struts-core")).isEqualTo("#duplicate_key#");
    assertThat(checkResults.get("org.struts:struts-ui")).isEqualTo("foo:struts-ui");
  }

}
