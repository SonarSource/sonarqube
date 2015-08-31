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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.db.DbSession;
import org.sonar.db.debt.CharacteristicDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.debt.DebtTesting;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RuleIndexMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();
  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  protected DbClient db;

  RuleDao dao;
  RuleIndex index;
  // IndexClient index;
  DbSession dbSession;

  @Before
  public void before() {
    dao = tester.get(RuleDao.class);
    index = tester.get(RuleIndex.class);
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    // index = tester.get(IndexClient.class);
    dbSession = tester.get(DbClient.class).openSession(false);

  }

  @After
  public void after() {
    if (dbSession != null) {
      dbSession.close();
    }
  }

  @Test
  public void getByKey() {
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("javascript", "S001"));
    dao.insert(dbSession, ruleDto);
    dbSession.commit();

    Rule rule = index.getByKey(RuleKey.of("javascript", "S001"));

    assertThat(rule.htmlDescription()).isEqualTo(ruleDto.getDescription());
    assertThat(rule.key()).isEqualTo(ruleDto.getKey());

    assertThat(rule.debtRemediationFunction().type().name())
      .isEqualTo(ruleDto.getRemediationFunction());

    assertThat(Sets.newHashSet(rule.tags())).isEqualTo(ruleDto.getTags());
    assertThat(Sets.newHashSet(rule.systemTags())).isEqualTo(ruleDto.getSystemTags());
  }

  @Test
  public void getByKey_null_if_not_found() {
    Rule rule = index.getNullableByKey(RuleKey.of("javascript", "unknown"));

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

    // should not have any facet!
    RuleQuery query = new RuleQuery();
    Result result = index.search(query, new QueryContext(userSessionRule));
    assertThat(result.getFacets()).isEmpty();

    // should not have any facet on non matching query!
    result = index.search(new RuleQuery().setQueryText("aeiou"), new QueryContext(userSessionRule).addFacets(Arrays.asList("repositories")));
    assertThat(result.getFacets()).isEmpty();

    // Repositories Facet is preset
    result = index.search(query, new QueryContext(userSessionRule).addFacets(Arrays.asList("repositories", "tags")));
    assertThat(result.getFacets()).isNotNull();
    assertThat(result.getFacets()).hasSize(2);

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

    QueryContext options = new QueryContext(userSessionRule).setFieldsToReturn(null);
    Result<Rule> results = index.search(new RuleQuery(), options);
    assertThat(results.getHits()).hasSize(1);
    Rule hit = Iterables.getFirst(results.getHits(), null);
    assertThat(hit.key()).isNotNull();
    assertThat(hit.htmlDescription()).isNotNull();
    assertThat(hit.name()).isNotNull();

    options = new QueryContext(userSessionRule).setFieldsToReturn(Collections.<String>emptyList());
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

    QueryContext options = new QueryContext(userSessionRule);
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
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);

    // substring
    query = new RuleQuery().setQueryText("partial match");
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);

    // case-insensitive
    query = new RuleQuery().setQueryText("TESTING");
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);

    // not found
    query = new RuleQuery().setQueryText("not present");
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).isEmpty();
  }

  @Test
  public void search_key_by_query() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "X001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("cobol", "X001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("php", "S002")));
    dbSession.commit();

    // key
    RuleQuery query = new RuleQuery().setQueryText("X001");
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);

    // partial key does not match
    query = new RuleQuery().setQueryText("X00");
    // TODO fix non-partial match for Key search
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).isEmpty();

    // repo:key -> nice-to-have !
    query = new RuleQuery().setQueryText("javascript:X001");
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);
  }

  @Test
  public void filter_by_key() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "X001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("cobol", "X001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("php", "S002")));
    dbSession.commit();

    // key
    RuleQuery query = new RuleQuery().setKey(RuleKey.of("javascript", "X001").toString());
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);

    // partial key does not match
    query = new RuleQuery().setKey("X001");
    // TODO fix non-partial match for Key search
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).isEmpty();
  }

  @Test
  public void search_all_rules() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "S001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")));
    dbSession.commit();

    Result results = index.search(new RuleQuery(), new QueryContext(userSessionRule));

    assertThat(results.getTotal()).isEqualTo(2);
    assertThat(results.getHits()).hasSize(2);
  }

  @Test
  public void scroll_all_rules() {
    int max = 100;
    for (int i = 0; i < max; i++) {
      dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "scroll_" + i)));
    }
    dbSession.commit();

    Result results = index.search(new RuleQuery(), new QueryContext(userSessionRule).setScroll(true));

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
  public void search_by_has_debt_characteristic() {
    CharacteristicDto char1 = DebtTesting.newCharacteristicDto("c1")
      .setEnabled(true)
      .setName("char1");
    db.debtCharacteristicDao().insert(dbSession, char1);
    dbSession.commit();

    CharacteristicDto char11 = DebtTesting.newCharacteristicDto("c11")
      .setEnabled(true)
      .setName("char11")
      .setParentId(char1.getId());
    db.debtCharacteristicDao().insert(dbSession, char11);

    // Rule with default characteristic
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("findbugs", "S001"))
      .setSubCharacteristicId(null)
      .setRemediationFunction(null)
      .setDefaultSubCharacteristicId(char11.getId())
      .setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h"));
    // Rule with overridden characteristic
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("pmd", "S002"))
      .setSubCharacteristicId(char11.getId())
      .setRemediationFunction("LINEAR").setRemediationCoefficient("2h")
      .setDefaultSubCharacteristicId(null)
      .setDefaultRemediationFunction(null));
    // Rule without debt characteristic
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("xoo", "S001"))
      .setSubCharacteristicId(null)
      .setRemediationFunction(null).setRemediationCoefficient(null)
      .setDefaultSubCharacteristicId(null)
      .setDefaultRemediationFunction(null).setDefaultRemediationCoefficient(null));
    // Rule with disabled debt characteristic
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("xoo", "S002"))
      .setSubCharacteristicId(-1)
      .setRemediationFunction(null).setRemediationCoefficient(null)
      .setDefaultSubCharacteristicId(null)
      .setDefaultRemediationFunction(null).setDefaultRemediationCoefficient(null));
    dbSession.commit();

    assertThat(index.search(new RuleQuery().setHasDebtCharacteristic(null), new QueryContext(userSessionRule)).getTotal()).isEqualTo(4);
    assertThat(index.search(new RuleQuery().setHasDebtCharacteristic(true), new QueryContext(userSessionRule)).getTotal()).isEqualTo(2);
    assertThat(index.search(new RuleQuery().setHasDebtCharacteristic(false), new QueryContext(userSessionRule)).getTotal()).isEqualTo(2);
  }

  @Test
  public void facet_on_debt_characteristic() {
    CharacteristicDto char1 = DebtTesting.newCharacteristicDto("c1")
      .setEnabled(true)
      .setName("char1");
    db.debtCharacteristicDao().insert(dbSession, char1);
    dbSession.commit();

    CharacteristicDto char11 = DebtTesting.newCharacteristicDto("c11")
      .setEnabled(true)
      .setName("char11")
      .setParentId(char1.getId());
    db.debtCharacteristicDao().insert(dbSession, char11);

    // Rule with default characteristic
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("findbugs", "S001"))
      .setSubCharacteristicId(null)
      .setRemediationFunction(null)
      .setDefaultSubCharacteristicId(char11.getId())
      .setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h"));
    // Rule with overridden characteristic
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("pmd", "S002"))
      .setSubCharacteristicId(char11.getId())
      .setRemediationFunction("LINEAR").setRemediationCoefficient("2h")
      .setDefaultSubCharacteristicId(null)
      .setDefaultRemediationFunction(null));
    // Rule without debt characteristic
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("xoo", "S001"))
      .setSubCharacteristicId(null)
      .setRemediationFunction(null).setRemediationCoefficient(null)
      .setDefaultSubCharacteristicId(null)
      .setDefaultRemediationFunction(null).setDefaultRemediationCoefficient(null));
    // Rule with disabled debt characteristic
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("xoo", "S002"))
      .setSubCharacteristicId(-1)
      .setRemediationFunction(null).setRemediationCoefficient(null)
      .setDefaultSubCharacteristicId(null)
      .setDefaultRemediationFunction(null).setDefaultRemediationCoefficient(null));
    dbSession.commit();

    QueryContext withDebtCharFacet = new QueryContext(userSessionRule).addFacets(Arrays.asList(RuleIndex.FACET_DEBT_CHARACTERISTICS));

    // Facet show results on characs, subcharacs and uncharacterized rules 
    Result<Rule> result1 = index.search(new RuleQuery(), withDebtCharFacet);
    assertThat(result1.getFacetValues(RuleIndex.FACET_DEBT_CHARACTERISTICS)).containsOnly(
      new FacetValue("c1", 2L),
      new FacetValue("c11", 2L),
      new FacetValue("", 1L),
      new FacetValue("NONE", 1L)
    );

    // Facet is sticky when using a charac filter
    Result<Rule> result2 = index.search(new RuleQuery().setDebtCharacteristics(Arrays.asList("c1")), withDebtCharFacet);
    assertThat(result2.getFacetValues(RuleIndex.FACET_DEBT_CHARACTERISTICS)).containsOnly(
      new FacetValue("c1", 2L),
      new FacetValue("c11", 2L),
      new FacetValue("", 1L),
      new FacetValue("NONE", 1L)
    );

    // Facet is sticky when using a sub-charac filter
    Result<Rule> result3 = index.search(new RuleQuery().setDebtCharacteristics(Arrays.asList("c11")), withDebtCharFacet);
    assertThat(result3.getFacetValues(RuleIndex.FACET_DEBT_CHARACTERISTICS)).containsOnly(
      new FacetValue("c1", 2L),
      new FacetValue("c11", 2L),
      new FacetValue("", 1L),
      new FacetValue("NONE", 1L)
    );

    // Facet is sticky when using hasCharac filter
    Result<Rule> result4 = index.search(new RuleQuery().setHasDebtCharacteristic(false), withDebtCharFacet);
    assertThat(result4.getFacetValues(RuleIndex.FACET_DEBT_CHARACTERISTICS)).containsOnly(
      new FacetValue("c1", 2L),
      new FacetValue("c11", 2L),
      new FacetValue("", 1L),
      new FacetValue("NONE", 1L)
    );

    // Facet applies other filters
    Result<Rule> result5 = index.search(new RuleQuery().setRepositories(Arrays.asList("xoo")), withDebtCharFacet);
    assertThat(result5.getFacetValues(RuleIndex.FACET_DEBT_CHARACTERISTICS)).containsOnly(
      new FacetValue("", 1L),
      new FacetValue("NONE", 1L)
    );
    Result<Rule> result6 = index.search(new RuleQuery().setRepositories(Arrays.asList("findbugs")), withDebtCharFacet);
    assertThat(result6.getFacetValues(RuleIndex.FACET_DEBT_CHARACTERISTICS)).containsOnly(
      new FacetValue("c1", 1L),
      new FacetValue("c11", 1L)
    );
  }

  @Test
  public void search_by_any_of_repositories() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("findbugs", "S001")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("pmd", "S002")));
    dbSession.commit();

    RuleQuery query = new RuleQuery().setRepositories(Arrays.asList("checkstyle", "pmd"));
    Result<Rule> results = index.search(query, new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");

    // no results
    query = new RuleQuery().setRepositories(Arrays.asList("checkstyle"));
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setRepositories(Collections.<String>emptyList());
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);
  }

  @Test
  public void search_by_any_of_languages() {
    dao.insert(dbSession,
      RuleTesting.newDto(RuleKey.of("java", "S001")).setLanguage("java"),
      RuleTesting.newDto(RuleKey.of("javascript", "S002")).setLanguage("js"));
    dbSession.commit();

    RuleQuery query = new RuleQuery().setLanguages(Arrays.asList("cobol", "js"));
    Result<Rule> results = index.search(query, new QueryContext(userSessionRule));

    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");

    // no results
    query = new RuleQuery().setLanguages(Arrays.asList("cpp"));
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setLanguages(Collections.<String>emptyList());
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setLanguages(null);
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);
  }

  @Test
  public void search_by_characteristics() {
    CharacteristicDto char1 = DebtTesting.newCharacteristicDto("RELIABILITY");
    db.debtCharacteristicDao().insert(dbSession, char1);

    CharacteristicDto char11 = DebtTesting.newCharacteristicDto("SOFT_RELIABILITY")
      .setParentId(char1.getId());
    db.debtCharacteristicDao().insert(dbSession, char11);
    dbSession.commit();

    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001"))
      .setSubCharacteristicId(char11.getId()));

    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("javascript", "S002")));

    dbSession.commit();
    dbSession.clearCache();

    RuleQuery query;
    Result<Rule> results;

    // 0. we have 2 rules in index
    results = index.search(new RuleQuery(), new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(2);

    // filter by non-subChar
    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of("toto"));
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).isEmpty();

    // filter by subChar
    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of(char11.getKey()));
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);

    // filter by Char
    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of(char1.getKey()));
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);

    // filter by Char and SubChar
    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of(char11.getKey(), char1.getKey()));
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);

    // match by Char
    query = new RuleQuery().setQueryText(char1.getKey());
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);

    // match by SubChar
    query = new RuleQuery().setQueryText(char11.getKey());
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);

    // match by SubChar & Char
    query = new RuleQuery().setQueryText(char11.getKey() + " " + char1.getKey());
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);
  }

  @Test
  public void search_by_characteristics_with_default_and_overridden_char() {
    CharacteristicDto char1 = DebtTesting.newCharacteristicDto("RELIABILITY");
    db.debtCharacteristicDao().insert(dbSession, char1);

    CharacteristicDto char11 = DebtTesting.newCharacteristicDto("SOFT_RELIABILITY")
      .setParentId(char1.getId());
    db.debtCharacteristicDao().insert(dbSession, char11);
    dbSession.commit();

    CharacteristicDto char2 = DebtTesting.newCharacteristicDto("TESTABILITY");
    db.debtCharacteristicDao().insert(dbSession, char2);

    CharacteristicDto char21 = DebtTesting.newCharacteristicDto("UNIT_TESTABILITY")
      .setParentId(char2.getId());
    db.debtCharacteristicDao().insert(dbSession, char21);
    dbSession.commit();

    // Rule with only default sub characteristic -> should be find by char11 and char1
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001"))
      .setSubCharacteristicId(char11.getId())
      .setDefaultSubCharacteristicId(null));

    // Rule with only sub characteristic -> should be find by char11 and char1
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002"))
      .setSubCharacteristicId(null)
      .setDefaultSubCharacteristicId(char11.getId()));

    // Rule with both default sub characteristic and overridden sub characteristic -> should only be find by char21 and char2
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S003"))
      .setSubCharacteristicId(char21.getId()))
      .setDefaultSubCharacteristicId(char11.getId());

    // Rule with both default sub characteristic and overridden sub characteristic and with same values -> should be find by char11 and
    // char1
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S004"))
      .setSubCharacteristicId(char11.getId()))
      .setDefaultSubCharacteristicId(char11.getId());

    dbSession.commit();
    dbSession.clearCache();

    RuleQuery query;
    Result<Rule> results;

    // 0. we have 4 rules in index
    results = index.search(new RuleQuery(), new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(4);

    // filter by subChar
    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of(char11.getKey()));
    assertThat(ruleKeys(index.search(query, new QueryContext(userSessionRule)).getHits())).containsOnly("S001", "S002", "S004");

    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of(char21.getKey()));
    assertThat(ruleKeys(index.search(query, new QueryContext(userSessionRule)).getHits())).containsOnly("S003");

    // filter by Char
    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of(char1.getKey()));
    assertThat(ruleKeys(index.search(query, new QueryContext(userSessionRule)).getHits())).containsOnly("S001", "S002", "S004");

    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of(char2.getKey()));
    assertThat(ruleKeys(index.search(query, new QueryContext(userSessionRule)).getHits())).containsOnly("S003");

    // filter by Char and SubChar
    query = new RuleQuery().setDebtCharacteristics(ImmutableSet.of(char11.getKey(), char1.getKey(), char2.getKey(), char21.getKey()));
    assertThat(ruleKeys(index.search(query, new QueryContext(userSessionRule)).getHits())).containsOnly("S001", "S002", "S003", "S004");
  }

  @Test
  public void search_by_any_of_severities() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")).setSeverity(Severity.BLOCKER));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")).setSeverity(Severity.INFO));
    dbSession.commit();

    RuleQuery query = new RuleQuery().setSeverities(Arrays.asList(Severity.INFO, Severity.MINOR));
    Result<Rule> results = index.search(query, new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");

    // no results
    query = new RuleQuery().setSeverities(Arrays.asList(Severity.MINOR));
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setSeverities(Collections.<String>emptyList());
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setSeverities(null);
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);
  }

  @Test
  public void search_by_any_of_statuses() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")).setStatus(RuleStatus.BETA));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")).setStatus(RuleStatus.READY));
    dbSession.commit();

    RuleQuery query = new RuleQuery().setStatuses(Arrays.asList(RuleStatus.DEPRECATED, RuleStatus.READY));
    Result<Rule> results = index.search(query, new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");

    // no results
    query = new RuleQuery().setStatuses(Arrays.asList(RuleStatus.DEPRECATED));
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setStatuses(Collections.<RuleStatus>emptyList());
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setStatuses(null);
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);
  }

  @Test
  public void sort_by_name() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")).setName("abcd"));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")).setName("ABC"));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S003")).setName("FGH"));
    dbSession.commit();

    // ascending
    RuleQuery query = new RuleQuery().setSortField(RuleNormalizer.RuleField.NAME);
    Result<Rule> results = index.search(query, new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(3);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");
    assertThat(Iterables.getLast(results.getHits(), null).key().rule()).isEqualTo("S003");

    // descending
    query = new RuleQuery().setSortField(RuleNormalizer.RuleField.NAME).setAscendingSort(false);
    results = index.search(query, new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(3);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S003");
    assertThat(Iterables.getLast(results.getHits(), null).key().rule()).isEqualTo("S002");
  }

  @Test
  public void fail_sort_by_language() {
    try {
      // Sorting on a field not tagged as sortable
      new RuleQuery().setSortField(RuleNormalizer.RuleField.LANGUAGE);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Field 'lang' is not sortable");
    }
  }

  @Test
  public void search_by_profile() {
    QualityProfileDto qualityProfileDto1 = QProfileTesting.newXooP1();
    QualityProfileDto qualityProfileDto2 = QProfileTesting.newXooP2();
    db.qualityProfileDao().insert(dbSession, qualityProfileDto1, qualityProfileDto2);

    RuleDto rule1 = RuleTesting.newXooX1();
    RuleDto rule2 = RuleTesting.newXooX2();
    RuleDto rule3 = RuleTesting.newXooX3();
    dao.insert(dbSession, rule1, rule2, rule3);

    db.activeRuleDao().insert(
      dbSession,
      ActiveRuleDto.createFor(qualityProfileDto1, rule1).setSeverity("BLOCKER"),
      ActiveRuleDto.createFor(qualityProfileDto2, rule1).setSeverity("BLOCKER"),
      ActiveRuleDto.createFor(qualityProfileDto1, rule2).setSeverity("BLOCKER"));
    dbSession.commit();
    dbSession.clearCache();

    // 1. get all active rules.
    Result<Rule> result = index.search(new RuleQuery().setActivation(true),
      new QueryContext(userSessionRule));
    assertThat(result.getHits()).hasSize(2);

    // 2. get all inactive rules.
    result = index.search(new RuleQuery().setActivation(false),
      new QueryContext(userSessionRule));
    assertThat(result.getHits()).hasSize(1);
    assertThat(result.getHits().get(0).name()).isEqualTo(rule3.getName());

    // 3. get all rules not active on profile
    index.search(new RuleQuery().setActivation(false).setQProfileKey(qualityProfileDto2.getKey()),
      new QueryContext(userSessionRule));
    // TODO
    assertThat(result.getHits()).hasSize(1);

    // 4. get all active rules on profile
    result = index.search(new RuleQuery().setActivation(true)
        .setQProfileKey(qualityProfileDto2.getKey()),
      new QueryContext(userSessionRule));
    assertThat(result.getHits()).hasSize(1);
    assertThat(result.getHits().get(0).name()).isEqualTo(rule1.getName());

  }

  @Test
  public void search_by_profile_and_inheritance() {
    QualityProfileDto qualityProfileDto1 = QProfileTesting.newXooP1();
    QualityProfileDto qualityProfileDto2 = QProfileTesting.newXooP2().setParentKee(QProfileTesting.XOO_P1_KEY);
    db.qualityProfileDao().insert(dbSession, qualityProfileDto1, qualityProfileDto2);

    RuleDto rule1 = RuleTesting.newDto(RuleKey.of("xoo", "S001"));
    RuleDto rule2 = RuleTesting.newDto(RuleKey.of("xoo", "S002"));
    RuleDto rule3 = RuleTesting.newDto(RuleKey.of("xoo", "S003"));
    RuleDto rule4 = RuleTesting.newDto(RuleKey.of("xoo", "S004"));
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
      new QueryContext(userSessionRule));
    assertThat(result.getHits()).hasSize(4);

    // 1. get all active rules
    result = index.search(new RuleQuery().setActivation(true),
      new QueryContext(userSessionRule));
    assertThat(result.getHits()).hasSize(3);

    // 2. get all inactive rules.
    result = index.search(new RuleQuery().setActivation(false),
      new QueryContext(userSessionRule));
    assertThat(result.getHits()).hasSize(1);
    assertThat(result.getHits().get(0).name()).isEqualTo(rule4.getName());

    // 3. get Inherited Rules on profile1
    result = index.search(new RuleQuery().setActivation(true)
        .setQProfileKey(qualityProfileDto1.getKey())
        .setInheritance(ImmutableSet.of(ActiveRule.Inheritance.INHERITED.name())),
      new QueryContext(userSessionRule)
    );
    assertThat(result.getHits()).hasSize(0);

    // 4. get Inherited Rules on profile2
    result = index.search(new RuleQuery().setActivation(true)
        .setQProfileKey(qualityProfileDto2.getKey())
        .setInheritance(ImmutableSet.of(ActiveRule.Inheritance.INHERITED.name())),
      new QueryContext(userSessionRule)
    );
    assertThat(result.getHits()).hasSize(2);

    // 5. get Overridden Rules on profile1
    result = index.search(new RuleQuery().setActivation(true)
        .setQProfileKey(qualityProfileDto1.getKey())
        .setInheritance(ImmutableSet.of(ActiveRule.Inheritance.OVERRIDES.name())),
      new QueryContext(userSessionRule)
    );
    assertThat(result.getHits()).hasSize(0);

    // 6. get Overridden Rules on profile2
    result = index.search(new RuleQuery().setActivation(true)
        .setQProfileKey(qualityProfileDto2.getKey())
        .setInheritance(ImmutableSet.of(ActiveRule.Inheritance.OVERRIDES.name())),
      new QueryContext(userSessionRule)
    );
    assertThat(result.getHits()).hasSize(1);

    // 7. get Inherited AND Overridden Rules on profile1
    result = index.search(new RuleQuery().setActivation(true)
        .setQProfileKey(qualityProfileDto1.getKey())
        .setInheritance(ImmutableSet.of(
          ActiveRule.Inheritance.INHERITED.name(), ActiveRule.Inheritance.OVERRIDES.name())),
      new QueryContext(userSessionRule)
    );
    assertThat(result.getHits()).hasSize(0);

    // 8. get Inherited AND Overridden Rules on profile2
    result = index.search(new RuleQuery().setActivation(true)
        .setQProfileKey(qualityProfileDto2.getKey())
        .setInheritance(ImmutableSet.of(
          ActiveRule.Inheritance.INHERITED.name(), ActiveRule.Inheritance.OVERRIDES.name())),
      new QueryContext(userSessionRule)
    );
    assertThat(result.getHits()).hasSize(3);
  }

  @Test
  public void search_by_profile_and_active_severity() {
    QualityProfileDto qualityProfileDto1 = QProfileTesting.newXooP1();
    QualityProfileDto qualityProfileDto2 = QProfileTesting.newXooP2();
    db.qualityProfileDao().insert(dbSession, qualityProfileDto1, qualityProfileDto2);

    RuleDto rule1 = RuleTesting.newXooX1().setSeverity("MAJOR");
    RuleDto rule2 = RuleTesting.newXooX2().setSeverity("MINOR");
    RuleDto rule3 = RuleTesting.newXooX3().setSeverity("INFO");
    dao.insert(dbSession, rule1, rule2, rule3);

    db.activeRuleDao().insert(
      dbSession,
      ActiveRuleDto.createFor(qualityProfileDto1, rule1).setSeverity("BLOCKER"),
      ActiveRuleDto.createFor(qualityProfileDto2, rule1).setSeverity("BLOCKER"),
      ActiveRuleDto.createFor(qualityProfileDto1, rule2).setSeverity("CRITICAL"));
    dbSession.commit();
    dbSession.clearCache();

    // 1. get all active rules.
    Result<Rule> result = index.search(new RuleQuery().setActivation(true).setQProfileKey(qualityProfileDto1.getKey()),
      new QueryContext(userSessionRule));
    assertThat(result.getHits()).hasSize(2);

    // 2. get rules with active severity critical.
    result = index.search(new RuleQuery().setActivation(true).setQProfileKey(qualityProfileDto1.getKey()).setActiveSeverities(Arrays.asList("CRITICAL")),
      new QueryContext(userSessionRule).addFacets(Arrays.asList(RuleIndex.FACET_ACTIVE_SEVERITIES)));
    assertThat(result.getHits()).hasSize(1);
    assertThat(result.getHits().get(0).name()).isEqualTo(rule2.getName());
    // check stickyness of active severity facet
    assertThat(result.getFacetValues(RuleIndex.FACET_ACTIVE_SEVERITIES)).containsOnly(new FacetValue("BLOCKER", 1), new FacetValue("CRITICAL", 1));

    // 3. count activation severities of all active rules
    result = index.search(new RuleQuery(),
      new QueryContext(userSessionRule).addFacets(Arrays.asList(RuleIndex.FACET_ACTIVE_SEVERITIES)));
    assertThat(result.getHits()).hasSize(3);
    assertThat(result.getFacetValues(RuleIndex.FACET_ACTIVE_SEVERITIES)).containsOnly(new FacetValue("BLOCKER", 2), new FacetValue("CRITICAL", 1));
  }

  @Test
  public void complex_param_value() {
    String value = "//expression[primary/qualifiedIdentifier[count(IDENTIFIER) = 2]/IDENTIFIER[2]/@tokenValue = 'firstOf' and primary/identifierSuffix/arguments/expression[not(primary) or primary[not(qualifiedIdentifier) or identifierSuffix]]]";

    QualityProfileDto profile = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profile);

    RuleDto rule = RuleTesting.newXooX1();
    dao.insert(dbSession, rule);

    RuleParamDto param = RuleParamDto.createFor(rule)
      .setName("testing")
      .setType("STRING")
      .setDefaultValue(value);
    dao.insertRuleParam(dbSession, rule, param);

    dbSession.commit();

    assertThat(index.getByKey(rule.getKey()).params()).hasSize(1);
    assertThat(index.getByKey(rule.getKey()).params().get(0).defaultValue()).isEqualTo(value);
  }

  @Test
  public void search_by_tag() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")).setTags(ImmutableSet.of("tag1")));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")).setTags(ImmutableSet.of("tag2")));
    dbSession.commit();

    // find all
    RuleQuery query = new RuleQuery();
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);

    // tag1 in query
    query = new RuleQuery().setQueryText("tag1");
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);
    assertThat(Iterables.getFirst(index.search(query, new QueryContext(userSessionRule)).getHits(), null).tags()).containsExactly("tag1");

    // tag1 and tag2 in query
    query = new RuleQuery().setQueryText("tag1 tag2");
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);

    // tag2 in filter
    query = new RuleQuery().setTags(ImmutableSet.of("tag2"));
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);
    assertThat(Iterables.getFirst(index.search(query, new QueryContext(userSessionRule)).getHits(), null).tags()).containsExactly("tag2");

    // tag2 in filter and tag1 tag2 in query
    query = new RuleQuery().setTags(ImmutableSet.of("tag2")).setQueryText("tag1");
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(0);

    // tag2 in filter and tag1 in query
    query = new RuleQuery().setTags(ImmutableSet.of("tag2")).setQueryText("tag1 tag2");
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(1);
    assertThat(Iterables.getFirst(index.search(query, new QueryContext(userSessionRule)).getHits(), null).tags()).containsExactly("tag2");

    // null list => no filter
    query = new RuleQuery().setTags(Collections.<String>emptySet());
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setTags(null);
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);
  }

  @Test
  public void search_by_is_template() {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")).setIsTemplate(false));
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")).setIsTemplate(true));
    dbSession.commit();

    // find all
    RuleQuery query = new RuleQuery();
    Result<Rule> results = index.search(query, new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(2);

    // Only template
    query = new RuleQuery().setIsTemplate(true);
    results = index.search(query, new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S002");
    assertThat(Iterables.getFirst(results.getHits(), null).isTemplate()).isTrue();

    // Only not template
    query = new RuleQuery().setIsTemplate(false);
    results = index.search(query, new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).isTemplate()).isFalse();
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S001");

    // null => no filter
    query = new RuleQuery().setIsTemplate(null);
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);
  }

  @Test
  public void search_by_template_key() {
    RuleDto templateRule = RuleTesting.newDto(RuleKey.of("java", "S001")).setIsTemplate(true);
    dao.insert(dbSession, templateRule);
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001_MY_CUSTOM")).setTemplateId(templateRule.getId()));
    dbSession.commit();

    // find all
    RuleQuery query = new RuleQuery();
    Result<Rule> results = index.search(query, new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(2);

    // Only custom rule
    query = new RuleQuery().setTemplateKey("java:S001");
    results = index.search(query, new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(1);
    assertThat(Iterables.getFirst(results.getHits(), null).key().rule()).isEqualTo("S001_MY_CUSTOM");
    assertThat(Iterables.getFirst(results.getHits(), null).templateKey()).isEqualTo(RuleKey.of("java", "S001"));

    // null => no filter
    query = new RuleQuery().setTemplateKey(null);
    assertThat(index.search(query, new QueryContext(userSessionRule)).getHits()).hasSize(2);
  }

  @Test
  public void search_by_template_key_with_params() {
    RuleDto templateRule = RuleTesting.newDto(RuleKey.of("java", "S001")).setIsTemplate(true);
    RuleParamDto ruleParamDto = RuleParamDto.createFor(templateRule).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*");
    dao.insert(dbSession, templateRule);
    dao.insertRuleParam(dbSession, templateRule, ruleParamDto);

    RuleDto customRule = RuleTesting.newDto(RuleKey.of("java", "S001_MY_CUSTOM")).setTemplateId(templateRule.getId());
    RuleParamDto customRuleParam = RuleParamDto.createFor(customRule).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue("a.*");
    dao.insert(dbSession, customRule);
    dao.insertRuleParam(dbSession, customRule, customRuleParam);
    dbSession.commit();

    // find all
    RuleQuery query = new RuleQuery();
    Result<Rule> results = index.search(query, new QueryContext(userSessionRule));
    assertThat(results.getHits()).hasSize(2);

    // get params
    assertThat(index.getByKey(templateRule.getKey()).params()).hasSize(1);
    assertThat(index.getByKey(customRule.getKey()).params()).hasSize(1);
  }

  @Test
  public void show_custom_rule() {
    RuleDto templateRule = RuleTesting.newDto(RuleKey.of("java", "S001")).setIsTemplate(true);
    dao.insert(dbSession, templateRule);
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001_MY_CUSTOM")).setTemplateId(templateRule.getId()));
    dbSession.commit();

    // find all
    RuleQuery query = new RuleQuery();
    Result<Rule> results = index.search(query, new QueryContext(userSessionRule));
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
    QueryContext options = new QueryContext(userSessionRule);
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

    // from 2 to 11 included
    options.setOffset(2).setLimit(0);
    results = index.search(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getHits()).hasSize(0);
  }

  @Test
  public void available_since() throws InterruptedException {
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S001")));
    dbSession.commit();

    Date since = new Date();
    dao.insert(dbSession, RuleTesting.newDto(RuleKey.of("java", "S002")));
    dbSession.commit();

    // 0. find all rules;
    assertThat(index.search(new RuleQuery(), new QueryContext(userSessionRule)).getHits()).hasSize(2);

    // 1. find all rules available since a date;
    RuleQuery availableSinceQuery = new RuleQuery()
      .setAvailableSince(since);
    List<Rule> hits = index.search(availableSinceQuery, new QueryContext(userSessionRule)).getHits();
    assertThat(hits).hasSize(1);
    assertThat(hits.get(0).key()).isEqualTo(RuleKey.of("java", "S002"));

    // 2. find no new rules since tomorrow.
    RuleQuery availableSinceNowQuery = new RuleQuery()
      .setAvailableSince(DateUtils.addDays(since, 1));
    assertThat(index.search(availableSinceNowQuery, new QueryContext(userSessionRule)).getHits()).hasSize(0);
  }

  @Test
  public void scroll_byIds() {
    Set<Integer> ids = new HashSet<>();
    for (int i = 0; i < 150; i++) {
      RuleDto rule = RuleTesting.newDto(RuleKey.of("scroll", "r_" + i));
      dao.insert(dbSession, rule);
      dbSession.commit();
      ids.add(rule.getId());
    }
    List<Rule> rules = index.getByIds(ids);
    assertThat(rules).hasSize(ids.size());
  }

  @Test
  public void search_protected_chars() {
    String nameWithProtectedChars = "ja#va&sc\"r:ipt";

    RuleDto ruleDto = RuleTesting.newXooX1().setName(nameWithProtectedChars);
    dao.insert(dbSession, ruleDto);
    dbSession.commit();

    Rule rule = index.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.name()).isEqualTo(nameWithProtectedChars);

    RuleQuery protectedCharsQuery = new RuleQuery().setQueryText(nameWithProtectedChars);
    List<Rule> results = index.search(protectedCharsQuery, new QueryContext(userSessionRule)).getHits();
    assertThat(results).hasSize(1);
    assertThat(results.get(0).key()).isEqualTo(RuleTesting.XOO_X1);
  }

  @Test
  public void sticky_facets() {

    dao.insert(dbSession,
      RuleTesting.newDto(RuleKey.of("xoo", "S001")).setLanguage("java").setTags(ImmutableSet.<String>of()),
      RuleTesting.newDto(RuleKey.of("xoo", "S002")).setLanguage("java").setTags(ImmutableSet.<String>of()),
      RuleTesting.newDto(RuleKey.of("xoo", "S003")).setLanguage("java").setTags(ImmutableSet.<String>of("T1", "T2")),
      RuleTesting.newDto(RuleKey.of("xoo", "S011")).setLanguage("cobol").setTags(ImmutableSet.<String>of()),
      RuleTesting.newDto(RuleKey.of("xoo", "S012")).setLanguage("cobol").setTags(ImmutableSet.<String>of()),
      RuleTesting.newDto(RuleKey.of("foo", "S013")).setLanguage("cobol").setTags(ImmutableSet.<String>of("T3", "T4")),
      RuleTesting.newDto(RuleKey.of("foo", "S111")).setLanguage("cpp").setTags(ImmutableSet.<String>of()),
      RuleTesting.newDto(RuleKey.of("foo", "S112")).setLanguage("cpp").setTags(ImmutableSet.<String>of()),
      RuleTesting.newDto(RuleKey.of("foo", "S113")).setLanguage("cpp").setTags(ImmutableSet.<String>of("T2", "T3")));
    dbSession.commit();

    // 0 assert Base
    assertThat(index.countAll()).isEqualTo(9);
    assertThat(index.search(new RuleQuery(), new QueryContext(userSessionRule)).getHits()).hasSize(9);

    // 1 Facet with no filters at all
    Map<String, Collection<FacetValue>> facets = index.search(new RuleQuery(), new QueryContext(userSessionRule).addFacets(Arrays.asList("languages", "repositories", "tags"))).getFacets();
    assertThat(facets.keySet()).hasSize(3);
    assertThat(facets.get(RuleIndex.FACET_LANGUAGES)).extracting("key").containsOnly("cpp", "java", "cobol");
    assertThat(facets.get(RuleIndex.FACET_REPOSITORIES)).extracting("key").containsOnly("xoo", "foo");
    assertThat(facets.get(RuleIndex.FACET_TAGS)).extracting("key").containsOnly("systag1", "systag2", "T1", "T2", "T3", "T4");

    // 2 Facet with a language filter
    // -- lang facet should still have all language
    Result<Rule> result = index.search(new RuleQuery()
      .setLanguages(ImmutableList.<String>of("cpp"))
      , new QueryContext(userSessionRule).addFacets(Arrays.asList("languages", "repositories", "tags")));
    assertThat(result.getHits()).hasSize(3);
    assertThat(result.getFacets()).hasSize(3);
    assertThat(result.getFacets().get(RuleIndex.FACET_LANGUAGES)).extracting("key").containsOnly("cpp", "java", "cobol");

    // 3 facet with 2 filters
    // -- lang facet for tag T2
    // -- tag facet for lang cpp
    // -- repository for cpp & T2
    result = index.search(new RuleQuery()
      .setLanguages(ImmutableList.<String>of("cpp"))
      .setTags(ImmutableList.<String>of("T2"))
      , new QueryContext(userSessionRule).addFacets(Arrays.asList("languages", "repositories", "tags")));
    assertThat(result.getHits()).hasSize(1);
    assertThat(result.getFacets().keySet()).hasSize(3);
    assertThat(result.getFacets().get(RuleIndex.FACET_LANGUAGES)).extracting("key").containsOnly("cpp", "java");
    assertThat(result.getFacets().get(RuleIndex.FACET_REPOSITORIES)).extracting("key").containsOnly("foo");
    assertThat(result.getFacets().get(RuleIndex.FACET_TAGS)).extracting("key").containsOnly("systag1", "systag2", "T2", "T3");

    // 4 facet with 2 filters
    // -- lang facet for tag T2
    // -- tag facet for lang cpp & java
    // -- repository for (cpp || java) & T2
    result = index.search(new RuleQuery()
      .setLanguages(ImmutableList.<String>of("cpp", "java"))
      .setTags(ImmutableList.<String>of("T2"))
      , new QueryContext(userSessionRule).addFacets(Arrays.asList("languages", "repositories", "tags")));
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getFacets().keySet()).hasSize(3);
    assertThat(result.getFacets().get(RuleIndex.FACET_LANGUAGES)).extracting("key").containsOnly("cpp", "java");
    assertThat(result.getFacets().get(RuleIndex.FACET_REPOSITORIES)).extracting("key").containsOnly("foo", "xoo");
    assertThat(result.getFacets().get(RuleIndex.FACET_TAGS)).extracting("key").containsOnly("systag1", "systag2", "T1", "T2", "T3");
  }

  private static List<String> ruleKeys(List<Rule> rules) {
    return newArrayList(Iterables.transform(rules, new Function<Rule, String>() {
      @Override
      public String apply(@Nullable Rule input) {
        return input != null ? input.key().rule() : null;
      }
    }));
  }
}
