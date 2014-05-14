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
package org.sonar.server.rule2.index;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.rule2.Rule;
import org.sonar.server.rule2.persistence.RuleDao;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;

import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class RuleIndexMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester()
    .setProperty("sonar.es.http.port","9200");

  MyBatis myBatis = tester.get(MyBatis.class);
  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDataStores();
    dbSession = myBatis.openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void insert_rule() throws InterruptedException {
    RuleDto ruleDto = newRuleDto(RuleKey.of("javascript", "S001"));
    dao.insert(ruleDto, dbSession);
    dbSession.commit();

    index.refresh();


    Rule rule = index.getByKey(RuleKey.of("javascript", "S001"));

    System.out.println("rule = " + rule);

    assertThat(rule.htmlDescription()).isEqualTo(ruleDto.getDescription());
    assertThat(rule.key()).isEqualTo(ruleDto.getKey());
//
//    assertThat(rule.debtSubCharacteristicKey())
//      .isEqualTo(ruleDto.getDefaultSubCharacteristicId().toString());
    assertThat(rule.debtRemediationFunction().type().name())
      .isEqualTo(ruleDto.getRemediationFunction());


//
//    assertThat(rule.tags()).containsExactly(ruleDto.getTags());
//    assertThat(rule.systemTags()).containsExactly(ruleDto.getSystemTags());


  }


  @Test
  public void facet_test_with_repository() {
    dao.insert(newRuleDto(RuleKey.of("javascript", "S001")).setRuleKey("X001"), dbSession);
    dao.insert(newRuleDto(RuleKey.of("cobol", "S001")).setRuleKey("X001"), dbSession);
    dao.insert(newRuleDto(RuleKey.of("php", "S002")), dbSession);
    dbSession.commit();
    index.refresh();

    // should not have any facet!
    RuleQuery query = new RuleQuery();
    Result result = index.search(query, new QueryOptions().setFacet(false));
    assertThat(result.getFacets()).isEmpty();

    // Repositories Facet is preset
    result = index.search(query, new QueryOptions().setFacet(true));

    assertThat(result.getFacets()).isNotNull();
    assertThat(result.getFacets()).hasSize(3);
    assertThat(result.getFacet("Repositories").size()).isEqualTo(3);
    assertThat(result.getFacetKeys("Repositories"))
      .contains("javascript", "cobol", "php");
  }

  @Test
  public void return_all_doc_fields_by_default() {
    dao.insert(newRuleDto(RuleKey.of("javascript", "S001")), dbSession);
    dbSession.commit();
    index.refresh();

    QueryOptions options = new QueryOptions().setFieldsToReturn(null);
    Result<Rule> results = index.search(new RuleQuery(), options);
    assertThat(results.getHits()).hasSize(1);
    Rule hit = Iterables.getFirst(results.getHits(), null);

    options = new QueryOptions().setFieldsToReturn(Collections.<String>emptyList());
    results = index.search(new RuleQuery(), options);
    assertThat(results.getHits()).hasSize(1);
    hit = Iterables.getFirst(results.getHits(), null);
  }

  @Test
  public void select_doc_fields_to_return() {
    dao.insert(newRuleDto(RuleKey.of("javascript", "S001")), dbSession);
    dbSession.commit();
    index.refresh();

    QueryOptions options = new QueryOptions();
    options.addFieldsToReturn(RuleNormalizer.RuleField.LANGUAGE.key(), RuleNormalizer.RuleField.STATUS.key());
    Result<Rule> results = index.search(new RuleQuery(), options);
    assertThat(results.getHits()).hasSize(1);

    Rule hit = Iterables.getFirst(results.getHits(), null);
    assertThat(hit.language()).isEqualTo("js");
    assertThat(hit.status()).isEqualTo(RuleStatus.READY);
    assertThat(hit.htmlDescription()).isNull();
  }

  @Test
  public void search_name_by_query() {
    dao.insert(newRuleDto(RuleKey.of("javascript", "S001"))
      .setName("testing the partial match and matching of rule"), dbSession);
    dbSession.commit();
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
      .setRuleKey("X001"), dbSession);
    dao.insert(newRuleDto(RuleKey.of("cobol", "S001"))
      .setRuleKey("X001"), dbSession);
    dao.insert(newRuleDto(RuleKey.of("php", "S002")), dbSession);
    dbSession.commit();
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
    dao.insert(newRuleDto(RuleKey.of("javascript", "S001")), dbSession);
    dao.insert(newRuleDto(RuleKey.of("java", "S002")), dbSession);
    dbSession.commit();
    index.refresh();

    Result results = index.search(new RuleQuery(), new QueryOptions());

    assertThat(results.getTotal()).isEqualTo(2);
    assertThat(results.getHits()).hasSize(2);
  }

  @Test
  public void search_by_any_of_repositories() {
    dao.insert(newRuleDto(RuleKey.of("findbugs", "S001")), dbSession);
    dao.insert(newRuleDto(RuleKey.of("pmd", "S002")), dbSession);
    dbSession.commit();
    index.refresh();

    RuleQuery query = new RuleQuery().setRepositories(Arrays.asList("checkstyle", "pmd"));
    Result<Rule> results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");

    // no results
    query = new RuleQuery().setRepositories(Arrays.asList("checkstyle"));
    assertThat(index.search(query, new QueryOptions()).getHits()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setRepositories(Collections.<String>emptyList());
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);
  }

  @Test
  public void search_by_any_of_languages() throws InterruptedException {
    dao.insert(newRuleDto(RuleKey.of("java", "S001")).setLanguage("java"), dbSession);
    dao.insert(newRuleDto(RuleKey.of("javascript", "S002")).setLanguage("js"), dbSession);
    dbSession.commit();
    index.refresh();

    RuleQuery query = new RuleQuery().setLanguages(Arrays.asList("cobol", "js"));
    Result<Rule> results = index.search(query, new QueryOptions());

    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");

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
  public void search_by_any_of_severities() throws InterruptedException {
    dao.insert(newRuleDto(RuleKey.of("java", "S001")).setSeverity(Severity.BLOCKER), dbSession);
    dao.insert(newRuleDto(RuleKey.of("java", "S002")).setSeverity(Severity.INFO), dbSession);
    dbSession.commit();
    index.refresh();

    RuleQuery query = new RuleQuery().setSeverities(Arrays.asList(Severity.INFO, Severity.MINOR));
    Result<Rule> results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");

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
  public void search_by_any_of_statuses() throws InterruptedException {
    dao.insert(newRuleDto(RuleKey.of("java", "S001")).setStatus(RuleStatus.BETA.name()), dbSession);
    dao.insert(newRuleDto(RuleKey.of("java", "S002")).setStatus(RuleStatus.READY.name()), dbSession);
    dbSession.commit();
    index.refresh();

    RuleQuery query = new RuleQuery().setStatuses(Arrays.asList(RuleStatus.DEPRECATED, RuleStatus.READY));
    Result<Rule> results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");

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
    dao.insert(newRuleDto(RuleKey.of("java", "S001")).setName("abcd"), dbSession);
    dao.insert(newRuleDto(RuleKey.of("java", "S002")).setName("ABC"), dbSession);
    dao.insert(newRuleDto(RuleKey.of("java", "S003")).setName("FGH"), dbSession);
    dbSession.commit();
    index.refresh();

    // ascending
    RuleQuery query = new RuleQuery().setSortField(RuleQuery.SortField.NAME);
    Result<Rule> results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(3);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");
    assertThat(Iterables.getLast(results.getHits(), null).key().rule()).isEqualTo("S003");

    // descending
    query = new RuleQuery().setSortField(RuleQuery.SortField.NAME).setAscendingSort(false);
    results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(3);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S003");
    assertThat(Iterables.getLast(results.getHits(), null).key().rule()).isEqualTo("S002");
  }

  @Test
  public void sort_by_language() {
    dao.insert(newRuleDto(RuleKey.of("java", "S001")).setLanguage("java"), dbSession);
    dao.insert(newRuleDto(RuleKey.of("java", "S002")).setLanguage("php"), dbSession);
    dbSession.commit();
    index.refresh();

    // ascending
    RuleQuery query = new RuleQuery().setSortField(RuleQuery.SortField.LANGUAGE);
    Result<Rule> results = index.search(query, new QueryOptions());
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S001");
    assertThat(Iterables.getLast(results.getHits(), null).key().rule()).isEqualTo("S002");

    // descending
    query = new RuleQuery().setSortField(RuleQuery.SortField.LANGUAGE).setAscendingSort(false);
    results = index.search(query, new QueryOptions());
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");
    assertThat(Iterables.getLast(results.getHits(), null).key().rule()).isEqualTo("S001");
  }

  @Test
  public void search_by_tag() {
    dao.insert(newRuleDto(RuleKey.of("java", "S001")).setTags(ImmutableSet.of("tag1")), dbSession);
    dao.insert(newRuleDto(RuleKey.of("java", "S002")).setTags(ImmutableSet.of("tag2")), dbSession);
    dbSession.commit();
    index.refresh();

    // find all
    RuleQuery query = new RuleQuery();
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);

    // tag1 in query
    query = new RuleQuery().setQueryText("tag1");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);
    assertThat(Iterables.getFirst(index.search(query, new QueryOptions()).getHits(),null).tags()).containsExactly("tag1");

    // tag1 and tag2 in query
    query = new RuleQuery().setQueryText("tag1 tag2");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);

    // tag2 in filter
    query = new RuleQuery().setTags(ImmutableSet.of("tag2"));
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);
    assertThat(Iterables.getFirst(index.search(query, new QueryOptions()).getHits(),null).tags()).containsExactly("tag2");

    // tag2 in filter and tag1 tag2 in query
    query = new RuleQuery().setTags(ImmutableSet.of("tag2")).setQueryText("tag1");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(0);

    // tag2 in filter and tag1 in query
    query = new RuleQuery().setTags(ImmutableSet.of("tag2")).setQueryText("tag1 tag2");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);
    assertThat(Iterables.getFirst(index.search(query, new QueryOptions()).getHits(),null).tags()).containsExactly("tag2");

    // null list => no filter
    query = new RuleQuery().setTags(Collections.<String>emptySet());
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setTags(null);
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);
  }

  @Test
  public void paging() {
    dao.insert(newRuleDto(RuleKey.of("java", "S001")), dbSession);
    dao.insert(newRuleDto(RuleKey.of("java", "S002")), dbSession);
    dao.insert(newRuleDto(RuleKey.of("java", "S003")), dbSession);
    dbSession.commit();
    index.refresh();

    // from 0 to 1 included
    QueryOptions options = new QueryOptions();
    options.setOffset(0).setLimit(2);
    Result results = index.search(new RuleQuery(), options);
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
      .setRemediationFunction("LINEAR")
      .setDefaultRemediationFunction("linear_offset")
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix");
  }
}
