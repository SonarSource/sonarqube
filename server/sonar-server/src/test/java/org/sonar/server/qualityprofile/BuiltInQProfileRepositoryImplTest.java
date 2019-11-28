/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.language.LanguageTesting;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.db.rule.RuleTesting.EXTERNAL_XOO;
import static org.sonar.server.qualityprofile.BuiltInQProfile.ActiveRule;

public class BuiltInQProfileRepositoryImplTest {
  private static final Language FOO_LANGUAGE = LanguageTesting.newLanguage("foo", "foo", "foo");
  private static final String SONAR_WAY_QP_NAME = "Sonar way";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();

  private DbClient dbClient = db.getDbClient();

  @Test
  public void create_qprofile_with_rule() {
    RuleDefinitionDto rule1 = db.rules().insert();
    RuleDefinitionDto rule2 = db.rules().insert();
    RuleDefinitionDto ruleNotToBeActivated = db.rules().insert();
    List<DummyProfileDefinition> definitions = singletonList(new DummyProfileDefinition("foo", "foo", false,
      asList(rule1.getKey(), rule2.getKey())));
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(dbClient, new Languages(FOO_LANGUAGE), definitions.toArray(new BuiltInQualityProfilesDefinition[0]));

    underTest.initialize();

    assertThat(underTest.get())
      .extracting(BuiltInQProfile::getName)
      .containsExactlyInAnyOrder("foo");
    assertThat(underTest.get().get(0).getActiveRules())
      .extracting(ActiveRule::getRuleId, ActiveRule::getRuleKey)
      .containsExactlyInAnyOrder(
        tuple(rule1.getId(), rule1.getKey()),
        tuple(rule2.getId(), rule2.getKey())
      );
  }

  @Test
  public void make_single_profile_of_a_language_default_even_if_not_flagged_as_so() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(dbClient, new Languages(FOO_LANGUAGE), new DummyProfileDefinition("foo", "foo1", false));

    underTest.initialize();

