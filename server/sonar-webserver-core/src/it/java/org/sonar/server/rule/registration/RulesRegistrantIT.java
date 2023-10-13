/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.rule.registration;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.elasticsearch.common.util.set.Sets;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.Context;
import org.sonar.api.server.rule.RuleDescriptionSection;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDescriptionSectionContextDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Scope;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.RuleDescriptionSectionsGenerator;
import org.sonar.server.rule.RuleDescriptionSectionsGeneratorResolver;
import org.sonar.server.rule.WebServerRuleFinder;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.rule.RuleStatus.READY;
import static org.sonar.api.rule.RuleStatus.REMOVED;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.RESOURCES_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;
import static org.sonar.api.server.rule.RulesDefinition.NewRepository;
import static org.sonar.api.server.rule.RulesDefinition.NewRule;
import static org.sonar.api.server.rule.RulesDefinition.OwaspTop10;
import static org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version.Y2021;
import static org.sonar.db.rule.RuleDescriptionSectionDto.DEFAULT_KEY;
import static org.sonar.db.rule.RuleDescriptionSectionDto.builder;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.DEACTIVATED;

@RunWith(DataProviderRunner.class)
public class RulesRegistrantIT {

  private static final String FAKE_PLUGIN_KEY = "unittest";
  private static final Date DATE1 = DateUtils.parseDateTime("2014-01-01T19:10:03+0100");
  private static final Date DATE2 = DateUtils.parseDateTime("2014-02-01T12:10:03+0100");
  private static final Date DATE3 = DateUtils.parseDateTime("2014-03-01T12:10:03+0100");

  private static final RuleKey EXTERNAL_RULE_KEY1 = RuleKey.of("external_eslint", "rule1");
  private static final RuleKey EXTERNAL_HOTSPOT_RULE_KEY = RuleKey.of("external_eslint", "hotspot");

  private static final RuleKey RULE_KEY1 = RuleKey.of("fake", "rule1");
  private static final RuleKey RULE_KEY2 = RuleKey.of("fake", "rule2");
  private static final RuleKey RULE_KEY3 = RuleKey.of("fake", "rule3");
  private static final RuleKey HOTSPOT_RULE_KEY = RuleKey.of("fake", "hotspot");

  private final TestSystem2 system = new TestSystem2().setNow(DATE1.getTime());

  @org.junit.Rule
  public DbTester db = DbTester.create(system);
  @org.junit.Rule
  public EsTester es = EsTester.create();
  @org.junit.Rule
  public LogTester logTester = new LogTester();

