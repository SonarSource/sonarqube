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
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.util.UtcDateUtils.formatDateTime;

public class RegisterQualityProfilesTest {

  private static final DummyLanguage FOO_LANGUAGE = new DummyLanguage("foo");
  private static final DummyLanguage BAR_LANGUAGE = new DummyLanguage("bar");
  private static final String TABLE_RULES_PROFILES = "RULES_PROFILES";
  private static final String TYPE_QUALITY_PROFILE = "QUALITY_PROFILE";
  public static final String SONAR_WAY_QP_NAME = "Sonar way";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private DbClient mockedDbClient = mock(DbClient.class);
  private UuidFactory mockedUuidFactory = mock(UuidFactory.class);
  private System2 mockedSystem2 = mock(System2.class);
  private ActiveRuleIndexer mockedActiveRuleIndexer = mock(ActiveRuleIndexer.class);

  @Test
  public void no_action_in_DB_nor_index_when_there_is_no_definition() {
    RegisterQualityProfiles underTest = mockedDBAndEs(Collections.emptyList(), new Languages(FOO_LANGUAGE));

    underTest.start();

    verifyZeroInteractions(mockedDbClient, mockedActiveRuleIndexer);
  }

  @Test
  public void no_action_in_DB_nor_index_when_all_definitions_apply_to_non_defined_languages() {
    RegisterQualityProfiles underTest = mockedDBAndEs(Collections.singletonList(new DummyProfileDefinition("foo", "P1", false)), new Languages());

    underTest.start();

    verifyZeroInteractions(mockedDbClient, mockedActiveRuleIndexer);
  }

  @Test
  public void start_throws_IAE_if_profileDefinition_creates_RulesProfile_with_null_name() {
    DummyProfileDefinition definition = new DummyProfileDefinition("foo", null, false);
    RegisterQualityProfiles underTest = mockedDBAndEs(Collections.singletonList(definition), new Languages());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Profile created by Definition " + definition + " can't have a blank name");

    underTest.start();
  }

  @Test
  public void start_throws_IAE_if_profileDefinition_creates_RulesProfile_with_empty_name() {
    DummyProfileDefinition definition = new DummyProfileDefinition("foo", "", false);
    RegisterQualityProfiles underTest = mockedDBAndEs(Collections.singletonList(definition), new Languages());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Profile created by Definition " + definition + " can't have a blank name");

    underTest.start();
  }

