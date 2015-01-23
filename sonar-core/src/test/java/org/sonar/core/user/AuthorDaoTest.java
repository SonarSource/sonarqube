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
package org.sonar.core.user;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class AuthorDaoTest extends AbstractDaoTestCase {

  private AuthorDao dao;

  @Before
  public void setUp() {
    dao = new AuthorDao(getMyBatis(), new ResourceDao(getMyBatis(), System2.INSTANCE));
  }

  @Test
  public void shouldSelectByLogin() {
    setupData("shouldSelectByLogin");

    AuthorDto authorDto = dao.selectByLogin("godin");
    assertThat(authorDto.getId()).isEqualTo(1L);
    assertThat(authorDto.getPersonId()).isEqualTo(13L);
    assertThat(authorDto.getLogin()).isEqualTo("godin");

    assertThat(dao.selectByLogin("simon")).isNull();
  }

  @Test
  public void shouldInsertAuthor() {
    setupData("shouldInsertAuthor");

    dao.insertAuthor("godin", 13L);

    checkTables("shouldInsertAuthor", new String[] {"created_at", "updated_at"}, "authors");
  }

  @Test
  public void countDeveloperLogins() {
    setupData("countDeveloperLogins");

    assertThat(dao.countDeveloperLogins(1L)).isEqualTo(2);
    assertThat(dao.countDeveloperLogins(98765L)).isEqualTo(0);
  }

  @Test
  public void shouldInsertAuthorAndDeveloper() throws Exception {
    setupData("shouldInsertAuthorAndDeveloper");

    String login = "developer@company.net";
    ResourceDto resourceDto = new ResourceDto().setName(login).setQualifier("DEV").setUuid("ABCD").setProjectUuid("ABCD").setModuleUuidPath(".");
    dao.insertAuthorAndDeveloper(login, resourceDto);

    checkTables("shouldInsertAuthorAndDeveloper",
      new String[] {"created_at", "updated_at", "copy_resource_id", "description", "enabled", "kee", "deprecated_kee", "path", "language", "long_name", "person_id", "root_id",
        "scope", "authorization_updated_at"},
      "authors", "projects");
  }

  @Test
  public void add_missing_module_uuid_path() throws Exception {
    setupData("add_missing_module_uuid_path");

    dao.insertAuthorAndDeveloper("developer@company.net", new ResourceDto().setName("developer@company.net").setQualifier("DEV").setUuid("ABCD").setProjectUuid("ABCD")
      .setModuleUuidPath(""));
    dao.insertAuthorAndDeveloper("developer2@company.net", new ResourceDto().setName("developer2@company.net").setQualifier("DEV").setUuid("BCDE").setProjectUuid("BCDE"));

    checkTables("add_missing_module_uuid_path",
      new String[] {"created_at", "updated_at", "copy_resource_id", "description", "enabled", "kee", "deprecated_kee", "path", "language", "long_name", "person_id", "root_id",
        "scope", "authorization_updated_at"},
      "authors", "projects");
  }

  @Test
  public void shouldPreventAuthorsDuplication() {
    setupData("shouldPreventAuthorsDuplication");

    try {
      dao.insertAuthor("godin", 20L);
      fail();
    } catch (RuntimeException ex) {
    }

    checkTables("shouldPreventAuthorsDuplication", new String[] {"created_at", "updated_at"}, "authors");
  }

  @Test
  public void shouldPreventAuthorsAndDevelopersDuplication() throws Exception {
    setupData("shouldPreventAuthorsAndDevelopersDuplication");

    String login = "developer@company.net";
    ResourceDto resourceDto = new ResourceDto().setName(login).setQualifier("DEV");

    try {
      dao.insertAuthorAndDeveloper("developer@company.net", resourceDto);
      fail();
    } catch (RuntimeException ex) {
    }

    checkTables("shouldPreventAuthorsAndDevelopersDuplication",
      new String[] {"created_at", "updated_at", "copy_resource_id", "description", "enabled", "kee", "deprecated_kee", "path", "language", "long_name", "person_id", "root_id",
        "scope", "authorization_updated_at"},
      "authors", "projects");
  }
}
