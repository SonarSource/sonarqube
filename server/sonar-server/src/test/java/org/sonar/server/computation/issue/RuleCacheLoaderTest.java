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
import org.sonar.api.rule.RuleKey;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.db.RuleDao;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RuleCacheLoaderTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
  }

  @Test
  public void load_by_key() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new RuleDao());
    RuleCacheLoader loader = new RuleCacheLoader(dbClient);

    assertThat(loader.load(RuleKey.of("squid", "R001")).getName()).isEqualTo("Rule One");
    assertThat(loader.load(RuleKey.of("squid", "MISSING"))).isNull();
  }

  @Test
  public void load_by_keys_is_not_supported() throws Exception {
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new RuleDao());
    RuleCacheLoader loader = new RuleCacheLoader(dbClient);
    try {
      loader.loadAll(Collections.<RuleKey>emptyList());
      fail();
    } catch (UnsupportedOperationException e) {
      // see RuleDao#getByKeys()
    }
  }

}
