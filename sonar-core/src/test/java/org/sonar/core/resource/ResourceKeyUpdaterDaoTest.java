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
package org.sonar.core.resource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceKeyUpdaterDaoTest extends AbstractDaoTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ResourceKeyUpdaterDao dao;

  @Before
  public void createDao() {
    dao = new ResourceKeyUpdaterDao(getMyBatis());
  }

  @Test
  public void shouldUpdateKey() {
    setupData("shared");

    dao.updateKey(2, "struts:core");

    checkTables("shouldUpdateKey", "projects");
  }

  @Test
  public void shouldNotUpdateKey() {
    setupData("shared");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Impossible to update key: a resource with \"org.struts:struts-ui\" key already exists.");

    dao.updateKey(2, "org.struts:struts-ui");
  }

  @Test
  public void shouldBulkUpdateKey() {
    setupData("shared");

    dao.bulkUpdateKey(1, "org.struts", "org.apache.struts");

    checkTables("shouldBulkUpdateKey", "projects");
  }

  @Test
  public void shouldBulkUpdateKeyOnOnlyOneSubmodule() {
    setupData("shared");

    dao.bulkUpdateKey(1, "struts-ui", "struts-web");

    checkTables("shouldBulkUpdateKeyOnOnlyOneSubmodule", "projects");
  }

  @Test
  public void shouldFailBulkUpdateKeyIfKeyAlreadyExist() {
    setupData("shared");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Impossible to update key: a resource with \"foo:struts-core\" key already exists.");

    dao.bulkUpdateKey(1, "org.struts", "foo");
  }

  @Test
  public void shouldNotUpdateAllSubmodules() throws Exception {
    setupData("shouldNotUpdateAllSubmodules");

    dao.bulkUpdateKey(1, "org.struts", "org.apache.struts");

    checkTables("shouldNotUpdateAllSubmodules", "projects");
  }

  @Test
  public void shouldCheckModuleKeysBeforeRenaming() {
    setupData("shared");

    Map<String, String> checkResults = dao.checkModuleKeysBeforeRenaming(1, "org.struts", "foo");
    assertThat(checkResults.size()).isEqualTo(3);
    assertThat(checkResults.get("org.struts:struts")).isEqualTo("foo:struts");
    assertThat(checkResults.get("org.struts:struts-core")).isEqualTo("#duplicate_key#");
    assertThat(checkResults.get("org.struts:struts-ui")).isEqualTo("foo:struts-ui");
  }

}
