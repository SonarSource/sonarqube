/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.issue.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.ProjectIndexer;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.issue.IssueDocTesting.newDoc;

public class IssueIndexerTest {

  private static final String A_PROJECT_UUID = "P1";

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings()));

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private IssueIndexer underTest = new IssueIndexer(system2, dbTester.getDbClient(), esTester.client());

  @Test
  public void index_nothing() {
    underTest.index(Collections.emptyIterator());

    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE)).isEqualTo(0L);
  }

  @Test
  public void index_all_issues() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    underTest.index();

    List<IssueDoc> docs = esTester.getDocuments("issues", "issue", IssueDoc.class);
    assertThat(docs).hasSize(1);
    IssueDoc doc = docs.get(0);
    assertThat(doc.key()).isEqualTo("ABCDE");
    assertThat(doc.projectUuid()).isEqualTo("THE_PROJECT");
    assertThat(doc.componentUuid()).isEqualTo("THE_FILE");
    assertThat(doc.moduleUuid()).isEqualTo("THE_PROJECT");
    assertThat(doc.modulePath()).isEqualTo(".THE_PROJECT.");
    assertThat(doc.directoryPath()).isEqualTo("src/main/java");
    assertThat(doc.severity()).isEqualTo("BLOCKER");
    assertThat(doc.ruleKey()).isEqualTo(RuleKey.of("squid", "AvoidCycles"));

    // functional date
    assertThat(doc.updateDate().getTime()).isEqualTo(1368828000000L);

    // technical date
    assertThat(doc.getTechnicalUpdateDate().getTime()).isEqualTo(1550000000000L);
  }

  @Test
  public void indexProject_creates_docs_of_specific_project() {
    dbTester.prepareDbUnit(getClass(), "index_project.xml");

    underTest.indexProject("THE_PROJECT_1", ProjectIndexer.Cause.NEW_ANALYSIS);

    verifyIssueKeys("ABCDE");
  }

  @Test
  public void indexProject_does_nothing_when_project_is_being_created() {
    dbTester.prepareDbUnit(getClass(), "index_project.xml");

    underTest.indexProject("THE_PROJECT_1", ProjectIndexer.Cause.PROJECT_CREATION);

    assertThat(esTester.countDocuments("issues", "issue")).isEqualTo(0);
  }

  @Test
  public void indexProject_does_nothing_when_project_key_is_being_renamed() {
    dbTester.prepareDbUnit(getClass(), "index_project.xml");

    underTest.indexProject("THE_PROJECT_1", ProjectIndexer.Cause.PROJECT_KEY_UPDATE);

    assertThat(esTester.countDocuments("issues", "issue")).isEqualTo(0);
  }

  @Test
  public void deleteProject_deletes_issues_of_a_specific_project() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    underTest.index();

    assertThat(esTester.countDocuments("issues", "issue")).isEqualTo(1);

    underTest.deleteProject("THE_PROJECT");

    assertThat(esTester.countDocuments("issues", "issue")).isZero();
  }

  @Test
  public void deleteByKeys_deletes_docs_by_keys() throws Exception {
    addIssue("P1", "Issue1");
    addIssue("P1", "Issue2");
    addIssue("P1", "Issue3");
    addIssue("P2", "Issue4");

    verifyIssueKeys("Issue1", "Issue2", "Issue3", "Issue4");

    underTest.deleteByKeys("P1", asList("Issue1", "Issue2"));

    verifyIssueKeys("Issue3", "Issue4");
  }

  @Test
  public void deleteByKeys_deletes_more_than_one_thousand_issues_by_keys() throws Exception {
    int numberOfIssues = 1010;
    List<String> keys = new ArrayList<>(numberOfIssues);
    IssueDoc[] issueDocs = new IssueDoc[numberOfIssues];
    for (int i = 0; i < numberOfIssues; i++) {
      String key = "Issue" + i;
      issueDocs[i] = newDoc().setKey(key).setProjectUuid(A_PROJECT_UUID);
      keys.add(key);
    }
    esTester.putDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE, issueDocs);

    assertThat(esTester.countDocuments("issues", "issue")).isEqualTo(numberOfIssues);
    underTest.deleteByKeys(A_PROJECT_UUID, keys);
    assertThat(esTester.countDocuments("issues", "issue")).isZero();
  }

  @Test
  public void nothing_to_do_when_delete_issues_on_empty_list() throws Exception {
    addIssue("P1", "Issue1");
    addIssue("P1", "Issue2");
    addIssue("P1", "Issue3");

    verifyIssueKeys("Issue1", "Issue2", "Issue3");

    underTest.deleteByKeys("P1", Collections.emptyList());

    verifyIssueKeys("Issue1", "Issue2", "Issue3");
  }

  private void addIssue(String projectUuid, String issueKey) throws Exception {
    esTester.putDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE,
      newDoc().setKey(issueKey).setProjectUuid(projectUuid));
  }

  private void verifyIssueKeys(String... expectedKeys) {
    List<IssueDoc> issues = esTester.getDocuments("issues", "issue", IssueDoc.class);
    assertThat(issues).extracting(IssueDoc::key).containsOnly(expectedKeys);
  }
}
