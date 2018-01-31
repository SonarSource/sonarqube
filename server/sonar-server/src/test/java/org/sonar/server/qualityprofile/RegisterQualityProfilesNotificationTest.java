/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.Multimap;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.TypeValidations;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.rules.RulePriority.MAJOR;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newRuleProfileDto;
import static org.sonar.server.language.LanguageTesting.newLanguage;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.DEACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.UPDATED;

public class RegisterQualityProfilesNotificationTest {

  private static final Random RANDOM = new SecureRandom();

  private System2 system2 = mock(System2.class);
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public BuiltInQProfileRepositoryRule builtInQProfileRepositoryRule = new BuiltInQProfileRepositoryRule();
  @Rule
  public LogTester logTester = new LogTester();

  private DbClient dbClient = db.getDbClient();
  private TypeValidations typeValidations = mock(TypeValidations.class);
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private BuiltInQProfileInsert builtInQProfileInsert = new BuiltInQProfileInsertImpl(dbClient, system2, UuidFactoryFast.getInstance(), typeValidations, activeRuleIndexer);
  private RuleActivator ruleActivator = new RuleActivator(system2, dbClient, typeValidations, userSessionRule);
  private QProfileRules qProfileRules = new QProfileRulesImpl(dbClient, ruleActivator, mock(RuleIndex.class), activeRuleIndexer);
  private BuiltInQProfileUpdate builtInQProfileUpdate = new BuiltInQProfileUpdateImpl(dbClient, ruleActivator, activeRuleIndexer);
  private BuiltInQualityProfilesUpdateListener builtInQualityProfilesNotification = mock(BuiltInQualityProfilesUpdateListener.class);
  private RegisterQualityProfiles underTest = new RegisterQualityProfiles(builtInQProfileRepositoryRule, dbClient,
    builtInQProfileInsert, builtInQProfileUpdate, builtInQualityProfilesNotification, system2);

  @Test
  public void do_not_send_notification_on_new_profile() {
    String language = newLanguageKey();
    builtInQProfileRepositoryRule.add(newLanguage(language), "Sonar way");
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    verifyZeroInteractions(builtInQualityProfilesNotification);
  }

  @Test
  public void do_not_send_notification_when_profile_is_not_updated() {
    String language = newLanguageKey();
    RuleDefinitionDto dbRule = db.rules().insert(r -> r.setLanguage(language));
    RulesProfileDto dbProfile = insertBuiltInProfile(language);
    activateRuleInDb(dbProfile, dbRule, MAJOR);
    addPluginProfile(dbProfile, dbRule);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    verifyZeroInteractions(builtInQualityProfilesNotification);
  }

