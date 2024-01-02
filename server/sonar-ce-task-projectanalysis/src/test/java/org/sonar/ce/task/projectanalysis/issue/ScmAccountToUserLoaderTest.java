/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ScmAccountToUserLoaderTest {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public LogTester logTester = new LogTester();

  private UserIndexer userIndexer = new UserIndexer(db.getDbClient(), es.client());

  @Test
  public void load_login_for_scm_account() {
    UserDto user = db.users().insertUser(u -> u.setScmAccounts(asList("charlie", "jesuis@charlie.com")));
    userIndexer.indexAll();

    UserIndex index = new UserIndex(es.client(), System2.INSTANCE);
    ScmAccountToUserLoader underTest = new ScmAccountToUserLoader(index);

    assertThat(underTest.load("missing")).isNull();
    assertThat(underTest.load("jesuis@charlie.com")).isEqualTo(user.getUuid());
  }

  @Test
  public void warn_if_multiple_users_share_the_same_scm_account() {
    db.users().insertUser(u -> u.setLogin("charlie").setScmAccounts(asList("charlie", "jesuis@charlie.com")));
    db.users().insertUser(u -> u.setLogin("another.charlie").setScmAccounts(asList("charlie")));
    userIndexer.indexAll();

    UserIndex index = new UserIndex(es.client(), System2.INSTANCE);
    ScmAccountToUserLoader underTest = new ScmAccountToUserLoader(index);

    assertThat(underTest.load("charlie")).isNull();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Multiple users share the SCM account 'charlie': another.charlie, charlie");
  }

  @Test
  public void load_by_multiple_scm_accounts_is_not_supported_yet() {
    UserIndex index = new UserIndex(es.client(), System2.INSTANCE);
    ScmAccountToUserLoader underTest = new ScmAccountToUserLoader(index);
    try {
      underTest.loadAll(emptyList());
      fail();
    } catch (UnsupportedOperationException ignored) {
    }
  }
}
