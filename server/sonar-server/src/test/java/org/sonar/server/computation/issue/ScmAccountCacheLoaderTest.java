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
package org.sonar.server.computation.issue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.server.es.EsTester;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ScmAccountCacheLoaderTest {

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new UserIndexDefinition(new Settings()));

  @Before
  public void setUp() throws Exception {
    esTester.truncateIndices();
  }

  @Test
  public void load_login_for_scm_account() throws Exception {
    esTester.putDocuments("users", "user", getClass(), "charlie.json");
    UserIndex index = new UserIndex(esTester.client());
    ScmAccountCacheLoader loader = new ScmAccountCacheLoader(index);

    assertThat(loader.load("missing")).isNull();
    assertThat(loader.load("jesuis@charlie.com")).isEqualTo("charlie");
  }

  @Test
  public void warn_if_multiple_users_share_same_scm_account() throws Exception {
    esTester.putDocuments("users", "user", getClass(), "charlie.json", "charlie_conflict.json");
    UserIndex index = new UserIndex(esTester.client());
    Logger log = mock(Logger.class);
    ScmAccountCacheLoader loader = new ScmAccountCacheLoader(index, log);

    assertThat(loader.load("charlie")).isNull();
    verify(log).warn("Multiple users share the SCM account 'charlie': charlie, another.charlie");
  }

  @Test
  public void load_by_multiple_scm_accounts_is_not_supported_yet() throws Exception {
    UserIndex index = new UserIndex(esTester.client());
    ScmAccountCacheLoader loader = new ScmAccountCacheLoader(index);
    try {
      loader.loadAll(Collections.<String>emptyList());
      fail();
    } catch (UnsupportedOperationException ignored) {
    }
  }
}
