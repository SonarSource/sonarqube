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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.IndexProperties;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RuleIndexMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester()
    .setProperty(IndexProperties.HTTP_PORT,"9200");

  MyBatis myBatis = tester.get(MyBatis.class);
  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);
  DbClient dbClient = tester.get(DbClient.class);
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndEs();
    dbSession = myBatis.openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void getByKey() throws InterruptedException {
    RuleDto ruleDto = newRuleDto(RuleKey.of("javascript", "S001"));
    dao.insert(dbSession, ruleDto);
    dbSession.commit();

    Rule rule = index.getByKey(RuleKey.of("javascript", "S001"));

    assertThat(rule.htmlDescription()).isEqualTo(ruleDto.getDescription());
    assertThat(rule.key()).isEqualTo(ruleDto.getKey());

//    assertThat(rule.debtSubCharacteristicKey())
//      .isEqualTo(ruleDto.getDefaultSubCharacteristicId().toString());
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
  public void global_facet_on_repositories() {
    dao.insert(dbSession, newRuleDto(RuleKey.of("javascript", "S001")).setRuleKey("X001"));
    dao.insert(dbSession, newRuleDto(RuleKey.of("php", "S001"))
      .setSystemTags(ImmutableSet.of("sysTag")));
    dao.insert(dbSession, newRuleDto(RuleKey.of("javascript", "S002")).setRuleKey("X002")
      .setTags(ImmutableSet.of("tag1")));
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
    assertThat(Iterables.get(repoFacets, 0).getKey()).isEqualTo("javascript");
    assertThat(Iterables.get(repoFacets, 0).getValue()).isEqualTo(2);
    assertThat(Iterables.get(repoFacets, 1).getKey()).isEqualTo("php");
    assertThat(Iterables.get(repoFacets, 1).getValue()).isEqualTo(1);

    // Check that tag facet has both Tags and SystemTags values
    Collection<FacetValue> tagFacet = result.getFacetValues("tags");
    assertThat(tagFacet).hasSize(2);
  }

  @Test
  public void return_all_doc_fields_by_default() {
    dao.insert(dbSession, newRuleDto(RuleKey.of("javascript", "S001")));
    dbSession.commit();


    QueryOptions options = new QueryOptions().setFieldsToReturn(null);
    Result<Rule> results = index.search(new RuleQuery(), options);
    assertThat(results.getHits()).hasSize(1);
    Rule hit = Iterables.getFirst(results.getHits(), null);
    // TODO complete

    options = new QueryOptions().setFieldsToReturn(Collections.<String>emptyList());
    results = index.search(new RuleQuery(), options);
    assertThat(results.getHits()).hasSize(1);
    hit = Iterables.getFirst(results.getHits(), null);
    // TODO complete
  }

  @Test
  public void select_doc_fields_to_return() {
    dao.insert(dbSession, newRuleDto(RuleKey.of("javascript", "S001")));
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
    dao.insert(dbSession, newRuleDto(RuleKey.of("javascript", "S001"))
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
    dao.insert(dbSession, newRuleDto(RuleKey.of("javascript", "S001"))
      .setRuleKey("X001"));
    dao.insert(dbSession, newRuleDto(RuleKey.of("cobol", "S001"))
      .setRuleKey("X001"));
    dao.insert(dbSession, newRuleDto(RuleKey.of("php", "S002")));
    dbSession.commit();


    // key
    RuleQuery query = new RuleQuery().setQueryText("X001");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(2);

    // partial key does not match
    query = new RuleQuery().setQueryText("X00");
    //TODO fix non-partial match for Key search
//    assertThat(index.search(query, new QueryOptions()).getHits()).isEmpty();

    // repo:key -> nice-to-have !
    query = new RuleQuery().setQueryText("javascript:X001");
    assertThat(index.search(query, new QueryOptions()).getHits()).hasSize(1);
  }

  @Test
  public void search_all_rules() throws InterruptedException {
    dao.insert(dbSession, newRuleDto(RuleKey.of("javascript", "S001")));
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S002")));
    dbSession.commit();

    Result results = index.search(new RuleQuery(), new QueryOptions());

    assertThat(results.getTotal()).isEqualTo(2);
    assertThat(results.getHits()).hasSize(2);
  }

  @Test
  public void search_by_any_of_repositories() {
    dao.insert(dbSession, newRuleDto(RuleKey.of("findbugs", "S001")));
    dao.insert(dbSession, newRuleDto(RuleKey.of("pmd", "S002")));
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
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S001")).setLanguage("java"));
    dao.insert(dbSession, newRuleDto(RuleKey.of("javascript", "S002")).setLanguage("js"));
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

    CharacteristicDto char1 = new CharacteristicDto().setName("char1")
      .setKey("char1")
      .setEnabled(true);
    dbClient.debtCharacteristicDao().insert(char1, dbSession);
    dbSession.commit();

    CharacteristicDto char11 = new CharacteristicDto().setName("char11")
      .setKey("char11")
      .setEnabled(true)
      .setParentId(char1.getId());
    dbClient.debtCharacteristicDao().insert(char11, dbSession);
    dbSession.commit();

    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S001"))
      .setSubCharacteristicId(char11.getId()));

    dao.insert(dbSession, newRuleDto(RuleKey.of("javascript", "S002")));

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
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S001")).setSeverity(Severity.BLOCKER));
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S002")).setSeverity(Severity.INFO));
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
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S001")).setStatus(RuleStatus.BETA));
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S002")).setStatus(RuleStatus.READY));
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
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S001")).setName("abcd"));
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S002")).setName("ABC"));
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S003")).setName("FGH"));
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

  @Test(expected = IllegalStateException.class)
  public void fail_sort_by_language() throws InterruptedException {

    try {
      // Sorting on a field not tagged as sortable
      RuleQuery query = new RuleQuery().setSortField(RuleNormalizer.RuleField.LANGUAGE);
      fail();
    } catch (IllegalStateException e){
      assertThat(e.getMessage()).isEqualTo("Field 'lang' is not sortable!");
    }
  }

  @Test
  public void search_by_profile() throws InterruptedException {
    QualityProfileDto qualityProfileDto1 = QualityProfileDto.createFor("profile1", "java");
    QualityProfileDto qualityProfileDto2 = QualityProfileDto.createFor("profile2", "java");
    dbClient.qualityProfileDao().insert(qualityProfileDto1);
    dbClient.qualityProfileDao().insert(qualityProfileDto2);

    RuleDto rule1 = newRuleDto(RuleKey.of("java", "S001"));
    dao.insert(dbSession, rule1);
    RuleDto rule2 = newRuleDto(RuleKey.of("java", "S002"));
    dao.insert(dbSession, rule2);
    RuleDto rule3 = newRuleDto(RuleKey.of("java", "S003"));
    dao.insert(dbSession, rule3);

    dbClient.activeRuleDao().insert(
      dbSession, ActiveRuleDto.createFor(qualityProfileDto1, rule1)
        .setSeverity("BLOCKER")
    );

    dbClient.activeRuleDao().insert(
      dbSession, ActiveRuleDto.createFor(qualityProfileDto2, rule1)
        .setSeverity("BLOCKER")
    );

    dbClient.activeRuleDao().insert(
      dbSession, ActiveRuleDto.createFor(qualityProfileDto1, rule2)
        .setSeverity("BLOCKER")
    );


    dbSession.commit();


    RuleResult result;

    // 1. get all active rules.
    result = index.search(new RuleQuery().setActivation(true),
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
    assertThat(result.getRules()).hasSize(1);

    // 4. get all active rules on profile
    result = index.search(new RuleQuery().setActivation(true)
        .setQProfileKey(qualityProfileDto2.getKey().toString()),
      new QueryOptions()
    );
    assertThat(result.getRules()).hasSize(1);
    assertThat(result.getHits().get(0).name()).isEqualTo(rule1.getName());

  }

  @Test
  public void complex_param_value() {
    String value = "//expression[primary/qualifiedIdentifier[count(IDENTIFIER) = 2]/IDENTIFIER[2]/@tokenValue = 'firstOf' and primary/identifierSuffix/arguments/expression[not(primary) or primary[not(qualifiedIdentifier) or identifierSuffix]]]";

    QualityProfileDto profile = QualityProfileDto.createFor("name", "Language");
    dbClient.qualityProfileDao().insert(dbSession, profile);

    RuleDto rule = newRuleDto(RuleKey.of("java", "S001"));
    dao.insert(dbSession, rule);

    RuleParamDto param = RuleParamDto.createFor(rule)
      .setName("testing")
      .setType("STRING")
      .setDefaultValue(value);
    dao.addRuleParam(rule, param, dbSession);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity("BLOCKER");

    ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(param);
    dbClient.activeRuleDao().insert(dbSession, activeRule);
    dbClient.activeRuleDao().addParam(activeRule, activeRuleParam, dbSession);
    dbSession.commit();

    assertThat(index.getByKey(rule.getKey()).params().get(0).defaultValue()).isEqualTo(value);


  }


  @Test
  public void search_by_tag() throws InterruptedException {
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S001")).setTags(ImmutableSet.of("tag1")));
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S002")).setTags(ImmutableSet.of("tag2")));
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
  public void paging() {
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S001")));
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S002")));
    dao.insert(dbSession, newRuleDto(RuleKey.of("java", "S003")));
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

  private RuleDto newRuleDto(RuleKey ruleKey) {
    return new RuleDto()
      .setRuleKey(ruleKey.rule())
      .setRepositoryKey(ruleKey.repository())
      .setName("Rule " + ruleKey.rule())
      .setDescription("Description " + ruleKey.rule())
      .setStatus(RuleStatus.READY)
      .setConfigKey("InternalKey" + ruleKey.rule())
      .setSeverity(Severity.INFO)
      .setCardinality(Cardinality.SINGLE)
      .setLanguage("js")
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix");
  }
}
