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
import java.util.List;
import java.util.Objects;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RegisterQualityProfilesTest {
  private static final Language FOO_LANGUAGE = LanguageTesting.newLanguage("foo", "foo", "foo");
  private static final Language BAR_LANGUAGE = LanguageTesting.newLanguage("bar", "bar", "bar");

  @Rule
  public DbTester dbTester = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public BuiltInQProfileRepositoryRule builtInQProfileRepositoryRule = new BuiltInQProfileRepositoryRule();
  @Rule
  public LogTester logTester = new LogTester();

  private DbClient dbClient = dbTester.getDbClient();
  private ActiveRuleIndexer mockedActiveRuleIndexer = mock(ActiveRuleIndexer.class);
  private DummyBuiltInQProfileInsert builtInQProfileCreation = new DummyBuiltInQProfileInsert();
  private RegisterQualityProfiles underTest = new RegisterQualityProfiles(
    builtInQProfileRepositoryRule,
    dbClient,
    builtInQProfileCreation,
    mockedActiveRuleIndexer);

  @Test
  public void start_fails_if_BuiltInQProfileRepository_has_not_been_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("initialize must be called first");

    underTest.start();
  }

  @Test
  public void create_built_in_profile_on_organizations_that_dont_have_it() {
    OrganizationDto organization1 = dbTester.organizations().insert();
    OrganizationDto organization2 = dbTester.organizations().insert();
    BuiltInQProfile builtInQProfile = builtInQProfileRepositoryRule.add(FOO_LANGUAGE, "foo1");
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(builtInQProfileCreation.getCallLogs())
      .containsExactly(
        callLog(builtInQProfile, dbTester.getDefaultOrganization()),
        callLog(builtInQProfile, organization1),
        callLog(builtInQProfile, organization2));
  }

  @Test
  public void start_creates_different_qps_and_their_loaded_templates_if_several_profile_has_same_name_for_different_languages() {
    String name = "doh";

    BuiltInQProfile builtInQProfile1 = builtInQProfileRepositoryRule.add(FOO_LANGUAGE, name, true);
    BuiltInQProfile builtInQProfile2 = builtInQProfileRepositoryRule.add(BAR_LANGUAGE, name, true);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(builtInQProfileCreation.getCallLogs())
      .containsExactly(callLog(builtInQProfile2, dbTester.getDefaultOrganization()), callLog(builtInQProfile1, dbTester.getDefaultOrganization()));
  }

  @Test
  public void rename_custom_outdated_profiles_if_same_name_than_builtin_profile() {
    OrganizationDto org1 = dbTester.organizations().insert(org -> org.setKey("org1"));
    OrganizationDto org2 = dbTester.organizations().insert(org -> org.setKey("org2"));

    RulesProfileDto outdatedProfileInOrg1 = dbTester.qualityProfiles().insert(org1, p -> p.setIsBuiltIn(false).setLanguage(FOO_LANGUAGE.getKey()).setName("Sonar way"));
    RulesProfileDto outdatedProfileInOrg2 = dbTester.qualityProfiles().insert(org2, p -> p.setIsBuiltIn(false).setLanguage(FOO_LANGUAGE.getKey()).setName("Sonar way"));
    builtInQProfileRepositoryRule.add(FOO_LANGUAGE, "Sonar way", false);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(dbTester.qualityProfiles().selectByKey(outdatedProfileInOrg1.getKey()).get().getName()).isEqualTo("Sonar way (outdated copy)");
    assertThat(dbTester.qualityProfiles().selectByKey(outdatedProfileInOrg2.getKey()).get().getName()).isEqualTo("Sonar way (outdated copy)");
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Rename Quality profiles [foo/Sonar way] to [Sonar way (outdated copy)] in 2 organizations");
  }

  private class DummyBuiltInQProfileInsert implements BuiltInQProfileInsert {
    private final List<CallLog> callLogs = new ArrayList<>();

    @Override
    public void create(DbSession session, DbSession batchSession, BuiltInQProfile qualityProfile, OrganizationDto organization) {
      callLogs.add(callLog(qualityProfile, organization));
    }

    List<CallLog> getCallLogs() {
      return callLogs;
    }
  }

  private static final class CallLog {
    private final BuiltInQProfile builtInQProfile;
    private final OrganizationDto organization;

    private CallLog(BuiltInQProfile builtInQProfile, OrganizationDto organization) {
      this.builtInQProfile = builtInQProfile;
      this.organization = organization;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CallLog callLog = (CallLog) o;
      return builtInQProfile == callLog.builtInQProfile &&
        organization.getUuid().equals(callLog.organization.getUuid());
    }

    @Override
    public int hashCode() {
      return Objects.hash(builtInQProfile, organization);
    }

    @Override
    public String toString() {
      return "CallLog{" +
        "qp=" + builtInQProfile.getLanguage() + '-' + builtInQProfile.getName() + '-' + builtInQProfile.isDefault() +
        ", org=" + organization.getKey() +
        '}';
    }
  }

  private static CallLog callLog(BuiltInQProfile builtInQProfile, OrganizationDto organizationDto) {
    return new CallLog(builtInQProfile, organizationDto);
  }
}
