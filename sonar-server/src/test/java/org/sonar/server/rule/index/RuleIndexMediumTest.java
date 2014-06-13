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
package org.sonar.server.rule.index;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.debt.DebtTesting;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RuleIndexMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);
  DbClient db;
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void getByKey() throws InterruptedException {
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("javascript", "S001"));
    dao.insert(dbSession, ruleDto);
    dbSession.commit();

    Rule rule = index.getByKey(RuleKey.of("javascript", "S001"));

    assertThat(rule.htmlDescription()).isEqualTo(ruleDto.getDescription());
    assertThat(rule.key()).isEqualTo(ruleDto.getKey());

    // TODO
    // assertThat(rule.debtSubCharacteristicKey())
    // .isEqualTo(ruleDto.getDefaultSubCharacteristicId().toString());
    assertThat(rule.debtRemediationFunction().type().name())
      .isEqualTo(ruleDto.getRemediationFunction());

    assertThat(Sets.newHashSet(rule.tags())).isEqualTo(ruleDto.getTags());
    assertThat(Sets.newHashSet(rule.systemTags())).isEqualTo(ruleDto.getSystemTags());
  }

  @Test
  public void getByKey_null_if_not_found() throws InterruptedException {
    Rule rule = index.getByKey(RuleKey.of("javascript", "unknown"));

    assertThat(rule).isNull();
  }

  @Test
  public void global_facet_on_repositories_and_tags() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("php", "S001"))
      .setSystemTags(ImmutableSet.of("sysTag")))
      .setTags(ImmutableSet.<String>of());
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("php", "S002"))
      .setSystemTags(ImmutableSet.<String>of()))
      .setTags(ImmutableSet.of("tag1"));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "S002"))
      .setTags(ImmutableSet.of("tag1", "tag2")))
      .setSystemTags(ImmutableSet.<String>of());
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

    // Verify the value of a given facet
    Collection<FacetValue> repoFacets = result.getFacetValues("repositories");
    assertThat(repoFacets).hasSize(2);
    assertThat(Iterables.get(repoFacets, 0).getKey()).isEqualTo("php");
    assertThat(Iterables.get(repoFacets, 0).getValue()).isEqualTo(2);
    assertThat(Iterables.get(repoFacets, 1).getKey()).isEqualTo("javascript");
    assertThat(Iterables.get(repoFacets, 1).getValue()).isEqualTo(1);

    // Check that tag facet has both Tags and SystemTags values
    Collection<FacetValue> tagFacet = result.getFacetValues("tags");
    assertThat(tagFacet).hasSize(3);
    assertThat(Iterables.get(tagFacet, 0).getKey()).isEqualTo("tag1");
    assertThat(Iterables.get(tagFacet, 0).getValue()).isEqualTo(2);
  }

  @Test
  public void return_all_doc_fields_by_default() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "S001")));
    dbSession.commit();

    QueryOptions options = new QueryOptions().setFieldsToReturn(null);
    Result<Rule> results = index.search(new RuleQuery(), options);
    assertThat(results.getHits()).hasSize(1);
    Rule hit = Iterables.getFirst(results.getHits(), null);
    assertThat(hit.key()).isNotNull();
    assertThat(hit.htmlDescription()).isNotNull();
    assertThat(hit.name()).isNotNull();

    options = new QueryOptions().setFieldsToReturn(Collections.<String>emptyList());
    results = index.search(new RuleQuery(), options);
    assertThat(results.getHits()).hasSize(1);
    hit = Iterables.getFirst(results.getHits(), null);
    assertThat(hit.key()).isNotNull();
    assertThat(hit.htmlDescription()).isNotNull();
    assertThat(hit.name()).isNotNull();
  }

  @Test
  public void select_doc_fields_to_return() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "S001")));
    dbSession.commit();

    QueryOptions options = new QueryOptions();
    options.addFieldsToReturn(RuleNormalizer.RuleField.LANGUAGE.field(), RuleNormalizer.RuleField.STATUS.field());
    Result<Rule> results = index.search(new RuleQuery(), options);
    assertThat(results.getHits()).hasSize(1);

    Rule hit = Iterables.getFirst(results.getHits(), null);
    assertThat(hit.language()).isEqualTo("js");
    assertThat(hit.status()).isEqualTo(RuleStatus.READY);

    try {
      hit.htmlDescription();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Field htmlDesc not specified in query options");
    }
  }

  @Test
  public void search_name_by_query() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "S001"))
      .setName("testing the partial match and matching of rule"));
    dbSession.commit();

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
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "X001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("cobol", "X001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("php", "S002")));
    dbSession.commit();

    // key
    RuleQuery query = new RuleQuery().setQueryText("X001");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);

    // partial key does not match
    query = new RuleQuery().setQueryText("X00");
    // TODO fix non-partial match for Key search
    assertThat(index.search(query, new QueryOptions()).getHits()).isEmpty();

    // repo:key -> nice-to-have !
    query = new RuleQuery().setQueryText("javascript:X001");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);
  }

  @Test
  public void filter_by_key() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "X001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("cobol", "X001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("php", "S002")));
    dbSession.commit();

    // key
    RuleQuery query = new RuleQuery().setKey(RuleKey.of("javascript", "X001").toString());
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);

    // partial key does not match
    query = new RuleQuery().setKey("X001");
    // TODO fix non-partial match for Key search
    assertThat(index.search(query, new QueryOptions()).getHits()).isEmpty();
  }

  @Test
  public void search_all_rules() throws InterruptedException {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "S001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")));
    dbSession.commit();

    Result results = index.search(new RuleQuery(), new QueryOptions());

    assertThat(results.getTotal()).isEqualTo(2);
    assertThat(results.getHits()).hasSize(2);
  }

  @Test
  public void scroll_all_rules() throws InterruptedException {
    int max = 100;
    for (int i = 0; i < max; i++) {
      dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "scroll_" + i)));
    }
    dbSession.commit();

    Result results = index.search(new RuleQuery(), new QueryOptions().setScroll(true));

    assertThat(results.getTotal()).isEqualTo(max);
    assertThat(results.getHits()).hasSize(0);

    Iterator<Rule> it = results.scroll();
    int count = 0;
    while (it.hasNext()) {
      count++;
      it.next();
    }
    assertThat(count).isEqualTo(max);

  }

  @Test
  public void search_by_has_subChar() {
    CharacteristicDto char1 = DebtTesting.newCharacteristicDto("c1")
      .setEnabled(true)
      .setName("char1");
    db.debtCharacteristicDao().insert(char1, dbSession);
    dbSession.commit();

    CharacteristicDto char11 = DebtTesting.newCharacteristicDto("c11")
      .setEnabled(true)
      .setName("char11")
      .setParentId(char1.getId());
    db.debtCharacteristicDao().insert(char11, dbSession);

    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("findbugs", "S001"))
      .setSubCharacteristicId(char11.getId()));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("pmd", "S002")));
    dbSession.commit();

    // 0. assert base case
    assertThat(index.search(new RuleQuery(), new QueryOptions()).getTotal()).isEqualTo(2);
    assertThat(db.debtCharacteristicDao().selectCharacteristics()).hasSize(2);

    // 1. assert hasSubChar filter
    assertThat(index.search(new RuleQuery().setHasDebtCharacteristic(true), new QueryOptions()).getTotal())
      .isEqualTo(1);
  }

  @Test
  public void search_by_any_of_repositories() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("findbugs", "S001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("pmd", "S002")));
    dbSession.commit();

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
    dao.insert(dbSession,
      RuleTesting.newDto(RuleKey.of("java", "S001")).setLanguage("java"),
      RuleTesting.newDto(RuleKey.of("javascript", "S002")).setLanguage("js"));
    dbSession.commit();

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
  public void search_by_characteristics() throws InterruptedException {
    CharacteristicDto char1 = DebtTesting.newCharacteristicDto("char1");
    db.debtCharacteristicDao().insert(char1, dbSession);

    CharacteristicDto char11 = DebtTesting.newCharacteristicDto("char11")
      .setParentId(char1.getId());
    db.debtCharacteristicDao().insert(char11, dbSession);
    dbSession.commit();

    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001"))
      .setSubCharacteristicId(char11.getId()));

    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "S002")));

    dbSession.commit();

    RuleQuery query;
    Result<Rule> results;

    // 0. we have 2 rules in index
    results = index.search(new RuleQuery(), new QueryOptions());
    assertThat(results.getHits()).hasSize(2);

    // filter by non-subChar
    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of("toto"));
    assertThat(index.search(query, new QueryOptions()).getHits()).isEmpty();

    // filter by subChar
    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of(char11.getKey()));
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);

    // filter by Char
    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of(char1.getKey()));
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);

    // filter by Char and SubChar
    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of(char11.getKey(), char1.getKey()));
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);

    // match by Char
    query = new RuleQuery().setQueryText(char1.getKey());
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);

    // match by SubChar
    query = new RuleQuery().setQueryText(char11.getKey());
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);

    // match by SubChar & Char
    query = new RuleQuery().setQueryText(char11.getKey() + " " + char1.getKey());
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);
  }

  @Test
  public void search_by_any_of_severities() throws InterruptedException {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")).setSeverity(Severity.BLOCKER));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")).setSeverity(Severity.INFO));
    dbSession.commit();

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
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")).setStatus(RuleStatus.BETA));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")).setStatus(RuleStatus.READY));
    dbSession.commit();

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
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")).setName("abcd"));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")).setName("ABC"));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S003")).setName("FGH"));
    dbSession.commit();

    // ascending
    RuleQuery query = new RuleQuery().setSortField(RuleNormalizer.RuleField.NAME);
    Result<Rule> results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(3);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");
    assertThat(Iterables.getLast(results.getHits(), null).key().rule()).isEqualTo("S003");

    // descending
    query = new RuleQuery().setSortField(RuleNormalizer.RuleField.NAME).setAscendingSort(false);
    results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(3);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S003");
    assertThat(Iterables.getLast(results.getHits(), null).key().rule()).isEqualTo("S002");
  }

  @Test
  public void fail_sort_by_language() throws InterruptedException {
    try {
      // Sorting on a field not tagged as sortable
      new RuleQuery().setSortField(RuleNormalizer.RuleField.LANGUAGE);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Field 'lang' is not sortable!");
    }
  }

  @Test
  public void search_by_profile() throws InterruptedException {
    QualityProfileDto qualityProfileDto1 = QualityProfileDto.createFor("profile1", "java");
    QualityProfileDto qualityProfileDto2 = QualityProfileDto.createFor("profile2", "java");
    db.qualityProfileDao().insert(dbSession, qualityProfileDto1, qualityProfileDto2);

    RuleDto rule1 = RuleTesting.newDto(RuleKey.of("java", "S001"));
    RuleDto rule2 = RuleTesting.newDto(RuleKey.of("java", "S002"));
    RuleDto rule3 = RuleTesting.newDto(RuleKey.of("java", "S003"));
    dao.insert(dbSession, rule1, rule2, rule3);

    db.activeRuleDao().insert(
      dbSession,
      ActiveRuleDto.createFor(qualityProfileDto1, rule1).setSeverity("BLOCKER"),
      ActiveRuleDto.createFor(qualityProfileDto2, rule1).setSeverity("BLOCKER"),
      ActiveRuleDto.createFor(qualityProfileDto1, rule2).setSeverity("BLOCKER"));

    dbSession.commit();

    // 1. get all active rules.
    Result<Rule> result = index.search(new RuleQuery().setActivation(true),
      new QueryOptions());
    assertThat(result.getHits()).hasSize(2);

    // 2. get all inactive rules.
    result = index.search(new RuleQuery().setActivation(false),
      new QueryOptions());
    assertThat(result.getHits()).hasSize(1);
    assertThat(result.getHits().get(0).name()).isEqualTo(rule3.getName());

    // 3. get all rules not active on profile
    index.search(new RuleQuery().setActivation(false).setQProfileKey(qualityProfileDto2.getKey().toString()),
      new QueryOptions());
    // TODO
    assertThat(result.getHits()).hasSize(1);

    // 4. get all active rules on profile
    result = index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(qualityProfileDto2.getKey().toString()),
      new QueryOptions());
    assertThat(result.getHits()).hasSize(1);
    assertThat(result.getHits().get(0).name()).isEqualTo(rule1.getName());

  }

  @Test
  public void search_by_profile_and_inheritance() throws InterruptedException {
    QualityProfileDto qualityProfileDto1 = QualityProfileDto.createFor("profile1", "java");
    QualityProfileDto qualityProfileDto2 = QualityProfileDto.createFor("profile2", "java")
      .setParent(qualityProfileDto1.getName());
    db.qualityProfileDao().insert(dbSession, qualityProfileDto1, qualityProfileDto2);

    RuleDto rule1 = RuleTesting.newDto(RuleKey.of("java", "S001"));
    RuleDto rule2 = RuleTesting.newDto(RuleKey.of("java", "S002"));
    RuleDto rule3 = RuleTesting.newDto(RuleKey.of("java", "S003"));
    RuleDto rule4 = RuleTesting.newDto(RuleKey.of("java", "S004"));
    dao.insert(dbSession, rule1, rule2, rule3, rule4);

    db.activeRuleDao().insert(
      dbSession,
      ActiveRuleDto.createFor(qualityProfileDto1, rule1)
        .setSeverity("BLOCKER"),
      ActiveRuleDto.createFor(qualityProfileDto1, rule2)
        .setSeverity("BLOCKER"),
      ActiveRuleDto.createFor(qualityProfileDto1, rule3)
        .setSeverity("BLOCKER"),

      ActiveRuleDto.createFor(qualityProfileDto2, rule1)
        .setSeverity("MINOR")
        .setInheritance(ActiveRule.Inheritance.INHERITED.name()),
      ActiveRuleDto.createFor(qualityProfileDto2, rule2)
        .setSeverity("BLOCKER")
        .setInheritance(ActiveRule.Inheritance.OVERRIDES.name()),
      ActiveRuleDto.createFor(qualityProfileDto2, rule3)
        .setSeverity("BLOCKER")
        .setInheritance(ActiveRule.Inheritance.INHERITED.name())
      );

    dbSession.commit();

    // 0. get all rules
    Result<Rule> result = index.search(new RuleQuery(),
      new QueryOptions());
    assertThat(result.getHits()).hasSize(4);

    // 1. get all active rules
    result = index.search(new RuleQuery().setActivation(true),
      new QueryOptions());
    assertThat(result.getHits()).hasSize(3);

    // 2. get all inactive rules.
    result = index.search(new RuleQuery().setActivation(false),
      new QueryOptions());
    assertThat(result.getHits()).hasSize(1);
    assertThat(result.getHits().get(0).name()).isEqualTo(rule4.getName());

    // 3. get Inherited Rules on profile1
    result = index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(qualityProfileDto1.getKey().toString())
      .setInheritance(ImmutableSet.of(ActiveRule.Inheritance.INHERITED.name())),
      new QueryOptions()
      );
    assertThat(result.getHits()).hasSize(0);

    // 4. get Inherited Rules on profile2
    result = index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(qualityProfileDto2.getKey().toString())
      .setInheritance(ImmutableSet.of(ActiveRule.Inheritance.INHERITED.name())),
      new QueryOptions()
      );
    assertThat(result.getHits()).hasSize(2);

    // 5. get Overridden Rules on profile1
    result = index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(qualityProfileDto1.getKey().toString())
      .setInheritance(ImmutableSet.of(ActiveRule.Inheritance.OVERRIDES.name())),
      new QueryOptions()
      );
    assertThat(result.getHits()).hasSize(0);

    // 6. get Overridden Rules on profile2
    result = index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(qualityProfileDto2.getKey().toString())
      .setInheritance(ImmutableSet.of(ActiveRule.Inheritance.OVERRIDES.name())),
      new QueryOptions()
      );
    assertThat(result.getHits()).hasSize(1);

    // 7. get Inherited AND Overridden Rules on profile1
    result = index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(qualityProfileDto1.getKey().toString())
      .setInheritance(ImmutableSet.of(
        ActiveRule.Inheritance.INHERITED.name(), ActiveRule.Inheritance.OVERRIDES.name())),
      new QueryOptions()
      );
    assertThat(result.getHits()).hasSize(0);

    // 8. get Inherited AND Overridden Rules on profile2
    result = index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(qualityProfileDto2.getKey().toString())
      .setInheritance(ImmutableSet.of(
        ActiveRule.Inheritance.INHERITED.name(), ActiveRule.Inheritance.OVERRIDES.name())),
      new QueryOptions()
      );
    assertThat(result.getHits()).hasSize(3);
  }

  @Test
  public void complex_param_value() throws InterruptedException {
    String value = "//expression[primary/qualifiedIdentifier[count(IDENTIFIER) = 2]/IDENTIFIER[2]/@tokenValue = 'firstOf' and primary/identifierSuffix/arguments/expression[not(primary) or primary[not(qualifiedIdentifier) or identifierSuffix]]]";

    QualityProfileDto profile = QualityProfileDto.createFor("name", "Language");
    db.qualityProfileDao().insert(dbSession, profile);

    RuleDto rule = RuleTesting.newDto(RuleKey.of("java", "S001"));
    dao.insert(dbSession, rule);

    RuleParamDto param = RuleParamDto.createFor(rule)
      .setName("testing")
      .setType("STRING")
      .setDefaultValue(value);
    dao.addRuleParam(dbSession, rule, param);

    dbSession.commit();

    assertThat(index.getByKey(rule.getKey()).params()).hasSize(1);
    assertThat(index.getByKey(rule.getKey()).params().get(0).defaultValue()).isEqualTo(value);
  }

  @Test
  public void search_by_tag() throws InterruptedException {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")).setTags(ImmutableSet.of("tag1")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")).setTags(ImmutableSet.of("tag2")));
    dbSession.commit();

    // find all
    RuleQuery query = new RuleQuery();
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);

    // tag1 in query
    query = new RuleQuery().setQueryText("tag1");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);
    assertThat(Iterables.getFirst(index.search(query, new QueryOptions()).getHits(), null).tags()).containsExactly("tag1");

    // tag1 and tag2 in query
    query = new RuleQuery().setQueryText("tag1 tag2");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);

    // tag2 in filter
    query = new RuleQuery().setTags(ImmutableSet.of("tag2"));
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);
    assertThat(Iterables.getFirst(index.search(query, new QueryOptions()).getHits(), null).tags()).containsExactly("tag2");

    // tag2 in filter and tag1 tag2 in query
    query = new RuleQuery().setTags(ImmutableSet.of("tag2")).setQueryText("tag1");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(0);

    // tag2 in filter and tag1 in query
    query = new RuleQuery().setTags(ImmutableSet.of("tag2")).setQueryText("tag1 tag2");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);
    assertThat(Iterables.getFirst(index.search(query, new QueryOptions()).getHits(), null).tags()).containsExactly("tag2");

    // null list => no filter
    query = new RuleQuery().setTags(Collections.<String>emptySet());
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setTags(null);
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);
  }

  @Test
  public void search_by_is_template() throws InterruptedException {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")).setIsTemplate(false));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")).setIsTemplate(true));
    dbSession.commit();

    // find all
    RuleQuery query = new RuleQuery();
    Result<Rule> results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(2);

    // Only template
    query = new RuleQuery().setIsTemplate(true);
    results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");
    assertThat(Iterables.getFirst(results.getHits(), null).isTemplate()).isTrue();

    // Only not template
    query = new RuleQuery().setIsTemplate(false);
    results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).isTemplate()).isFalse();
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S001");

    // null => no filter
    query = new RuleQuery().setIsTemplate(null);
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);
  }

  @Test
  public void search_by_template_key() throws InterruptedException {
    RuleDto templateRule = RuleTesting.newDto(RuleKey.of("java", "S001")).setIsTemplate(true);
    dao.insert(dbSession, templateRule);
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001_MY_CUSTOM")).setTemplateId(templateRule.getId()));
    dbSession.commit();

    // find all
    RuleQuery query = new RuleQuery();
    Result<Rule> results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(2);

    // Only custom rule
    query = new RuleQuery().setTemplateKey("java:S001");
    results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S001_MY_CUSTOM");
    assertThat(Iterables.getFirst(results.getHits(), null).templateKey()).isEqualTo(RuleKey.of("java", "S001"));

    // null => no filter
    query = new RuleQuery().setTemplateKey(null);
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);
  }

  @Test
  public void search_by_template_key_with_params() throws InterruptedException {
    RuleDto templateRule = RuleTesting.newDto(RuleKey.of("java", "S001")).setIsTemplate(true);
    RuleParamDto ruleParamDto = RuleParamDto.createFor(templateRule).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*");
    dao.insert(dbSession, templateRule);
    dao.addRuleParam(dbSession, templateRule, ruleParamDto);

    RuleDto customRule = RuleTesting.newDto(RuleKey.of("java", "S001_MY_CUSTOM")).setTemplateId(templateRule.getId());
    RuleParamDto customRuleParam = RuleParamDto.createFor(customRule).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue("a.*");
    dao.insert(dbSession, customRule);
    dao.addRuleParam(dbSession, customRule, customRuleParam);
    dbSession.commit();

    // find all
    RuleQuery query = new RuleQuery();
    Result<Rule> results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(2);

    // get params
    assertThat(index.getByKey(templateRule.getKey()).params()).hasSize(1);
    assertThat(index.getByKey(customRule.getKey()).params()).hasSize(1);
  }

  @Test
  public void show_custom_rule() throws InterruptedException {
    RuleDto templateRule = RuleTesting.newDto(RuleKey.of("java", "S001")).setIsTemplate(true);
    dao.insert(dbSession, templateRule);
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001_MY_CUSTOM")).setTemplateId(templateRule.getId()));
    dbSession.commit();

    // find all
    RuleQuery query = new RuleQuery();
    Result<Rule> results = index.search(query, new QueryOptions());
    assertThat(results.getHits()).hasSize(2);

    // find custom rule
    assertThat(index.getByKey(RuleKey.of("java", "S001_MY_CUSTOM")).templateKey()).isEqualTo(RuleKey.of("java", "S001"));
  }

  @Test
  public void paging() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S003")));
    dbSession.commit();

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

  @Test
  public void available_since() throws InterruptedException {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")));
    dbSession.commit();
    Thread.sleep(500);
    Date since = new Date();
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")));
    dbSession.commit();

    // 0. find all rules;
    assertThat(index.search(new RuleQuery(), new QueryOptions()).getHits()).hasSize(2);

    // 1. find all rules available since a date;
    RuleQuery availableSinceQuery = new RuleQuery()
      .setAvailableSince(since);
    List<Rule> hits = index.search(availableSinceQuery, new QueryOptions()).getHits();
    assertThat(hits).hasSize(1);
    assertThat(hits.get(0).key()).isEqualTo(RuleKey.of("java", "S002"));

    // 2. find no new rules since tomorrow.
    RuleQuery availableSinceNowQuery = new RuleQuery()
      .setAvailableSince(DateUtils.addDays(since, 1));
    assertThat(index.search(availableSinceNowQuery, new QueryOptions()).getHits()).hasSize(0);
  }
}
