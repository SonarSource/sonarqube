/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.rule.ws;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.rule.RuleType;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbTester;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDescriptionSectionContextDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.TestMetadataType;
import org.sonar.server.TestMetadataType2;
import org.sonar.server.common.text.MacroInterpreter;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.qualityprofile.QProfileRulesImpl;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.Rule;
import org.sonarqube.ws.Rules.SearchResponse;
import org.sonarsource.compliancereports.reports.MetadataLoader;
import org.sonarsource.compliancereports.reports.MetadataRules;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.RESOURCES_SECTION_KEY;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.db.rule.RuleTesting.newRuleWithoutDescriptionSection;
import static org.sonar.db.rule.RuleTesting.setSystemTags;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonar.server.rule.index.RuleIndex.FACET_COMPLIANCE_STANDARDS;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_CLEAN_CODE_ATTRIBUTE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_COMPARE_TO_PROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_COMPLIANCE_STANDARDS;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_QPROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_RULE_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.Rules.Rule.DescriptionSection.Context.newBuilder;

class SearchActionIT {

  private static final String JAVA = "java";

  @RegisterExtension
  private final UserSessionRule userSession = UserSessionRule.standalone();

  private final System2 system2 = new AlwaysIncreasingSystem2();
  @RegisterExtension
  private final DbTester db = DbTester.create(system2);
  @RegisterExtension
  private final EsTester es = EsTester.create();
  private final Configuration config = mock(Configuration.class);
  private final MetadataLoader metadataLoader = new MetadataLoader(Set.of(new TestMetadataType(), new TestMetadataType2()));
  private final MetadataRules metadataRules = new MetadataRules(metadataLoader);
  private final RuleIndex ruleIndex = new RuleIndex(es.client(), system2, config);
  private final RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
  private final ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(db.getDbClient(), es.client());
  private final Languages languages = LanguageTesting.newLanguages(JAVA, "js");
  private final RuleQueryFactory ruleQueryFactory = new RuleQueryFactory(db.getDbClient(), metadataRules);
  private static final MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  private final QualityProfileChangeEventService qualityProfileChangeEventService = mock(QualityProfileChangeEventService.class);
  private final RuleMapper ruleMapper = new RuleMapper(languages, macroInterpreter, new RuleDescriptionFormatter());
  private final SearchAction underTest = new SearchAction(ruleIndex, ruleQueryFactory, db.getDbClient(),
    new RulesResponseFormatter(db.getDbClient(), new RuleWsSupport(db.getDbClient(), userSession), ruleMapper, languages), metadataLoader,
    metadataRules);
  private final TypeValidations typeValidations = new TypeValidations(asList(new StringTypeValidation(), new IntegerTypeValidation()));
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));
  private final RuleActivator ruleActivator = new RuleActivator(System2.INSTANCE, db.getDbClient(), UuidFactoryImpl.INSTANCE,
    typeValidations, userSession,
    mock(Configuration.class), sonarQubeVersion);
  private final QProfileRules qProfileRules = new QProfileRulesImpl(db.getDbClient(), ruleActivator, ruleIndex, activeRuleIndexer,
    qualityProfileChangeEventService);
  private final WsActionTester ws = new WsActionTester(underTest);
  private final UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;

  @BeforeAll
  static void before() {
    doReturn("interpreted").when(macroInterpreter).interpret(anyString());
  }

  @Test
  void test_definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.isPost()).isFalse();
    assertThat(def.since()).isEqualTo("4.4");
    assertThat(def.isInternal()).isFalse();
    assertThat(def.responseExampleAsString()).isNotEmpty();
    assertThat(def.params()).hasSize(35);

    WebService.Param compareToProfile = def.param("compareToProfile");
    assertThat(compareToProfile.since()).isEqualTo("6.5");
    assertThat(compareToProfile.isRequired()).isFalse();
    assertThat(compareToProfile.isInternal()).isTrue();
    assertThat(compareToProfile.description()).isEqualTo("Quality profile key to filter rules that are activated. Meant to compare easily to profile set in 'qprofile'");
  }

  @Test
  void return_empty_result() {
    Rules.SearchResponse response = ws.newRequest()
      .setParam(WebService.Param.FIELDS, "actives")
      .executeProtobuf(Rules.SearchResponse.class);

    assertThat(response.getTotal()).isZero();
    assertThat(response.getP()).isOne();
    assertThat(response.getPaging().getTotal()).isZero();
    assertThat(response.getPaging().getPageIndex()).isOne();
    assertThat(response.getPaging().getPageSize()).isNotZero();
    assertThat(response.getRulesCount()).isZero();
  }

  @Test
  void return_all_rules() {
    RuleDto rule1 = db.rules().insert(r1 -> r1.setLanguage("java").setNoteUserUuid(null));
    RuleDto rule2 = db.rules().insert(r1 -> r1.setLanguage("java").setNoteUserUuid(null));
    indexRules();

    verify(r -> {
    }, rule1, rule2);
  }

  @Test
  void return_note_login() {
    UserDto user1 = db.users().insertUser();
    RuleDto rule1 = db.rules().insert(r -> r.setNoteUserUuid(user1.getUuid()));
    UserDto disableUser = db.users().insertDisabledUser();
    RuleDto rule2 = db.rules().insert(r -> r.setNoteUserUuid(disableUser.getUuid()));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "noteLogin")
      .executeProtobuf(SearchResponse.class);

    assertThat(result.getRulesList())
      .extracting(Rule::getKey, Rule::getNoteLogin)
      .containsExactlyInAnyOrder(
        tuple(rule1.getKey().toString(), user1.getLogin()),
        tuple(rule2.getKey().toString(), disableUser.getLogin()));
  }

  @Test
  void dont_fail_if_note_author_no_longer_exists() {
    // note: this can only happen due to DB corruption (user deleted)
    RuleDto rule1 = db.rules().insert(r -> r.setNoteUserUuid("non-existent"));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "noteLogin")
      .executeProtobuf(SearchResponse.class);

    assertThat(result.getRulesList())
      .extracting(Rule::getKey, Rule::getNoteLogin)
      .containsExactlyInAnyOrder(
        tuple(rule1.getKey().toString(), ""));
  }

  @Test
  void filter_by_rule_key() {
    RuleDto rule1 = db.rules().insert(r1 -> r1.setLanguage("java").setNoteUserUuid(null));
    db.rules().insert(r1 -> r1.setLanguage("java").setNoteUserUuid(null));
    indexRules();

    verify(r -> r.setParam(PARAM_RULE_KEY, rule1.getKey().toString()), rule1);
    verifyNoResults(r -> r.setParam(PARAM_RULE_KEY, "missing"));
  }

  @Test
  void filter_by_rule_name() {
    RuleDto rule1 = db.rules().insert(r1 -> r1.setName("Best rule ever").setNoteUserUuid(null));
    RuleDto rule2 = db.rules().insert(r1 -> r1.setName("Some other stuff").setNoteUserUuid(null));
    indexRules();

    verify(r -> r.setParam("q", "Be"), rule1);
    verify(r -> r.setParam("q", "Bes"), rule1);
    verify(r -> r.setParam("q", "Best"), rule1);
    verify(r -> r.setParam("q", "Best "), rule1);
    verify(r -> r.setParam("q", "Best rule"), rule1);
    verify(r -> r.setParam("q", "Best rule eve"), rule1);
    verify(r -> r.setParam("q", "Best rule ever"), rule1);
    verify(r -> r.setParam("q", "ru ev"), rule1);
    verify(r -> r.setParam("q", "ru ever"), rule1);
    verify(r -> r.setParam("q", "ev ve ver ru le"), rule1);
    verify(r -> r.setParam("q", "other"), rule2);
  }

  @Test
  void filter_by_rule_name_requires_all_words_to_match() {
    RuleDto rule1 = db.rules().insert(r1 -> r1.setName("Best rule ever").setNoteUserUuid(null));
    RuleDto rule2 = db.rules().insert(r1 -> r1.setName("Some other stuff").setNoteUserUuid(null));
    indexRules();

    verify(r -> r.setParam("q", "Best other"));
    verify(r -> r.setParam("q", "Best rule"), rule1);
    verify(r -> r.setParam("q", "rule ever"), rule1);
  }

  @Test
  void filter_by_rule_name_does_not_interpret_query() {
    RuleDto rule1 = db.rules().insert(r1 -> r1.setName("Best rule for-ever").setNoteUserUuid(null));
    RuleDto rule2 = db.rules().insert(r1 -> r1.setName("Some other stuff").setNoteUserUuid(null));
    indexRules();

    // do not interpret "-" as a "not"
    verify(r -> r.setParam("q", "-ever"), rule1);
  }

  @Test
  void filter_by_rule_description() {
    RuleDto rule1 = db.rules().insert(
      newRule(createDefaultRuleDescriptionSection(uuidFactory.create(), "This is the <bold>best</bold> rule now&amp;for<b>ever</b>"))
        .setNoteUserUuid(null));
    db.rules().insert(r1 -> r1.setName("Some other stuff").setNoteUserUuid(null));
    indexRules();

    verify(r -> r.setParam("q", "Best "), rule1);
    verify(r -> r.setParam("q", "bold"));
    verify(r -> r.setParam("q", "now&forever"), rule1);
  }

  @Test
  void filter_with_context_specific_rule_description() {
    RuleDescriptionSectionDto section1context1 = createRuleDescriptionSectionWithContext(RESOURCES_SECTION_KEY, "<div>I want to fix with Spring</div>", "ctx1");
    RuleDescriptionSectionDto section1context2 = createRuleDescriptionSectionWithContext(RESOURCES_SECTION_KEY, "<div>Another context</div>", "ctx2");

    RuleDto ruleDto = newRuleWithoutDescriptionSection()
      .setNoteUserUuid(null)
      .addRuleDescriptionSectionDto(section1context1)
      .addRuleDescriptionSectionDto(section1context2);
    db.rules().insert(ruleDto);
    indexRules();

    verify(r -> r.setParam("q", "Spring "), ruleDto);
    verify(r -> r.setParam("q", "bold"));
    verify(r -> r.setParam("q", "context"), ruleDto);
  }

  @Test
  void filter_by_rule_name_or_descriptions_requires_all_words_to_match_anywhere() {
    RuleDto rule1 = db.rules().insert(newRuleWithoutDescriptionSection().setName("Best rule ever").setNoteUserUuid(null)
      .addRuleDescriptionSectionDto(createDefaultRuleDescriptionSection(uuidFactory.create(), "This is a good rule")));
    db.rules().insert(newRuleWithoutDescriptionSection().setName("Another thing").setNoteUserUuid(null)
      .addRuleDescriptionSectionDto(createDefaultRuleDescriptionSection(uuidFactory.create(), "Another thing")));
    indexRules();

    verify(r -> r.setParam("q", "Best good"), rule1);
    verify(r -> r.setParam("q", "Best Another"));
  }

  @Test
  void return_all_rule_fields_by_default() {
    OffsetDateTime dateTime = OffsetDateTime.now(Clock.fixed(Instant.ofEpochMilli(1687816800000L), ZoneId.systemDefault()));

    RuleDto rule = db.rules().insert(
      r -> r.setCreatedAt(dateTime.toInstant().toEpochMilli()),
      r -> r.setUpdatedAt(dateTime.toInstant().toEpochMilli()),
      r -> r.setGapDescription("Gap Description"),
      r -> r.setIsTemplate(true),
      r -> r.setName("Name"),
      r -> r.setRepositoryKey("repo_key"),
      r -> r.setSeverity("MINOR"),
      r -> r.replaceAllDefaultImpacts(List.of(
        new ImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.BLOCKER),
        new ImpactDto(SoftwareQuality.SECURITY, Severity.INFO))),
      r -> r.setLanguage("java"));
    indexRules();

    Rules.SearchResponse response = ws.newRequest().executeProtobuf(Rules.SearchResponse.class);
    Rules.Rule result = response.getRules(0);
    assertThat(result.getCreatedAt()).isEqualTo(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")));
    assertThat(result.getUpdatedAt()).isEqualTo(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")));
    assertThat(result.getGapDescription()).isEqualTo("Gap Description");
    assertThat(result.hasIsTemplate()).isTrue();
    assertThat(result.getName()).isEqualTo("Name");
    assertThat(result.getRepo()).isEqualTo("repo_key");
    assertThat(result.getSeverity()).isEqualTo("MINOR");
    assertThat(result.getImpacts().getImpactsList()).containsExactlyInAnyOrder(
      Common.Impact.newBuilder()
        .setSoftwareQuality(Common.SoftwareQuality.MAINTAINABILITY)
        .setSeverity(Common.ImpactSeverity.ImpactSeverity_BLOCKER).build(),
      Common.Impact.newBuilder()
        .setSoftwareQuality(Common.SoftwareQuality.SECURITY)
        .setSeverity(Common.ImpactSeverity.ImpactSeverity_INFO).build());
    assertThat(result.getType().name()).isEqualTo(RuleType.fromDbConstant(rule.getType()).name());
    assertThat(result.getLang()).isEqualTo("java");
  }

  @Test
  void return_subset_of_fields() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("java"));
    indexRules();

    Rules.SearchResponse response = ws.newRequest()
      .setParam(WebService.Param.FIELDS, "createdAt,langName,educationPrinciples")
      .executeProtobuf(Rules.SearchResponse.class);
    Rules.Rule result = response.getRules(0);

    // mandatory fields
    assertThat(result.getKey()).isEqualTo(rule.getKey().toString());
    assertThat(result.getType().getNumber()).isEqualTo(rule.getType());

    // selected fields
    assertThat(result.getCreatedAt()).isNotEmpty();
    assertThat(result.getLangName()).isNotEmpty();
    assertThat(result.getEducationPrinciples().getEducationPrinciplesList()).containsExactlyElementsOf(rule.getEducationPrinciples());

    // not returned fields
    assertThat(result.hasGapDescription()).isFalse();
    assertThat(result.hasIsTemplate()).isFalse();
    assertThat(result.hasLang()).isFalse();
    assertThat(result.hasName()).isFalse();
    assertThat(result.hasSeverity()).isFalse();
    assertThat(result.hasRepo()).isFalse();
    assertThat(result.hasUpdatedAt()).isFalse();
  }

  @Test
  void return_deprecatedKeys_in_response_on_demand() {
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("java"));
    db.rules().insertDeprecatedKey(r -> r.setRuleUuid(rule1.getUuid()).setOldRuleKey("oldrulekey").setOldRepositoryKey("oldrepositorykey"));
    db.rules().insertDeprecatedKey(r -> r.setRuleUuid(rule1.getUuid()).setOldRuleKey("oldrulekey2").setOldRepositoryKey("oldrepositorykey2"));
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage("javascript"));

    indexRules();

    Rules.SearchResponse response = ws.newRequest()
      .setParam(WebService.Param.FIELDS, "deprecatedKeys")
      .executeProtobuf(Rules.SearchResponse.class);

    System.err.println(response.getRulesList());

    assertThat(response.getRulesList()).satisfies(l -> {
      assertThat(l).hasSize(2);

      assertThat(l).anySatisfy(e -> {
        assertThat(e.getKey()).isEqualTo(rule1.getKey().toString());
        assertThat(e.getType().getNumber()).isEqualTo(rule1.getType());
        assertThat(e.getDeprecatedKeys()).isNotNull();
        assertThat(e.getDeprecatedKeys().getDeprecatedKeyList()).contains("oldrepositorykey:oldrulekey", "oldrepositorykey2:oldrulekey2");
      });
      assertThat(l).anySatisfy(e -> {
        assertThat(e.getKey()).isEqualTo(rule2.getKey().toString());
        assertThat(e.getType().getNumber()).isEqualTo(rule2.getType());
        assertThat(e.getDeprecatedKeys()).isNotNull();
        assertThat(e.getDeprecatedKeys().getDeprecatedKeyList()).isEmpty();
      });
    });
  }

  @Test
  void should_filter_on_specific_tags() {
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("java").setTags(Set.of("tag1", "tag2")));
    db.rules().insert(r -> r.setLanguage("java"));
    indexRules();

    Consumer<TestRequest> request = r -> r
      .setParam("f", "repo,name")
      .setParam("tags", rule1.getTags().stream().collect(Collectors.joining(",")));
    verify(request, rule1);
  }

  @Test
  void should_filter_on_compliance_standard() {
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S001")));
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S002")));
    db.rules().insert(r -> r.setLanguage("java"));

    db.rules().insert(r -> r.setLanguage("java"));
    indexRules();

    Consumer<TestRequest> request = r -> r
      .setParam("complianceStandards", "test:V1=category1");
    verify(request, rule1, rule2);
  }

  @Test
  void should_filter_on_multiple_compliance_standards() {
    RuleDto rule1 = db.rules().insert(
      r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S001"))
    );

    db.rules().insert(
      r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S002"))
    );

    db.rules().insert(r -> r.setLanguage("java"));
    db.rules().insert(r -> r.setLanguage("java"));
    indexRules();

    Consumer<TestRequest> request = r -> r
      .setParam("complianceStandards", "test:V1=category1&test2:V2=cat1");

    verify(request, rule1);
  }

  @Test
  void should_return_empty_if_invalid_compliance_standard_category() {
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S001")));
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S002")));
    db.rules().insert(r -> r.setLanguage("java"));

    db.rules().insert(r -> r.setLanguage("java"));
    indexRules();

    Consumer<TestRequest> request = r -> r.setParam("complianceStandards", "test:V1=nonexisting");
    verify(request);
  }

  @Test
  void when_searching_for_several_tags_combine_them_with_OR() {
    RuleDto bothTagsRule = db.rules().insert(r -> r.setLanguage("java"), setTags("tag1", "tag2"));
    RuleDto oneTagRule = db.rules().insert(r -> r.setLanguage("java"), setTags("tag1"));
    RuleDto otherTagRule = db.rules().insert(r -> r.setLanguage("java"), setTags("tag2"));
    db.rules().insert(r -> r.setLanguage("java"), setTags());
    indexRules();

    Consumer<TestRequest> request = r -> r
      .setParam("f", "repo,name")
      .setParam("tags", "tag1,tag2");
    verify(request, bothTagsRule, oneTagRule, otherTagRule);
  }

  @Test
  void should_list_tags_in_tags_facet() {
    String[] tags = get101Tags();
    db.rules().insert(setSystemTags("x"), setTags(tags));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "repo,name")
      .setParam("facets", "tags")
      .executeProtobuf(SearchResponse.class);
    assertThatFacet("tags", result)
      .contains(tuple("tag0", 1L), tuple("tag25", 1L), tuple("tag99", 1L))
      .doesNotContain(tuple("x", 1L));
  }

  @Test
  void should_list_tags_ordered_by_count_then_by_name_in_tags_facet() {
    db.rules().insert(setSystemTags("tag7", "tag5", "tag3", "tag1", "tag9"), setTags("tag2", "tag4", "tag6", "tag8", "tagA"));
    db.rules().insert(setSystemTags("tag2"), setTags());
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "repo,name")
      .setParam("facets", "tags")
      .executeProtobuf(SearchResponse.class);
    assertThatFacet("tags", result)
      .containsExactly(tuple("tag2", 2L), tuple("tag1", 1L), tuple("tag3", 1L), tuple("tag4", 1L), tuple("tag5", 1L), tuple("tag6", 1L),
        tuple("tag7", 1L), tuple("tag8", 1L), tuple("tag9", 1L), tuple("tagA", 1L));
  }

  @Test
  void should_include_selected_matching_tag_in_facet() {
    db.rules().insert(
      setSystemTags("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tagA", "x"),
      r -> r.setNoteUserUuid(null));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("facets", "tags")
      .setParam("tags", "x")
      .executeProtobuf(SearchResponse.class);
    assertThatFacet("tags", result).contains(tuple("x", 1L));
  }

  @Test
  void execute_whenFacetIsSoftwareQuality_shouldReturnCorrectMatch() {
    db.rules().insert(
      r -> r.replaceAllDefaultImpacts(List.of(new ImpactDto().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY).setSeverity(Severity.HIGH))));
    db.rules().insert(
      r -> r.replaceAllDefaultImpacts(List.of(new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(Severity.MEDIUM))));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("facets", "impactSoftwareQualities")
      .setParam("impactSeverities", Severity.HIGH.name())
      .executeProtobuf(SearchResponse.class);
    assertThatFacet("impactSoftwareQualities", result).contains(
        tuple(SoftwareQuality.MAINTAINABILITY.name(), 1L),
        tuple(SoftwareQuality.RELIABILITY.name(), 0L),
        tuple(SoftwareQuality.SECURITY.name(), 0L));
  }

  @Test
  void execute_whenFacetIsImpactSeverity_shouldReturnCorrectMatch() {
    db.rules().insert(
      r -> r.replaceAllDefaultImpacts(List.of(new ImpactDto().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY).setSeverity(Severity.HIGH))));
    db.rules().insert(
      r -> r.replaceAllDefaultImpacts(List.of(new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(Severity.MEDIUM),
        new ImpactDto().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY).setSeverity(Severity.INFO))));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("facets", "impactSeverities")
      .setParam("impactSoftwareQualities", SoftwareQuality.MAINTAINABILITY.name())
      .executeProtobuf(SearchResponse.class);
    assertThatFacet("impactSeverities", result).contains(
      tuple(Severity.HIGH.name(), 1L),
      tuple(Severity.MEDIUM.name(), 0L),
      tuple(Severity.LOW.name(), 0L),
      tuple(Severity.INFO.name(), 1L));
  }

  @Test
  void execute_whenFacetIsCleanCodeAttributeCategories_shouldReturnCorrectMatch() {
    db.rules().insert(
      r -> r.setCleanCodeAttribute(CleanCodeAttribute.COMPLETE)
        .replaceAllDefaultImpacts(List.of(new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(Severity.HIGH))));
    db.rules().insert(
      r -> r.setCleanCodeAttribute(CleanCodeAttribute.CONVENTIONAL));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("facets", "cleanCodeAttributeCategories")
      .setParam("impactSoftwareQualities", SoftwareQuality.RELIABILITY.name())
      .executeProtobuf(SearchResponse.class);
    assertThatFacet("cleanCodeAttributeCategories", result).contains(
        tuple(CleanCodeAttribute.COMPLETE.getAttributeCategory().name(), 1L),
        tuple(CleanCodeAttribute.CONVENTIONAL.getAttributeCategory().name(), 0L));
  }

  @Test
  void should_included_selected_non_matching_tag_in_facet() {
    RuleDto rule = db.rules().insert(setSystemTags("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tagA"));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("facets", "tags")
      .setParam("tags", "x")
      .executeProtobuf(SearchResponse.class);
    assertThatFacet("tags", result).contains(tuple("x", 0L));
  }

  @Test
  void should_return_specific_tags() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage("java"), setTags("tag1", "tag2"));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "tags")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getRulesList()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
    assertThat(result.getRulesList())
      .extracting(Rule::getTags).flatExtracting(Rules.Tags::getTagsList)
      .containsExactly(rule.getTags().toArray(new String[0]));
  }

  @Test
  void returnRuleCleanCodeFields_whenEndpointIsCalled() {
    db.rules().insert();
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam(WebService.Param.FIELDS, FIELD_CLEAN_CODE_ATTRIBUTE)
      .executeProtobuf(SearchResponse.class);

    // mandatory fields
    assertThat(result.getRulesList())
      .extracting(r -> r.getImpacts().getImpactsList().stream().findFirst().orElseThrow(() -> new IllegalStateException("Impact is a mandatory field in the response.")))
      .extracting(Common.Impact::getSoftwareQuality, Common.Impact::getSeverity)
      .containsExactly(tuple(Common.SoftwareQuality.MAINTAINABILITY, Common.ImpactSeverity.HIGH));

    // selected fields
    assertThat(result.getRulesList()).extracting(Rule::getCleanCodeAttribute).containsExactly(Common.CleanCodeAttribute.CLEAR);
    assertThat(result.getRulesList()).extracting(Rule::getCleanCodeAttributeCategory).containsExactly(Common.CleanCodeAttributeCategory.INTENTIONAL);
  }

  @Test
  void should_return_specified_fields() {
    when(macroInterpreter.interpret(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

    RuleDescriptionSectionDto section1context1 = createRuleDescriptionSectionWithContext(RESOURCES_SECTION_KEY, "<div>I want to fix with Spring</div>", "ctx1");
    RuleDescriptionSectionDto section1context2 = createRuleDescriptionSectionWithContext(RESOURCES_SECTION_KEY, "<div>Another context</div>", "ctx2");
    RuleDto rule = newRuleWithoutDescriptionSection()
      .setLanguage("java")
      .addRuleDescriptionSectionDto(section1context1)
      .addRuleDescriptionSectionDto(section1context2);
    db.rules().insert(rule);
    indexRules();

    checkField(rule, "repo", Rule::getRepo, rule.getRepositoryKey());
    checkField(rule, "name", Rule::getName, rule.getName());
    checkField(rule, "severity", Rule::getSeverity, rule.getSeverityString());
    checkField(rule, "status", r -> r.getStatus().toString(), rule.getStatus().toString());
    checkField(rule, "internalKey", Rule::getInternalKey, rule.getConfigKey());
    checkField(rule, "isTemplate", Rule::getIsTemplate, rule.isTemplate());
    checkField(rule, "lang", Rule::getLang, rule.getLanguage());
    checkField(rule, "langName", Rule::getLangName, languages.get(rule.getLanguage()).getName());
    checkField(rule, "gapDescription", Rule::getGapDescription, rule.getGapDescription());
    checkTags(rule, rule.getSystemTags());
    checkDescriptionSections(rule, rule.getRuleDescriptionSectionDtos().stream()
      .map(SearchActionIT::toProtobufDto)
      .collect(Collectors.toSet()));
  }

  private RuleDescriptionSectionDto createRuleDescriptionSectionWithContext(String key, String content, @Nullable String contextKey) {
    RuleDescriptionSectionContextDto contextDto = Optional.ofNullable(contextKey)
      .map(c -> RuleDescriptionSectionContextDto.of(contextKey, contextKey + " display name"))
      .orElse(null);
    return RuleDescriptionSectionDto.builder()
      .uuid(uuidFactory.create())
      .key(key)
      .content(content)
      .context(contextDto)
      .build();
  }

  private static Rule.DescriptionSection toProtobufDto(RuleDescriptionSectionDto section) {
    Rule.DescriptionSection.Builder builder = Rule.DescriptionSection.newBuilder().setKey(section.getKey()).setContent(section.getContent());
    if (section.getContext() != null) {
      RuleDescriptionSectionContextDto context = section.getContext();
      builder.setContext(newBuilder().setKey(context.getKey()).setDisplayName(context.getDisplayName()).build());
    }
    return builder.build();
  }

  @Test
  void return_lang_key_field_when_language_name_is_not_available() {
    String unknownLanguage = "unknown_" + secure().nextAlphanumeric(5);
    RuleDto rule = db.rules().insert(r -> r.setLanguage(unknownLanguage));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "langName")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getPaging().getTotal()).isOne();
    assertThat(result.getPaging().getPageIndex()).isOne();
    assertThat(result.getRulesCount()).isOne();

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getLangName()).isEqualTo(unknownLanguage);
  }

  @Test
  void search_debt_rules_with_default_and_overridden_debt_values() {
    db.rules().insert(r -> r.setLanguage("java")
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefRemediationGapMultiplier("1h")
      .setDefRemediationBaseEffort("15min")
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setRemediationGapMultiplier("2h")
      .setRemediationBaseEffort("25min"));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "debtRemFn,remFnOverloaded,defaultDebtRemFn")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getPaging().getTotal()).isOne();
    assertThat(result.getPaging().getPageIndex()).isOne();
    assertThat(result.getRulesCount()).isOne();

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getDefaultRemFnGapMultiplier()).isEqualTo("1h");
    assertThat(searchedRule.getDefaultRemFnBaseEffort()).isEqualTo("15min");
    assertThat(searchedRule.getDefaultDebtRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(searchedRule.getDefaultRemFnBaseEffort()).isEqualTo("15min");
    assertThat(searchedRule.getDefaultRemFnGapMultiplier()).isEqualTo("1h");
    assertThat(searchedRule.getDefaultRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(searchedRule.getRemFnOverloaded()).isTrue();
    assertThat(searchedRule.getRemFnGapMultiplier()).isEqualTo("2h");
    assertThat(searchedRule.getRemFnBaseEffort()).isEqualTo("25min");
    assertThat(searchedRule.getDebtRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
  }

  @Test
  void search_debt_rules_with_default_linear_offset_and_overridden_constant_debt() {
    db.rules().insert(r -> r.setLanguage("java")
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefRemediationGapMultiplier("1h")
      .setDefRemediationBaseEffort("15min")
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationGapMultiplier(null)
      .setRemediationBaseEffort("5min"));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "debtRemFn,remFnOverloaded,defaultDebtRemFn")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getPaging().getTotal()).isOne();
    assertThat(result.getPaging().getPageIndex()).isOne();
    assertThat(result.getRulesCount()).isOne();

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getDefaultRemFnGapMultiplier()).isEqualTo("1h");
    assertThat(searchedRule.getDefaultRemFnBaseEffort()).isEqualTo("15min");
    assertThat(searchedRule.getDefaultDebtRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(searchedRule.getDefaultRemFnBaseEffort()).isEqualTo("15min");
    assertThat(searchedRule.getDefaultRemFnGapMultiplier()).isEqualTo("1h");
    assertThat(searchedRule.getDefaultRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(searchedRule.getRemFnOverloaded()).isTrue();
    assertThat(searchedRule.getRemFnGapMultiplier()).isEmpty();
    assertThat(searchedRule.getRemFnBaseEffort()).isEqualTo("5min");
    assertThat(searchedRule.getDebtRemFnType()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
  }

  @Test
  void search_debt_rules_with_default_linear_offset_and_overridden_linear_debt() {
    db.rules().insert(r -> r.setLanguage("java")
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefRemediationGapMultiplier("1h")
      .setDefRemediationBaseEffort("15min")
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setRemediationGapMultiplier("1h")
      .setRemediationBaseEffort(null));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "debtRemFn,remFnOverloaded,defaultDebtRemFn")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getPaging().getTotal()).isOne();
    assertThat(result.getPaging().getPageIndex()).isOne();
    assertThat(result.getRulesCount()).isOne();

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getDefaultRemFnGapMultiplier()).isEqualTo("1h");
    assertThat(searchedRule.getDefaultRemFnBaseEffort()).isEqualTo("15min");
    assertThat(searchedRule.getDefaultDebtRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(searchedRule.getDefaultRemFnBaseEffort()).isEqualTo("15min");
    assertThat(searchedRule.getDefaultRemFnGapMultiplier()).isEqualTo("1h");
    assertThat(searchedRule.getDefaultRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(searchedRule.getRemFnOverloaded()).isTrue();
    assertThat(searchedRule.getRemFnGapMultiplier()).isEqualTo("1h");
    assertThat(searchedRule.getRemFnBaseEffort()).isEmpty();
    assertThat(searchedRule.getDebtRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
  }

  @Test
  void search_template_rules() {
    RuleDto templateRule = db.rules().insert(r -> r.setLanguage("java")
      .setIsTemplate(true));
    RuleDto rule = db.rules().insert(r -> r.setLanguage("java")
      .setTemplateUuid(templateRule.getUuid()));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "isTemplate")
      .setParam("is_template", "true")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getPaging().getTotal()).isOne();
    assertThat(result.getPaging().getPageIndex()).isOne();
    assertThat(result.getRulesCount()).isOne();

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getIsTemplate()).isTrue();
    assertThat(searchedRule.getKey()).isEqualTo(templateRule.getRepositoryKey() + ":" + templateRule.getRuleKey());
  }

  @Test
  void search_custom_rules_from_template_key() {
    RuleDto templateRule = db.rules().insert(r -> r.setLanguage("java")
      .setIsTemplate(true));
    RuleDto rule = db.rules().insert(r -> r.setLanguage("java")
      .setTemplateUuid(templateRule.getUuid()));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "templateKey")
      .setParam("template_key", templateRule.getRepositoryKey() + ":" + templateRule.getRuleKey())
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getPaging().getTotal()).isOne();
    assertThat(result.getPaging().getPageIndex()).isOne();
    assertThat(result.getRulesCount()).isOne();

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getKey()).isEqualTo(rule.getRepositoryKey() + ":" + rule.getRuleKey());
    assertThat(searchedRule.getTemplateKey()).isEqualTo(templateRule.getRepositoryKey() + ":" + templateRule.getRuleKey());
  }

  @Test
  void do_not_return_external_rule() {
    db.rules().insert(r -> r.setIsExternal(true));
    indexRules();

    SearchResponse result = ws.newRequest().executeProtobuf(SearchResponse.class);

    assertThat(result.getTotal()).isZero();
    assertThat(result.getPaging().getTotal()).isZero();
    assertThat(result.getPaging().getPageIndex()).isOne();
    assertThat(result.getRulesCount()).isZero();
  }

  @Test
  void search_all_active_rules() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage("java"));
    RuleDto rule = db.rules().insert(r -> r.setLanguage("java").setNoteUserUuid(null));
    RuleActivation activation = RuleActivation.create(rule.getUuid(), BLOCKER, null);
    qProfileRules.activateAndCommit(db.getSession(), profile, singleton(activation));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("q", rule.getName())
      .setParam("activation", "true")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getPaging().getTotal()).isOne();
    assertThat(result.getPaging().getPageIndex()).isOne();
    assertThat(result.getRulesCount()).isOne();

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getKey()).isEqualTo(rule.getRepositoryKey() + ":" + rule.getRuleKey());
    assertThat(searchedRule.getName()).isEqualTo(rule.getName());
  }

  @Test
  void search_profile_active_rules() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage("java"));
    QProfileDto waterproofProfile = db.qualityProfiles().insert(p -> p.setLanguage("java"));
    RuleDto rule = db.rules().insert(r -> r.setLanguage("java"));
    RuleParamDto ruleParam1 = db.rules().insertRuleParam(rule, p -> p.setDefaultValue("some value")
      .setType("STRING")
      .setDescription("My small description")
      .setName("my_var"));

    RuleParamDto ruleParam2 = db.rules().insertRuleParam(rule, p -> p.setDefaultValue("1")
      .setType("INTEGER")
      .setDescription("My small description")
      .setName("the_var"));

    // SONAR-7083
    RuleParamDto ruleParam3 = db.rules().insertRuleParam(rule, p -> p.setDefaultValue(null)
      .setType("STRING")
      .setDescription("Empty Param")
      .setName("empty_var"));

    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> activeRuleChanges1 = qProfileRules.activateAndCommit(db.getSession(), profile, singleton(activation));
    qProfileRules.activateAndCommit(db.getSession(), waterproofProfile, singleton(activation));
    assertThat(activeRuleChanges1).hasSize(1);
    indexRules();
    indexActiveRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "actives")
      .setParam("q", rule.getName())
      .setParam("activation", "true")
      .setParam("qprofile", profile.getKee())
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getPaging().getTotal()).isOne();
    assertThat(result.getPaging().getPageIndex()).isOne();
    assertThat(result.getRulesCount()).isOne();
    assertThat(result.getActives()).isNotNull();
    assertThat(result.getActives().getActives().get(rule.getKey().toString())).isNotNull();
    assertThat(result.getActives().getActives().get(rule.getKey().toString()).getActiveListList()).hasSize(1);

    // The rule without value is not inserted
    Rules.Active activeList = result.getActives().getActives().get(rule.getKey().toString()).getActiveList(0);
    assertThat(activeList.getParamsCount()).isEqualTo(2);
    assertThat(activeList.getParamsList()).extracting("key", "value").containsExactlyInAnyOrder(
      tuple(ruleParam1.getName(), ruleParam1.getDefaultValue()),
      tuple(ruleParam2.getName(), ruleParam2.getDefaultValue()));

    String unknownProfile = "unknown_profile" + secure().nextAlphanumeric(5);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("activation", "true")
        .setParam("qprofile", unknownProfile)
        .executeProtobuf(SearchResponse.class);
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("The specified qualityProfile '" + unknownProfile + "' does not exist");
  }

  @Test
  void search_for_active_rules_when_parameter_value_is_null() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage("java"));

    RuleDto rule = db.rules().insert(r -> r.setLanguage("java"));

    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setDefaultValue("some value")
      .setType("STRING")
      .setDescription("My small description")
      .setName("my_var"));

    RuleActivation activation = RuleActivation.create(rule.getUuid());
    List<ActiveRuleChange> activeRuleChanges = qProfileRules.activateAndCommit(db.getSession(), profile, singleton(activation));

    // Insert directly in database a rule parameter with a null value
    ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(ruleParam).setValue(null);
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRuleChanges.get(0).getActiveRule(), activeRuleParam);

    db.commit();

    indexRules();
    indexActiveRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "actives")
      .setParam("q", rule.getName())
      .setParam("activation", "true")
      .setParam("qprofile", profile.getKee())
      .executeProtobuf(SearchResponse.class);

    assertThat(result.getTotal()).isOne();
    assertThat(result.getPaging().getTotal()).isOne();
    assertThat(result.getPaging().getPageIndex()).isOne();
    assertThat(result.getRulesCount()).isOne();
    assertThat(result.getActives()).isNotNull();
    assertThat(result.getActives().getActives().get(rule.getKey().toString())).isNotNull();
    assertThat(result.getActives().getActives().get(rule.getKey().toString()).getActiveListList()).hasSize(1);

    Rules.Active activeList = result.getActives().getActives().get(rule.getKey().toString()).getActiveList(0);
    assertThat(activeList.getParamsCount()).isEqualTo(2);
    assertThat(activeList.getParamsList()).extracting("key", "value").containsExactlyInAnyOrder(
      tuple(ruleParam.getName(), ruleParam.getDefaultValue()),
      tuple(activeRuleParam.getKey(), ""));
  }

  /**
   * When the user searches for inactive rules (for example for to "activate more"), then
   * only rules of the quality profiles' language are relevant
   */
  @Test
  void facet_filtering_when_searching_for_inactive_rules() {
    QProfileDto profile = db.qualityProfiles().insert(q -> q.setLanguage("language1"));

    // on same language, not activated => match
    RuleDto rule1 = db.rules().insert(r -> r
      .setLanguage(profile.getLanguage())
      .setRepositoryKey("repositoryKey1")
      .setSystemTags(new HashSet<>(singletonList("tag1")))
      .setTags(emptySet())
      .setSeverity("CRITICAL")
      .setNoteUserUuid(null)
      .setStatus(RuleStatus.BETA)
      .setType(RuleType.CODE_SMELL));

    // on same language, activated => no match
    RuleDto rule2 = db.rules().insert(r -> r
      .setLanguage(profile.getLanguage())
      .setRepositoryKey("repositoryKey2")
      .setSystemTags(new HashSet<>(singletonList("tag2")))
      .setTags(emptySet())
      .setSeverity("MAJOR")
      .setNoteUserUuid(null)
      .setStatus(RuleStatus.DEPRECATED)
      .setType(RuleType.VULNERABILITY));
    RuleActivation activation = RuleActivation.create(rule2.getUuid(), null, null);
    qProfileRules.activateAndCommit(db.getSession(), profile, singleton(activation));

    // on other language, not activated => no match
    RuleDto rule3 = db.rules().insert(r -> r
      .setLanguage("language3")
      .setRepositoryKey("repositoryKey3")
      .setSystemTags(new HashSet<>(singletonList("tag3")))
      .setTags(emptySet())
      .setNoteUserUuid(null)
      .setSeverity("BLOCKER")
      .setStatus(RuleStatus.READY)
      .setType(RuleType.BUG));

    indexRules();
    indexActiveRules();

    SearchResponse result = ws.newRequest()
      .setParam("facets", "languages,repositories,tags,severities,statuses,types")
      .setParam("activation", "false")
      .setParam("qprofile", profile.getKee())
      .executeProtobuf(SearchResponse.class);

    assertThat(result.getRulesList())
      .extracting(Rule::getKey)
      .containsExactlyInAnyOrder(
        rule1.getKey().toString());

    assertThatFacet("languages", result).as("Facet languages").containsExactlyInAnyOrder(
      tuple(rule1.getLanguage(), 1L),

      // known limitation: irrelevant languages are shown in this case (SONAR-9683)
      tuple(rule3.getLanguage(), 1L));

    assertThatFacet("tags", result).as("Facet tags").containsExactlyInAnyOrder(
      tuple(rule1.getSystemTags().iterator().next(), 1L));

    assertThatFacet("repositories", result).as("Facet repositories").containsExactlyInAnyOrder(
      tuple(rule1.getRepositoryKey(), 1L));

    assertThatFacet("severities", result).as("Facet severities").containsExactlyInAnyOrder(
      tuple("BLOCKER" /* rule2 */, 0L),
      tuple("CRITICAL"/* rule1 */, 1L),
      tuple("MAJOR", 0L),
      tuple("MINOR", 0L),
      tuple("INFO", 0L));

    assertThatFacet("statuses", result).as("Facet statuses").containsExactlyInAnyOrder(
      tuple("READY"/* rule2 */, 0L),
      tuple("BETA" /* rule1 */, 1L),
      tuple("DEPRECATED", 0L));

    assertThatFacet("types", result).as("Facet types").containsExactlyInAnyOrder(
      tuple("BUG" /* rule2 */, 0L),
      tuple("CODE_SMELL"/* rule1 */, 1L),
      tuple("VULNERABILITY", 0L),
      tuple("SECURITY_HOTSPOT", 0L));
  }

  @Test
  void statuses_facet_should_be_sticky() {
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("java"));
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage("java").setStatus(RuleStatus.BETA));
    RuleDto rule3 = db.rules().insert(r -> r.setLanguage("java").setStatus(RuleStatus.DEPRECATED));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "status")
      .setParam("status", "DEPRECATED")
      .executeProtobuf(SearchResponse.class);

    assertThat(result.getRulesCount()).isEqualTo(3);
    assertThat(result.getRulesList()).extracting(Rule::getKey, r -> r.getStatus().name()).containsExactlyInAnyOrder(
      tuple(rule1.getKey().toString(), rule1.getStatus().name()),
      tuple(rule2.getKey().toString(), rule2.getStatus().name()),
      tuple(rule3.getKey().toString(), rule3.getStatus().name()));
  }

  @Test
  void return_compliance_facets() {
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S001")));
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S002")));
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S003")));
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S004")));
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S005")));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam(FACETS, FACET_COMPLIANCE_STANDARDS)
      .executeProtobuf(SearchResponse.class);

    assertThat(result.getRulesCount()).isEqualTo(5);
    assertThatFacet("test:V1", result).containsOnly(tuple("category1", 2L), tuple("category2", 1L));
  }

  @Test
  void return_compliance_facets_with_filters() {
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S001")).setSeverity(MINOR));
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S002")).setSeverity(MINOR));
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S003")).setSeverity(MINOR));
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "1")).setSeverity(MAJOR));
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "2")).setSeverity(MAJOR));
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "3")).setSeverity(MAJOR));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam(FACETS, FACET_COMPLIANCE_STANDARDS)
      .setParam(PARAM_COMPLIANCE_STANDARDS, "test2:V2=cat1&test:V1=category1")
      .setParam(PARAM_SEVERITIES, "MINOR")
      .executeProtobuf(SearchResponse.class);

    /*
        Filter on severities reduces V1 facets to S001, S002 and S003
        Filter on V2 reduces V1 facets to S001 and S003
        Filter on V1 should not affect V1 facet
     */
    assertThatFacet("test:V1", result).containsOnly(tuple("category1", 1L), tuple("category2", 1L));
  }

  @Test
  void return_compliance_facets_with_filters_for_wildcard_repos() {
    db.rules().insert(r -> r.setLanguage("secrets").setRuleKey(RuleKey.of("secrets", "S001")).setSeverity(MINOR));
    db.rules().insert(r -> r.setLanguage("java").setRuleKey(RuleKey.of("java", "S001")).setSeverity(MINOR));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam(FACETS, FACET_COMPLIANCE_STANDARDS)
      .setParam(PARAM_COMPLIANCE_STANDARDS, "test:V1=category4withsecretrules")
      .setParam(PARAM_SEVERITIES, "MINOR")
      .executeProtobuf(SearchResponse.class);

    assertThatFacet("test:V1", result).containsOnly(tuple("category1", 1L), tuple("category4withsecretrules", 1L));
  }

  @Test
  void always_return_selected_standard_in_compliance_facets() {
    SearchResponse result = ws.newRequest()
      .setParam(FACETS, FACET_COMPLIANCE_STANDARDS)
      .setParam(PARAM_COMPLIANCE_STANDARDS, "test:V1=category1")
      .executeProtobuf(SearchResponse.class);

    assertThatFacet("test:V1", result).containsOnly(tuple("category1", 0L));
  }

  @Test
  void paging() {
    for (int i = 0; i < 12; i++) {
      db.rules().insert(r -> r.setLanguage("java"));
    }
    indexRules();

    ws.newRequest()
      .setParam(WebService.Param.PAGE, "2")
      .setParam(WebService.Param.PAGE_SIZE, "9")
      .execute()
      .assertJson(this.getClass(), "paging.json");
  }

  @Test
  void compare_to_another_profile() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(JAVA));
    QProfileDto anotherProfile = db.qualityProfiles().insert(p -> p.setLanguage(JAVA));
    RuleDto commonRule = db.rules().insertRule(r -> r.setLanguage(JAVA));
    RuleDto profileRule1 = db.rules().insertRule(r -> r.setLanguage(JAVA));
    RuleDto profileRule2 = db.rules().insertRule(r -> r.setLanguage(JAVA));
    RuleDto profileRule3 = db.rules().insertRule(r -> r.setLanguage(JAVA));
    RuleDto anotherProfileRule1 = db.rules().insertRule(r -> r.setLanguage(JAVA));
    RuleDto anotherProfileRule2 = db.rules().insertRule(r -> r.setLanguage(JAVA));
    db.qualityProfiles().activateRule(profile, commonRule);
    db.qualityProfiles().activateRule(profile, profileRule1);
    db.qualityProfiles().activateRule(profile, profileRule2);
    db.qualityProfiles().activateRule(profile, profileRule3);
    db.qualityProfiles().activateRule(anotherProfile, commonRule);
    db.qualityProfiles().activateRule(anotherProfile, anotherProfileRule1);
    db.qualityProfiles().activateRule(anotherProfile, anotherProfileRule2);
    indexRules();
    indexActiveRules();

    SearchResponse result = ws.newRequest()
      .setParam(PARAM_QPROFILE, profile.getKee())
      .setParam(PARAM_ACTIVATION, "false")
      .setParam(PARAM_COMPARE_TO_PROFILE, anotherProfile.getKee())
      .executeProtobuf(SearchResponse.class);

    assertThat(result.getRulesList())
      .extracting(Rule::getKey)
      .containsExactlyInAnyOrder(anotherProfileRule1.getKey().toString(), anotherProfileRule2.getKey().toString());
  }

  @SafeVarargs
  private <T> void checkField(RuleDto rule, String fieldName, Function<Rule, T> responseExtractor, T... expected) {
    SearchResponse result = ws.newRequest()
      .setParam("f", fieldName)
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getRulesList()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
    assertThat(result.getRulesList()).extracting(responseExtractor).containsExactlyInAnyOrder(expected);
  }

  private AbstractListAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> assertThatFacet(String name, SearchResponse result) {
    return assertThat(result.getFacets().getFacetsList().stream()
      .filter(f -> name.equals(f.getProperty())).findAny().get().getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount);
  }

  private void checkTags(RuleDto rule, Set<String> expected) {
    SearchResponse result = ws.newRequest()
      .setParam("f", "sysTags")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getRulesList()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
    Set<String> actualTags = new HashSet<>(result.getRules(0).getSysTags().getSysTagsList());
    assertThat(actualTags).hasSameElementsAs(expected);
  }

  private void checkDescriptionSections(RuleDto rule, Set<Rule.DescriptionSection> expected) {
    SearchResponse result = ws.newRequest()
      .setParam("f", "descriptionSections")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getRulesList()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
    List<Rule.DescriptionSection> actualSections = result.getRules(0).getDescriptionSections().getDescriptionSectionsList();
    assertThat(actualSections).hasSameElementsAs(expected);
  }

  private void verifyNoResults(Consumer<TestRequest> requestPopulator) {
    verify(requestPopulator);
  }

  private void verify(Consumer<TestRequest> requestPopulator, RuleDto... expectedRules) {
    TestRequest request = ws.newRequest();
    requestPopulator.accept(request);
    Rules.SearchResponse response = request.executeProtobuf(Rules.SearchResponse.class);

    assertThat(response.getP()).isOne();
    assertThat(response.getPaging().getPageIndex()).isOne();
    assertThat(response.getPaging().getPageSize()).isNotZero();
    RuleKey[] expectedRuleKeys = stream(expectedRules).map(RuleDto::getKey).toList().toArray(new RuleKey[0]);
    assertThat(response.getRulesList())
      .extracting(r -> RuleKey.parse(r.getKey()))
      .containsExactlyInAnyOrder(expectedRuleKeys);
    assertThat(response.getTotal()).isEqualTo(expectedRules.length);
    assertThat(response.getPaging().getTotal()).isEqualTo(expectedRules.length);
    assertThat(response.getRulesCount()).isEqualTo(expectedRules.length);
  }

  private void indexRules() {
    ruleIndexer.indexAll();
  }

  private void indexActiveRules() {
    activeRuleIndexer.indexAll();
  }

  private String[] get101Tags() {
    String[] tags = new String[101];
    for (int i = 0; i < 100; i++) {
      tags[i] = "tag" + i;
    }
    tags[100] = "tagA";
    return tags;
  }

}
