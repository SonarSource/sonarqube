/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.es.EsTester;

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

  @Test
  public void index_nothing() {
    IssueIndexer indexer = createIndexer();
    indexer.index(Iterators.emptyIterator());
    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE)).isEqualTo(0L);
  }

  @Test
  public void index() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    IssueIndexer indexer = createIndexer();
    indexer.index();

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
  public void deleteProject_deletes_issues() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    IssueIndexer indexer = createIndexer();
    indexer.index();

    List<SearchHit> docs = esTester.getDocuments("issues", "issue");
    assertThat(esTester.countDocuments("issues", "issue")).isEqualTo(1);

    indexer.deleteProject("THE_PROJECT");

    assertThat(esTester.countDocuments("issues", "issue")).isZero();
  }

  @Test
  public void index_issues_from_project() {
    dbTester.prepareDbUnit(getClass(), "index_project.xml");

    IssueIndexer indexer = createIndexer();
    indexer.index("THE_PROJECT_1");

    verifyIssueKeys("ABCDE");
  }

  @Test
  public void delete_issues_by_keys() throws Exception {
    addIssue("P1", "Issue1");
    addIssue("P1", "Issue2");
    addIssue("P1", "Issue3");
    addIssue("P2", "Issue4");

    IssueIndexer indexer = createIndexer();
    verifyIssueKeys("Issue1", "Issue2", "Issue3", "Issue4");

    indexer.deleteByKeys("P1", asList("Issue1", "Issue2"));

    verifyIssueKeys("Issue3", "Issue4");
  }

  @Test
  public void delete_more_than_one_thousand_issues_by_keys() throws Exception {
    int numberOfIssues = 1010;
    List<String> keys = new ArrayList<>(numberOfIssues);
    IssueDoc[] issueDocs = new IssueDoc[numberOfIssues];
    for (int i = 0; i < numberOfIssues; i++) {
      String key = "Issue" + i;
      issueDocs[i] = newDoc().setKey(key).setProjectUuid(A_PROJECT_UUID);
      keys.add(key);
    }
    esTester.putDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE, issueDocs);
    IssueIndexer indexer = createIndexer();

    assertThat(esTester.countDocuments("issues", "issue")).isEqualTo(numberOfIssues);
    indexer.deleteByKeys(A_PROJECT_UUID, keys);
    assertThat(esTester.countDocuments("issues", "issue")).isZero();
  }

  @Test
  public void nothing_to_do_when_delete_issues_on_empty_list() throws Exception {
    addIssue("P1", "Issue1");
    addIssue("P1", "Issue2");
    addIssue("P1", "Issue3");

    IssueIndexer indexer = createIndexer();
    verifyIssueKeys("Issue1", "Issue2", "Issue3");

    indexer.deleteByKeys("P1", Collections.<String>emptyList());

    verifyIssueKeys("Issue1", "Issue2", "Issue3");
  }

  private IssueIndexer createIndexer() {
    return new IssueIndexer(system2, new DbClient(dbTester.database(), dbTester.myBatis()), esTester.client());
  }

  private void addIssue(String projectUuid, String issueKey) throws Exception {
    esTester.putDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE,
      newDoc().setKey(issueKey).setProjectUuid(projectUuid));
  }

  private void verifyIssueKeys(String... expectedKeys) {
    List<String> keys = FluentIterable.from(esTester.getDocuments("issues", "issue", IssueDoc.class))
      .transform(DocToKey.INSTANCE)
      .toList();
    assertThat(keys).containsOnly(expectedKeys);
  }

  private enum DocToKey implements Function<IssueDoc, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull IssueDoc input) {
      return input.key();
    }
  }
}
