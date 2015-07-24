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

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@Category(DbTests.class)
public class RuleCacheLoaderTest {

  @org.junit.Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Before
  public void setUp() {
    dbTester.truncateTables();
  }

  @Test
  public void load_by_key() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    RuleCacheLoader loader = new RuleCacheLoader(dbTester.getDbClient());

    Rule javaRule = loader.load(RuleKey.of("java", "JAV01"));
    assertThat(javaRule.getName()).isEqualTo("Java One");

    Rule jsRule = loader.load(RuleKey.of("js", "JS01"));
    assertThat(jsRule.getName()).isEqualTo("JS One");

    assertThat(loader.load(RuleKey.of("java", "MISSING"))).isNull();
  }

  @Test
  public void load_by_keys_is_not_supported() {
    RuleCacheLoader loader = new RuleCacheLoader(dbTester.getDbClient());
    try {
      loader.loadAll(Collections.<RuleKey>emptyList());
      fail();
    } catch (UnsupportedOperationException e) {
      // see RuleDao#getByKeys()
    }
  }

}