  @Test
  public void send_notification_when_a_new_rule_is_activated() {
    String language = newLanguageKey();
    RuleDefinitionDto existingRule = db.rules().insert(r -> r.setLanguage(language));
    RulesProfileDto dbProfile = insertBuiltInProfile(language);
    activateRuleInDb(dbProfile, existingRule, MAJOR);
    RuleDefinitionDto newRule = db.rules().insert(r -> r.setLanguage(language));
    addPluginProfile(dbProfile, existingRule, newRule);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    ArgumentCaptor<Multimap> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(builtInQualityProfilesNotification).onChange(captor.capture(), anyLong(), anyLong());
    Multimap<QProfileName, ActiveRuleChange> updatedProfiles = captor.<Multimap<QProfileName, ActiveRuleChange>>getValue();
    assertThat(updatedProfiles.keySet())
      .extracting(QProfileName::getName, QProfileName::getLanguage)
      .containsExactlyInAnyOrder(tuple(dbProfile.getName(), dbProfile.getLanguage()));
    assertThat(updatedProfiles.values())
      .extracting(value -> value.getActiveRule().getRuleId(), ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(tuple(newRule.getId(), ACTIVATED));
  }

  @Test
  public void send_notification_when_a_rule_is_deactivated() {
    String language = newLanguageKey();
    RuleDefinitionDto existingRule = db.rules().insert(r -> r.setLanguage(language));
    RulesProfileDto dbProfile = insertBuiltInProfile(language);
    activateRuleInDb(dbProfile, existingRule, MAJOR);
    addPluginProfile(dbProfile);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    ArgumentCaptor<Multimap> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(builtInQualityProfilesNotification).onChange(captor.capture(), anyLong(), anyLong());
    Multimap<QProfileName, ActiveRuleChange> updatedProfiles = captor.<Multimap<QProfileName, ActiveRuleChange>>getValue();
    assertThat(updatedProfiles.keySet())
      .extracting(QProfileName::getName, QProfileName::getLanguage)
      .containsExactlyInAnyOrder(tuple(dbProfile.getName(), dbProfile.getLanguage()));
    assertThat(updatedProfiles.values())
      .extracting(value -> value.getActiveRule().getRuleId(), ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(tuple(existingRule.getId(), DEACTIVATED));
  }

  @Test
  public void send_a_single_notification_when_multiple_rules_are_activated() {
    String language = newLanguageKey();

    RuleDefinitionDto existingRule1 = db.rules().insert(r -> r.setLanguage(language));
    RuleDefinitionDto newRule1 = db.rules().insert(r -> r.setLanguage(language));
    RulesProfileDto dbProfile1 = insertBuiltInProfile(language);
    activateRuleInDb(dbProfile1, existingRule1, MAJOR);
    addPluginProfile(dbProfile1, existingRule1, newRule1);

    RuleDefinitionDto existingRule2 = db.rules().insert(r -> r.setLanguage(language));
    RuleDefinitionDto newRule2 = db.rules().insert(r -> r.setLanguage(language));
    RulesProfileDto dbProfile2 = insertBuiltInProfile(language);
    activateRuleInDb(dbProfile2, existingRule2, MAJOR);
    addPluginProfile(dbProfile2, existingRule2, newRule2);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    ArgumentCaptor<Multimap> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(builtInQualityProfilesNotification).onChange(captor.capture(), anyLong(), anyLong());
    Multimap<QProfileName, ActiveRuleChange> updatedProfiles = captor.<Multimap<QProfileName, ActiveRuleChange>>getValue();
    assertThat(updatedProfiles.keySet())
      .extracting(QProfileName::getName, QProfileName::getLanguage)
      .containsExactlyInAnyOrder(
        tuple(dbProfile1.getName(), dbProfile1.getLanguage()),
        tuple(dbProfile2.getName(), dbProfile2.getLanguage()));
    assertThat(updatedProfiles.values())
      .extracting(value -> value.getActiveRule().getRuleId(), ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(
        tuple(newRule1.getId(), ACTIVATED),
        tuple(newRule2.getId(), ACTIVATED));
  }

  @Test
  public void notification_does_not_include_inherited_profiles_when_rule_is_added() {
    String language = newLanguageKey();
    RuleDefinitionDto newRule = db.rules().insert(r -> r.setLanguage(language));
    OrganizationDto organization = db.organizations().insert();

    QProfileDto builtInQProfileDto = insertProfile(organization, orgQProfile -> orgQProfile.setIsBuiltIn(true).setLanguage(language));
    QProfileDto childQProfileDto = insertProfile(organization, orgQProfile -> orgQProfile.setIsBuiltIn(false).setLanguage(language).setParentKee(builtInQProfileDto.getKee()));
    addPluginProfile(builtInQProfileDto, newRule);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    ArgumentCaptor<Multimap> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(builtInQualityProfilesNotification).onChange(captor.capture(), anyLong(), anyLong());
    Multimap<QProfileName, ActiveRuleChange> updatedProfiles = captor.<Multimap<QProfileName, ActiveRuleChange>>getValue();
    assertThat(updatedProfiles.keySet())
      .extracting(QProfileName::getName, QProfileName::getLanguage)
      .containsExactlyInAnyOrder(tuple(builtInQProfileDto.getName(), builtInQProfileDto.getLanguage()));
    assertThat(updatedProfiles.values())
      .extracting(value -> value.getActiveRule().getRuleId(), ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(tuple(newRule.getId(), ACTIVATED));
  }

  @Test
  public void notification_does_not_include_inherited_profiled_when_rule_is_changed() {
    String language = newLanguageKey();
    RuleDefinitionDto rule = db.rules().insert(r -> r.setLanguage(language).setSeverity(Severity.MINOR));
    OrganizationDto organization = db.organizations().insert();

    QProfileDto builtInProfile = insertProfile(organization, orgQProfile -> orgQProfile.setIsBuiltIn(true).setLanguage(language));
    db.qualityProfiles().activateRule(builtInProfile, rule, ar -> ar.setSeverity(Severity.MINOR));
    QProfileDto childProfile = insertProfile(organization, orgQProfile -> orgQProfile.setIsBuiltIn(false).setLanguage(language).setParentKee(builtInProfile.getKee()));
    db.qualityProfiles().activateRule(childProfile, rule, ar -> ar.setInheritance(ActiveRuleDto.INHERITED).setSeverity(Severity.MINOR));
    addPluginProfile(builtInProfile, rule);
    builtInQProfileRepositoryRule.initialize();
    db.commit();

    underTest.start();

    ArgumentCaptor<Multimap> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(builtInQualityProfilesNotification).onChange(captor.capture(), anyLong(), anyLong());
    Multimap<QProfileName, ActiveRuleChange> updatedProfiles = captor.<Multimap<QProfileName, ActiveRuleChange>>getValue();
    assertThat(updatedProfiles.keySet())
      .extracting(QProfileName::getName, QProfileName::getLanguage)
      .containsExactlyInAnyOrder(tuple(builtInProfile.getName(), builtInProfile.getLanguage()));
    assertThat(updatedProfiles.values())
      .extracting(value -> value.getActiveRule().getRuleId(), ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(tuple(rule.getId(), UPDATED));
  }

  @Test
  public void notification_does_not_include_inherited_profiles_when_rule_is_deactivated() {
    String language = newLanguageKey();
    RuleDefinitionDto rule = db.rules().insert(r -> r.setLanguage(language).setSeverity(Severity.MINOR));
    OrganizationDto organization = db.organizations().insert();

    QProfileDto builtInQProfileDto = insertProfile(organization,
      orgQProfile -> orgQProfile.setIsBuiltIn(true).setLanguage(language));
    db.qualityProfiles().activateRule(builtInQProfileDto, rule);
    QProfileDto childQProfileDto = insertProfile(organization,
      orgQProfile -> orgQProfile.setIsBuiltIn(false).setLanguage(language).setParentKee(builtInQProfileDto.getKee()));
    qProfileRules.activateAndCommit(db.getSession(), childQProfileDto, singleton(RuleActivation.create(rule.getId())));
    db.commit();

    addPluginProfile(builtInQProfileDto);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    ArgumentCaptor<Multimap> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(builtInQualityProfilesNotification).onChange(captor.capture(), anyLong(), anyLong());
    Multimap<QProfileName, ActiveRuleChange> updatedProfiles = captor.<Multimap<QProfileName, ActiveRuleChange>>getValue();
    assertThat(updatedProfiles.keySet())
      .extracting(QProfileName::getName, QProfileName::getLanguage)
      .containsExactlyInAnyOrder(tuple(builtInQProfileDto.getName(), builtInQProfileDto.getLanguage()));
    assertThat(updatedProfiles.values())
      .extracting(value -> value.getActiveRule().getRuleId(), ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(tuple(rule.getId(), DEACTIVATED));
  }

  @Test
  public void notification_contains_send_start_and_end_date() {
    String language = newLanguageKey();
    RuleDefinitionDto existingRule = db.rules().insert(r -> r.setLanguage(language));
    RulesProfileDto dbProfile = insertBuiltInProfile(language);
    activateRuleInDb(dbProfile, existingRule, MAJOR);
    RuleDefinitionDto newRule = db.rules().insert(r -> r.setLanguage(language));
    addPluginProfile(dbProfile, existingRule, newRule);
    builtInQProfileRepositoryRule.initialize();
    long startDate = RANDOM.nextInt(5000);
    long endDate = startDate + RANDOM.nextInt(5000);
    when(system2.now()).thenReturn(startDate, endDate);

    underTest.start();

    verify(builtInQualityProfilesNotification).onChange(any(), eq(startDate), eq(endDate));
  }

  private void addPluginProfile(RulesProfileDto dbProfile, RuleDefinitionDto... dbRules) {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile(dbProfile.getName(), dbProfile.getLanguage());

    Arrays.stream(dbRules).forEach(dbRule -> newQp.activateRule(dbRule.getRepositoryKey(), dbRule.getRuleKey()).overrideSeverity(Severity.MAJOR));
    newQp.done();
    List<BuiltInActiveRule> rules = context.profile(dbProfile.getLanguage(), dbProfile.getName()).rules();
    BuiltInQProfile.ActiveRule[] activeRules = toActiveRules(rules, dbRules);
    builtInQProfileRepositoryRule.add(newLanguage(dbProfile.getLanguage()), dbProfile.getName(), false, activeRules);
  }

  private static BuiltInQProfile.ActiveRule[] toActiveRules(List<BuiltInActiveRule> rules, RuleDefinitionDto[] dbRules) {
    Map<RuleKey, RuleDefinitionDto> dbRulesByRuleKey = Arrays.stream(dbRules)
      .collect(MoreCollectors.uniqueIndex(RuleDefinitionDto::getKey));
    return rules.stream()
      .map(r -> {
        RuleKey ruleKey = RuleKey.of(r.repoKey(), r.ruleKey());
        RuleDefinitionDto ruleDefinitionDto = dbRulesByRuleKey.get(ruleKey);
        checkState(ruleDefinitionDto != null, "Rule '%s' not found", ruleKey);
        return new BuiltInQProfile.ActiveRule(ruleDefinitionDto.getId(), r);
      }).toArray(BuiltInQProfile.ActiveRule[]::new);
  }

  private void addPluginProfile(QProfileDto profile, RuleDefinitionDto... dbRules) {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile(profile.getName(), profile.getLanguage());

    Arrays.stream(dbRules).forEach(dbRule -> newQp.activateRule(dbRule.getRepositoryKey(), dbRule.getRuleKey()).overrideSeverity(Severity.MAJOR));
    newQp.done();
    BuiltInQProfile.ActiveRule[] activeRules = toActiveRules(context.profile(profile.getLanguage(), profile.getName()).rules(), dbRules);
    builtInQProfileRepositoryRule.add(newLanguage(profile.getLanguage()), profile.getName(), false, activeRules);
  }

  private RulesProfileDto insertBuiltInProfile(String language) {
    RulesProfileDto ruleProfileDto = newRuleProfileDto(rp -> rp.setIsBuiltIn(true).setLanguage(language));
    db.getDbClient().qualityProfileDao().insert(db.getSession(), ruleProfileDto);
    db.commit();
    return ruleProfileDto;
  }

  private void activateRuleInDb(RulesProfileDto profile, RuleDefinitionDto rule, RulePriority severity) {
    ActiveRuleDto dto = new ActiveRuleDto()
      .setProfileId(profile.getId())
      .setSeverity(severity.name())
      .setRuleId(rule.getId())
      .setCreatedAt(nextLong())
      .setUpdatedAt(nextLong());
    db.getDbClient().activeRuleDao().insert(db.getSession(), dto);
    db.commit();
  }

  private QProfileDto insertProfile(OrganizationDto organization, Consumer<QProfileDto> consumer) {
    QProfileDto builtInQProfileDto = db.qualityProfiles().insert(organization, consumer);
    db.commit();
    return builtInQProfileDto;
  }

  private static String newLanguageKey() {
    return randomAlphanumeric(20).toLowerCase();
  }
}
