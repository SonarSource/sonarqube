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
package org.sonar.server.rule2;

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.check.Cardinality;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.search.Hit;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Results;
import org.sonar.server.tester.ServerTester;

import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class RuleSearchMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);

  @Before
  public void clear_data_store() {
    tester.clearDataStores();
  }

  @Test
  public void return_all_doc_fields_by_default() {
    dao.insert(newRuleDto(RuleKey.of("javascript", "S001")));
    index.refresh();

    QueryOptions options = new QueryOptions().setFieldsToReturn(null);
    Results results = index.search(new RuleQuery(), options);
    assertThat(results.getHits()).hasSize(1);
    Hit hit = Iterables.getFirst(results.getHits(), null);
    assertThat(hit.getFields()).hasSize(RuleNormalizer.RuleField.values().length);

    options = new QueryOptions().setFieldsToReturn(Collections.<String>emptyList());
    results = index.search(new RuleQuery(), options);
    assertThat(results.getHits()).hasSize(1);
    hit = Iterables.getFirst(results.getHits(), null);
    assertThat(hit.getFields()).hasSize(RuleNormalizer.RuleField.values().length);
  }

  @Test
  public void select_doc_fields_to_load() {
    dao.insert(newRuleDto(RuleKey.of("javascript", "S001")));
    index.refresh();

    QueryOptions options = new QueryOptions();
    options.addFieldsToReturn(RuleNormalizer.RuleField.LANGUAGE.key(), RuleNormalizer.RuleField.STATUS.key());
    Results results = index.search(new RuleQuery(), options);

    assertThat(results.getHits()).hasSize(1);
    Hit hit = Iterables.getFirst(results.getHits(), null);
    assertThat(hit.getFields()).hasSize(2);
    assertThat(hit.getFieldAsString(RuleNormalizer.RuleField.LANGUAGE.key())).isEqualTo("js");
    assertThat(hit.getFieldAsString(RuleNormalizer.RuleField.STATUS.key())).isEqualTo(RuleStatus.READY.name());
  }

  @Test
  public void search_by_name() {
    dao.insert(newRuleDto(RuleKey.of("javascript", "S001"))
      .setName("testing the partial match and matching of rule"));
    index.refresh();

    // substring
    RuleQuery query = new RuleQuery().setQueryText("test");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);

    // substring
    query = new RuleQuery().setQueryText("partial match");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);

    // case-insensitive
    query = new RuleQuery().setQueryText("TESTING");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);

    // not found
    query = new RuleQuery().setQueryText("not present");
    assertThat(index.search(query, new QueryOptions()).getHits()).isEmpty();
  }

  @Test
  public void search_key_by_query() {
    dao.insert(newRuleDto(RuleKey.of("javascript", "S001"))
      .setRuleKey("X001"));
    dao.insert(newRuleDto(RuleKey.of("cobol", "S001"))
      .setRuleKey("X001"));
    dao.insert(newRuleDto(RuleKey.of("php", "S002")));
    index.refresh();

    // key
    RuleQuery query = new RuleQuery().setQueryText("X001");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);

    // partial key does not match
    query = new RuleQuery().setQueryText("X00");
    assertThat(index.search(query, new QueryOptions()).getHits()).isEmpty();

    // repo:key -> nice-to-have !
    query = new RuleQuery().setQueryText("javascript:X001");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);
  }

  @Test
  public void search_all_rules() {
    dao.insert(newRuleDto(RuleKey.of("javascript", "S001")));
    dao.insert(newRuleDto(RuleKey.of("java", "S002")));
    index.refresh();

    Results results = index.search(new RuleQuery(), new QueryOptions());

    assertThat(results.getTotal()).isEqualTo(2);
    assertThat(results.getHits()).hasSize(2);
  }

  @Test
  public void search_rules_by_any_of_repositories() {
    dao.insert(newRuleDto(RuleKey.of("findbugs", "S001")));
    dao.insert(newRuleDto(RuleKey.of("pmd", "S002")));
    index.refresh();

    RuleQuery query = new RuleQuery().setRepositories(Arrays.asList("checkstyle", "pmd"));
    Results results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).getFieldAsString("key")).isEqualTo("S002");

    // no results
    query = new RuleQuery().setRepositories(Arrays.asList("checkstyle"));
    assertThat(index.search(query, new QueryOptions()).getHits()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setRepositories(Collections.<String>emptyList());
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);
  }

  @Test
  public void search_rules_by_any_of_languages() throws InterruptedException {
    dao.insert(newRuleDto(RuleKey.of("java", "S001")).setLanguage("java"));
    dao.insert(newRuleDto(RuleKey.of("javascript", "S002")).setLanguage("js"));
    index.refresh();

    RuleQuery query = new RuleQuery().setLanguages(Arrays.asList("cobol", "js"));
    Results results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).getFieldAsString("key")).isEqualTo("S002");

    // no results
    query = new RuleQuery().setLanguages(Arrays.asList("cpp"));
    assertThat(index.search(query, new QueryOptions()).getHits()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setLanguages(Collections.<String>emptyList());
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setLanguages(null);
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);
  }

  @Test
  public void search_rules_by_any_of_severities() throws InterruptedException {
    dao.insert(newRuleDto(RuleKey.of("java", "S001")).setSeverity(Severity.BLOCKER));
    dao.insert(newRuleDto(RuleKey.of("java", "S002")).setSeverity(Severity.INFO));
    index.refresh();

    RuleQuery query = new RuleQuery().setSeverities(Arrays.asList(Severity.INFO, Severity.MINOR));
    Results results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).getFieldAsString("key")).isEqualTo("S002");

    // no results
    query = new RuleQuery().setSeverities(Arrays.asList(Severity.MINOR));
    assertThat(index.search(query, new QueryOptions()).getHits()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setSeverities(Collections.<String>emptyList());
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setSeverities(null);
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);
  }

  @Test
  public void search_rules_by_any_of_statuses() throws InterruptedException {
    dao.insert(newRuleDto(RuleKey.of("java", "S001")).setStatus(RuleStatus.BETA.name()));
    dao.insert(newRuleDto(RuleKey.of("java", "S002")).setStatus(RuleStatus.READY.name()));
    index.refresh();

    RuleQuery query = new RuleQuery().setStatuses(Arrays.asList(RuleStatus.DEPRECATED, RuleStatus.READY));
    Results results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).getFieldAsString("key")).isEqualTo("S002");

    // no results
    query = new RuleQuery().setStatuses(Arrays.asList(RuleStatus.DEPRECATED));
    assertThat(index.search(query, new QueryOptions()).getHits()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setStatuses(Collections.<RuleStatus>emptyList());
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setStatuses(null);
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);
  }

  @Test
  public void sort_by_name() {
    dao.insert(newRuleDto(RuleKey.of("java", "S001")).setName("abcd"));
    dao.insert(newRuleDto(RuleKey.of("java", "S002")).setName("ABC"));
    dao.insert(newRuleDto(RuleKey.of("java", "S003")).setName("FGH"));
    index.refresh();

    // ascending
    RuleQuery query = new RuleQuery().setSortField(RuleQuery.SortField.NAME);
    Results results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(3);
    assertThat(Iterables.getFirst(results.getHits(), null).getFieldAsString("key")).isEqualTo("S002");
    assertThat(Iterables.getLast(results.getHits(), null).getFieldAsString("key")).isEqualTo("S003");

    // descending
    query = new RuleQuery().setSortField(RuleQuery.SortField.NAME).setAscendingSort(false);
    results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(3);
    assertThat(Iterables.getFirst(results.getHits(), null).getFieldAsString("key")).isEqualTo("S003");
    assertThat(Iterables.getLast(results.getHits(), null).getFieldAsString("key")).isEqualTo("S002");
  }

  @Test
  public void sort_by_language() {
    dao.insert(newRuleDto(RuleKey.of("java", "S001")).setLanguage("java"));
    dao.insert(newRuleDto(RuleKey.of("java", "S002")).setLanguage("php"));
    index.refresh();

    // ascending
    RuleQuery query = new RuleQuery().setSortField(RuleQuery.SortField.LANGUAGE);
    Results results = index.search(query, new QueryOptions());
    assertThat(Iterables.getFirst(results.getHits(), null).getFieldAsString("key")).isEqualTo("S001");
    assertThat(Iterables.getLast(results.getHits(), null).getFieldAsString("key")).isEqualTo("S002");

    // descending
    query = new RuleQuery().setSortField(RuleQuery.SortField.LANGUAGE).setAscendingSort(false);
    results = index.search(query, new QueryOptions());
    assertThat(Iterables.getFirst(results.getHits(), null).getFieldAsString("key")).isEqualTo("S002");
    assertThat(Iterables.getLast(results.getHits(), null).getFieldAsString("key")).isEqualTo("S001");
  }

  @Test
  public void paging() {
    dao.insert(newRuleDto(RuleKey.of("java", "S001")));
    dao.insert(newRuleDto(RuleKey.of("java", "S002")));
    dao.insert(newRuleDto(RuleKey.of("java", "S003")));
    index.refresh();

    // from 0 to 1 included
    QueryOptions options = new QueryOptions();
    options.setOffset(0).setLimit(2);
    Results results = index.search(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getHits()).hasSize(2);

    // from 0 to 9 included
    options.setOffset(0).setLimit(10);
    results = index.search(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getHits()).hasSize(3);

    // from 2 to 11 included
    options.setOffset(2).setLimit(10);
    results = index.search(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getHits()).hasSize(1);
  }

  private RuleDto newRuleDto(RuleKey ruleKey) {
    return new RuleDto()
      .setRuleKey(ruleKey.rule())
      .setRepositoryKey(ruleKey.repository())
      .setName("Rule " + ruleKey.rule())
      .setDescription("Description " + ruleKey.rule())
      .setStatus(RuleStatus.READY.toString())
      .setConfigKey("InternalKey" + ruleKey.rule())
      .setSeverity(Severity.INFO)
      .setCardinality(Cardinality.SINGLE)
      .setLanguage("js")
      .setRemediationFunction("linear")
      .setDefaultRemediationFunction("linear_offset")
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix")
      .setCreatedAt(DateUtils.parseDate("2013-12-16"))
      .setUpdatedAt(DateUtils.parseDate("2013-12-17"));
  }
}
