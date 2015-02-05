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

package org.sonar.server.platform;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.index.RuleDoc;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndexDefinition;
import org.sonar.server.view.index.ViewDoc;
import org.sonar.server.view.index.ViewIndexDefinition;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class BackendCleanupMediumTest {

  @ClassRule
  public static EsTester esTester = new EsTester();

  @ClassRule
  public static DbTester dbTester = new DbTester();

  BackendCleanup backendCleanup;

  @Before
  public void setUp() throws Exception {
    backendCleanup = new BackendCleanup(esTester.client(), dbTester.myBatis());
  }

  @Test
  public void clear_db() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    backendCleanup.clearDb();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
  }

  @Test
  public void clear_indexes() throws Exception {
    esTester.putDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE, IssueTesting.newDoc());
    esTester.putDocuments(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexType(), newRuleDoc());

    backendCleanup.clearIndexes();

    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE)).isEqualTo(0);
  }

  @Test
  public void clear_all() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    esTester.putDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE, IssueTesting.newDoc());
    esTester.putDocuments(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexType(), newRuleDoc());

    backendCleanup.clearAll();

    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE)).isEqualTo(0);
    assertThat(esTester.countDocuments(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexType())).isEqualTo(0);

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
  }

  @Test
  public void reset_data() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    esTester.putDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE, IssueTesting.newDoc());
    esTester.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE, new SourceLineDoc().setProjectUuid("ABCD").setFileUuid("BCDE"));
    esTester.putDocuments(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW, new ViewDoc().setUuid("CDEF").setProjects(newArrayList("DEFG")));
    esTester.putDocuments(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexType(), newRuleDoc());

    backendCleanup.resetData();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE)).isEqualTo(0);
    assertThat(esTester.countDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE)).isEqualTo(0);
    assertThat(esTester.countDocuments(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW)).isEqualTo(0);

    // Rules should not be removed
    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(1);
    assertThat(esTester.countDocuments(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexType())).isEqualTo(1);
  }

  private static RuleDoc newRuleDoc() {
    return new RuleDoc(ImmutableMap.<String, Object>of(RuleNormalizer.RuleField.RULE_KEY.field(), RuleTesting.XOO_X1));
  }
}
