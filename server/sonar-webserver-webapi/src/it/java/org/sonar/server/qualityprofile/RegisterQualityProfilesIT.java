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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfile;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileInsert;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileRepositoryRule;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileUpdate;
import org.sonar.server.qualityprofile.builtin.BuiltInQualityProfilesUpdateListener;
import org.sonar.server.tester.UserSessionRule;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newQualityProfileDto;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newRuleProfileDto;

public class RegisterQualityProfilesIT {
  private static final Language FOO_LANGUAGE = LanguageTesting.newLanguage("foo");
  private static final Language BAR_LANGUAGE = LanguageTesting.newLanguage("bar");

  private final System2 system2 = new TestSystem2().setNow(1659510722633L);
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public BuiltInQProfileRepositoryRule builtInQProfileRepositoryRule = new BuiltInQProfileRepositoryRule();
  @Rule
  public LogTester logTester = new LogTester();

  private final DbClient dbClient = db.getDbClient();
  private final DummyBuiltInQProfileInsert insert = new DummyBuiltInQProfileInsert();
  private final DummyBuiltInQProfileUpdate update = new DummyBuiltInQProfileUpdate();
  private final Languages languages = LanguageTesting.newLanguages("foo");
  private final RegisterQualityProfiles underTest = new RegisterQualityProfiles(builtInQProfileRepositoryRule, dbClient, insert, update,
    mock(BuiltInQualityProfilesUpdateListener.class), system2, languages);

  @Test
  public void start_fails_if_BuiltInQProfileRepository_has_not_been_initialized() {
    assertThatThrownBy(underTest::start)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("initialize must be called first");
  }

  @Test
  public void persist_built_in_profiles_that_are_not_persisted_yet() {
    BuiltInQProfile builtInQProfile = builtInQProfileRepositoryRule.add(FOO_LANGUAGE, "Sonar way");
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(insert.callLogs).containsExactly(builtInQProfile);
    assertThat(update.callLogs).isEmpty();
    assertThat(logTester.logs(Level.INFO)).contains("Register profile foo/Sonar way");
  }

  @Test
  public void dont_persist_built_in_profiles_that_are_already_persisted() {
    String name = "doh";

    BuiltInQProfile persistedBuiltIn = builtInQProfileRepositoryRule.add(FOO_LANGUAGE, name, true);
    BuiltInQProfile nonPersistedBuiltIn = builtInQProfileRepositoryRule.add(BAR_LANGUAGE, name, true);
    builtInQProfileRepositoryRule.initialize();
    insertRulesProfile(persistedBuiltIn);

    underTest.start();

    assertThat(insert.callLogs).containsExactly(nonPersistedBuiltIn);
    assertThat(update.callLogs).containsExactly(persistedBuiltIn);
  }

  @Test
  public void rename_custom_outdated_profiles_if_same_name_than_built_in_profile() {
    QProfileDto outdatedProfile = db.qualityProfiles().insert(p -> p.setIsBuiltIn(false)
      .setLanguage(FOO_LANGUAGE.getKey()).setName("Sonar way"));
    builtInQProfileRepositoryRule.add(FOO_LANGUAGE, "Sonar way", false);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(selectPersistedName(outdatedProfile)).isEqualTo("Sonar way (outdated copy)");
    assertThat(logTester.logs(Level.INFO)).contains("Rename Quality profiles [foo/Sonar way] to [Sonar way (outdated copy)]");
  }

  @Test
  public void update_built_in_profile_if_it_already_exists() {
    RulesProfileDto ruleProfile = newRuleProfileDto(rp -> rp.setIsBuiltIn(true).setName("Sonar way").setLanguage(FOO_LANGUAGE.getKey()));
    db.getDbClient().qualityProfileDao().insert(db.getSession(), ruleProfile);
    db.commit();

    BuiltInQProfile builtIn = builtInQProfileRepositoryRule.add(FOO_LANGUAGE, ruleProfile.getName(), false);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(insert.callLogs).isEmpty();
    assertThat(update.callLogs).containsExactly(builtIn);
    assertThat(logTester.logs(Level.INFO)).contains("Update profile foo/Sonar way");
  }

