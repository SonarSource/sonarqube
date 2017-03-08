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

import java.util.Collections;
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
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class RegisterQualityProfilesTest {

  private static final DummyLanguage FOO_LANGUAGE = new DummyLanguage("foo");

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private DbClient mockedDbClient = mock(DbClient.class);
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

  private RegisterQualityProfiles mockedDBAndEs(List<ProfileDefinition> definitions, Languages languages) {
    return new RegisterQualityProfiles(mockedDbClient, null, null, definitions, languages, mockedActiveRuleIndexer, null);
  }

  public RegisterQualityProfiles mockedEs(List<ProfileDefinition> definitions, Languages languages) {
    return new RegisterQualityProfiles(
        dbClient,
        new QProfileFactory(dbClient, UuidFactoryFast.getInstance()),
        new CachingRuleActivator(AlwaysIncreasingSystem2.INSTANCE, dbClient, null, new RuleActivatorContextFactory(dbClient), null, null, userSessionRule),
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
      return RulesProfile.create(name, language);
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
