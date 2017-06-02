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
import org.apache.commons.lang.RandomStringUtils;
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
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;

public class RegisterQualityProfilesTest {
  private static final Language FOO_LANGUAGE = LanguageTesting.newLanguage("foo");
  private static final Language BAR_LANGUAGE = LanguageTesting.newLanguage("bar");

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public BuiltInQProfileRepositoryRule builtInQProfileRepositoryRule = new BuiltInQProfileRepositoryRule();
  @Rule
  public LogTester logTester = new LogTester();

  private DbClient dbClient = db.getDbClient();
  private DummyBuiltInQProfileInsert insert = new DummyBuiltInQProfileInsert();
  private RegisterQualityProfiles underTest = new RegisterQualityProfiles(
    builtInQProfileRepositoryRule,
    dbClient,
    insert);

  @Test
  public void start_fails_if_BuiltInQProfileRepository_has_not_been_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("initialize must be called first");

    underTest.start();
  }

  @Test
  public void persist_built_in_profiles_that_are_not_persisted_yet() {
    BuiltInQProfile builtInQProfile = builtInQProfileRepositoryRule.add(FOO_LANGUAGE, "Sonar way");
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(insert.callLogs)
      .containsExactly(builtInQProfile);
  }

  @Test
  public void dont_persist_built_in_profiles_that_are_already_persisted() {
    String name = "doh";

    BuiltInQProfile persistedBuiltIn = builtInQProfileRepositoryRule.add(FOO_LANGUAGE, name, true);
    BuiltInQProfile nonPersistedBuiltIn = builtInQProfileRepositoryRule.add(BAR_LANGUAGE, name, true);
    builtInQProfileRepositoryRule.initialize();
    insertRulesProfile(persistedBuiltIn);

    underTest.start();

    assertThat(insert.callLogs)
      .containsExactly(nonPersistedBuiltIn);
  }

  @Test
  public void rename_custom_outdated_profiles_if_same_name_than_built_in_profile() {
    OrganizationDto org1 = db.organizations().insert(org -> org.setKey("org1"));
    OrganizationDto org2 = db.organizations().insert(org -> org.setKey("org2"));

    QProfileDto outdatedProfileInOrg1 = db.qualityProfiles().insert(org1, p -> p.setIsBuiltIn(false)
      .setLanguage(FOO_LANGUAGE.getKey()).setName("Sonar way"));
    QProfileDto outdatedProfileInOrg2 = db.qualityProfiles().insert(org2, p -> p.setIsBuiltIn(false)
      .setLanguage(FOO_LANGUAGE.getKey()).setName("Sonar way"));
    builtInQProfileRepositoryRule.add(FOO_LANGUAGE, "Sonar way", false);
    builtInQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(selectPersistedName(outdatedProfileInOrg1)).isEqualTo("Sonar way (outdated copy)");
    assertThat(selectPersistedName(outdatedProfileInOrg2)).isEqualTo("Sonar way (outdated copy)");
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Rename Quality profiles [foo/Sonar way] to [Sonar way (outdated copy)] in 2 organizations");
  }

  private String selectPersistedName(QProfileDto profile) {
    return db.qualityProfiles().selectByUuid(profile.getKee()).get().getName();
  }

  private void insertRulesProfile(BuiltInQProfile builtIn) {
    RulesProfileDto dto = new RulesProfileDto()
      .setIsBuiltIn(true)
      .setKee(RandomStringUtils.randomAlphabetic(40))
      .setLanguage(builtIn.getLanguage())
      .setName(builtIn.getName());
    dbClient.qualityProfileDao().insert(db.getSession(), dto);
    db.commit();
  }

  private static class DummyBuiltInQProfileInsert implements BuiltInQProfileInsert {
    private final List<BuiltInQProfile> callLogs = new ArrayList<>();

    @Override
    public void create(DbSession dbSession, DbSession batchDbSession, BuiltInQProfile builtIn) {
      callLogs.add(builtIn);
    }
  }
}
