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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
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
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.qualityprofile.QProfileRulesImpl;
import org.sonar.server.qualityprofile.QProfileTree;
import org.sonar.server.qualityprofile.QProfileTreeImpl;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualityprofiles.InheritanceWsResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class InheritanceActionIT {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private EsClient esClient = es.client();
  private RuleIndexer ruleIndexer = new RuleIndexer(esClient, dbClient);
  private ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(dbClient, esClient);

  private RuleIndex ruleIndex = new RuleIndex(esClient, System2.INSTANCE);
  private QualityProfileChangeEventService qualityProfileChangeEventService = mock(QualityProfileChangeEventService.class);
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));
  private RuleActivator ruleActivator = new RuleActivator(System2.INSTANCE, dbClient, UuidFactoryImpl.INSTANCE, new TypeValidations(new ArrayList<>()), userSession,
    mock(Configuration.class), sonarQubeVersion);
  private QProfileRules qProfileRules = new QProfileRulesImpl(dbClient, ruleActivator, ruleIndex, activeRuleIndexer, qualityProfileChangeEventService);
  private QProfileTree qProfileTree = new QProfileTreeImpl(dbClient, ruleActivator, System2.INSTANCE, activeRuleIndexer, mock(QualityProfileChangeEventService.class));

  private WsActionTester ws = new WsActionTester(new InheritanceAction(
    dbClient,
    new QProfileWsSupport(dbClient, userSession),
    new Languages()));

  @Test
  public void inheritance_nominal() {
    RuleDto rule1 = createRule("xoo", "rule1");
    RuleDto rule2 = createRule("xoo", "rule2");
    RuleDto rule3 = createRule("xoo", "rule3");

    /*
     * sonar way (2) <- companyWide (2) <- buWide (2, 1 overriding) <- (forProject1 (2), forProject2 (2))
     */
    QProfileDto sonarway = db.qualityProfiles().insert(p -> p.setKee("xoo-sonar-way").setLanguage("xoo").setName("Sonar way").setIsBuiltIn(true));
    createActiveRule(rule1, sonarway);
    createActiveRule(rule2, sonarway);

    dbSession.commit();
    activeRuleIndexer.indexAll();

    QProfileDto companyWide = createProfile("xoo", "My Company Profile", "xoo-my-company-profile-12345");
    setParent(sonarway, companyWide);

    QProfileDto buWide = createProfile("xoo", "My BU Profile", "xoo-my-bu-profile-23456");
    setParent(companyWide, buWide);
    overrideActiveRuleSeverity(rule1, buWide, Severity.CRITICAL);

    QProfileDto forProject1 = createProfile("xoo", "For Project One", "xoo-for-project-one-34567");
    setParent(buWide, forProject1);
    createActiveRule(rule3, forProject1);
    dbSession.commit();
    activeRuleIndexer.indexAll();

    QProfileDto forProject2 = createProfile("xoo", "For Project Two", "xoo-for-project-two-45678");
    setParent(buWide, forProject2);
    overrideActiveRuleSeverity(rule2, forProject2, Severity.CRITICAL);

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, buWide.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, buWide.getName())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("InheritanceActionIT/inheritance-buWide.json"));
  }

  @Test
  public void inheritance_parent_child() throws Exception {
    String language = "java";
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage(language));
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage(language));
    RuleDto rule3 = db.rules().insert(r -> r.setLanguage(language));
    ruleIndexer.commitAndIndex(db.getSession(), asList(rule1.getUuid(), rule2.getUuid(), rule3.getUuid()));

    QProfileDto parent = db.qualityProfiles().insert(p -> p.setLanguage(language));
    db.qualityProfiles().activateRule(parent, rule1);
    db.qualityProfiles().activateRule(parent, rule2);

    QProfileDto child = db.qualityProfiles().insert(q -> q.setParentKee(parent.getKee()).setLanguage(language));
    db.qualityProfiles().activateRule(child, rule3);

    activeRuleIndexer.indexAll();

    InputStream response = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .execute()
      .getInputStream();

    InheritanceWsResponse result = InheritanceWsResponse.parseFrom(response);

    assertThat(result.getProfile().getKey()).isEqualTo(child.getKee());
    assertThat(result.getProfile().getActiveRuleCount()).isEqualTo(1);
    assertThat(result.getProfile().getInactiveRuleCount()).isEqualTo(2);

    assertThat(result.getAncestorsList()).extracting(InheritanceWsResponse.QualityProfile::getKey).containsExactly(parent.getKee());
    assertThat(result.getAncestorsList()).extracting(InheritanceWsResponse.QualityProfile::getActiveRuleCount).containsExactly(2L);
  }

  @Test
  public void handle_whenNoRulesActivated_shouldReturnExpectedInactivateRulesForLanguage() throws IOException {
    String language = "java";
    QProfileDto qualityProfile = db.qualityProfiles().insert(p -> p.setLanguage(language));
    RuleDto rule = db.rules().insert(r -> r.setLanguage(language));

    InputStream response = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .execute()
      .getInputStream();
    InheritanceWsResponse result = InheritanceWsResponse.parseFrom(response);

    assertThat(result.getProfile().getKey()).isEqualTo(qualityProfile.getKee());
    assertThat(result.getProfile().getActiveRuleCount()).isZero();
    assertThat(result.getProfile().getInactiveRuleCount()).isEqualTo(1);

  }

  @Test
  public void inheritance_ignores_removed_rules() throws Exception {
    RuleDto rule = db.rules().insert(r -> r.setStatus(RuleStatus.REMOVED));
    ruleIndexer.commitAndIndex(db.getSession(), rule.getUuid());

    QProfileDto profile = db.qualityProfiles().insert();
    db.qualityProfiles().activateRule(profile, rule);
    long activeRules = 0;

    activeRuleIndexer.indexAll();

    InputStream response = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .execute()
      .getInputStream();

    InheritanceWsResponse result = InheritanceWsResponse.parseFrom(response);
    assertThat(result.getProfile().getKey()).isEqualTo(profile.getKee());
    assertThat(result.getProfile().getActiveRuleCount()).isEqualTo(activeRules);
  }

  @Test
  public void inheritance_no_family() {
    // Simple profile, no parent, no child
    QProfileDto remi = createProfile("xoo", "Nobodys Boy", "xoo-nobody-s-boy-01234");

    String response = ws.newRequest()
      .setParam(PARAM_LANGUAGE, remi.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, remi.getName())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("InheritanceActionIT/inheritance-simple.json"));
  }

  @Test
  public void fail_if_not_found() {
    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_LANGUAGE, "xoo")
        .setParam(PARAM_QUALITY_PROFILE, "asd")
        .execute();
    })
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("inheritance");
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder("language", "qualityProfile");
  }

  private QProfileDto createProfile(String lang, String name, String key) {
    return db.qualityProfiles().insert(qp -> qp.setKee(key).setName(name).setLanguage(lang));
  }

  private void setParent(QProfileDto profile, QProfileDto parent) {
    qProfileTree.setParentAndCommit(dbSession, parent, profile);
  }

  private RuleDto createRule(String lang, String id) {
    long now = new Date().getTime();
    RuleDto rule = RuleTesting.newRule(RuleKey.of("blah", id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.READY)
      .setUpdatedAt(now)
      .setCreatedAt(now);
    dbClient.ruleDao().insert(dbSession, rule);
    ruleIndexer.commitAndIndex(dbSession, rule.getUuid());
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDto rule, QProfileDto profile) {
    long now = new Date().getTime();
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString())
      .setUpdatedAt(now)
      .setCreatedAt(now);
    dbClient.activeRuleDao().insert(dbSession, activeRule);
    return activeRule;
  }

  private void overrideActiveRuleSeverity(RuleDto rule, QProfileDto profile, String severity) {
    qProfileRules.activateAndCommit(dbSession, profile, singleton(RuleActivation.create(rule.getUuid(), severity, null)));
  }
}
