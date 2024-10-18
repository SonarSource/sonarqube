/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.QProfileTreeImpl;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARENT_QUALITY_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class ChangeParentActionIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient;
  private DbSession dbSession;
  private RuleIndex ruleIndex;
  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;
  private WsActionTester ws;
  private Language language = LanguageTesting.newLanguage(secure().nextAlphanumeric(20));
  private String ruleRepository = secure().nextAlphanumeric(5);
  private QProfileTreeImpl qProfileTree;
  private SonarQubeVersion sonarQubeVersion;

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    dbSession = db.getSession();
    EsClient esClient = es.client();
    ruleIndex = new RuleIndex(esClient, System2.INSTANCE);
    ruleIndexer = new RuleIndexer(esClient, dbClient);
    activeRuleIndexer = new ActiveRuleIndexer(dbClient, esClient);
    TypeValidations typeValidations = new TypeValidations(Collections.emptyList());
    sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));
    RuleActivator ruleActivator = new RuleActivator(System2.INSTANCE, dbClient, UuidFactoryImpl.INSTANCE, typeValidations, userSession, mock(Configuration.class),
      sonarQubeVersion);
    qProfileTree = new QProfileTreeImpl(dbClient, ruleActivator, System2.INSTANCE, activeRuleIndexer, mock(QualityProfileChangeEventService.class));
    ChangeParentAction underTest = new ChangeParentAction(
      dbClient,
      qProfileTree,
      new Languages(),
      new QProfileWsSupport(
        dbClient,
        userSession),
      userSession);

    ws = new WsActionTester(underTest);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder(
      "qualityProfile", "language", "parentQualityProfile");
  }

  @Test
  public void change_parent_with_no_parent_before() {
    QProfileDto parent1 = createProfile();
    QProfileDto child = createProfile();

    RuleDto rule1 = createRule();
    createActiveRule(rule1, parent1);
    ruleIndexer.commitAndIndex(dbSession, rule1.getUuid());
    activeRuleIndexer.indexAll();

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();

    // Set parent
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .setParam(PARAM_PARENT_QUALITY_PROFILE, parent1.getName())
      .execute();

    // Check rule 1 enabled
    List<OrgActiveRuleDto> activeRules1 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules1).hasSize(1);
    assertThat(activeRules1.get(0).getKey().getRuleKey().rule()).isEqualTo(rule1.getRuleKey());

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getUuids()).hasSize(1);
  }

  @Test
  public void replace_existing_parent() {
    QProfileDto parent1 = createProfile();
    QProfileDto parent2 = createProfile();
    QProfileDto child = createProfile();

    RuleDto rule1 = createRule();
    RuleDto rule2 = createRule();
    createActiveRule(rule1, parent1);
    createActiveRule(rule2, parent2);
    ruleIndexer.commitAndIndex(dbSession, asList(rule1.getUuid(), rule2.getUuid()));
    activeRuleIndexer.indexAll();

    // Set parent 1
    qProfileTree.setParentAndCommit(dbSession, child, parent1);

    // Set parent 2 through WS
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .setParam(PARAM_PARENT_QUALITY_PROFILE, parent2.getName())
      .execute();

    // Check rule 2 enabled
    List<OrgActiveRuleDto> activeRules2 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules2).hasSize(1);
    assertThat(activeRules2.get(0).getKey().getRuleKey().rule()).isEqualTo(rule2.getRuleKey());

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getUuids()).hasSize(1);
  }

  @Test
  public void remove_parent() {
    QProfileDto parent = createProfile();
    QProfileDto child = createProfile();

    RuleDto rule1 = createRule();
    createActiveRule(rule1, parent);
    ruleIndexer.commitAndIndex(dbSession, rule1.getUuid());
    activeRuleIndexer.indexAll();

    // Set parent
    qProfileTree.setParentAndCommit(dbSession, child, parent);

    // Remove parent through WS
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .execute();

    // Check no rule enabled
    assertThat(dbClient.activeRuleDao().selectByProfile(dbSession, child)).isEmpty();

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getUuids()).isEmpty();
  }

  @Test
  public void change_parent_with_names() {
    QProfileDto parent1 = createProfile();
    QProfileDto parent2 = createProfile();
    QProfileDto child = createProfile();

    RuleDto rule1 = createRule();
    RuleDto rule2 = createRule();
    createActiveRule(rule1, parent1);
    createActiveRule(rule2, parent2);
    ruleIndexer.commitAndIndex(dbSession, rule1.getUuid());
    activeRuleIndexer.indexAll();

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();

    // 1. Set parent 1
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .setParam(PARAM_PARENT_QUALITY_PROFILE, parent1.getName())
      .execute();

    // 1. check rule 1 enabled
    List<OrgActiveRuleDto> activeRules1 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules1).hasSize(1);
    assertThat(activeRules1.get(0).getKey().getRuleKey().rule()).isEqualTo(rule1.getRuleKey());
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getUuids()).hasSize(1);

    // 2. Set parent 2
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .setParam(QualityProfileWsParameters.PARAM_PARENT_QUALITY_PROFILE, parent2.getName())
      .execute();

    // 2. check rule 2 enabled
    List<OrgActiveRuleDto> activeRules2 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules2).hasSize(1);
    assertThat(activeRules2.get(0).getKey().getRuleKey().rule()).isEqualTo(rule2.getRuleKey());

    // 3. Remove parent
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .setParam(QualityProfileWsParameters.PARAM_PARENT_QUALITY_PROFILE, "")
      .execute();

    // 3. check no rule enabled
    List<OrgActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getUuids()).isEmpty();
  }

  @Test
  public void remove_parent_with_empty_key() {
    QProfileDto parent = createProfile();
    QProfileDto child = createProfile();

    RuleDto rule1 = createRule();
    createActiveRule(rule1, parent);
    ruleIndexer.commitAndIndex(dbSession, rule1.getUuid());
    activeRuleIndexer.indexAll();

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();

    // Set parent
    qProfileTree.setParentAndCommit(dbSession, child, parent);

    // Remove parent
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .setParam(PARAM_PARENT_QUALITY_PROFILE, "")
      .execute();

    // Check no rule enabled
    assertThat(dbClient.activeRuleDao().selectByProfile(dbSession, child)).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getUuids()).isEmpty();
  }

  @Test
  public void as_qprofile_editor() {
    QProfileDto parent1 = createProfile();
    QProfileDto parent2 = createProfile();
    QProfileDto child = createProfile();

    RuleDto rule1 = createRule();
    RuleDto rule2 = createRule();
    createActiveRule(rule1, parent1);
    createActiveRule(rule2, parent2);
    ruleIndexer.commitAndIndex(dbSession, asList(rule1.getUuid(), rule2.getUuid()));
    activeRuleIndexer.indexAll();
    // Set parent 1
    qProfileTree.setParentAndCommit(dbSession, child, parent1);
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(child, user);
    userSession.logIn(user);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .setParam(PARAM_PARENT_QUALITY_PROFILE, parent2.getName())
      .execute();

    List<OrgActiveRuleDto> activeRules2 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules2).hasSize(1);
    assertThat(activeRules2.get(0).getKey().getRuleKey().rule()).isEqualTo(rule2.getRuleKey());

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getUuids()).hasSize(1);
  }

  @Test
  public void fail_if_built_in_profile() {
    QProfileDto child = db.qualityProfiles().insert(p -> p
      .setLanguage(language.getKey())
      .setIsBuiltIn(true));

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getUuids()).isEmpty();

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .setParam(PARAM_PARENT_QUALITY_PROFILE, "palap");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_if_missing_permission() {
    userSession.logIn(db.users().insertUser());

    QProfileDto child = createProfile();

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName());

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  private QProfileDto createProfile() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setLanguage(language.getKey());
    dbClient.qualityProfileDao().insert(dbSession, profile);
    dbSession.commit();
    return profile;
  }

  private RuleDto createRule() {
    RuleDto rule = RuleTesting.newRule(RuleKey.of(ruleRepository, secure().nextAlphanumeric(5)))
      .setLanguage(language.getKey())
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.READY);
    dbClient.ruleDao().insert(dbSession, rule);
    dbSession.commit();
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDto rule, QProfileDto profile) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString());
    dbClient.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();
    return activeRule;
  }
}
