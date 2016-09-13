/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.io.Reader;
import java.util.Collections;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.RuleActivatorContextFactory;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_PROFILE_ADMIN;
import static org.sonar.server.language.LanguageTesting.newLanguages;

public class CreateActionTest {

  static final String XOO_LANGUAGE = "xoo";

  static final RuleDto RULE = RuleTesting.newXooX1().setSeverity("MINOR").setLanguage(XOO_LANGUAGE);

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public EsTester esTester = new EsTester(new RuleIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  DbClient dbClient = dbTester.getDbClient();
  DbSession dbSession = dbTester.getSession();

  RuleIndex ruleIndex = new RuleIndex(esTester.client());
  RuleIndexer ruleIndexer = new RuleIndexer(dbClient, esTester.client());

  ActiveRuleIndex activeRuleIndex = new ActiveRuleIndex(esTester.client());
  ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(dbClient, esTester.client());

  ProfileImporter[] profileImporters = createImporters();

  QProfileExporters qProfileExporters = new QProfileExporters(dbClient,
    new QProfileLoader(dbClient, activeRuleIndex, ruleIndex), null,
    new RuleActivator(mock(System2.class), dbClient, ruleIndex, new RuleActivatorContextFactory(dbClient), null, activeRuleIndexer, userSession),
    profileImporters);

  CreateAction underTest = new CreateAction(dbClient, new QProfileFactory(dbClient), qProfileExporters, newLanguages(XOO_LANGUAGE), profileImporters, userSession);
  WsActionTester wsTester = new WsActionTester(underTest);

  @Test
  public void create_profile() {
    setUserAsQualityProfileAdmin();

    executeRequest("New Profile", XOO_LANGUAGE);

    QualityProfileDto profile = dbClient.qualityProfileDao().selectByNameAndLanguage("New Profile", XOO_LANGUAGE, dbSession);
    assertThat(profile.getKey()).isNotNull();
    assertThat(profile.getLanguage()).isEqualTo(XOO_LANGUAGE);
    assertThat(profile.getName()).isEqualTo("New Profile");
  }

  @Test
  public void return_created_profile_in_json() throws Exception {
    setUserAsQualityProfileAdmin();

    TestResponse response = wsTester.newRequest()
      .setMethod("POST")
      .setMediaType(MediaTypes.JSON)
      .setParam("language", XOO_LANGUAGE)
      .setParam("name", "Yeehaw!")
      .execute();

    JsonAssert.assertJson(response.getInput()).isSimilarTo(getClass().getResource("CreateActionTest/create-no-importer.json"));
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.JSON);
  }

  @Test
  public void create_profile_from_xml() {
    setUserAsQualityProfileAdmin();
    insertRule(RULE);

    executeRequest("New Profile", XOO_LANGUAGE, ImmutableMap.of("xoo_lint", "<xml/>"));

    QualityProfileDto profile = dbClient.qualityProfileDao().selectByNameAndLanguage("New Profile", XOO_LANGUAGE, dbSession);
    assertThat(profile.getKey()).isNotNull();
    assertThat(dbClient.activeRuleDao().selectByProfileKey(dbSession, profile.getKey())).hasSize(1);
    // FIXME
    // assertThat(ruleIndex.searchAll(new RuleQuery().setQProfileKey(profile.getKey()).setActivation(true))).hasSize(1);
  }

  private void insertRule(RuleDto ruleDto) {
    dbClient.ruleDao().insert(dbSession, ruleDto);
    dbSession.commit();
    ruleIndexer.index();
  }

  private void executeRequest(String name, String language) {
    executeRequest(name, language, Collections.emptyMap());
  }

  private void executeRequest(String name, String language, Map<String, String> xmls) {
    TestRequest request = wsTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("name", name)
      .setParam("language", language);
    for (Map.Entry<String, String> entry : xmls.entrySet()) {
      request.setParam("backup_" + entry.getKey(), entry.getValue());
    }
    request.execute();
  }

  private void setUserAsQualityProfileAdmin() {
    userSession.login("admin").setGlobalPermissions(QUALITY_PROFILE_ADMIN);
  }

  private ProfileImporter[] createImporters() {
    class DefaultProfileImporter extends ProfileImporter {
      private DefaultProfileImporter(String key, String name, String... languages) {
        super(key, name);
        setSupportedLanguages(languages);
      }

      @Override
      public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
        RulesProfile rulesProfile = RulesProfile.create();
        rulesProfile.activateRule(org.sonar.api.rules.Rule.create(RULE.getRepositoryKey(), RULE.getRuleKey()), RulePriority.BLOCKER);
        return rulesProfile;
      }

    }
    return new ProfileImporter[] {
      new DefaultProfileImporter("xoo_lint", "Xoo Lint", XOO_LANGUAGE)
    };
  }
}
