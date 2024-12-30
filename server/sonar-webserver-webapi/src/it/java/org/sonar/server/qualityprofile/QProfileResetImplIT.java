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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TypeValidations;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;

public class QProfileResetImplIT {

  private static final String LANGUAGE = "xoo";

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private System2 system2 = new AlwaysIncreasingSystem2();
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private QualityProfileChangeEventService qualityProfileChangeEventService = mock(QualityProfileChangeEventService.class);
  private TypeValidations typeValidations = new TypeValidations(asList(new StringTypeValidation(), new IntegerTypeValidation()));
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));
  private RuleActivator ruleActivator = new RuleActivator(system2, db.getDbClient(), UuidFactoryImpl.INSTANCE, typeValidations, userSession, mock(Configuration.class),
    sonarQubeVersion);
  private QProfileTree qProfileTree = new QProfileTreeImpl(db.getDbClient(), ruleActivator, system2, activeRuleIndexer, mock(QualityProfileChangeEventService.class));
  private QProfileRules qProfileRules = new QProfileRulesImpl(db.getDbClient(), ruleActivator, null, activeRuleIndexer, qualityProfileChangeEventService);
  private QProfileResetImpl underTest = new QProfileResetImpl(db.getDbClient(), ruleActivator, activeRuleIndexer, mock(QualityProfileChangeEventService.class));

  @Test
  public void reset() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE));
    RuleDto existingRule = db.rules().insert(r -> r.setLanguage(LANGUAGE));
    qProfileRules.activateAndCommit(db.getSession(), profile, singleton(RuleActivation.create(existingRule.getUuid())));
    RuleDto newRule = db.rules().insert(r -> r.setLanguage(LANGUAGE));

    BulkChangeResult result = underTest.reset(db.getSession(), profile, singletonList(RuleActivation.create(newRule.getUuid())));

    assertThat(db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile))
      .extracting(OrgActiveRuleDto::getRuleKey)
      .containsExactlyInAnyOrder(newRule.getKey());
    // Only activated rules are returned in the result
    assertThat(result.getChanges())
      .extracting(ActiveRuleChange::getKey, ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(tuple(ActiveRuleKey.of(profile, newRule.getKey()), ACTIVATED));
    verify(qualityProfileChangeEventService).distributeRuleChangeEvent(any(), any(), eq(profile.getLanguage()));
  }

  @Test
  public void reset_whenRuleInherited_canBeDisabled() {
    QProfileDto parentProfile = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE));
    QProfileDto childProfile = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE));
    qProfileTree.setParentAndCommit(db.getSession(), childProfile, parentProfile);
    RuleDto existingRule = db.rules().insert(r -> r.setLanguage(LANGUAGE));
    qProfileRules.activateAndCommit(db.getSession(), parentProfile, singleton(RuleActivation.create(existingRule.getUuid())));
    qProfileRules.activateAndCommit(db.getSession(), childProfile, singleton(RuleActivation.create(existingRule.getUuid())));
    RuleDto newRule = db.rules().insert(r -> r.setLanguage(LANGUAGE));

    underTest.reset(db.getSession(), childProfile, singletonList(RuleActivation.create(newRule.getUuid())));

    assertThat(db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), childProfile))
      .extracting(OrgActiveRuleDto::getRuleKey)
      .containsExactlyInAnyOrder(newRule.getKey());
    verify(qualityProfileChangeEventService, times(2)).distributeRuleChangeEvent(any(), any(), eq(childProfile.getLanguage()));
  }

  @Test
  public void fail_when_profile_is_built_in() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(LANGUAGE).setIsBuiltIn(true));
    RuleDto defaultRule = db.rules().insert(r -> r.setLanguage(LANGUAGE));

    assertThatThrownBy(() -> {
      underTest.reset(db.getSession(), profile, singletonList(RuleActivation.create(defaultRule.getUuid())));
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Operation forbidden for built-in Quality Profile '%s'", profile.getKee()));
    verifyNoInteractions(qualityProfileChangeEventService);
  }

  @Test
  public void fail_when_profile_is_not_persisted() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto().setRulesProfileUuid(null).setLanguage(LANGUAGE);
    RuleDto defaultRule = db.rules().insert(r -> r.setLanguage(LANGUAGE));

    assertThatThrownBy(() -> {
      underTest.reset(db.getSession(), profile, singletonList(RuleActivation.create(defaultRule.getUuid())));
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Quality profile must be persisted");

    verifyNoInteractions(qualityProfileChangeEventService);
  }
}