  private final QProfileRules qProfileRules = mock();
  private final WebServerRuleFinder webServerRuleFinder = mock();
  private final DbClient dbClient = db.getDbClient();
  private final MetadataIndex metadataIndex = mock();
  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));

  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;
  private RuleIndex ruleIndex;
  private final RuleDescriptionSectionsGenerator ruleDescriptionSectionsGenerator = mock();
  private final RuleDescriptionSectionsGeneratorResolver resolver = mock();

  private final RulesKeyVerifier rulesKeyVerifier = new RulesKeyVerifier();
  private final StartupRuleUpdater startupRuleUpdater = new StartupRuleUpdater(dbClient, system, uuidFactory, resolver);
  private final NewRuleCreator newRuleCreator = new NewRuleCreator(dbClient, resolver, uuidFactory, system);
  private final QualityProfileChangesUpdater qualityProfileChangesUpdater = mock();

  @Before
  public void before() {
    ruleIndexer = new RuleIndexer(es.client(), dbClient);
    ruleIndex = new RuleIndex(es.client(), system);
    activeRuleIndexer = new ActiveRuleIndexer(dbClient, es.client());
    when(resolver.generateFor(any())).thenAnswer(answer -> {
      RulesDefinition.Rule rule = answer.getArgument(0, RulesDefinition.Rule.class);
      String description = rule.htmlDescription() == null ? rule.markdownDescription() : rule.htmlDescription();

      Set<RuleDescriptionSectionDto> ruleDescriptionSectionDtos = rule.ruleDescriptionSections().stream()
        .map(s -> builder()
          .uuid(UuidFactoryFast.getInstance().create())
          .key(s.getKey())
          .content(s.getHtmlContent())
          .context(s.getContext().map(c -> RuleDescriptionSectionContextDto.of(c.getKey(), c.getDisplayName())).orElse(null))
          .build()
        )
        .collect(Collectors.toSet());
      return Sets.union(ruleDescriptionSectionDtos, Set.of(builder().uuid(UuidFactoryFast.getInstance().create()).key("default").content(description).build()));
    });

    when(ruleDescriptionSectionsGenerator.isGeneratorForRule(any())).thenReturn(true);
  }

  @Test
  public void insert_new_rules() {
    execute(new FakeRepositoryV1());

    // verify db
    assertThat(dbClient.ruleDao().selectAll(db.getSession())).hasSize(3);
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY1);
    verifyRule(rule1, RuleType.CODE_SMELL, BLOCKER);
    assertThat(rule1.isExternal()).isFalse();
    assertThat(rule1.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.CONVENTIONAL);
    assertThat(rule1.getDefaultImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity).containsOnly(tuple(SoftwareQuality.RELIABILITY, Severity.HIGH));
    assertThat(rule1.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(rule1.getDefRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(rule1.getDefRemediationBaseEffort()).isEqualTo("10h");

    RuleDto hotspotRule = dbClient.ruleDao().selectOrFailByKey(db.getSession(), HOTSPOT_RULE_KEY);
    verifyHotspot(hotspotRule);

    List<RuleParamDto> params = dbClient.ruleDao().selectRuleParamsByRuleKey(db.getSession(), RULE_KEY1);
    assertThat(params).hasSize(2);
    RuleParamDto param = getParam(params, "param1");
    assertThat(param.getDescription()).isEqualTo("parameter one");
    assertThat(param.getDefaultValue()).isEqualTo("default1");

    // verify index
    RuleDto rule2 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY2);
    assertThat(rule2.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.EFFICIENT);
    assertThat(rule2.getDefaultImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity).containsOnly(tuple(SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM));
    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getUuids()).containsOnly(rule1.getUuid(), rule2.getUuid(), hotspotRule.getUuid());
    verifyIndicesMarkedAsInitialized();

    // verify repositories
    assertThat(dbClient.ruleRepositoryDao().selectAll(db.getSession())).extracting(RuleRepositoryDto::getKey).containsOnly("fake");
  }

  private void verifyHotspot(RuleDto hotspotRule) {
    assertThat(hotspotRule.getName()).isEqualTo("Hotspot");
    assertThat(hotspotRule.getDefaultRuleDescriptionSection().getContent()).isEqualTo("Minimal hotspot");
    assertThat(hotspotRule.getCreatedAt()).isEqualTo(RulesRegistrantIT.DATE1.getTime());
    assertThat(hotspotRule.getUpdatedAt()).isEqualTo(RulesRegistrantIT.DATE1.getTime());
    assertThat(hotspotRule.getType()).isEqualTo(RuleType.SECURITY_HOTSPOT.getDbConstant());
    assertThat(hotspotRule.getSecurityStandards()).containsExactly("cwe:1", "cwe:123", "cwe:863", "owaspTop10-2021:a1", "owaspTop10-2021:a3");
    assertThat(hotspotRule.getDefaultImpacts()).isEmpty();
    assertThat(hotspotRule.getCleanCodeAttribute()).isNull();
  }

  @Test
  public void insert_new_external_rule() {
    execute(new ExternalRuleRepository());

    // verify db
    assertThat(dbClient.ruleDao().selectAll(db.getSession())).hasSize(2);
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), EXTERNAL_RULE_KEY1);
    verifyRule(rule1, RuleType.CODE_SMELL, BLOCKER);
    assertThat(rule1.isExternal()).isTrue();
    assertThat(rule1.getDefRemediationFunction()).isNull();
    assertThat(rule1.getDefRemediationGapMultiplier()).isNull();
    assertThat(rule1.getDefRemediationBaseEffort()).isNull();

    RuleDto hotspotRule = dbClient.ruleDao().selectOrFailByKey(db.getSession(), EXTERNAL_HOTSPOT_RULE_KEY);
    verifyHotspot(hotspotRule);
  }

  private void verifyRule(RuleDto rule, RuleType type, String expectedSeverity) {
    assertThat(rule.getName()).isEqualTo("One");
    assertThat(rule.getDefaultRuleDescriptionSection().getContent()).isEqualTo("Description of One");
    assertThat(rule.getSeverityString()).isEqualTo(expectedSeverity);
    assertThat(rule.getTags()).isEmpty();
    assertThat(rule.getSystemTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(rule.getConfigKey()).isEqualTo("config1");
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.getCreatedAt()).isEqualTo(DATE1.getTime());
    assertThat(rule.getScope()).isEqualTo(Scope.ALL);
    assertThat(rule.getUpdatedAt()).isEqualTo(DATE1.getTime());
    assertThat(rule.getType()).isEqualTo(type.getDbConstant());
    assertThat(rule.getPluginKey()).isEqualTo(FAKE_PLUGIN_KEY);
    assertThat(rule.isAdHoc()).isFalse();
    assertThat(rule.getEducationPrinciples()).containsOnly("concept1", "concept2", "concept3");
  }

  @Test
  public void insert_then_remove_rule() {
    String ruleKey = randomAlphanumeric(5);

    // register one rule
    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      repo.createRule(ruleKey)
        .setName(randomAlphanumeric(5))
        .setHtmlDescription(randomAlphanumeric(20));
      repo.done();
    });

    // verify db
    List<RuleDto> rules = dbClient.ruleDao().selectAll(db.getSession());
    assertThat(rules)
      .extracting(RuleDto::getKey)
      .extracting(RuleKey::rule)
      .containsExactly(ruleKey);
    RuleDto rule = rules.iterator().next();

    // verify index
    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getUuids())
      .containsExactly(rule.getUuid());
    verifyIndicesMarkedAsInitialized();

    // register no rule
    execute(context -> context.createRepository("fake", "java").done());

    // verify db
    assertThat(dbClient.ruleDao().selectAll(db.getSession()))
      .extracting(RuleDto::getKey)
      .extracting(RuleKey::rule)
      .containsExactly(ruleKey);
    assertThat(dbClient.ruleDao().selectAll(db.getSession()))
      .extracting(RuleDto::getStatus)
      .containsExactly(REMOVED);

    // verify index
    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getUuids())
      .isEmpty();
    verifyIndicesNotMarkedAsInitialized();
  }

  @Test
  public void mass_insert_then_remove_rule() {
    int numberOfRules = 5000;

    // register many rules
    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      IntStream.range(0, numberOfRules)
        .mapToObj(i -> "rule-" + i)
        .forEach(ruleKey -> repo.createRule(ruleKey)
          .setName(randomAlphanumeric(20))
          .setHtmlDescription(randomAlphanumeric(20)));
      repo.done();
    });

    // verify db
    assertThat(dbClient.ruleDao().selectAll(db.getSession()))
      .hasSize(numberOfRules)
      .extracting(RuleDto::getStatus)
      .containsOnly(READY);

    // verify index
    assertThat(es.countDocuments(RuleIndexDefinition.TYPE_RULE)).isEqualTo(numberOfRules);
    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getUuids())
      .isNotEmpty();

    // register no rule
    execute(context -> context.createRepository("fake", "java").done());

    // verify db
    assertThat(dbClient.ruleDao().selectAll(db.getSession()))
      .hasSize(numberOfRules)
      .extracting(RuleDto::getStatus)
      .containsOnly(REMOVED);

    // verify index (documents are still in the index, but all are removed)
    assertThat(es.countDocuments(RuleIndexDefinition.TYPE_RULE)).isEqualTo(numberOfRules);
    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getUuids())
      .isEmpty();
  }

  @Test
  public void delete_repositories_that_have_been_uninstalled() {
    RuleRepositoryDto repository = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    DbSession dbSession = db.getSession();
    db.getDbClient().ruleRepositoryDao().insert(dbSession, singletonList(repository));
    dbSession.commit();

    execute(new FakeRepositoryV1());

    assertThat(db.getDbClient().ruleRepositoryDao().selectAll(dbSession)).extracting(RuleRepositoryDto::getKey).containsOnly("fake");
  }

  @Test
  public void update_and_remove_rules_on_changes() {
    execute(new FakeRepositoryV1());
    assertThat(dbClient.ruleDao().selectAll(db.getSession())).hasSize(3);
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY1);
    RuleDto rule2 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY2);
    RuleDto hotspotRule = dbClient.ruleDao().selectOrFailByKey(db.getSession(), HOTSPOT_RULE_KEY);
    assertThat(es.getIds(RuleIndexDefinition.TYPE_RULE)).containsOnly(valueOf(rule1.getUuid()), valueOf(rule2.getUuid()), valueOf(hotspotRule.getUuid()));
    verifyIndicesMarkedAsInitialized();

    // user adds tags and sets markdown note
    rule1.setTags(newHashSet("usertag1", "usertag2"));
    rule1.setNoteData("user *note*");
    rule1.setNoteUserUuid("marius");
    dbClient.ruleDao().update(db.getSession(), rule1);
    db.getSession().commit();

    system.setNow(DATE2.getTime());
    execute(new FakeRepositoryV2());

    verifyIndicesNotMarkedAsInitialized();
    // rule1 has been updated
    rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY1);
    assertThatRule1IsV2(rule1);

    List<RuleParamDto> params = dbClient.ruleDao().selectRuleParamsByRuleKey(db.getSession(), RULE_KEY1);
    assertThat(params).hasSize(2);
    RuleParamDto param = getParam(params, "param1");
    assertThat(param.getDescription()).isEqualTo("parameter one v2");
    assertThat(param.getDefaultValue()).isEqualTo("default1 v2");

    // rule2 has been removed -> status set to REMOVED but db row is not deleted
    rule2 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY2);
    assertThat(rule2.getStatus()).isEqualTo(REMOVED);
    assertThat(rule2.getUpdatedAt()).isEqualTo(DATE2.getTime());

    // rule3 has been created
    RuleDto rule3 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY3);
    assertThat(rule3).isNotNull();
    assertThat(rule3.getStatus()).isEqualTo(READY);

    // verify index
    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getUuids()).containsOnly(rule1.getUuid(), rule3.getUuid());

    // verify repositories
    assertThat(dbClient.ruleRepositoryDao().selectAll(db.getSession())).extracting(RuleRepositoryDto::getKey).containsOnly("fake");

    system.setNow(DATE3.getTime());
    execute(new FakeRepositoryV3());
    rule3 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY3);
    assertThat(rule3.getDefaultRuleDescriptionSection().getContent()).isEqualTo("Rule Three V2");
    assertThat(rule3.getDescriptionFormat()).isEqualTo(RuleDto.Format.MARKDOWN);
  }

  private void assertThatRule1IsV2(RuleDto rule1) {
    assertThat(rule1.getName()).isEqualTo("One v2");
    RuleDescriptionSectionDto defaultRuleDescriptionSection = rule1.getDefaultRuleDescriptionSection();
    assertThat(defaultRuleDescriptionSection.getContent()).isEqualTo("Description of One v2");
    assertThat(defaultRuleDescriptionSection.getKey()).isEqualTo(DEFAULT_KEY);
    assertThat(rule1.getDescriptionFormat()).isEqualTo(RuleDto.Format.HTML);
    assertThat(rule1.getSeverityString()).isEqualTo(INFO);
    assertThat(rule1.getTags()).containsOnly("usertag1", "usertag2");
    assertThat(rule1.getSystemTags()).containsOnly("tag1", "tag4");
    assertThat(rule1.getConfigKey()).isEqualTo("config1 v2");
    assertThat(rule1.getNoteData()).isEqualTo("user *note*");
    assertThat(rule1.getNoteUserUuid()).isEqualTo("marius");
    assertThat(rule1.getStatus()).isEqualTo(READY);
    assertThat(rule1.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(rule1.getCreatedAt()).isEqualTo(DATE1.getTime());
    assertThat(rule1.getUpdatedAt()).isEqualTo(DATE2.getTime());
    assertThat(rule1.getEducationPrinciples()).containsOnly("concept1","concept4");
  }

  @Test
  public void add_new_tag() {
    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      repo.createRule("rule1")
        .setName("Rule One")
        .setHtmlDescription("Description of Rule One")
        .setTags("tag1");
      repo.done();
    });

    RuleDto rule = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY1);
    assertThat(rule.getSystemTags()).containsOnly("tag1");

    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      repo.createRule("rule1")
        .setName("Rule One")
        .setHtmlDescription("Description of Rule One")
        .setTags("tag1", "tag2");
      repo.done();
    });

    rule = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY1);
    assertThat(rule.getSystemTags()).containsOnly("tag1", "tag2");
  }

  @Test
  public void add_new_security_standards() {
    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      repo.createRule("rule1")
        .setName("Rule One")
        .setHtmlDescription("Description of Rule One")
        .addOwaspTop10(Y2021, OwaspTop10.A1)
        .addCwe(123);
      repo.done();
    });

    RuleDto rule = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY1);
    assertThat(rule.getSecurityStandards()).containsOnly("cwe:123", "owaspTop10-2021:a1");

    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      repo.createRule("rule1")
        .setName("Rule One")
        .setHtmlDescription("Description of Rule One")
        .addOwaspTop10(Y2021, OwaspTop10.A1, OwaspTop10.A3)
        .addCwe(1, 123, 863);
      repo.done();
    });

    rule = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY1);
    assertThat(rule.getSecurityStandards()).containsOnly("cwe:1", "cwe:123", "cwe:863", "owaspTop10-2021:a1", "owaspTop10-2021:a3");
  }

  @Test
  public void update_only_rule_name() {
    system.setNow(DATE1.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      repo.createRule("rule")
        .setName("Name1")
        .setHtmlDescription("Description");
      repo.done();
    });

    system.setNow(DATE2.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      repo.createRule("rule")
        .setName("Name2")
        .setHtmlDescription("Description");
      repo.done();
    });

    // rule1 has been updated
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of("fake", "rule"));
    assertThat(rule1.getName()).isEqualTo("Name2");
    assertThat(rule1.getDefaultRuleDescriptionSection().getContent()).isEqualTo("Description");

    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Name2"), new SearchOptions()).getTotal()).isOne();
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Name1"), new SearchOptions()).getTotal()).isZero();
  }

  @Test
  public void update_template_rule_key_should_also_update_custom_rules() {
    system.setNow(DATE1.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository("squid", "java");
      repo.createRule("rule")
        .setName("Name1")
        .setHtmlDescription("Description")
        .setTemplate(true);
      repo.done();
    });

    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of("squid", "rule"));

    // insert custom rule
    db.rules().insert(new RuleDto()
      .setRuleKey(RuleKey.of("squid", "custom"))
      .setLanguage("java")
      .setScope(Scope.ALL)
      .setTemplateUuid(rule1.getUuid())
      .setName("custom1"));
    db.commit();

    // re-key rule
    execute(context -> {
      NewRepository repo = context.createRepository("java", "java");
      repo.createRule("rule")
        .setName("Name1")
        .setHtmlDescription("Description")
        .addDeprecatedRuleKey("squid", "rule")
        .setTemplate(true);
      repo.done();
    });

    // template rule and custom rule have been updated
    rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of("java", "rule"));
    RuleDto custom = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of("java", "custom"));
  }

  @Test
  public void update_if_rule_key_renamed_and_deprecated_key_declared() {
    String ruleKey1 = "rule1";
    String ruleKey2 = "rule2";
    String repository = "fake";

    system.setNow(DATE1.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository(repository, "java");
      repo.createRule(ruleKey1)
        .setName("Name1")
        .setHtmlDescription("Description");
      repo.done();
    });

    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of(repository, ruleKey1));
    SearchIdResult<String> searchRule1 = ruleIndex.search(new RuleQuery().setQueryText("Name1"), new SearchOptions());
    assertThat(searchRule1.getUuids()).containsOnly(rule1.getUuid());
    assertThat(searchRule1.getTotal()).isOne();

    system.setNow(DATE2.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository(repository, "java");
      repo.createRule(ruleKey2)
        .setName("Name2")
        .setHtmlDescription("Description")
        .addDeprecatedRuleKey(repository, ruleKey1);
      repo.done();
    });

    // rule2 is actually rule1
    RuleDto rule2 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of(repository, ruleKey2));
    assertThat(rule2.getUuid()).isEqualTo(rule1.getUuid());
    assertThat(rule2.getName()).isEqualTo("Name2");
    assertThat(rule2.getDefaultRuleDescriptionSection().getContent()).isEqualTo(rule1.getDefaultRuleDescriptionSection().getContent());

    SearchIdResult<String> searchRule2 = ruleIndex.search(new RuleQuery().setQueryText("Name2"), new SearchOptions());
    assertThat(searchRule2.getUuids()).containsOnly(rule2.getUuid());
    assertThat(searchRule2.getTotal()).isOne();
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Name1"), new SearchOptions()).getTotal()).isZero();
  }

  @Test
  public void update_if_repository_changed_and_deprecated_key_declared() {
    String ruleKey = "rule";
    String repository1 = "fake1";
    String repository2 = "fake2";

    system.setNow(DATE1.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository(repository1, "java");
      repo.createRule(ruleKey)
        .setName("Name1")
        .setHtmlDescription("Description");
      repo.done();
    });

    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of(repository1, ruleKey));
    SearchIdResult<String> searchRule1 = ruleIndex.search(new RuleQuery().setQueryText("Name1"), new SearchOptions());
    assertThat(searchRule1.getUuids()).containsOnly(rule1.getUuid());
    assertThat(searchRule1.getTotal()).isOne();

    system.setNow(DATE2.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository(repository2, "java");
      repo.createRule(ruleKey)
        .setName("Name2")
        .setHtmlDescription("Description")
        .addDeprecatedRuleKey(repository1, ruleKey);
      repo.done();
    });

    // rule2 is actually rule1
    RuleDto rule2 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of(repository2, ruleKey));
    assertThat(rule2.getUuid()).isEqualTo(rule1.getUuid());
    assertThat(rule2.getName()).isEqualTo("Name2");
    assertThat(rule2.getDefaultRuleDescriptionSection().getContent()).isEqualTo(rule1.getDefaultRuleDescriptionSection().getContent());

    SearchIdResult<String> searchRule2 = ruleIndex.search(new RuleQuery().setQueryText("Name2"), new SearchOptions());
    assertThat(searchRule2.getUuids()).containsOnly(rule2.getUuid());
    assertThat(searchRule2.getTotal()).isOne();
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Name1"), new SearchOptions()).getTotal()).isZero();
  }

  @Test
  @UseDataProvider("allRenamingCases")
  public void update_if_only_renamed_and_deprecated_key_declared(String ruleKey1, String repo1, String ruleKey2, String repo2) {
    String name = "Name1";
    String description = "Description";
    system.setNow(DATE1.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository(repo1, "java");
      repo.createRule(ruleKey1)
        .setName(name)
        .setHtmlDescription(description);
      repo.done();
    });

    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of(repo1, ruleKey1));
    assertThat(ruleIndex.search(new RuleQuery().setQueryText(name), new SearchOptions()).getUuids())
      .containsOnly(rule1.getUuid());

    system.setNow(DATE2.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository(repo2, "java");
      repo.createRule(ruleKey2)
        .setName(name)
        .setHtmlDescription(description)
        .addDeprecatedRuleKey(repo1, ruleKey1);
      repo.done();
    });

    // rule2 is actually rule1
    RuleDto rule2 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of(repo2, ruleKey2));
    assertThat(rule2.getUuid()).isEqualTo(rule1.getUuid());
    assertThat(rule2.getName()).isEqualTo(rule1.getName());
    assertThat(rule2.getDefaultRuleDescriptionSection().getContent()).isEqualTo(rule1.getDefaultRuleDescriptionSection().getContent());

    assertThat(ruleIndex.search(new RuleQuery().setQueryText(name), new SearchOptions()).getUuids())
      .containsOnly(rule2.getUuid());
  }

  @DataProvider
  public static Object[][] allRenamingCases() {
    return new Object[][]{
      {"repo1", "rule1", "repo1", "rule2"},
      {"repo1", "rule1", "repo2", "rule1"},
      {"repo1", "rule1", "repo2", "rule2"},
    };
  }

  @Test
  public void update_if_repository_and_key_changed_and_deprecated_key_declared_among_others() {
    String ruleKey1 = "rule1";
    String ruleKey2 = "rule2";
    String repository1 = "fake1";
    String repository2 = "fake2";

    system.setNow(DATE1.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository(repository1, "java");
      repo.createRule(ruleKey1)
        .setName("Name1")
        .setHtmlDescription("Description");
      repo.done();
    });

    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of(repository1, ruleKey1));
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Name1"), new SearchOptions()).getUuids())
      .containsOnly(rule1.getUuid());

    system.setNow(DATE2.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository(repository2, "java");
      repo.createRule(ruleKey2)
        .setName("Name2")
        .setHtmlDescription("Description")
        .addDeprecatedRuleKey("foo", "bar")
        .addDeprecatedRuleKey(repository1, ruleKey1)
        .addDeprecatedRuleKey("some", "noise");
      repo.done();
    });

    // rule2 is actually rule1
    RuleDto rule2 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of(repository2, ruleKey2));
    assertThat(rule2.getUuid()).isEqualTo(rule1.getUuid());

    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Name2"), new SearchOptions()).getUuids())
      .containsOnly(rule1.getUuid());
  }

  @Test
  public void update_only_rule_description() {
    system.setNow(DATE1.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      repo.createRule("rule")
        .setName("Name")
        .setHtmlDescription("Desc1");
      repo.done();
    });

    system.setNow(DATE2.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      repo.createRule("rule")
        .setName("Name")
        .setHtmlDescription("Desc2");
      repo.done();
    });

    // rule1 has been updated
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of("fake", "rule"));
    assertThat(rule1.getName()).isEqualTo("Name");
    assertThat(rule1.getDefaultRuleDescriptionSection().getContent()).isEqualTo("Desc2");

    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Desc2"), new SearchOptions()).getTotal()).isOne();
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Desc1"), new SearchOptions()).getTotal()).isZero();
  }

  @Test
  public void update_several_rule_descriptions() {
    system.setNow(DATE1.getTime());

    RuleDescriptionSection section1context1 = createRuleDescriptionSection(HOW_TO_FIX_SECTION_KEY, "section1 ctx1 content", "ctx_1");
    RuleDescriptionSection section1context2 = createRuleDescriptionSection(HOW_TO_FIX_SECTION_KEY, "section1 ctx2 content", "ctx_2");
    RuleDescriptionSection section2context1 = createRuleDescriptionSection(RESOURCES_SECTION_KEY, "section2 content", "ctx_1");
    RuleDescriptionSection section2context2 = createRuleDescriptionSection(RESOURCES_SECTION_KEY,"section2 ctx2 content", "ctx_2");
    RuleDescriptionSection section3noContext = createRuleDescriptionSection(ASSESS_THE_PROBLEM_SECTION_KEY, "section3 content", null);
    RuleDescriptionSection section4noContext = createRuleDescriptionSection(ROOT_CAUSE_SECTION_KEY, "section4 content", null);
    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      repo.createRule("rule")
        .setName("Name")
        .addDescriptionSection(section1context1)
        .addDescriptionSection(section1context2)
        .addDescriptionSection(section2context1)
        .addDescriptionSection(section2context2)
        .addDescriptionSection(section3noContext)
        .addDescriptionSection(section4noContext)
        .setHtmlDescription("Desc1");
      repo.done();
    });

    RuleDescriptionSection section1context2updated = createRuleDescriptionSection(HOW_TO_FIX_SECTION_KEY, "section1 ctx2 updated content", "ctx_2");
    RuleDescriptionSection section2updatedWithoutContext = createRuleDescriptionSection(RESOURCES_SECTION_KEY, section2context1.getHtmlContent(), null);
    RuleDescriptionSection section4updatedWithContext1 = createRuleDescriptionSection(ROOT_CAUSE_SECTION_KEY, section4noContext.getHtmlContent(), "ctx_1");
    RuleDescriptionSection section4updatedWithContext2 = createRuleDescriptionSection(ROOT_CAUSE_SECTION_KEY, section4noContext.getHtmlContent(), "ctx_2");
    system.setNow(DATE2.getTime());
    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      repo.createRule("rule")
        .setName("Name")
        .addDescriptionSection(section1context1)
        .addDescriptionSection(section1context2updated)
        .addDescriptionSection(section2updatedWithoutContext)
        .addDescriptionSection(section3noContext)
        .addDescriptionSection(section4updatedWithContext1)
        .addDescriptionSection(section4updatedWithContext2)
        .setHtmlDescription("Desc2");
      repo.done();

    });

    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RuleKey.of("fake", "rule"));
    assertThat(rule1.getName()).isEqualTo("Name");
    assertThat(rule1.getDefaultRuleDescriptionSection().getContent()).isEqualTo("Desc2");

    Set<RuleDescriptionSection> expectedSections = Set.of(section1context1, section1context2updated,
      section2updatedWithoutContext, section3noContext, section4updatedWithContext1, section4updatedWithContext2);
    assertThat(rule1.getRuleDescriptionSectionDtos()).hasSize(expectedSections.size() + 1);
    expectedSections.forEach(apiSection -> assertSectionExists(apiSection, rule1.getRuleDescriptionSectionDtos()));
  }

  private static RuleDescriptionSection createRuleDescriptionSection(String sectionKey, String description, @Nullable String contextKey) {
    Context context = Optional.ofNullable(contextKey).map(key -> new Context(contextKey, contextKey + randomAlphanumeric(10))).orElse(null);
    return RuleDescriptionSection.builder().sectionKey(sectionKey)
      .htmlContent(description)
      .context(context)
      .build();
  }

  private static void assertSectionExists(RuleDescriptionSection apiSection, Set<RuleDescriptionSectionDto> sectionDtos) {
    sectionDtos.stream()
      .filter(sectionDto -> sectionDto.getKey().equals(apiSection.getKey()) && sectionDto.getContent().equals(apiSection.getHtmlContent()))
      .filter(sectionDto -> isSameContext(apiSection.getContext(), sectionDto.getContext()))
      .findAny()
      .orElseThrow(() -> new AssertionError(format("Impossible to find a section dto matching the API section %s", apiSection.getKey())));
  }

  private static boolean isSameContext(Optional<Context> apiContext, @Nullable RuleDescriptionSectionContextDto contextDto) {
    if (apiContext.isEmpty() && contextDto == null) {
      return true;
    }
    return apiContext.filter(context -> isSameContext(context, contextDto)).isPresent();
  }

  private static boolean isSameContext(Context apiContext, @Nullable RuleDescriptionSectionContextDto contextDto) {
    if (contextDto == null) {
      return false;
    }
    return Objects.equals(apiContext.getKey(), contextDto.getKey()) && Objects.equals(apiContext.getDisplayName(), contextDto.getDisplayName());
  }

  @Test
  public void rule_previously_created_as_adhoc_becomes_none_adhoc() {
    RuleDto rule = db.rules().insert(r -> r.setRepositoryKey("external_fake").setIsExternal(true).setIsAdHoc(true));
    system.setNow(DATE2.getTime());
    execute(context -> {
      NewRepository repo = context.createExternalRepository("fake", rule.getLanguage());
      repo.createRule(rule.getRuleKey())
        .setName(rule.getName())
        .setHtmlDescription(rule.getDefaultRuleDescriptionSection().getContent());
      repo.done();
    });

    RuleDto reloaded = dbClient.ruleDao().selectByKey(db.getSession(), rule.getKey()).get();
    assertThat(reloaded.isAdHoc()).isFalse();
  }

  @Test
  public void remove_no_more_defined_external_rule() {
    RuleDto rule = db.rules().insert(r -> r.setRepositoryKey("external_fake")
      .setStatus(READY)
      .setIsExternal(true)
      .setIsAdHoc(false));

    execute();

    RuleDto reloaded = dbClient.ruleDao().selectByKey(db.getSession(), rule.getKey()).get();
    assertThat(reloaded.getStatus()).isEqualTo(REMOVED);
  }

  @Test
  public void do_not_remove_no_more_defined_ad_hoc_rule() {
    RuleDto rule = db.rules().insert(r -> r.setRepositoryKey("external_fake")
      .setStatus(READY)
      .setIsExternal(true)
      .setIsAdHoc(true));

    execute();

    RuleDto reloaded = dbClient.ruleDao().selectByKey(db.getSession(), rule.getKey()).get();
    assertThat(reloaded.getStatus()).isEqualTo(READY);
  }

  @Test
  public void disable_then_enable_rule() {
    // Install rule
    system.setNow(DATE1.getTime());
    execute(new FakeRepositoryV1());

    // Uninstall rule
    system.setNow(DATE2.getTime());
    execute();

    RuleDto rule = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY1);
    assertThat(rule.getStatus()).isEqualTo(REMOVED);
    assertThat(ruleIndex.search(new RuleQuery().setKey(RULE_KEY1.toString()), new SearchOptions()).getTotal()).isZero();

    // Re-install rule
    system.setNow(DATE3.getTime());
    execute(new FakeRepositoryV1());

    rule = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY1);
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.BETA);
    assertThat(ruleIndex.search(new RuleQuery().setKey(RULE_KEY1.toString()), new SearchOptions()).getTotal()).isOne();
  }

  @Test
  public void do_not_update_rules_when_no_changes() {
    execute(new FakeRepositoryV1());
    assertThat(dbClient.ruleDao().selectAll(db.getSession())).hasSize(3);

    system.setNow(DATE2.getTime());
    execute(new FakeRepositoryV1());

    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY1);
    assertThat(rule1.getCreatedAt()).isEqualTo(DATE1.getTime());
    assertThat(rule1.getUpdatedAt()).isEqualTo(DATE1.getTime());
  }

  @Test
  public void do_not_update_already_removed_rules() {
    execute(new FakeRepositoryV1());
    assertThat(dbClient.ruleDao().selectAll(db.getSession())).hasSize(3);

    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY1);
    RuleDto rule2 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY2);
    RuleDto hotspotRule = dbClient.ruleDao().selectOrFailByKey(db.getSession(), HOTSPOT_RULE_KEY);
    assertThat(es.getIds(RuleIndexDefinition.TYPE_RULE)).containsOnly(valueOf(rule1.getUuid()), valueOf(rule2.getUuid()), valueOf(hotspotRule.getUuid()));

    assertThat(rule2.getStatus()).isEqualTo(READY);

    system.setNow(DATE2.getTime());
    execute(new FakeRepositoryV2());

    // On MySQL, need to update a rule otherwise rule2 will be seen as READY, but why ???
    dbClient.ruleDao().update(db.getSession(), rule1);
    db.getSession().commit();

    // rule2 is removed
    rule2 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY2);
    RuleDto rule3 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY3);
    assertThat(rule2.getStatus()).isEqualTo(REMOVED);

    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getUuids()).containsOnly(rule1.getUuid(), rule3.getUuid());

    system.setNow(DATE3.getTime());
    execute(new FakeRepositoryV2());
    db.getSession().commit();

    // -> rule2 is still removed, but not update at DATE3
    rule2 = dbClient.ruleDao().selectOrFailByKey(db.getSession(), RULE_KEY2);
    assertThat(rule2.getStatus()).isEqualTo(REMOVED);
    assertThat(rule2.getUpdatedAt()).isEqualTo(DATE2.getTime());

    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getUuids()).containsOnly(rule1.getUuid(), rule3.getUuid());
  }

  @Test
  public void mass_insert() {
    execute(new BigRepository());
    assertThat(db.countRowsOfTable("rules")).isEqualTo(BigRepository.SIZE);
    assertThat(db.countRowsOfTable("rules_parameters")).isEqualTo(BigRepository.SIZE * 20);
    assertThat(es.getIds(RuleIndexDefinition.TYPE_RULE)).hasSize(BigRepository.SIZE);
  }

  @Test
  public void manage_repository_extensions() {
    execute(new FindbugsRepository(), new FbContribRepository());
    List<RuleDto> rules = dbClient.ruleDao().selectAll(db.getSession());
    assertThat(rules).hasSize(2);
    for (RuleDto rule : rules) {
      assertThat(rule.getRepositoryKey()).isEqualTo("findbugs");
    }
  }

  @Test
  public void remove_system_tags_when_plugin_does_not_provide_any() {
    // Rule already exists in DB, with some system tags
    db.rules().insert(new RuleDto()
      .setRuleKey("rule1")
      .setRepositoryKey("findbugs")
      .setName("Rule One")
      .setType(RuleType.CODE_SMELL)
      .setScope(Scope.ALL)
      .addRuleDescriptionSectionDto(createDefaultRuleDescriptionSection(uuidFactory.create(), "Rule one description"))
      .setDescriptionFormat(RuleDto.Format.HTML)
      .setSystemTags(newHashSet("tag1", "tag2")));
    db.getSession().commit();

    // Synchronize rule without tag
    execute(new FindbugsRepository());

    List<RuleDto> rules = dbClient.ruleDao().selectAll(db.getSession());
    assertThat(rules).hasSize(1).extracting(RuleDto::getKey, RuleDto::getSystemTags)
      .containsOnly(tuple(RuleKey.of("findbugs", "rule1"), emptySet()));
  }

  @Test
  public void rules_that_deprecate_previous_rule_must_be_recorded() {
    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      createRule(repo, "rule1");
      repo.done();
    });

    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      createRule(repo, "newKey")
        .addDeprecatedRuleKey("fake", "rule1")
        .addDeprecatedRuleKey("fake", "rule2");
      repo.done();
    });

    List<RuleDto> rules = dbClient.ruleDao().selectAll(db.getSession());
    Set<DeprecatedRuleKeyDto> deprecatedRuleKeys = dbClient.ruleDao().selectAllDeprecatedRuleKeys(db.getSession());
    assertThat(rules).hasSize(1);
    assertThat(deprecatedRuleKeys).hasSize(2);
  }

  @Test
  public void rules_that_remove_deprecated_key_must_remove_records() {
    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      createRule(repo, "rule1");
      repo.done();
    });

    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      createRule(repo, "newKey")
        .addDeprecatedRuleKey("fake", "rule1")
        .addDeprecatedRuleKey("fake", "rule2");
      repo.done();
    });

    assertThat(dbClient.ruleDao().selectAll(db.getSession())).hasSize(1);
    Set<DeprecatedRuleKeyDto> deprecatedRuleKeys = dbClient.ruleDao().selectAllDeprecatedRuleKeys(db.getSession());
    assertThat(deprecatedRuleKeys).hasSize(2);

    execute(context -> {
      NewRepository repo = context.createRepository("fake", "java");
      createRule(repo, "newKey");
      repo.done();
    });

    assertThat(dbClient.ruleDao().selectAll(db.getSession())).hasSize(1);
    deprecatedRuleKeys = dbClient.ruleDao().selectAllDeprecatedRuleKeys(db.getSession());
    assertThat(deprecatedRuleKeys).isEmpty();
  }

  @Test
  public void declaring_two_rules_with_same_deprecated_RuleKey_should_throw_ISE() {
    assertThatThrownBy(() -> {
      execute(context -> {
        NewRepository repo = context.createRepository("fake", "java");
        createRule(repo, "newKey1")
          .addDeprecatedRuleKey("fake", "old");
        createRule(repo, "newKey2")
          .addDeprecatedRuleKey("fake", "old");
        repo.done();
      });
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("The following deprecated rule keys are declared at least twice [fake:old]");
  }

  @Test
  public void declaring_a_rule_with_a_deprecated_RuleKey_still_used_should_throw_ISE() {
    assertThatThrownBy(() -> {
      execute(context -> {
        NewRepository repo = context.createRepository("fake", "java");
        createRule(repo, "newKey1");
        createRule(repo, "newKey2")
          .addDeprecatedRuleKey("fake", "newKey1");
        repo.done();
      });
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("The following rule keys are declared both as deprecated and used key [fake:newKey1]");
  }

  @Test
  public void updating_the_deprecated_to_a_new_ruleKey_should_throw_an_ISE() {
    // On this new rule add a deprecated key
    execute(context -> createRule(context, "javascript", "javascript", "s103",
      r -> r.addDeprecatedRuleKey("javascript", "linelength")));

    assertThatThrownBy(() -> {
      // This rule should have been moved to another repository
      execute(context -> createRule(context, "javascript", "sonarjs", "s103",
        r -> r.addDeprecatedRuleKey("javascript", "linelength")));
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("An incorrect state of deprecated rule keys has been detected.\n " +
        "The deprecated rule key [javascript:linelength] was previously deprecated by [javascript:s103]. [javascript:s103] should be a deprecated key of [sonarjs:s103],");
  }

  @Test
  public void deprecate_rule_that_deprecated_another_rule() {
    execute(context -> createRule(context, "javascript", "javascript", "s103"));
    execute(context -> createRule(context, "javascript", "javascript", "s104",
      r -> r.addDeprecatedRuleKey("javascript", "s103")));

    // This rule should have been moved to another repository
    execute(context -> createRule(context, "javascript", "sonarjs", "s105",
      r -> r.addDeprecatedRuleKey("javascript", "s103")
        .addDeprecatedRuleKey("javascript", "s104")));
  }

  @Test
  public void declaring_a_rule_with_an_existing_RuleKey_still_used_should_throw_IAE() {
    assertThatThrownBy(() -> {
      execute(context -> {
        NewRepository repo = context.createRepository("fake", "java");
        createRule(repo, "newKey1");
        createRule(repo, "newKey1");
        repo.done();
      });
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The rule 'newKey1' of repository 'fake' is declared several times");
  }

  @Test
  public void removed_rule_should_appear_in_changelog() {
    //GIVEN
    QProfileDto qProfileDto = db.qualityProfiles().insert();
    RuleDto ruleDto = db.rules().insert(RULE_KEY1);
    db.qualityProfiles().activateRule(qProfileDto, ruleDto);
    ActiveRuleChange arChange = new ActiveRuleChange(DEACTIVATED, ActiveRuleDto.createFor(qProfileDto, ruleDto), ruleDto);
    when(qProfileRules.deleteRule(any(DbSession.class), eq(ruleDto))).thenReturn(List.of(arChange));
    //WHEN
    execute(context -> context.createRepository("fake", "java").done());
    //THEN
    List<QProfileChangeDto> qProfileChangeDtos = dbClient.qProfileChangeDao().selectByQuery(db.getSession(), new QProfileChangeQuery(qProfileDto.getKee()));
    assertThat(qProfileChangeDtos).extracting(QProfileChangeDto::getRulesProfileUuid, QProfileChangeDto::getChangeType, QProfileChangeDto::getSqVersion)
      .contains(tuple(qProfileDto.getRulesProfileUuid(), "DEACTIVATED", sonarQubeVersion.toString()));
  }

  @Test
  public void removed_rule_should_be_deleted_when_renamed_repository() {
    //GIVEN
    RuleDto removedRuleDto = db.rules().insert(RuleKey.of("old_repo", "removed_rule"));
    RuleDto renamedRuleDto = db.rules().insert(RuleKey.of("old_repo", "renamed_rule"));
    //WHEN
    execute(context -> createRule(context, "java", "new_repo", renamedRuleDto.getRuleKey(),
      rule -> rule.addDeprecatedRuleKey(renamedRuleDto.getRepositoryKey(), renamedRuleDto.getRuleKey())));
    //THEN
    verify(qProfileRules).deleteRule(any(DbSession.class), eq(removedRuleDto));
  }

  private void execute(RulesDefinition... defs) {
    ServerPluginRepository pluginRepository = mock(ServerPluginRepository.class);
    when(pluginRepository.getPluginKey(any(RulesDefinition.class))).thenReturn(FAKE_PLUGIN_KEY);
    RuleDefinitionsLoader loader = new RuleDefinitionsLoader(pluginRepository, defs);
    Languages languages = mock(Languages.class);
    when(languages.get(any())).thenReturn(mock(Language.class));
    reset(webServerRuleFinder);

    RulesRegistrant task = new RulesRegistrant(loader, qProfileRules, dbClient, ruleIndexer, activeRuleIndexer, languages, system, webServerRuleFinder, metadataIndex,
      rulesKeyVerifier, startupRuleUpdater, newRuleCreator, qualityProfileChangesUpdater, sonarQubeVersion);
    task.start();
    // Execute a commit to refresh session state as the task is using its own session
    db.getSession().commit();

    verify(webServerRuleFinder).startCaching();
  }

  private NewRule createRule(NewRepository repo, String key) {
    return repo.createRule(key)
      .setName(key + " name")
      .setHtmlDescription("Description of " + key)
      .setSeverity(BLOCKER)
      .setInternalKey("config1")
      .setTags("tag1", "tag2", "tag3")
      .setType(RuleType.CODE_SMELL)
      .setStatus(RuleStatus.BETA);
  }

  @SafeVarargs
  private void createRule(RulesDefinition.Context context, String language, String repositoryKey, String ruleKey, Consumer<NewRule>... consumers) {
    NewRepository repo = context.createRepository(repositoryKey, language);
    NewRule newRule = repo.createRule(ruleKey)
      .setName(ruleKey)
      .setHtmlDescription("Description of One")
      .setSeverity(BLOCKER)
      .setType(RuleType.CODE_SMELL)
      .setStatus(RuleStatus.BETA);

    Arrays.stream(consumers).forEach(c -> c.accept(newRule));
    repo.done();
  }

  private void verifyIndicesMarkedAsInitialized() {
    verify(metadataIndex).setInitialized(RuleIndexDefinition.TYPE_RULE, true);
    verify(metadataIndex).setInitialized(RuleIndexDefinition.TYPE_ACTIVE_RULE, true);
    reset(metadataIndex);
  }

  private void verifyIndicesNotMarkedAsInitialized() {
    verifyNoInteractions(metadataIndex);
  }

  private RuleParamDto getParam(List<RuleParamDto> params, String key) {
    for (RuleParamDto param : params) {
      if (param.getName().equals(key)) {
        return param;
      }
    }
    return null;
  }

  static class FakeRepositoryV1 implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("fake", "java");
      NewRule rule1 = repo.createRule(RULE_KEY1.rule())
        .setName("One")
        .setHtmlDescription("Description of One")
        .setSeverity(BLOCKER)
        .setInternalKey("config1")
        .setTags("tag1", "tag2", "tag3")
        .setScope(RuleScope.ALL)
        .setType(RuleType.CODE_SMELL)
        .setStatus(RuleStatus.BETA)
        .setGapDescription("java.S115.effortToFix")
        .addEducationPrincipleKeys("concept1", "concept2", "concept3")
        .addDefaultImpact(SoftwareQuality.RELIABILITY, Severity.HIGH);
      rule1.setDebtRemediationFunction(rule1.debtRemediationFunctions().linearWithOffset("5d", "10h"));

      rule1.createParam("param1").setDescription("parameter one").setDefaultValue("default1");
      rule1.createParam("param2").setDescription("parameter two").setDefaultValue("default2");

      repo.createRule(HOTSPOT_RULE_KEY.rule())
        .setName("Hotspot")
        .setHtmlDescription("Minimal hotspot")
        .setType(RuleType.SECURITY_HOTSPOT)
        .addOwaspTop10(Y2021, OwaspTop10.A1, OwaspTop10.A3)
        .addCwe(1, 123, 863);

      repo.createRule(RULE_KEY2.rule())
        .setName("Two")
        .setHtmlDescription("Minimal rule")
        .setCleanCodeAttribute(CleanCodeAttribute.EFFICIENT);
      repo.done();
    }
  }

  /**
   * FakeRepositoryV1 with some changes
   */
  static class FakeRepositoryV2 implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("fake", "java");

      // almost all the attributes of rule1 are changed
      NewRule rule1 = repo.createRule(RULE_KEY1.rule())
        .setName("One v2")
        .setHtmlDescription("Description of One v2")
        .setSeverity(INFO)
        .setInternalKey("config1 v2")
        // tag2 and tag3 removed, tag4 added
        .setTags("tag1", "tag4")
        .setType(RuleType.BUG)
        .setStatus(READY)
        .setGapDescription("java.S115.effortToFix.v2")
        .addEducationPrincipleKeys("concept1", "concept4");
      rule1.setDebtRemediationFunction(rule1.debtRemediationFunctions().linearWithOffset("6d", "2h"));
      rule1.createParam("param1").setDescription("parameter one v2").setDefaultValue("default1 v2");
      rule1.createParam("param2").setDescription("parameter two v2").setDefaultValue("default2 v2");

      // rule2 is dropped, rule3 is new
      repo.createRule(RULE_KEY3.rule())
        .setName("Three")
        .setHtmlDescription("Rule Three");

      repo.done();
    }
  }

  static class FakeRepositoryV3 implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("fake", "java");
      // rule 3 is dropped
      repo.createRule(RULE_KEY3.rule())
        .setName("Three")
        .setMarkdownDescription("Rule Three V2");

      repo.done();
    }
  }

  static class ExternalRuleRepository implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createExternalRepository("eslint", "js");
      repo.createRule(RULE_KEY1.rule())
        .setName("One")
        .setHtmlDescription("Description of One")
        .setSeverity(BLOCKER)
        .setInternalKey("config1")
        .setTags("tag1", "tag2", "tag3")
        .setScope(RuleScope.ALL)
        .setType(RuleType.CODE_SMELL)
        .setStatus(RuleStatus.BETA)
        .addEducationPrincipleKeys("concept1", "concept2", "concept3");

      repo.createRule(EXTERNAL_HOTSPOT_RULE_KEY.rule())
        .setName("Hotspot")
        .setHtmlDescription("Minimal hotspot")
        .setType(RuleType.SECURITY_HOTSPOT)
        .addOwaspTop10(Y2021, OwaspTop10.A1, OwaspTop10.A3)
        .addCwe(1, 123, 863);

      repo.done();
    }
  }

  static class BigRepository implements RulesDefinition {
    static final int SIZE = 500;

    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("big", "java");
      for (int i = 0; i < SIZE; i++) {
        NewRule rule = repo.createRule("rule" + i)
          .setName("name of " + i)
          .setHtmlDescription("description of " + i);
        for (int j = 0; j < 20; j++) {
          rule.createParam("param" + j);
        }

      }
      repo.done();
    }
  }

  static class FindbugsRepository implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("findbugs", "java");
      repo.createRule("rule1")
        .setName("Rule One")
        .setHtmlDescription("Description of Rule One");
      repo.done();
    }
  }

  static class FbContribRepository implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewExtendedRepository repo = context.createRepository("findbugs", "java");
      repo.createRule("rule2")
        .setName("Rule Two")
        .setHtmlDescription("Description of Rule Two");
      repo.done();
    }
  }
}
