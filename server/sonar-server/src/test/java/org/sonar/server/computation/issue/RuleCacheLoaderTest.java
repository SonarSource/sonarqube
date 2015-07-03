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
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.DbTester;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.test.DbTests;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class RuleCacheLoaderTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @org.junit.Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Before
  public void setUp() {
    dbTester.truncateTables();
  }

  @Test
  public void load_by_key() {
    BatchReport.Metadata metadata = BatchReport.Metadata.newBuilder()
      .addAllActiveRuleKey(asList("java:JAV01")).build();
    reportReader.setMetadata(metadata);

    dbTester.prepareDbUnit(getClass(), "shared.xml");
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new RuleDao(mock(System2.class)));
    RuleCacheLoader loader = new RuleCacheLoader(dbClient, reportReader);

    Rule javaRule = loader.load(RuleKey.of("java", "JAV01"));
    assertThat(javaRule.getName()).isEqualTo("Java One");
    assertThat(javaRule.isActivated()).isTrue();

    Rule jsRule = loader.load(RuleKey.of("js", "JS01"));
    assertThat(jsRule.getName()).isEqualTo("JS One");
    assertThat(jsRule.isActivated()).isFalse();

    assertThat(loader.load(RuleKey.of("java", "MISSING"))).isNull();
  }

  @Test
  public void load_by_keys_is_not_supported() {
    reportReader.setMetadata(BatchReport.Metadata.newBuilder().build());

    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new RuleDao(mock(System2.class)));
    RuleCacheLoader loader = new RuleCacheLoader(dbClient, reportReader);
    try {
      loader.loadAll(Collections.<RuleKey>emptyList());
      fail();
    } catch (UnsupportedOperationException e) {
      // see RuleDao#getByKeys()
    }
  }

}