  @Test
  public void update_default_built_in_quality_profile() {
    RulesProfileDto ruleProfileWithoutRule = newRuleProfileDto(rp -> rp.setIsBuiltIn(true).setName("Sonar way").setLanguage(FOO_LANGUAGE.getKey()));
    RulesProfileDto ruleProfileWithOneRule = newRuleProfileDto(rp -> rp.setIsBuiltIn(true).setName("Sonar way 2").setLanguage(FOO_LANGUAGE.getKey()));

    QProfileDto qProfileWithoutRule = newQualityProfileDto()
      .setIsBuiltIn(true)
      .setLanguage(FOO_LANGUAGE.getKey())
      .setName("Sonar way")
      .setRulesProfileUuid(ruleProfileWithoutRule.getUuid());
    QProfileDto qProfileWithOneRule = newQualityProfileDto()
      .setIsBuiltIn(true)
      .setName("Sonar way 2")
      .setLanguage(FOO_LANGUAGE.getKey())
      .setRulesProfileUuid(ruleProfileWithOneRule.getUuid());

    db.qualityProfiles().insert(qProfileWithoutRule, qProfileWithOneRule);
    db.qualityProfiles().setAsDefault(qProfileWithoutRule);

    RuleDto ruleDto = db.rules().insert();
    db.qualityProfiles().activateRule(qProfileWithOneRule, ruleDto);
    db.commit();

    builtInQProfileRepositoryRule.add(FOO_LANGUAGE, ruleProfileWithoutRule.getName(), true);
    builtInQProfileRepositoryRule.add(FOO_LANGUAGE, ruleProfileWithOneRule.getName(), false);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(logTester.logs(Level.INFO)).containsAnyOf(
      format("Default built-in quality profile for language [foo] has been updated from [%s] to [%s] since previous default does not have active rules.",
        qProfileWithoutRule.getName(), qProfileWithOneRule.getName()));

    assertThat(selectUuidOfDefaultProfile(FOO_LANGUAGE.getKey()))
      .isPresent().get()
      .isEqualTo(qProfileWithOneRule.getKee());
  }

