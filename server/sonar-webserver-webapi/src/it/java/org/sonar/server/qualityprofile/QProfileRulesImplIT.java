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
package org.sonar.server.qualityprofile;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.TypeValidations;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class QProfileRulesImplIT {

  @RegisterExtension
  private final UserSessionRule userSession = UserSessionRule.standalone();
  @RegisterExtension
  private final DbTester db = DbTester.create();
  @RegisterExtension
  private final EsTester es = EsTester.create();

  private final Configuration config = mock(Configuration.class);

  private final RuleIndex ruleIndex = new RuleIndex(es.client(), System2.INSTANCE, config);
  private final ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(db.getDbClient(), es.client());
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));
  private final RuleActivator ruleActivator = new RuleActivator(System2.INSTANCE, db.getDbClient(), UuidFactoryImpl.INSTANCE,
    new TypeValidations(singletonList(new IntegerTypeValidation())),
    userSession, mock(Configuration.class), sonarQubeVersion);
  private final QualityProfileChangeEventService qualityProfileChangeEventService = mock(QualityProfileChangeEventService.class);

  private final QProfileRules qProfileRules = new QProfileRulesImpl(db.getDbClient(), ruleActivator, ruleIndex, activeRuleIndexer,
    qualityProfileChangeEventService);

  @Test
  void activate_one_rule() {
    QProfileDto qProfile = db.qualityProfiles().insert();
    RuleDto rule = db.rules().insert(r -> r.setLanguage(qProfile.getLanguage()));
    RuleActivation ruleActivation = RuleActivation.create(rule.getUuid(), Severity.CRITICAL, true, Collections.emptyMap());

    qProfileRules.activateAndCommit(db.getSession(), qProfile, singleton(ruleActivation));

    assertThat(db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), qProfile))
      .extracting(ActiveRuleDto::getRuleKey, ActiveRuleDto::getSeverityString, ActiveRuleDto::isPrioritizedRule)
      .containsExactlyInAnyOrder(tuple(rule.getKey(), Severity.CRITICAL, true));
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(qProfile.getLanguage()));
  }

  @Test
  void active_rule_change() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    QProfileDto qProfile = db.qualityProfiles().insert();
    RuleDto rule = db.rules().insert(r -> r.setLanguage(qProfile.getLanguage()));
    RuleActivation ruleActivation = RuleActivation.create(rule.getUuid(), Severity.CRITICAL, Collections.emptyMap());

    qProfileRules.activateAndCommit(db.getSession(), qProfile, singleton(ruleActivation));

    assertThat(db.getDbClient().qProfileChangeDao().selectByQuery(db.getSession(), new QProfileChangeQuery(qProfile.getKee())))
      .extracting(QProfileChangeDto::getUserUuid, QProfileChangeDto::getDataAsMap)
      .containsExactlyInAnyOrder(tuple(user.getUuid(), ImmutableMap.of("ruleUuid", rule.getUuid(), "severity", Severity.CRITICAL)));
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(qProfile.getLanguage()));
  }
}
