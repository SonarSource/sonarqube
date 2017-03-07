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
package org.sonar.server.qualityprofile.ws;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.RuleActivatorContextFactory;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse.QualityProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.language.LanguageTesting.newLanguages;
import static org.sonarqube.ws.QualityProfiles.CreateWsResponse.parseFrom;

public class CreateActionTest {

  private static final String XOO_LANGUAGE = "xoo";
  private static final RuleDto RULE = RuleTesting.newXooX1().setSeverity("MINOR").setLanguage(XOO_LANGUAGE);
  private System2 system2 = System2.INSTANCE;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create(system2);
  @Rule
  public EsTester esTester = new EsTester(new RuleIndexDefinition(new MapSettings()));
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private RuleIndex ruleIndex = new RuleIndex(esTester.client());
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private RuleIndexer ruleIndexer = new RuleIndexer(system2, dbClient, esTester.client());
  private ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(system2, dbClient, esTester.client());
  private ProfileImporter[] profileImporters = createImporters();
  private QProfileExporters qProfileExporters = new QProfileExporters(dbClient, null,
    new RuleActivator(mock(System2.class), dbClient, ruleIndex, new RuleActivatorContextFactory(dbClient), null, activeRuleIndexer, userSession),
    profileImporters);
  private OrganizationDto organization;

  private CreateAction underTest = new CreateAction(dbClient, new QProfileFactory(dbClient, UuidFactoryFast.getInstance()), qProfileExporters,
    newLanguages(XOO_LANGUAGE), new QProfileWsSupport(dbClient, userSession, defaultOrganizationProvider),
    userSession, activeRuleIndexer, profileImporters);
  private WsActionTester wsTester = new WsActionTester(underTest);

  @Before
  public void before() {
    organization = dbTester.organizations().insert();
  }

  @Test
  public void create_profile() {
    logInAsQProfileAdministrator();

    CreateWsResponse response = executeRequest("New Profile", XOO_LANGUAGE);

    QualityProfileDto dto = dbClient.qualityProfileDao().selectByNameAndLanguage("New Profile", XOO_LANGUAGE, dbSession);
    assertThat(dto.getKey()).isNotNull();
    assertThat(dto.getLanguage()).isEqualTo(XOO_LANGUAGE);
    assertThat(dto.getName()).isEqualTo("New Profile");

    QualityProfile profile = response.getProfile();
    assertThat(profile.getKey()).isEqualTo(dto.getKey());
    assertThat(profile.getName()).isEqualTo("New Profile");
    assertThat(profile.getLanguage()).isEqualTo(XOO_LANGUAGE);
    assertThat(profile.getIsInherited()).isFalse();
    assertThat(profile.getIsDefault()).isFalse();
    assertThat(profile.hasInfos()).isFalse();
    assertThat(profile.hasWarnings()).isFalse();
  }

  @Test
  public void create_profile_from_backup_xml() {
    logInAsQProfileAdministrator();
    insertRule(RULE);

    executeRequest("New Profile", XOO_LANGUAGE, ImmutableMap.of("xoo_lint", "<xml/>"));

    QualityProfileDto dto = dbClient.qualityProfileDao().selectByNameAndLanguage("New Profile", XOO_LANGUAGE, dbSession);
    assertThat(dto.getKey()).isNotNull();
    assertThat(dbClient.activeRuleDao().selectByProfileKey(dbSession, dto.getKey())).hasSize(1);
    assertThat(ruleIndex.searchAll(new RuleQuery().setQProfileKey(dto.getKey()).setActivation(true))).hasSize(1);
  }

  @Test
  public void create_profile_with_messages() {
    logInAsQProfileAdministrator();

    CreateWsResponse response = executeRequest("Profile with messages", XOO_LANGUAGE, ImmutableMap.of("with_messages", "<xml/>"));

    QualityProfile profile = response.getProfile();
    assertThat(profile.getInfos().getInfosList()).containsOnly("an info");
    assertThat(profile.getWarnings().getWarningsList()).containsOnly("a warning");
  }

  @Test
  public void create_profile_for_specific_organization() {
    logInAsQProfileAdministrator();

    String orgKey = organization.getKey();

    TestRequest request = wsTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("organization", orgKey)
      .setParam("name", "Profile with messages")
      .setParam("language", XOO_LANGUAGE)
      .setParam("backup_with_messages", "<xml/>");

    assertThat(executeRequest(request).getProfile().getOrganization())
      .isEqualTo(orgKey);
  }

