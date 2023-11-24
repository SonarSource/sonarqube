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
package org.sonar.server.qualityprofile;

import com.google.common.collect.Multimap;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfile;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileInsert;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileInsertImpl;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileRepositoryRule;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileUpdate;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileUpdateImpl;
import org.sonar.server.qualityprofile.builtin.BuiltInQualityProfilesUpdateListener;
import org.sonar.server.qualityprofile.builtin.QProfileName;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.rule.ServerRuleFinder;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.TypeValidations;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.rules.RulePriority.MAJOR;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newRuleProfileDto;
import static org.sonar.server.language.LanguageTesting.newLanguage;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.DEACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.UPDATED;

public class RegisterQualityProfilesNotificationIT {

  private static final Random RANDOM = new SecureRandom();

  private System2 system2 = mock(System2.class);
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public BuiltInQProfileRepositoryRule builtInQProfileRepositoryRule = new BuiltInQProfileRepositoryRule();
  @Rule
  public LogTester logTester = new LogTester();

  private DbClient dbClient = db.getDbClient();
  private TypeValidations typeValidations = mock(TypeValidations.class);
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private QualityProfileChangeEventService qualityProfileChangeEventService = mock(QualityProfileChangeEventService.class);
  private ServerRuleFinder ruleFinder = new DefaultRuleFinder(dbClient, mock(RuleDescriptionFormatter.class));
  private SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));
  private BuiltInQProfileInsert builtInQProfileInsert = new BuiltInQProfileInsertImpl(dbClient, ruleFinder, system2, UuidFactoryFast.getInstance(),
    typeValidations, activeRuleIndexer, sonarQubeVersion);
  private RuleActivator ruleActivator = new RuleActivator(system2, dbClient, typeValidations, userSessionRule, mock(Configuration.class), sonarQubeVersion);
  private QProfileRules qProfileRules = new QProfileRulesImpl(dbClient, ruleActivator, mock(RuleIndex.class), activeRuleIndexer, qualityProfileChangeEventService);
  private BuiltInQProfileUpdate builtInQProfileUpdate = new BuiltInQProfileUpdateImpl(dbClient, ruleActivator, activeRuleIndexer, qualityProfileChangeEventService);
  private BuiltInQualityProfilesUpdateListener builtInQualityProfilesNotification = mock(BuiltInQualityProfilesUpdateListener.class);
  private final Languages languages = LanguageTesting.newLanguages();
  private RegisterQualityProfiles underTest = new RegisterQualityProfiles(builtInQProfileRepositoryRule, dbClient,
    builtInQProfileInsert, builtInQProfileUpdate, builtInQualityProfilesNotification, system2, languages);

  @Test
  public void do_not_send_notification_on_new_profile() {
    String language = newLanguageKey();
    builtInQProfileRepositoryRule.add(newLanguage(language), "Sonar way");
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    verifyNoInteractions(builtInQualityProfilesNotification);
  }

  @Test
  public void do_not_send_notification_when_profile_is_not_updated() {
    String language = newLanguageKey();
    RuleDto dbRule = db.rules().insert(r -> r.setLanguage(language));
    RulesProfileDto dbProfile = insertBuiltInProfile(language);
    activateRuleInDb(dbProfile, dbRule, MAJOR);
    addPluginProfile(dbProfile, dbRule);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    verifyNoInteractions(builtInQualityProfilesNotification);
  }

  @Test
  public void send_notification_when_a_new_rule_is_activated() {
    String language = newLanguageKey();
    RuleDto existingRule = db.rules().insert(r -> r.setLanguage(language));
    RulesProfileDto dbProfile = insertBuiltInProfile(language);
    activateRuleInDb(dbProfile, existingRule, MAJOR);
    RuleDto newRule = db.rules().insert(r -> r.setLanguage(language));
    addPluginProfile(dbProfile, existingRule, newRule);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    ArgumentCaptor<Multimap> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(builtInQualityProfilesNotification).onChange(captor.capture(), anyLong(), anyLong());
    Multimap<QProfileName, ActiveRuleChange> updatedProfiles = captor.getValue();
    assertThat(updatedProfiles.keySet())
      .extracting(QProfileName::getName, QProfileName::getLanguage)
      .containsExactlyInAnyOrder(tuple(dbProfile.getName(), dbProfile.getLanguage()));
    assertThat(updatedProfiles.values())
      .extracting(value -> value.getActiveRule().getRuleUuid(), ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(tuple(newRule.getUuid(), ACTIVATED));
  }

  @Test
  public void send_notification_when_a_rule_is_deactivated() {
    String language = newLanguageKey();
    RuleDto existingRule = db.rules().insert(r -> r.setLanguage(language));
    RulesProfileDto dbProfile = insertBuiltInProfile(language);
    activateRuleInDb(dbProfile, existingRule, MAJOR);
    addPluginProfile(dbProfile);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    ArgumentCaptor<Multimap> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(builtInQualityProfilesNotification).onChange(captor.capture(), anyLong(), anyLong());
    Multimap<QProfileName, ActiveRuleChange> updatedProfiles = captor.getValue();
    assertThat(updatedProfiles.keySet())
      .extracting(QProfileName::getName, QProfileName::getLanguage)
      .containsExactlyInAnyOrder(tuple(dbProfile.getName(), dbProfile.getLanguage()));
    assertThat(updatedProfiles.values())
      .extracting(value -> value.getActiveRule().getRuleUuid(), ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(tuple(existingRule.getUuid(), DEACTIVATED));
  }

  @Test
  public void send_a_single_notification_when_multiple_rules_are_activated() {
    String language = newLanguageKey();

    RuleDto existingRule1 = db.rules().insert(r -> r.setLanguage(language));
    RuleDto newRule1 = db.rules().insert(r -> r.setLanguage(language));
    RulesProfileDto dbProfile1 = insertBuiltInProfile(language);
    activateRuleInDb(dbProfile1, existingRule1, MAJOR);
    addPluginProfile(dbProfile1, existingRule1, newRule1);

    RuleDto existingRule2 = db.rules().insert(r -> r.setLanguage(language));
    RuleDto newRule2 = db.rules().insert(r -> r.setLanguage(language));
    RulesProfileDto dbProfile2 = insertBuiltInProfile(language);
    activateRuleInDb(dbProfile2, existingRule2, MAJOR);
    addPluginProfile(dbProfile2, existingRule2, newRule2);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    ArgumentCaptor<Multimap> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(builtInQualityProfilesNotification).onChange(captor.capture(), anyLong(), anyLong());
    Multimap<QProfileName, ActiveRuleChange> updatedProfiles = captor.getValue();
    assertThat(updatedProfiles.keySet())
      .extracting(QProfileName::getName, QProfileName::getLanguage)
      .containsExactlyInAnyOrder(
        tuple(dbProfile1.getName(), dbProfile1.getLanguage()),
        tuple(dbProfile2.getName(), dbProfile2.getLanguage()));
    assertThat(updatedProfiles.values())
      .extracting(value -> value.getActiveRule().getRuleUuid(), ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(
        tuple(newRule1.getUuid(), ACTIVATED),
        tuple(newRule2.getUuid(), ACTIVATED));
  }

  @Test
  public void notification_does_not_include_inherited_profiles_when_rule_is_added() {
    String language = newLanguageKey();
    RuleDto newRule = db.rules().insert(r -> r.setLanguage(language));

    QProfileDto builtInQProfileDto = insertProfile(orgQProfile -> orgQProfile.setIsBuiltIn(true).setLanguage(language));
    QProfileDto childQProfileDto = insertProfile(orgQProfile -> orgQProfile.setIsBuiltIn(false).setLanguage(language).setParentKee(builtInQProfileDto.getKee()));
    addPluginProfile(builtInQProfileDto, newRule);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    ArgumentCaptor<Multimap> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(builtInQualityProfilesNotification).onChange(captor.capture(), anyLong(), anyLong());
    Multimap<QProfileName, ActiveRuleChange> updatedProfiles = captor.getValue();
    assertThat(updatedProfiles.keySet())
      .extracting(QProfileName::getName, QProfileName::getLanguage)
      .containsExactlyInAnyOrder(tuple(builtInQProfileDto.getName(), builtInQProfileDto.getLanguage()));
    assertThat(updatedProfiles.values())
      .extracting(value -> value.getActiveRule().getRuleUuid(), ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(tuple(newRule.getUuid(), ACTIVATED));
  }

  @Test
  public void notification_does_not_include_inherited_profiled_when_rule_is_changed() {
    String language = newLanguageKey();
    RuleDto rule = db.rules().insert(r -> r.setLanguage(language).setSeverity(Severity.MINOR));

    QProfileDto builtInProfile = insertProfile(orgQProfile -> orgQProfile.setIsBuiltIn(true).setLanguage(language));
    db.qualityProfiles().activateRule(builtInProfile, rule, ar -> ar.setSeverity(Severity.MINOR));
    QProfileDto childProfile = insertProfile(orgQProfile -> orgQProfile.setIsBuiltIn(false).setLanguage(language).setParentKee(builtInProfile.getKee()));
    db.qualityProfiles().activateRule(childProfile, rule, ar -> ar.setInheritance(ActiveRuleDto.INHERITED).setSeverity(Severity.MINOR));
    addPluginProfile(builtInProfile, rule);
    builtInQProfileRepositoryRule.initialize();
    db.commit();

    underTest.start();

    ArgumentCaptor<Multimap> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(builtInQualityProfilesNotification).onChange(captor.capture(), anyLong(), anyLong());
    Multimap<QProfileName, ActiveRuleChange> updatedProfiles = captor.getValue();
    assertThat(updatedProfiles.keySet())
      .extracting(QProfileName::getName, QProfileName::getLanguage)
      .containsExactlyInAnyOrder(tuple(builtInProfile.getName(), builtInProfile.getLanguage()));
    assertThat(updatedProfiles.values())
      .extracting(value -> value.getActiveRule().getRuleUuid(), ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(tuple(rule.getUuid(), UPDATED));
  }

  @Test
  public void notification_does_not_include_inherited_profiles_when_rule_is_deactivated() {
    String language = newLanguageKey();
    RuleDto rule = db.rules().insert(r -> r.setLanguage(language).setSeverity(Severity.MINOR));

    QProfileDto builtInQProfileDto = insertProfile(orgQProfile -> orgQProfile.setIsBuiltIn(true).setLanguage(language));
    db.qualityProfiles().activateRule(builtInQProfileDto, rule);
    QProfileDto childQProfileDto = insertProfile(orgQProfile -> orgQProfile.setIsBuiltIn(false).setLanguage(language).setParentKee(builtInQProfileDto.getKee()));
    qProfileRules.activateAndCommit(db.getSession(), childQProfileDto, singleton(RuleActivation.create(rule.getUuid())));
    db.commit();

    addPluginProfile(builtInQProfileDto);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    ArgumentCaptor<Multimap> captor = ArgumentCaptor.forClass(Multimap.class);
    verify(builtInQualityProfilesNotification).onChange(captor.capture(), anyLong(), anyLong());
    Multimap<QProfileName, ActiveRuleChange> updatedProfiles = captor.getValue();
    assertThat(updatedProfiles.keySet())
      .extracting(QProfileName::getName, QProfileName::getLanguage)
      .containsExactlyInAnyOrder(tuple(builtInQProfileDto.getName(), builtInQProfileDto.getLanguage()));
    assertThat(updatedProfiles.values())
      .extracting(value -> value.getActiveRule().getRuleUuid(), ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(tuple(rule.getUuid(), DEACTIVATED));
  }

  @Test
  public void notification_contains_send_start_and_end_date() {
    String language = newLanguageKey();
    RuleDto existingRule = db.rules().insert(r -> r.setLanguage(language));
    RulesProfileDto dbProfile = insertBuiltInProfile(language);
    activateRuleInDb(dbProfile, existingRule, MAJOR);
    RuleDto newRule = db.rules().insert(r -> r.setLanguage(language));
    addPluginProfile(dbProfile, existingRule, newRule);
    builtInQProfileRepositoryRule.initialize();
    long startDate = RANDOM.nextInt(5000);
    long endDate = startDate + RANDOM.nextInt(5000);
    when(system2.now()).thenReturn(startDate, endDate);

    underTest.start();

    verify(builtInQualityProfilesNotification).onChange(any(), eq(startDate), eq(endDate));
  }

  private void addPluginProfile(RulesProfileDto dbProfile, RuleDto... dbRules) {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile(dbProfile.getName(), dbProfile.getLanguage());

    Arrays.stream(dbRules).forEach(dbRule -> newQp.activateRule(dbRule.getRepositoryKey(), dbRule.getRuleKey()).overrideSeverity(Severity.MAJOR));
    newQp.done();
    List<BuiltInActiveRule> rules = context.profile(dbProfile.getLanguage(), dbProfile.getName()).rules();
    BuiltInQProfile.ActiveRule[] activeRules = toActiveRules(rules, dbRules);
    builtInQProfileRepositoryRule.add(newLanguage(dbProfile.getLanguage()), dbProfile.getName(), false, activeRules);
  }

  private static BuiltInQProfile.ActiveRule[] toActiveRules(List<BuiltInActiveRule> rules, RuleDto[] dbRules) {
    Map<RuleKey, RuleDto> dbRulesByRuleKey = Arrays.stream(dbRules)
      .collect(Collectors.toMap(RuleDto::getKey, Function.identity()));
    return rules.stream()
      .map(r -> {
        RuleKey ruleKey = RuleKey.of(r.repoKey(), r.ruleKey());
        RuleDto ruleDefinitionDto = dbRulesByRuleKey.get(ruleKey);
        checkState(ruleDefinitionDto != null, "Rule '%s' not found", ruleKey);
        return new BuiltInQProfile.ActiveRule(ruleDefinitionDto.getUuid(), r);
      }).toArray(BuiltInQProfile.ActiveRule[]::new);
  }

  private void addPluginProfile(QProfileDto profile, RuleDto... dbRules) {
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

  private void activateRuleInDb(RulesProfileDto profile, RuleDto rule, RulePriority severity) {
    ActiveRuleDto dto = new ActiveRuleDto()
      .setProfileUuid(profile.getUuid())
      .setSeverity(severity.name())
      .setRuleUuid(rule.getUuid())
      .setCreatedAt(nextLong())
      .setUpdatedAt(nextLong());
    db.getDbClient().activeRuleDao().insert(db.getSession(), dto);
    db.commit();
  }

  private QProfileDto insertProfile(Consumer<QProfileDto> consumer) {
    QProfileDto builtInQProfileDto = db.qualityProfiles().insert(consumer);
    db.commit();
    return builtInQProfileDto;
  }

  private static String newLanguageKey() {
    return randomAlphanumeric(20).toLowerCase();
  }
}
