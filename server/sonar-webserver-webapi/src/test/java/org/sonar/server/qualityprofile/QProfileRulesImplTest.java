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

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.TypeValidations;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class QProfileRulesImplTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();

  private RuleIndex ruleIndex = new RuleIndex(es.client(), System2.INSTANCE);
  private ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(db.getDbClient(), es.client());
  private RuleActivator ruleActivator = new RuleActivator(System2.INSTANCE, db.getDbClient(), new TypeValidations(singletonList(new IntegerTypeValidation())), userSession);

  private QProfileRules qProfileRules = new QProfileRulesImpl(db.getDbClient(), ruleActivator, ruleIndex, activeRuleIndexer);

  @Test
  public void activate_one_rule() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto qProfile = db.qualityProfiles().insert(organization);
    RuleDefinitionDto rule = db.rules().insert(r -> r.setLanguage(qProfile.getLanguage()));
    RuleActivation ruleActivation = RuleActivation.create(rule.getId(), Severity.CRITICAL, Collections.emptyMap());

    qProfileRules.activateAndCommit(db.getSession(), qProfile, singleton(ruleActivation));

    assertThat(db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), qProfile))
      .extracting(ActiveRuleDto::getRuleKey, ActiveRuleDto::getSeverityString)
      .containsExactlyInAnyOrder(tuple(rule.getKey(), Severity.CRITICAL));
  }

  @Test
  public void active_rule_change() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    OrganizationDto organization = db.organizations().insert();
    QProfileDto qProfile = db.qualityProfiles().insert(organization);
    RuleDefinitionDto rule = db.rules().insert(r -> r.setLanguage(qProfile.getLanguage()));
    RuleActivation ruleActivation = RuleActivation.create(rule.getId(), Severity.CRITICAL, Collections.emptyMap());

    qProfileRules.activateAndCommit(db.getSession(), qProfile, singleton(ruleActivation));

    assertThat(db.getDbClient().qProfileChangeDao().selectByQuery(db.getSession(), new QProfileChangeQuery(qProfile.getKee())))
      .extracting(QProfileChangeDto::getUserUuid, QProfileChangeDto::getDataAsMap)
      .containsExactlyInAnyOrder(tuple(user.getUuid(), ImmutableMap.of("ruleId", Integer.toString(rule.getId()), "severity", Severity.CRITICAL)));
  }
}
