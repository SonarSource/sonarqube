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
package org.sonar.db.user;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceDto;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@Category(DbTests.class)
public class AuthorDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  AuthorDao dao = dbTester.getDbClient().authorDao();

  @Test
  public void shouldSelectByLogin() {
    dbTester.prepareDbUnit(getClass(), "shouldSelectByLogin.xml");

    AuthorDto authorDto = dao.selectByLogin("godin");
    assertThat(authorDto.getId()).isEqualTo(1L);
    assertThat(authorDto.getPersonId()).isEqualTo(13L);
    assertThat(authorDto.getLogin()).isEqualTo("godin");

    assertThat(dao.selectByLogin("simon")).isNull();
  }

  @Test
  public void shouldInsertAuthor() {
    dbTester.prepareDbUnit(getClass(), "shouldInsertAuthor.xml");

    dao.insertAuthor("godin", 13L);

    dbTester.assertDbUnit(getClass(), "shouldInsertAuthor-result.xml", new String[] {"created_at", "updated_at"}, "authors");
  }

  @Test
  public void countDeveloperLogins() {
    dbTester.prepareDbUnit(getClass(), "countDeveloperLogins.xml");

    assertThat(dao.countDeveloperLogins(1L)).isEqualTo(2);
    assertThat(dao.countDeveloperLogins(98765L)).isEqualTo(0);
  }

  @Test
  public void shouldInsertAuthorAndDeveloper() {
    dbTester.prepareDbUnit(getClass(), "shouldInsertAuthorAndDeveloper.xml");

    String login = "developer@company.net";
    ResourceDto resourceDto = new ResourceDto().setName(login).setQualifier("DEV").setUuid("ABCD").setProjectUuid("ABCD").setModuleUuidPath(".");
    dao.insertAuthorAndDeveloper(login, resourceDto);

    dbTester.assertDbUnit(getClass(), "shouldInsertAuthorAndDeveloper-result.xml",
      new String[] {"created_at", "updated_at", "copy_resource_id", "description", "enabled", "kee", "deprecated_kee", "path", "language", "long_name", "person_id", "root_id",
        "scope", "authorization_updated_at"},
      "authors", "projects");
  }

  @Test
  public void add_missing_module_uuid_path() {
    dbTester.prepareDbUnit(getClass(), "add_missing_module_uuid_path.xml");

    dao.insertAuthorAndDeveloper("developer@company.net", new ResourceDto().setKey("developer").setName("developer@company.net").setQualifier("DEV").setUuid("ABCD")
      .setProjectUuid("ABCD")
      .setModuleUuidPath(""));
    dao.insertAuthorAndDeveloper("developer2@company.net", new ResourceDto().setKey("developer2").setName("developer2@company.net").setQualifier("DEV").setUuid("BCDE")
      .setProjectUuid("BCDE"));

    dbTester.assertDbUnit(getClass(), "add_missing_module_uuid_path-result.xml",
      new String[] {"created_at", "updated_at", "copy_resource_id", "description", "enabled", "kee", "deprecated_kee", "path", "language", "long_name", "person_id", "root_id",
        "scope", "authorization_updated_at"},
      "authors", "projects");
  }

  @Test
  public void shouldPreventAuthorsDuplication() {
    dbTester.prepareDbUnit(getClass(), "shouldPreventAuthorsDuplication.xml");

    try {
      dao.insertAuthor("godin", 20L);
      fail();
    } catch (RuntimeException ex) {
    }

    dbTester.assertDbUnit(getClass(), "shouldPreventAuthorsDuplication-result.xml", new String[] {"created_at", "updated_at"}, "authors");
  }

  @Test
  public void shouldPreventAuthorsAndDevelopersDuplication() {
    dbTester.prepareDbUnit(getClass(), "shouldPreventAuthorsAndDevelopersDuplication.xml");

    String login = "developer@company.net";
    ResourceDto resourceDto = new ResourceDto().setName(login).setQualifier("DEV");

    try {
      dao.insertAuthorAndDeveloper("developer@company.net", resourceDto);
      fail();
    } catch (RuntimeException ex) {
    }

    dbTester.assertDbUnit(getClass(), "shouldPreventAuthorsAndDevelopersDuplication-result.xml",
      new String[] {"created_at", "updated_at", "copy_resource_id", "description", "enabled", "kee", "deprecated_kee", "path", "language", "long_name", "person_id", "root_id",
        "scope", "authorization_updated_at"},
      "authors", "projects");
  }
}
