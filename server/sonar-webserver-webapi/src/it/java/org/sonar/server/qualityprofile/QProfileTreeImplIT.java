/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TypeValidations;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.server.qualityprofile.ActiveRuleInheritance.INHERITED;

public class QProfileTreeImplIT {

  private System2 system2 = new AlwaysIncreasingSystem2();
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(db.getDbClient(), es.client());
  private TypeValidations typeValidations = new TypeValidations(asList(new StringTypeValidation(), new IntegerTypeValidation()));
  private QualityProfileChangeEventService qualityProfileChangeEventService = mock(QualityProfileChangeEventService.class);
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));
  private RuleActivator ruleActivator = new RuleActivator(system2, db.getDbClient(), UuidFactoryImpl.INSTANCE, typeValidations, userSession, mock(Configuration.class),
    sonarQubeVersion);
  private QProfileRules qProfileRules = new QProfileRulesImpl(db.getDbClient(), ruleActivator, null, activeRuleIndexer, qualityProfileChangeEventService);
  private QProfileTree underTest = new QProfileTreeImpl(db.getDbClient(), ruleActivator, System2.INSTANCE, activeRuleIndexer, mock(QualityProfileChangeEventService.class));

  @Test
  public void set_itself_as_parent_fails() {
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule);

    assertThatThrownBy(() -> underTest.setParentAndCommit(db.getSession(), profile, profile))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(" can not be selected as parent of ");
  }

  @Test
  public void set_child_as_parent_fails() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    assertThatThrownBy(() -> underTest.setParentAndCommit(db.getSession(), parentProfile, childProfile))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(" can not be selected as parent of ");
  }

  @Test
  public void set_grandchild_as_parent_fails() {
    RuleDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandchildProfile = createChildProfile(childProfile);

    assertThatThrownBy(() -> underTest.setParentAndCommit(db.getSession(), parentProfile, grandchildProfile))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(" can not be selected as parent of ");
  }

  @Test
  public void cannot_set_parent_if_language_is_different() {
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("foo"));
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage("bar"));

    QProfileDto parentProfile = createProfile(rule1);
    List<ActiveRuleChange> changes = activate(parentProfile, RuleActivation.create(rule1.getUuid()));
    assertThat(changes).hasSize(1);

    QProfileDto childProfile = createProfile(rule2);
    changes = activate(childProfile, RuleActivation.create(rule2.getUuid()));
    assertThat(changes).hasSize(1);

    assertThatThrownBy(() -> underTest.setParentAndCommit(db.getSession(), childProfile, parentProfile))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Cannot set the profile");
  }

  @Test
  public void set_then_unset_parent() {
    RuleDto rule1 = createJavaRule();
    RuleDto rule2 = createJavaRule();

    QProfileDto profile1 = createProfile(rule1);
    List<ActiveRuleChange> changes = activate(profile1, RuleActivation.create(rule1.getUuid()));
    assertThat(changes).hasSize(1);

    QProfileDto profile2 = createProfile(rule2);
    changes = activate(profile2, RuleActivation.create(rule2.getUuid()));
    assertThat(changes).hasSize(1);

    changes = underTest.setParentAndCommit(db.getSession(), profile2, profile1);
    assertThat(changes).hasSize(1);
    assertThatRuleIsActivated(profile2, rule1, changes, rule1.getSeverityString(), INHERITED, emptyMap());
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());
    verify(qualityProfileChangeEventService, times(2)).distributeRuleChangeEvent(any(), any(), eq(profile2.getLanguage()));

    changes = underTest.removeParentAndCommit(db.getSession(), profile2);
    assertThat(changes).hasSize(1);
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());
    assertThatRuleIsNotPresent(profile2, rule1);
    verify(qualityProfileChangeEventService, times(2)).distributeRuleChangeEvent(any(), any(), eq(profile2.getLanguage()));
  }

  @Test
  public void set_then_unset_parent_keep_overridden_rules() {
    RuleDto rule1 = createJavaRule();
    RuleDto rule2 = createJavaRule();
    QProfileDto profile1 = createProfile(rule1);
    List<ActiveRuleChange> changes = activate(profile1, RuleActivation.create(rule1.getUuid()));
    assertThat(changes).hasSize(1);

    QProfileDto profile2 = createProfile(rule2);
    changes = activate(profile2, RuleActivation.create(rule2.getUuid()));
    assertThat(changes).hasSize(1);

    changes = underTest.setParentAndCommit(db.getSession(), profile2, profile1);
    assertThat(changes).hasSize(1);
    assertThatRuleIsActivated(profile2, rule1, changes, rule1.getSeverityString(), INHERITED, emptyMap());
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());
    verify(qualityProfileChangeEventService, times(2)).distributeRuleChangeEvent(any(), any(), eq(profile2.getLanguage()));

    RuleActivation activation = RuleActivation.create(rule1.getUuid(), BLOCKER, null);
    changes = activate(profile2, activation);
    assertThat(changes).hasSize(1);
    assertThatRuleIsUpdated(profile2, rule1, BLOCKER, ActiveRuleInheritance.OVERRIDES, emptyMap());
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());

    changes = underTest.removeParentAndCommit(db.getSession(), profile2);
    assertThat(changes).hasSize(1);
    // Not testing changes here since severity is not set in changelog
    assertThatRuleIsActivated(profile2, rule1, null, BLOCKER, null, emptyMap());
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());
    verify(qualityProfileChangeEventService, times(3)).distributeRuleChangeEvent(anyList(), any(), eq(profile2.getLanguage()));
  }

  @Test
  public void change_parent_keep_overridden_rules() {
    RuleDto parentRule = createJavaRule();
    RuleDto childRule = createJavaRule();

    QProfileDto parentProfile1 = createProfile(parentRule);
    List<ActiveRuleChange> changes = activate(parentProfile1, RuleActivation.create(parentRule.getUuid()));
    assertThat(changes).hasSize(1);

    QProfileDto childProfile = createProfile(childRule);
    changes = activate(childProfile, RuleActivation.create(childRule.getUuid()));
    assertThat(changes).hasSize(1);

    changes = underTest.setParentAndCommit(db.getSession(), childProfile, parentProfile1);
    assertThat(changes).hasSize(1);
    assertThatRuleIsActivated(childProfile, parentRule, changes, parentRule.getSeverityString(), INHERITED, emptyMap());
    assertThatRuleIsActivated(childProfile, childRule, null, childRule.getSeverityString(), null, emptyMap());
    verify(qualityProfileChangeEventService, times(2)).distributeRuleChangeEvent(any(), any(), eq(childProfile.getLanguage()));

    RuleActivation activation = RuleActivation.create(parentRule.getUuid(), BLOCKER, null);
    changes = activate(childProfile, activation);
    assertThat(changes).hasSize(1);
    assertThatRuleIsUpdated(childProfile, parentRule, BLOCKER, ActiveRuleInheritance.OVERRIDES, emptyMap());
    assertThatRuleIsActivated(childProfile, childRule, null, childRule.getSeverityString(), null, emptyMap());

    QProfileDto parentProfile2 = createProfile(parentRule);
    changes = activate(parentProfile2, RuleActivation.create(parentRule.getUuid()));
    assertThat(changes).hasSize(1);

    changes = underTest.setParentAndCommit(db.getSession(), childProfile, parentProfile2);
    assertThat(changes).isEmpty();
    assertThatRuleIsUpdated(childProfile, parentRule, BLOCKER, ActiveRuleInheritance.OVERRIDES, emptyMap());
    assertThatRuleIsActivated(childProfile, childRule, null, childRule.getSeverityString(), null, emptyMap());
    verify(qualityProfileChangeEventService, times(4)).distributeRuleChangeEvent(any(), any(), eq(childProfile.getLanguage()));
  }

  @Test
  public void activation_errors_are_ignored_when_setting_a_parent() {
    RuleDto rule1 = createJavaRule();
    RuleDto rule2 = createJavaRule();
    QProfileDto parentProfile = createProfile(rule1);
    activate(parentProfile, RuleActivation.create(rule1.getUuid()));
    activate(parentProfile, RuleActivation.create(rule2.getUuid()));

    rule1.setStatus(RuleStatus.REMOVED);
    db.rules().update(rule1);

    QProfileDto childProfile = createProfile(rule1);
    List<ActiveRuleChange> changes = underTest.setParentAndCommit(db.getSession(), childProfile, parentProfile);
    verify(qualityProfileChangeEventService, times(2)).distributeRuleChangeEvent(any(), any(), eq(childProfile.getLanguage()));

    assertThatRuleIsNotPresent(childProfile, rule1);
    assertThatRuleIsActivated(childProfile, rule2, changes, rule2.getSeverityString(), INHERITED, emptyMap());
  }

  private List<ActiveRuleChange> activate(QProfileDto profile, RuleActivation activation) {
    return qProfileRules.activateAndCommit(db.getSession(), profile, singleton(activation));
  }

  private QProfileDto createProfile(RuleDto rule) {
    return db.qualityProfiles().insert(p -> p.setLanguage(rule.getLanguage()));
  }

  private QProfileDto createChildProfile(QProfileDto parent) {
    return db.qualityProfiles().insert(p -> p
      .setLanguage(parent.getLanguage())
      .setParentKee(parent.getKee())
      .setName("Child of " + parent.getName()));
  }

  private void assertThatRuleIsActivated(QProfileDto profile, RuleDto rule, @Nullable List<ActiveRuleChange> changes,
    String expectedSeverity, @Nullable ActiveRuleInheritance expectedInheritance, Map<String, String> expectedParams) {
    OrgActiveRuleDto activeRule = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)
      .stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst()
      .orElseThrow(IllegalStateException::new);

    assertThat(activeRule.getSeverityString()).isEqualTo(expectedSeverity);
    assertThat(activeRule.getInheritance()).isEqualTo(expectedInheritance != null ? expectedInheritance.name() : null);

    List<ActiveRuleParamDto> params = db.getDbClient().activeRuleDao().selectParamsByActiveRuleUuid(db.getSession(), activeRule.getUuid());
    assertThat(params).hasSize(expectedParams.size());

    if (changes != null) {
      ActiveRuleChange change = changes.stream()
        .filter(c -> c.getActiveRule().getUuid().equals(activeRule.getUuid()))
        .findFirst().orElseThrow(IllegalStateException::new);
      assertThat(change.getInheritance()).isEqualTo(expectedInheritance);
      assertThat(change.getSeverity()).isEqualTo(expectedSeverity);
      assertThat(change.getType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED);
    }
  }

  private void assertThatRuleIsNotPresent(QProfileDto profile, RuleDto rule) {
    Optional<OrgActiveRuleDto> activeRule = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)
      .stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst();

    assertThat(activeRule).isEmpty();
  }

  private void assertThatRuleIsUpdated(QProfileDto profile, RuleDto rule,
    String expectedSeverity, @Nullable ActiveRuleInheritance expectedInheritance, Map<String, String> expectedParams) {
    OrgActiveRuleDto activeRule = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)
      .stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst()
      .orElseThrow(IllegalStateException::new);

    assertThat(activeRule.getSeverityString()).isEqualTo(expectedSeverity);
    assertThat(activeRule.getInheritance()).isEqualTo(expectedInheritance != null ? expectedInheritance.name() : null);

    List<ActiveRuleParamDto> params = db.getDbClient().activeRuleDao().selectParamsByActiveRuleUuid(db.getSession(), activeRule.getUuid());
    assertThat(params).hasSize(expectedParams.size());
  }

  private RuleDto createRule() {
    return db.rules().insert(r -> r.setSeverity(Severity.MAJOR));
  }

  private RuleDto createJavaRule() {
    return db.rules().insert(r -> r.setSeverity(Severity.MAJOR).setLanguage("java"));
  }
}