  @Test
  public void rename_and_drop_built_in_flag_for_quality_profile() {
    System.out.println(System.currentTimeMillis());

    RulesProfileDto ruleProfileWithoutRule = newRuleProfileDto(rp -> rp.setIsBuiltIn(true).setName("Foo way").setLanguage(FOO_LANGUAGE.getKey()));
    RulesProfileDto ruleProfileLongNameWithoutRule = newRuleProfileDto(rp -> rp.setIsBuiltIn(true).setName("That's a very very very very very very "
                                                                                                           + "very very very very long name").setLanguage(FOO_LANGUAGE.getKey()));
    RulesProfileDto ruleProfileWithOneRuleToBeRenamed = newRuleProfileDto(rp -> rp.setIsBuiltIn(true).setName("Foo way 2").setLanguage(FOO_LANGUAGE.getKey()));
    RulesProfileDto ruleProfileWithOneRule = newRuleProfileDto(rp -> rp.setIsBuiltIn(true).setName("Foo way 3").setLanguage(FOO_LANGUAGE.getKey()));

    QProfileDto qProfileWithoutRule = newQualityProfileDto()
      .setIsBuiltIn(true)
      .setLanguage(FOO_LANGUAGE.getKey())
      .setName(ruleProfileWithoutRule.getName())
      .setRulesProfileUuid(ruleProfileWithoutRule.getUuid());

    QProfileDto qProfileLongNameWithoutRule = newQualityProfileDto()
      .setIsBuiltIn(true)
      .setName(ruleProfileLongNameWithoutRule.getName())
      .setLanguage(FOO_LANGUAGE.getKey())
      .setRulesProfileUuid(ruleProfileLongNameWithoutRule.getUuid());

    QProfileDto qProfileWithOneRule = newQualityProfileDto()
      .setIsBuiltIn(true)
      .setName(ruleProfileWithOneRule.getName())
      .setLanguage(FOO_LANGUAGE.getKey())
      .setRulesProfileUuid(ruleProfileWithOneRule.getUuid());

    QProfileDto qProfileWithOneRuleToBeRenamed = newQualityProfileDto()
      .setIsBuiltIn(true)
      .setName(ruleProfileWithOneRuleToBeRenamed.getName())
      .setLanguage(FOO_LANGUAGE.getKey())
      .setRulesProfileUuid(ruleProfileWithOneRuleToBeRenamed.getUuid());

    db.qualityProfiles().insert(qProfileWithoutRule, qProfileWithOneRule, qProfileLongNameWithoutRule, qProfileWithOneRuleToBeRenamed);
    RuleDto ruleDto = db.rules().insert();
    db.qualityProfiles().activateRule(qProfileWithOneRule, ruleDto);
    db.qualityProfiles().activateRule(qProfileWithOneRuleToBeRenamed, ruleDto);
    db.commit();

    // adding only one profile as the other does not exist in plugins
    builtInQProfileRepositoryRule.add(FOO_LANGUAGE, ruleProfileWithOneRule.getName(), true);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd yyyy 'at' hh:mm a")
      .withLocale(Locale.getDefault())
      .withZone(ZoneId.systemDefault());

    var expectedSuffix = " (outdated copy since " + formatter.format(Instant.ofEpochMilli(system2.now())) + ")";

    assertThat(logTester.logs(Level.INFO)).contains(
      format("Quality profile [%s] for language [%s] is no longer built-in and has been renamed to [%s] "
             + "since it does not have any active rules.",
        qProfileWithoutRule.getName(), qProfileWithoutRule.getLanguage(), qProfileWithoutRule.getName() + expectedSuffix),
      format("Quality profile [%s] for language [%s] is no longer built-in and has been renamed to [%s] "
             + "since it does not have any active rules.",
        qProfileLongNameWithoutRule.getName(), qProfileLongNameWithoutRule.getLanguage(), "That's a very very very very very ver..." + expectedSuffix),
      format("Quality profile [%s] for language [%s] is no longer built-in and has been renamed to [%s] "
             + "since it does not have any active rules.",
        qProfileWithOneRuleToBeRenamed.getName(), qProfileWithOneRuleToBeRenamed.getLanguage(), qProfileWithOneRuleToBeRenamed.getName() + expectedSuffix));

    assertThat(dbClient.qualityProfileDao().selectByUuid(db.getSession(), qProfileWithoutRule.getKee()))
      .extracting(QProfileDto::isBuiltIn, QProfileDto::getName)
      .containsExactly(false, qProfileWithoutRule.getName() + expectedSuffix);

    assertThat(dbClient.qualityProfileDao().selectByUuid(db.getSession(), qProfileLongNameWithoutRule.getKee()))
      .extracting(QProfileDto::isBuiltIn, QProfileDto::getName)
      .containsExactly(false, "That's a very very very very very ver..." + expectedSuffix);

    assertThat(dbClient.qualityProfileDao().selectByUuid(db.getSession(), qProfileWithOneRuleToBeRenamed.getKee()))
      .extracting(QProfileDto::isBuiltIn, QProfileDto::getName)
      .containsExactly(false, qProfileWithOneRuleToBeRenamed.getName() + expectedSuffix);

    // the other profile did not change
    assertThat(dbClient.qualityProfileDao().selectByUuid(db.getSession(), qProfileWithOneRule.getKee()))
      .extracting(QProfileDto::isBuiltIn, QProfileDto::getName)
      .containsExactly(true, qProfileWithOneRule.getName());
  }