  @Test
  public void create_two_qprofiles_in_different_organizations_with_same_name_and_language() {

    // this name will be used twice
    String profileName = "Profile123";

    OrganizationDto organization1 = dbTester.organizations().insert();
    logInAsQProfileAdministrator(organization1);
    TestRequest request1 = wsTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("organization", organization1.getKey())
      .setParam("name", profileName)
      .setParam("language", XOO_LANGUAGE);
    assertThat(executeRequest(request1).getProfile().getOrganization())
      .isEqualTo(organization1.getKey());

    OrganizationDto organization2 = dbTester.organizations().insert();
    logInAsQProfileAdministrator(organization2);
    TestRequest request2 = wsTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("organization", organization2.getKey())
      .setParam("name", profileName)
      .setParam("language", XOO_LANGUAGE);
    assertThat(executeRequest(request2).getProfile().getOrganization())
      .isEqualTo(organization2.getKey());
  }

  @Test
  public void fail_if_unsufficient_privileges() {
    OrganizationDto organizationX = dbTester.organizations().insert();
    OrganizationDto organizationY = dbTester.organizations().insert();

    logInAsQProfileAdministrator(organizationX);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    executeRequest(wsTester.newRequest()
      .setParam("organization", organizationY.getKey())
      .setParam("name", "some Name")
      .setParam("language", XOO_LANGUAGE));
  }

  @Test
  public void fail_if_import_generate_error() {
    logInAsQProfileAdministrator();

    expectedException.expect(BadRequestException.class);
    executeRequest("Profile with errors", XOO_LANGUAGE, ImmutableMap.of("with_errors", "<xml/>"));
  }

  @Test
  public void test_json() throws Exception {
    logInAsQProfileAdministrator(dbTester.getDefaultOrganization());

    TestResponse response = wsTester.newRequest()
      .setMethod("POST")
      .setMediaType(MediaTypes.JSON)
      .setParam("language", XOO_LANGUAGE)
      .setParam("name", "Yeehaw!")
      .execute();

    JsonAssert.assertJson(response.getInput()).isSimilarTo(getClass().getResource("CreateActionTest/test_json.json"));
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.JSON);
  }

  private void insertRule(RuleDto ruleDto) {
    dbClient.ruleDao().insert(dbSession, ruleDto);
    dbSession.commit();
    ruleIndexer.index();
  }

  private CreateWsResponse executeRequest(String name, String language) {
    return executeRequest(name, language, Collections.emptyMap());
  }

  private CreateWsResponse executeRequest(String name, String language, Map<String, String> xmls) {
    TestRequest request = wsTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("organization", organization.getKey())
      .setParam("name", name)
      .setParam("language", language);
    for (Map.Entry<String, String> entry : xmls.entrySet()) {
      request.setParam("backup_" + entry.getKey(), entry.getValue());
    }
    return executeRequest(request);
  }

  private CreateWsResponse executeRequest(TestRequest request) {
    try {
      return parseFrom(request.execute().getInputStream());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private ProfileImporter[] createImporters() {
    class DefaultProfileImporter extends ProfileImporter {
      private DefaultProfileImporter() {
        super("xoo_lint", "Xoo Lint");
        setSupportedLanguages(XOO_LANGUAGE);
      }

      @Override
      public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
        RulesProfile rulesProfile = RulesProfile.create();
        rulesProfile.activateRule(org.sonar.api.rules.Rule.create(RULE.getRepositoryKey(), RULE.getRuleKey()), RulePriority.BLOCKER);
        return rulesProfile;
      }
    }

    class ProfileImporterGeneratingMessages extends ProfileImporter {
      private ProfileImporterGeneratingMessages() {
        super("with_messages", "With messages");
        setSupportedLanguages(XOO_LANGUAGE);
      }

      @Override
      public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
        RulesProfile rulesProfile = RulesProfile.create();
        messages.addWarningText("a warning");
        messages.addInfoText("an info");
        return rulesProfile;
      }
    }

    class ProfileImporterGeneratingErrors extends ProfileImporter {
      private ProfileImporterGeneratingErrors() {
        super("with_errors", "With errors");
        setSupportedLanguages(XOO_LANGUAGE);
      }

      @Override
      public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
        RulesProfile rulesProfile = RulesProfile.create();
        messages.addErrorText("error!");
        return rulesProfile;
      }
    }

    return new ProfileImporter[] {
      new DefaultProfileImporter(), new ProfileImporterGeneratingMessages(), new ProfileImporterGeneratingErrors()
    };
  }

  private void logInAsQProfileAdministrator() {
    logInAsQProfileAdministrator(this.organization);
  }

  private void logInAsQProfileAdministrator(OrganizationDto organization) {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organization);
  }
}