  @Test
  public void start_creates_qp_if_language_exists_and_store_flag_in_loaded_templates() {
    String uuid = "generated uuid";
    long now = 2_456_789;
    RegisterQualityProfiles underTest = mockedEs(Collections.singletonList(new DummyProfileDefinition("foo", "foo1", false)), new Languages(FOO_LANGUAGE));
    mockForSingleQPInsert(uuid, now);

    underTest.start();

    QualityProfileDto dto = getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, "foo1");
    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getLanguage()).isEqualTo(FOO_LANGUAGE.getKey());
    assertThat(dto.getName()).isEqualTo("foo1");
    assertThat(dto.getKee()).isEqualTo(uuid);
    assertThat(dto.getKey()).isEqualTo(uuid);
    assertThat(dto.getParentKee()).isNull();
    assertThat(dto.getRulesUpdatedAt()).isEqualTo(formatDateTime(new Date(now)));
    assertThat(dto.getLastUsed()).isNull();
    assertThat(dto.getUserUpdatedAt()).isNull();
    assertThat(dto.isDefault()).isTrue();
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), TABLE_RULES_PROFILES)).isEqualTo(1);

    assertThat(dbClient.loadedTemplateDao().countByTypeAndKey(TYPE_QUALITY_PROFILE, "foo:foo1", dbTester.getSession()))
      .isEqualTo(1);
  }

  @Test
  public void start_makes_single_qp_of_a_language_default_even_if_flagged_as_so() {
    RegisterQualityProfiles underTest = mockedEs(Collections.singletonList(new DummyProfileDefinition("foo", "foo1", true)), new Languages(FOO_LANGUAGE));
    mockForSingleQPInsert();

    underTest.start();

    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, "foo1").isDefault()).isTrue();
  }

  @Test
  public void start_makes_single_qp_of_a_language_default_even_if_not_flagged_as_so() {
    RegisterQualityProfiles underTest = mockedEs(Collections.singletonList(new DummyProfileDefinition("foo", "foo1", false)), new Languages(FOO_LANGUAGE));
    mockForSingleQPInsert();

    underTest.start();

    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, "foo1").isDefault()).isTrue();
  }

  @Test
  public void start_makes_first_qp_of_a_language_default_when_none_flagged_as_so() {
    List<ProfileDefinition> definitions = new ArrayList<>(asList(new DummyProfileDefinition("foo", "foo1", false), new DummyProfileDefinition("foo", "foo2", false)));
    Collections.shuffle(definitions);
    String firstQPName = ((DummyProfileDefinition) definitions.iterator().next()).getName();
    RegisterQualityProfiles underTest = mockedEs(definitions, new Languages(FOO_LANGUAGE));
    mockForTwoQPInserts();

    underTest.start();

    QualityProfileDto dto = getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, "foo1");
    assertThat(dto.isDefault()).isEqualTo(dto.getName().equals(firstQPName));
    dto = getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, "foo2");
    assertThat(dto.isDefault()).isEqualTo(dto.getName().equals(firstQPName));
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), TABLE_RULES_PROFILES)).isEqualTo(2);
  }

  @Test
  public void start_does_not_create_sq_if_loaded_profile_already_exists() {
    dbClient.loadedTemplateDao().insert(new LoadedTemplateDto("foo:foo1", TYPE_QUALITY_PROFILE), dbTester.getSession());
    dbTester.commit();
    RegisterQualityProfiles underTest = mockedEs(Collections.singletonList(new DummyProfileDefinition("foo", "foo1", false)), new Languages(FOO_LANGUAGE));

    underTest.start();

    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), TABLE_RULES_PROFILES)).isEqualTo(0);
  }

  @Test
  public void start_fails_with_ISE_when_two_sq_with_different_name_are_default_for_the_same_language() {
    RegisterQualityProfiles underTest = mockedEs(
      asList(new DummyProfileDefinition("foo", "foo1", true), new DummyProfileDefinition("foo", "foo2", true)),
      new Languages(FOO_LANGUAGE));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Several Quality profiles are flagged as default for the language foo: [foo1, foo2]");

    underTest.start();
  }

  @Test
  public void starts_creates_single_qp_if_several_profile_have_the_same_name_for_a_given_language() {
    RegisterQualityProfiles underTest = mockedEs(
      asList(new DummyProfileDefinition("foo", "foo1", true), new DummyProfileDefinition("foo", "foo1", true)),
      new Languages(FOO_LANGUAGE));
    mockForSingleQPInsert();

    underTest.start();

    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, "foo1").isDefault()).isTrue();
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), TABLE_RULES_PROFILES)).isEqualTo(1);
  }

  @Test
  public void start_creates_different_qp_and_their_loaded_templates_if_several_profile_has_same_name_for_different_languages() {
    String uuid1 = "uuid1";
    String uuid2 = "uuid2";
    long date1 = 2_456_789L;
    long date2 = 4_231_654L;
    String name = "doh";

    RegisterQualityProfiles underTest = mockedEs(
      asList(new DummyProfileDefinition("foo", name, true), new DummyProfileDefinition("bar", name, true)),
      new Languages(FOO_LANGUAGE, BAR_LANGUAGE));
    when(mockedUuidFactory.create()).thenReturn(uuid1).thenReturn(uuid2).thenThrow(new UnsupportedOperationException("uuidFactory should be called only twice"));
    when(mockedSystem2.now()).thenReturn(date1).thenReturn(date2).thenThrow(new UnsupportedOperationException("now should be called only twice"));

    underTest.start();

    OrganizationDto organization = dbTester.getDefaultOrganization();
    QualityProfileDto dto = getPersistedQP(organization, FOO_LANGUAGE, name);
    String uuidQP1 = dto.getKee();
    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getLanguage()).isEqualTo(FOO_LANGUAGE.getKey());
    assertThat(dto.getName()).isEqualTo(name);
    assertThat(uuidQP1).isIn(uuid1, uuid2);
    assertThat(dto.getKey()).isEqualTo(uuidQP1);
    assertThat(dto.getParentKee()).isNull();
    assertThat(dto.getRulesUpdatedAt()).isIn(formatDateTime(new Date(date1)), formatDateTime(new Date(date2)));
    assertThat(dto.getLastUsed()).isNull();
    assertThat(dto.getUserUpdatedAt()).isNull();
    assertThat(dto.isDefault()).isTrue();
    dto = getPersistedQP(dbTester.getDefaultOrganization(), BAR_LANGUAGE, name);
    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getLanguage()).isEqualTo(BAR_LANGUAGE.getKey());
    assertThat(dto.getName()).isEqualTo(name);
    String uuidQP2 = uuid1.equals(uuidQP1) ? uuid2 : uuid1;
    long dateQP2 = uuid1.equals(uuidQP1) ? date2 : date1;
    assertThat(dto.getKee()).isEqualTo(uuidQP2);
    assertThat(dto.getKey()).isEqualTo(uuidQP2);
    assertThat(dto.getParentKee()).isNull();
    assertThat(dto.getRulesUpdatedAt()).isEqualTo(formatDateTime(new Date(dateQP2)));
    assertThat(dto.getLastUsed()).isNull();
    assertThat(dto.getUserUpdatedAt()).isNull();
    assertThat(dto.isDefault()).isTrue();
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), TABLE_RULES_PROFILES)).isEqualTo(2);

    assertThat(dbClient.loadedTemplateDao().countByTypeAndKey(TYPE_QUALITY_PROFILE, "foo:doh", dbTester.getSession()))
      .isEqualTo(1);
    assertThat(dbClient.loadedTemplateDao().countByTypeAndKey(TYPE_QUALITY_PROFILE, "bar:doh", dbTester.getSession()))
      .isEqualTo(1);
  }

  @Test
  public void start_create_qp_as_default_even_if_only_one_profile_with_given_name_has_default_flag_true() {
    String name = "doh";
    RegisterQualityProfiles underTest = mockedEs(
      asList(new DummyProfileDefinition("foo", name, false), new DummyProfileDefinition("foo", name, true)),
      new Languages(FOO_LANGUAGE));
    mockForSingleQPInsert();

    underTest.start();

    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, name).isDefault()).isTrue();
  }

  @Test
  public void start_creates_qp_Sonar_Way_as_default_if_none_other_is_defined_default_for_a_given_language() {

    RegisterQualityProfiles underTest = mockedEs(
      asList(
        new DummyProfileDefinition("foo", "doh", false), new DummyProfileDefinition("foo", "boo", false),
        new DummyProfileDefinition("foo", SONAR_WAY_QP_NAME, false), new DummyProfileDefinition("foo", "goo", false)),
      new Languages(FOO_LANGUAGE));
    when(mockedUuidFactory.create()).thenReturn("uuid1").thenReturn("uuid2").thenReturn("uuid3").thenReturn("uuid4");
    when(mockedSystem2.now()).thenReturn(2_456_789L);

    underTest.start();

    Arrays.asList("doh", "boo", "goo")
      .forEach(name -> assertThat(getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, name).isDefault())
        .describedAs("QP with name " + name + " should not be default")
        .isFalse());
    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, SONAR_WAY_QP_NAME).isDefault()).isTrue();
  }

  @Test
  public void start_does_not_create_Sonar_Way_as_default_if_other_profile_is_defined_as_default() {
    RegisterQualityProfiles underTest = mockedEs(
      asList(
        new DummyProfileDefinition("foo", SONAR_WAY_QP_NAME, false), new DummyProfileDefinition("foo", "goo", true)),
      new Languages(FOO_LANGUAGE));
    mockForTwoQPInserts();

    underTest.start();

    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, SONAR_WAY_QP_NAME).isDefault()).isFalse();
    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, "goo").isDefault()).isTrue();
  }

  @Test
  public void start_matches_Sonar_Way_default_with_case_sensitivity() {
    String sonarWayInOtherCase = SONAR_WAY_QP_NAME.toUpperCase();
    RegisterQualityProfiles underTest = mockedEs(
      asList(
        new DummyProfileDefinition("foo", "goo", false), new DummyProfileDefinition("foo", sonarWayInOtherCase, false)),
      new Languages(FOO_LANGUAGE));
    mockForTwoQPInserts();

    underTest.start();

    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, sonarWayInOtherCase).isDefault()).isFalse();
    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, "goo").isDefault()).isTrue();
  }

  private void mockForSingleQPInsert() {
    mockForSingleQPInsert("generated uuid", 2_456_789);
  }

  private void mockForTwoQPInserts() {
    when(mockedUuidFactory.create()).thenReturn("uuid1").thenReturn("uuid2").thenThrow(new UnsupportedOperationException("uuidFactory should be called only twice"));
    when(mockedSystem2.now()).thenReturn(2_456_789L).thenReturn(3_789_159L).thenThrow(new UnsupportedOperationException("now should be called only twice"));
  }

  private void mockForSingleQPInsert(String uuid, long now) {
    when(mockedUuidFactory.create()).thenReturn(uuid).thenThrow(new UnsupportedOperationException("uuidFactory should be called only once"));
    when(mockedSystem2.now()).thenReturn(now).thenThrow(new UnsupportedOperationException("now should be called only once"));
  }

  private QualityProfileDto getPersistedQP(OrganizationDto organization, Language language, String name) {
    return dbClient.qualityProfileDao().selectByNameAndLanguage(organization, name, language.getKey(), dbTester.getSession());
  }

  private RegisterQualityProfiles mockedDBAndEs(List<ProfileDefinition> definitions, Languages languages) {
    return new RegisterQualityProfiles(mockedDbClient, null, null, definitions, languages, mockedActiveRuleIndexer, null);
  }

  private RegisterQualityProfiles mockedEs(List<ProfileDefinition> definitions, Languages languages) {
    return new RegisterQualityProfiles(
      dbClient,
      new QProfileFactory(dbClient, mockedUuidFactory, mockedSystem2),
      new CachingRuleActivator(mockedSystem2, dbClient, null, new RuleActivatorContextFactory(dbClient), null, null, userSessionRule),
      definitions,
      languages,
      mockedActiveRuleIndexer,
      defaultOrganizationProvider);
  }

  private static final class DummyProfileDefinition extends ProfileDefinition {
    private final String language;
    private final String name;
    private final boolean defaultProfile;

    private DummyProfileDefinition(String language, String name, boolean defaultProfile) {
      this.language = language;
      this.name = name;
      this.defaultProfile = defaultProfile;
    }

    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      RulesProfile res = RulesProfile.create(name, language);
      res.setDefaultProfile(defaultProfile);
      return res;
    }

    String getLanguage() {
      return language;
    }

    String getName() {
      return name;
    }

    boolean isDefaultProfile() {
      return defaultProfile;
    }
  }

  private static class DummyLanguage implements Language {
    private final String key;

    private DummyLanguage(String key) {
      this.key = key;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public String getName() {
      return key;
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[] {key};
    }
  }
}