  private Optional<String> selectUuidOfDefaultProfile(String language) {
    return db.select("select qprofile_uuid as \"profileUuid\" " +
                     " from default_qprofiles " +
                     " where language='" + language + "'")
      .stream()
      .findFirst()
      .map(m -> (String) m.get("profileUuid"));
  }

  private String selectPersistedName(QProfileDto profile) {
    return db.qualityProfiles().selectByUuid(profile.getKee()).get().getName();
  }

  private void insertRulesProfile(BuiltInQProfile builtIn) {
    RulesProfileDto dto = newRuleProfileDto(rp -> rp
      .setIsBuiltIn(true)
      .setLanguage(builtIn.getLanguage())
      .setName(builtIn.getName()));
    dbClient.qualityProfileDao().insert(db.getSession(), dto);
    db.commit();
  }

  private static class DummyBuiltInQProfileInsert implements BuiltInQProfileInsert {
    private final List<BuiltInQProfile> callLogs = new ArrayList<>();

    @Override
    public void create(DbSession batchDbSession, BuiltInQProfile builtIn) {
      callLogs.add(builtIn);
    }
  }

  private static class DummyBuiltInQProfileUpdate implements BuiltInQProfileUpdate {
    private final List<BuiltInQProfile> callLogs = new ArrayList<>();

    @Override
    public List<ActiveRuleChange> update(DbSession dbSession, BuiltInQProfile builtIn, RulesProfileDto ruleProfile) {
      callLogs.add(builtIn);
      return Collections.emptyList();
    }
  }

  @Test
  public void start_shouldSetBuiltInProfileAsDefault_whenCustomDefaultProfileHasNoRule() {
    QProfileDto customProfile = newQualityProfileDto()
      .setIsBuiltIn(false)
      .setLanguage(FOO_LANGUAGE.getKey())
      .setName("Name")
      .setRulesProfileUuid(Uuids.createFast());

    QProfileDto builtinProfile = newQualityProfileDto()
      .setIsBuiltIn(true)
      .setName(Uuids.createFast())
      .setLanguage(FOO_LANGUAGE.getKey())
      .setRulesProfileUuid(Uuids.createFast());

    db.qualityProfiles().insert(customProfile, builtinProfile);
    db.qualityProfiles().setAsDefault(customProfile);

    builtInQProfileRepositoryRule.add(FOO_LANGUAGE, builtinProfile.getName(), false);
    builtInQProfileRepositoryRule.initialize();
    underTest.start();

    assertThat(selectUuidOfDefaultProfile(FOO_LANGUAGE.getKey()))
      .isPresent().contains(builtinProfile.getKee());

  }

  @Test
  public void start_shouldNotSetBuiltInProfileAsDefault_whenCustomDefaultProfileHasRule() {
    QProfileDto customProfile = newQualityProfileDto()
      .setIsBuiltIn(false)
      .setLanguage(FOO_LANGUAGE.getKey())
      .setName("Name")
      .setRulesProfileUuid(Uuids.createFast());

    QProfileDto builtinProfile = newQualityProfileDto()
      .setIsBuiltIn(true)
      .setName(Uuids.createFast())
      .setLanguage(FOO_LANGUAGE.getKey())
      .setRulesProfileUuid(Uuids.createFast());

    db.qualityProfiles().insert(customProfile, builtinProfile);
    db.qualityProfiles().setAsDefault(customProfile);

    RuleDto ruleDto = db.rules().insert(r -> r.setLanguage(FOO_LANGUAGE.getKey()));
    db.qualityProfiles().activateRule(customProfile, ruleDto);

    builtInQProfileRepositoryRule.add(FOO_LANGUAGE, builtinProfile.getName(), false);
    builtInQProfileRepositoryRule.initialize();
    underTest.start();

    assertThat(selectUuidOfDefaultProfile(FOO_LANGUAGE.getKey()))
      .isPresent().contains(customProfile.getKee());

  }
}