    assertThat(underTest.get())
      .extracting(BuiltInQProfile::getLanguage, BuiltInQProfile::isDefault)
      .containsExactly(tuple(FOO_LANGUAGE.getKey(), true));
  }

  @Test
  public void make_single_profile_of_a_language_default_even_if_flagged_as_so() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(dbClient, new Languages(FOO_LANGUAGE), new DummyProfileDefinition("foo", "foo1", true));

    underTest.initialize();

    assertThat(underTest.get())
      .extracting(BuiltInQProfile::getLanguage, BuiltInQProfile::isDefault)
      .containsExactly(tuple(FOO_LANGUAGE.getKey(), true));
  }

  @Test
  public void make_first_profile_of_a_language_default_when_none_flagged_as_so() {
    List<DummyProfileDefinition> definitions = new ArrayList<>(
      asList(new DummyProfileDefinition("foo", "foo1", false), new DummyProfileDefinition("foo", "foo2", false)));
    String firstName = definitions.get(0).getName();
    String secondName = definitions.get(1).getName();
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(dbClient, new Languages(FOO_LANGUAGE), definitions.toArray(new BuiltInQualityProfilesDefinition[0]));

    underTest.initialize();

    assertThat(underTest.get())
      .extracting(BuiltInQProfile::getName, BuiltInQProfile::isDefault)
      .containsExactlyInAnyOrder(tuple(firstName, true), tuple(secondName, false));
  }

  @Test
  public void create_profile_Sonar_Way_as_default_if_none_other_is_defined_default_for_a_given_language() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(
      dbClient, new Languages(FOO_LANGUAGE),
      new DummyProfileDefinition("foo", "doh", false), new DummyProfileDefinition("foo", "boo", false),
      new DummyProfileDefinition("foo", SONAR_WAY_QP_NAME, false), new DummyProfileDefinition("foo", "goo", false));

    underTest.initialize();

    assertThat(underTest.get())
      .filteredOn(b -> FOO_LANGUAGE.getKey().equals(b.getLanguage()))
      .filteredOn(BuiltInQProfile::isDefault)
      .extracting(BuiltInQProfile::getName)
      .containsExactly(SONAR_WAY_QP_NAME);
  }

  @Test
  public void do_not_create_Sonar_Way_as_default_if_other_profile_is_defined_as_default() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(
      dbClient, new Languages(FOO_LANGUAGE),
      new DummyProfileDefinition("foo", SONAR_WAY_QP_NAME, false), new DummyProfileDefinition("foo", "goo", true));

    underTest.initialize();

    assertThat(underTest.get())
      .filteredOn(b -> FOO_LANGUAGE.getKey().equals(b.getLanguage()))
      .filteredOn(BuiltInQProfile::isDefault)
      .extracting(BuiltInQProfile::getName)
      .containsExactly("goo");
  }

  @Test
  public void match_Sonar_Way_default_with_case_sensitivity() {
    String sonarWayInOtherCase = SONAR_WAY_QP_NAME.toUpperCase();
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(
      dbClient, new Languages(FOO_LANGUAGE),
      new DummyProfileDefinition("foo", "goo", false), new DummyProfileDefinition("foo", sonarWayInOtherCase, false));

    underTest.initialize();

    assertThat(underTest.get())
      .filteredOn(b -> FOO_LANGUAGE.getKey().equals(b.getLanguage()))
      .filteredOn(BuiltInQProfile::isDefault)
      .extracting(BuiltInQProfile::getName)
      .containsExactly("goo");
  }

  @Test
  public void create_no_BuiltInQProfile_when_all_definitions_apply_to_non_defined_languages() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(mock(DbClient.class), new Languages(), new DummyProfileDefinition("foo", "P1", false));

    underTest.initialize();

    assertThat(underTest.get()).isEmpty();
  }

  @Test
  public void create_qprofile_with_deprecated_rule() {
    RuleDefinitionDto rule1 = db.rules().insert();
    db.rules().insertDeprecatedKey(d -> d.setRuleId(rule1.getId()).setOldRepositoryKey("oldRepo").setOldRuleKey("oldKey"));
    RuleDefinitionDto rule2 = db.rules().insert();
    List<DummyProfileDefinition> definitions = singletonList(new DummyProfileDefinition("foo", "foo", false,
      asList(RuleKey.of("oldRepo", "oldKey"), rule2.getKey())));
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(dbClient, new Languages(FOO_LANGUAGE), definitions.toArray(new BuiltInQualityProfilesDefinition[0]));

    underTest.initialize();

    assertThat(underTest.get())
      .extracting(BuiltInQProfile::getName)
      .containsExactlyInAnyOrder("foo");
    assertThat(underTest.get().get(0).getActiveRules())
      .extracting(ActiveRule::getRuleId, ActiveRule::getRuleKey)
      .containsExactlyInAnyOrder(
        tuple(rule1.getId(), rule1.getKey()),
        tuple(rule2.getId(), rule2.getKey())
      );
  }

  @Test
  public void fail_with_ISE_when_rule_does_not_exist() {
    List<DummyProfileDefinition> definitions = singletonList(new DummyProfileDefinition("foo", "foo", false, singletonList(EXTERNAL_XOO)));
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(dbClient, new Languages(FOO_LANGUAGE), definitions.toArray(new BuiltInQualityProfilesDefinition[0]));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(String.format("Rule with key '%s' not found", EXTERNAL_XOO.toString()));

    underTest.initialize();
  }

  @Test
  public void fail_with_ISE_when_two_profiles_with_different_name_are_default_for_the_same_language() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(dbClient, new Languages(FOO_LANGUAGE),
      new DummyProfileDefinition("foo", "foo1", true), new DummyProfileDefinition("foo", "foo2", true));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Several Quality profiles are flagged as default for the language foo: [foo1, foo2]");

    underTest.initialize();
  }

  @Test
  public void get_throws_ISE_if_called_before_initialize() {
    BuiltInQProfileRepositoryImpl underTest = new BuiltInQProfileRepositoryImpl(mock(DbClient.class), new Languages());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("initialize must be called first");

    underTest.get();
  }

  @Test
  public void initialize_throws_ISE_if_called_twice() {
    BuiltInQProfileRepositoryImpl underTest = new BuiltInQProfileRepositoryImpl(mock(DbClient.class), new Languages());
    underTest.initialize();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("initialize must be called only once");

    underTest.initialize();
  }

  @Test
  public void initialize_throws_ISE_if_language_has_no_builtin_qp() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(mock(DbClient.class), new Languages(FOO_LANGUAGE));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("The following languages have no built-in quality profiles: foo");

    underTest.initialize();
  }

  private static final class DummyProfileDefinition implements BuiltInQualityProfilesDefinition {
    private final String language;
    private final String name;
    private final boolean defaultProfile;
    private final List<RuleKey> activeRuleKeys;

    private DummyProfileDefinition(String language, String name, boolean defaultProfile, List<RuleKey> activeRuleKeys) {
      this.language = language;
      this.name = name;
      this.defaultProfile = defaultProfile;
      this.activeRuleKeys = activeRuleKeys;
    }

    private DummyProfileDefinition(String language, String name, boolean defaultProfile) {
      this(language, name, defaultProfile, emptyList());
    }

    @Override
    public void define(Context context) {
      NewBuiltInQualityProfile builtInQualityProfile = context.createBuiltInQualityProfile(name, language);
      activeRuleKeys.stream().forEach(activeRuleKey -> builtInQualityProfile.activateRule(activeRuleKey.repository(), activeRuleKey.rule()));
      builtInQualityProfile.setDefault(defaultProfile);
      builtInQualityProfile.done();
    }

    String getName() {
      return name;
    }

  }

}
