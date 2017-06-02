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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.server.language.LanguageTesting;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class BuiltInQProfileRepositoryImplTest {
  private static final Language FOO_LANGUAGE = LanguageTesting.newLanguage("foo", "foo", "foo");
  private static final String SONAR_WAY_QP_NAME = "Sonar way";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void get_throws_ISE_if_called_before_initialize() {
    BuiltInQProfileRepositoryImpl underTest = new BuiltInQProfileRepositoryImpl(new Languages());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("initialize must be called first");

    underTest.get();
  }

  @Test
  public void initialize_throws_ISE_if_called_twice() {
    BuiltInQProfileRepositoryImpl underTest = new BuiltInQProfileRepositoryImpl(new Languages());
    underTest.initialize();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("initialize must be called only once");

    underTest.initialize();
  }

  @Test
  public void initialize_creates_no_BuiltInQProfile_when_there_is_no_definition() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(new Languages(FOO_LANGUAGE));

    underTest.initialize();

    assertThat(underTest.get()).isEmpty();
  }

  @Test
  public void initialize_creates_no_BuiltInQProfile_when_all_definitions_apply_to_non_defined_languages() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(new Languages(), new DummyProfileDefinition("foo", "P1", false));

    underTest.initialize();

    assertThat(underTest.get()).isEmpty();
  }

  @Test
  public void initialize_throws_IAE_if_profileDefinition_creates_RulesProfile_with_null_name() {
    DummyProfileDefinition definition = new DummyProfileDefinition("foo", null, false);
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(new Languages(), definition);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Profile created by Definition " + definition + " can't have a blank name");

    underTest.initialize();
  }

  @Test
  public void initialize_throws_IAE_if_profileDefinition_creates_RulesProfile_with_empty_name() {
    DummyProfileDefinition definition = new DummyProfileDefinition("foo", "", false);
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(new Languages(), definition);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Profile created by Definition " + definition + " can't have a blank name");

    underTest.initialize();
  }

  @Test
  public void initialize_makes_single_profile_of_a_language_default_even_if_not_flagged_as_so() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(new Languages(FOO_LANGUAGE), new DummyProfileDefinition("foo", "foo1", false));

    underTest.initialize();

    assertThat(underTest.get())
      .extracting(BuiltInQProfile::getLanguage, BuiltInQProfile::isDefault)
      .containsExactly(tuple(FOO_LANGUAGE.getKey(), true));
  }

  @Test
  public void initialize_makes_single_profile_of_a_language_default_even_if_flagged_as_so() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(new Languages(FOO_LANGUAGE), new DummyProfileDefinition("foo", "foo1", true));

    underTest.initialize();

    assertThat(underTest.get())
      .extracting(BuiltInQProfile::getLanguage, BuiltInQProfile::isDefault)
      .containsExactly(tuple(FOO_LANGUAGE.getKey(), true));
  }

  @Test
  public void initialize_makes_first_profile_of_a_language_default_when_none_flagged_as_so() {
    List<DummyProfileDefinition> definitions = new ArrayList<>(
      asList(new DummyProfileDefinition("foo", "foo1", false), new DummyProfileDefinition("foo", "foo2", false)));
    Collections.shuffle(definitions);
    String firstName = definitions.get(0).getName();
    String secondName = definitions.get(1).getName();
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(new Languages(FOO_LANGUAGE), definitions.toArray(new ProfileDefinition[0]));

    underTest.initialize();

    assertThat(underTest.get())
      .extracting(BuiltInQProfile::getName, BuiltInQProfile::isDefault)
      .containsExactlyInAnyOrder(tuple(firstName, true), tuple(secondName, false));
  }

  @Test
  public void initialize_fails_with_ISE_when_two_profiles_with_different_name_are_default_for_the_same_language() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(new Languages(FOO_LANGUAGE),
      new DummyProfileDefinition("foo", "foo1", true), new DummyProfileDefinition("foo", "foo2", true));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Several Quality profiles are flagged as default for the language foo: [foo1, foo2]");

    underTest.initialize();
  }

  @Test
  public void initialize_creates_profile_as_default_even_if_only_one_profile_with_given_name_has_default_flag_true() {
    String name = "doh";
    boolean flag = new Random().nextBoolean();
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(new Languages(FOO_LANGUAGE),
      new DummyProfileDefinition("foo", name, flag), new DummyProfileDefinition("foo", name, !flag));

    underTest.initialize();

    assertThat(underTest.get())
      .extracting(BuiltInQProfile::getLanguage, BuiltInQProfile::isDefault)
      .containsExactly(tuple("foo", true));
  }

  @Test
  public void initialize_creates_single_profile_if_several_profile_have_the_same_name_for_a_given_language() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(new Languages(FOO_LANGUAGE),
      new DummyProfileDefinition("foo", "aName", true), new DummyProfileDefinition("foo", "aName", true));

    underTest.initialize();

    assertThat(underTest.get())
      .extracting(BuiltInQProfile::getLanguage, BuiltInQProfile::getName)
      .containsExactlyInAnyOrder(tuple(FOO_LANGUAGE.getKey(), "aName"));
  }

  @Test
  public void initialize_creates_profile_Sonar_Way_as_default_if_none_other_is_defined_default_for_a_given_language() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(
      new Languages(FOO_LANGUAGE),
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
  public void initialize_does_not_create_Sonar_Way_as_default_if_other_profile_is_defined_as_default() {
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(
      new Languages(FOO_LANGUAGE),
      new DummyProfileDefinition("foo", SONAR_WAY_QP_NAME, false), new DummyProfileDefinition("foo", "goo", true));

    underTest.initialize();

    assertThat(underTest.get())
      .filteredOn(b -> FOO_LANGUAGE.getKey().equals(b.getLanguage()))
      .filteredOn(BuiltInQProfile::isDefault)
      .extracting(BuiltInQProfile::getName)
      .containsExactly("goo");
  }

  @Test
  public void initialize_matches_Sonar_Way_default_with_case_sensitivity() {
    String sonarWayInOtherCase = SONAR_WAY_QP_NAME.toUpperCase();
    BuiltInQProfileRepository underTest = new BuiltInQProfileRepositoryImpl(
      new Languages(FOO_LANGUAGE),
      new DummyProfileDefinition("foo", "goo", false), new DummyProfileDefinition("foo", sonarWayInOtherCase, false));

    underTest.initialize();

    assertThat(underTest.get())
      .filteredOn(b -> FOO_LANGUAGE.getKey().equals(b.getLanguage()))
      .filteredOn(BuiltInQProfile::isDefault)
      .extracting(BuiltInQProfile::getName)
      .containsExactly("goo");
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

}
