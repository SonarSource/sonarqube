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
package org.sonar.server.qualityprofile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.util.UtcDateUtils.formatDateTime;

public class DefinedQProfileCreationImplTest {
  private static final Language FOO_LANGUAGE = LanguageTesting.newLanguage("foo", "foo", "foo");
  private static final String TABLE_RULES_PROFILES = "RULES_PROFILES";
  private static final String TABLE_LOADED_TEMPLATES = "loaded_templates";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public DefinedQProfileRepositoryRule definedQProfileRepositoryRule = new DefinedQProfileRepositoryRule();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbClient.openSession(false);
  private UuidFactory mockedUuidFactory = mock(UuidFactory.class);
  private System2 mockedSystem2 = mock(System2.class);
  private RuleActivator mockedRuleActivator = mock(RuleActivator.class);
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private DefinedQProfileCreationImpl underTest = new DefinedQProfileCreationImpl(
    dbClient,
    new QProfileFactory(dbClient, mockedUuidFactory, mockedSystem2, activeRuleIndexer),
    mockedRuleActivator);
  private List<ActiveRuleChange> activeRuleChanges = new ArrayList<>();

  @After
  public void tearDown() throws Exception {
    dbSession.close();
  }

  @Test
  public void create_creates_qp_and_store_flag_in_loaded_templates_for_specified_organization() {
    OrganizationDto organization = dbTester.organizations().insert();
    DefinedQProfile definedQProfile = definedQProfileRepositoryRule.create(FOO_LANGUAGE, "foo1", false);
    long date = 2_456_789L;
    String uuid = "uuid 1";
    mockForSingleQPInsert(uuid, date);

    underTest.create(dbSession, definedQProfile, organization, activeRuleChanges);
    dbSession.commit();

    QualityProfileDto dto = getPersistedQP(organization, FOO_LANGUAGE, "foo1");
    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getOrganizationUuid()).isEqualTo(organization.getUuid());
    assertThat(dto.getLanguage()).isEqualTo(FOO_LANGUAGE.getKey());
    assertThat(dto.getName()).isEqualTo("foo1");
    assertThat(dto.getKee()).isEqualTo(uuid);
    assertThat(dto.getKey()).isEqualTo(dto.getKee());
    assertThat(dto.getParentKee()).isNull();
    assertThat(dto.getRulesUpdatedAt()).isIn(formatDateTime(new Date(date)));
    assertThat(dto.getLastUsed()).isNull();
    assertThat(dto.getUserUpdatedAt()).isNull();
    assertThat(dto.isDefault()).isFalse();
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), TABLE_RULES_PROFILES)).isEqualTo(1);

    assertThat(dbClient.loadedTemplateDao().countByTypeAndKey(definedQProfile.getLoadedTemplateType(), organization.getUuid(), dbTester.getSession()))
      .isEqualTo(1);
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), TABLE_LOADED_TEMPLATES)).isEqualTo(1);
    assertThat(activeRuleChanges).isEmpty();
  }

  @Test
  public void create_persists_default_flag_of_DefinedQProfile() {
    OrganizationDto organization = dbTester.organizations().insert();
    DefinedQProfile definedQProfile = definedQProfileRepositoryRule.create(FOO_LANGUAGE, "foo1", true);
    mockForSingleQPInsert();

    underTest.create(dbSession, definedQProfile, organization, activeRuleChanges);
    dbSession.commit();

    assertThat(getPersistedQP(organization, FOO_LANGUAGE, "foo1").isDefault()).isTrue();
    assertThat(activeRuleChanges).isEmpty();
  }

  @Test
  public void create_does_not_update_existing_profile_if_it_already_exists() {
    OrganizationDto organization = dbTester.organizations().insert();
    DefinedQProfile definedQProfile = definedQProfileRepositoryRule.create(FOO_LANGUAGE, "foo1", false);
    long date = 2_456_789L;
    String uuid = "uuid 1";
    mockForSingleQPInsert(uuid, date);
    QualityProfileDto existing = dbTester.qualityProfiles().insertQualityProfile(
      QualityProfileDto.createFor("a key")
        .setName(definedQProfile.getName())
        .setLanguage(definedQProfile.getLanguage())
        .setOrganizationUuid(organization.getUuid()));

    underTest.create(dbSession, definedQProfile, organization, activeRuleChanges);
    dbSession.commit();

    QualityProfileDto dto = getPersistedQP(organization, FOO_LANGUAGE, "foo1");
    assertThat(dto.getId()).isEqualTo(existing.getId());
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), TABLE_RULES_PROFILES)).isEqualTo(1);
  }

  @Test
  public void create_persists_relies_on_ruleActivator_to_persist_activerules_and_return_all_changes_in_order() {
    List<CallLog> callLogs = new ArrayList<>();
    ActiveRuleChange[] changes = {newActiveRuleChange("0"), newActiveRuleChange("1"), newActiveRuleChange("2"),
      newActiveRuleChange("3"), newActiveRuleChange("4")};
    mockRuleActivatorActivate(callLogs,
      asList(changes[4], changes[1]),
      Collections.emptyList(),
      Collections.singletonList(changes[0]),
      Collections.emptyList(),
      asList(changes[2], changes[3]));

    OrganizationDto organization = dbTester.organizations().insert();
    DefinedQProfile definedQProfile = definedQProfileRepositoryRule.create(FOO_LANGUAGE, "foo1", true,
      activeRule("A", RulePriority.INFO), activeRule("B", RulePriority.MINOR), activeRule("C", RulePriority.MAJOR),
      activeRule("D", RulePriority.CRITICAL), activeRule("E", RulePriority.BLOCKER));
    mockForSingleQPInsert();

    underTest.create(dbSession, definedQProfile, organization, activeRuleChanges);
    dbSession.commit();

    assertThat(callLogs)
      .hasSize(5);
    assertThat(activeRuleChanges)
      .containsExactly(changes[4], changes[1], changes[0], changes[2], changes[3]);
  }

  @Test
  public void create_creates_ruleKey_in_RuleActivation_from_ActiveRule() {
    List<CallLog> callLogs = new ArrayList<>();
    mockRuleActivatorActivate(callLogs, Collections.emptyList());

    OrganizationDto organization = dbTester.organizations().insert();
    ActiveRule activeRule = activeRule("A", RulePriority.BLOCKER);
    DefinedQProfile definedQProfile = definedQProfileRepositoryRule.create(FOO_LANGUAGE, "foo1", true, activeRule);
    mockForSingleQPInsert();

    underTest.create(dbSession, definedQProfile, organization, activeRuleChanges);
    dbSession.commit();

    assertThat(callLogs)
      .hasSize(1);
    assertThat(callLogs.get(0).getRuleActivation())
      .matches(s -> s.getRuleKey().equals(RuleKey.of(activeRule.getRepositoryKey(), activeRule.getRuleKey())));
    assertThat(callLogs.get(0).qualityProfileDto.getId())
      .isEqualTo(getPersistedQP(organization, FOO_LANGUAGE, "foo1").getId());
  }

  @Test
  public void create_supports_all_RulePriority_values_and_null() {
    List<CallLog> callLogs = new ArrayList<>();
    mockRuleActivatorActivate(callLogs,
      Collections.emptyList(),
      Collections.emptyList(),
      Collections.emptyList(),
      Collections.emptyList(),
      Collections.emptyList(),
      Collections.emptyList());

    OrganizationDto organization = dbTester.organizations().insert();
    DefinedQProfile definedQProfile = definedQProfileRepositoryRule.create(FOO_LANGUAGE, "foo1", true,
      activeRule("A", RulePriority.INFO), activeRule("B", RulePriority.MINOR), activeRule("C", RulePriority.MAJOR),
      activeRule("D", RulePriority.CRITICAL), activeRule("E", RulePriority.BLOCKER), activeRule("F", null));
    mockForSingleQPInsert();

    underTest.create(dbSession, definedQProfile, organization, activeRuleChanges);
    dbSession.commit();

    assertThat(callLogs)
      .extracting(CallLog::getRuleActivation)
      .extracting(RuleActivation::getSeverity)
      .containsExactly("INFO", "MINOR", "MAJOR", "CRITICAL", "BLOCKER", "MAJOR");
    assertThat(callLogs)
      .extracting(CallLog::getQualityProfileDto)
      .extracting(QualityProfileDto::getId)
      .containsOnly(getPersistedQP(organization, FOO_LANGUAGE, "foo1").getId());
  }

  @Test
  public void create_adds_all_parameters_to_RuleActivation() {
    List<CallLog> callLogs = new ArrayList<>();
    mockRuleActivatorActivate(callLogs, Collections.emptyList());

    OrganizationDto organization = dbTester.organizations().insert();
    DefinedQProfile definedQProfile = definedQProfileRepositoryRule.create(FOO_LANGUAGE, "foo1", true,
      activeRule("A", RulePriority.INFO,
        "param1", "value1",
        "param2", "value2",
        "param3", "value3",
        "param4", "value4"));
    mockForSingleQPInsert();

    underTest.create(dbSession, definedQProfile, organization, activeRuleChanges);
    dbSession.commit();

    Set<Map.Entry<String, String>> parameters = callLogs.get(0).getRuleActivation().getParameters().entrySet();
    assertThat(parameters)
      .extracting(Map.Entry::getKey)
      .containsOnlyOnce("param1", "param2", "param3", "param4");
    assertThat(parameters)
      .extracting(Map.Entry::getValue)
      .containsOnlyOnce("value1", "value2", "value3", "value4");
  }

  private void mockRuleActivatorActivate(List<CallLog> callLogs, List<ActiveRuleChange>... changesPerCall) {
    Iterator<List<ActiveRuleChange>> changesPerCallIt = Arrays.asList(changesPerCall).iterator();
    doAnswer(t -> {
      RuleActivation ruleActivation = t.getArgumentAt(1, RuleActivation.class);
      QualityProfileDto qualityProfileDto = t.getArgumentAt(2, QualityProfileDto.class);
      callLogs.add(new CallLog(ruleActivation, qualityProfileDto));
      return changesPerCallIt.next();
    }).when(mockedRuleActivator)
      .activate(any(DbSession.class), any(RuleActivation.class), any(QualityProfileDto.class));
  }

  private static ActiveRule activeRule(String id, @Nullable RulePriority rulePriority, String... parameters) {
    org.sonar.api.rules.Rule rule = new org.sonar.api.rules.Rule("plugin_name_" + id, "rule_key_" + id);
    rule.setParams(Arrays.stream(parameters)
      .filter(new EvenElementPredicate())
      .map(s -> new RuleParam(rule, s, "desc of s", "type of s"))
      .collect(MoreCollectors.toList(parameters.length / 2)));
    ActiveRule res = new ActiveRule(
      new RulesProfile("rule_profile_name_" + id, "language_" + id),
      rule.setSeverity(rulePriority),
      rulePriority);
    for (int i = 0; i < parameters.length; i++) {
      res.setParameter(parameters[i], parameters[i + 1]);
      i++;
    }
    return res;
  }

  private static final class CallLog {
    private final RuleActivation ruleActivation;
    private final QualityProfileDto qualityProfileDto;

    private CallLog(RuleActivation ruleActivation, QualityProfileDto qualityProfileDto) {
      this.ruleActivation = ruleActivation;
      this.qualityProfileDto = qualityProfileDto;
    }

    public RuleActivation getRuleActivation() {
      return ruleActivation;
    }

    public QualityProfileDto getQualityProfileDto() {
      return qualityProfileDto;
    }
  }

  private void mockForSingleQPInsert() {
    mockForSingleQPInsert("generated uuid", 2_456_789);
  }

  private void mockForSingleQPInsert(String uuid, long now) {
    when(mockedUuidFactory.create()).thenReturn(uuid).thenThrow(new UnsupportedOperationException("uuidFactory should be called only once"));
    when(mockedSystem2.now()).thenReturn(now).thenThrow(new UnsupportedOperationException("now should be called only once"));
  }

  private QualityProfileDto getPersistedQP(OrganizationDto organization, Language language, String name) {
    return dbClient.qualityProfileDao().selectByNameAndLanguage(organization, name, language.getKey(), dbTester.getSession());
  }

  private static ActiveRuleChange newActiveRuleChange(String id) {
    return ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(id, RuleKey.of(id + "1", id + "2")));
  }

  private static class EvenElementPredicate implements Predicate<String> {
    private int counter = -1;

    @Override
    public boolean test(String s) {
      counter++;
      return counter % 2 == 0;
    }
  }
}
