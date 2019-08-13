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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TypeValidations;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.server.qualityprofile.ActiveRuleInheritance.INHERITED;

public class QProfileTreeImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system2 = new AlwaysIncreasingSystem2();
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(db.getDbClient(), es.client());
  private TypeValidations typeValidations = new TypeValidations(asList(new StringTypeValidation(), new IntegerTypeValidation()));
  private RuleActivator ruleActivator = new RuleActivator(system2, db.getDbClient(), typeValidations, userSession);
  private QProfileRules qProfileRules = new QProfileRulesImpl(db.getDbClient(), ruleActivator, null, activeRuleIndexer);
  private QProfileTree underTest = new QProfileTreeImpl(db.getDbClient(), ruleActivator, System2.INSTANCE, activeRuleIndexer);

  @Test
  public void set_itself_as_parent_fails() {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(" can not be selected as parent of ");

    underTest.setParentAndCommit(db.getSession(), profile, profile);
  }

  @Test
  public void set_child_as_parent_fails() {
    RuleDefinitionDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(" can not be selected as parent of ");
    underTest.setParentAndCommit(db.getSession(), parentProfile, childProfile);
  }

  @Test
  public void set_grandchild_as_parent_fails() {
    RuleDefinitionDto rule = createRule();
    QProfileDto parentProfile = createProfile(rule);
    QProfileDto childProfile = createChildProfile(parentProfile);
    QProfileDto grandchildProfile = createChildProfile(childProfile);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(" can not be selected as parent of ");
    underTest.setParentAndCommit(db.getSession(), parentProfile, grandchildProfile);
  }

  @Test
  public void cannot_set_parent_if_language_is_different() {
    RuleDefinitionDto rule1 = db.rules().insert(r -> r.setLanguage("foo"));
    RuleDefinitionDto rule2 = db.rules().insert(r -> r.setLanguage("bar"));

    QProfileDto parentProfile = createProfile(rule1);
    List<ActiveRuleChange> changes = activate(parentProfile, RuleActivation.create(rule1.getId()));
    assertThat(changes).hasSize(1);

    QProfileDto childProfile = createProfile(rule2);
    changes = activate(childProfile, RuleActivation.create(rule2.getId()));
    assertThat(changes).hasSize(1);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Cannot set the profile");

    underTest.setParentAndCommit(db.getSession(), childProfile, parentProfile);
  }

  @Test
  public void set_then_unset_parent() {
    RuleDefinitionDto rule1 = createJavaRule();
    RuleDefinitionDto rule2 = createJavaRule();

    QProfileDto profile1 = createProfile(rule1);
    List<ActiveRuleChange> changes = activate(profile1, RuleActivation.create(rule1.getId()));
    assertThat(changes).hasSize(1);

    QProfileDto profile2 = createProfile(rule2);
    changes = activate(profile2, RuleActivation.create(rule2.getId()));
    assertThat(changes).hasSize(1);

    changes = underTest.setParentAndCommit(db.getSession(), profile2, profile1);
    assertThat(changes).hasSize(1);
    assertThatRuleIsActivated(profile2, rule1, changes, rule1.getSeverityString(), INHERITED, emptyMap());
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());

    changes = underTest.removeParentAndCommit(db.getSession(), profile2);
    assertThat(changes).hasSize(1);
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());
    assertThatRuleIsNotPresent(profile2, rule1);
  }

  @Test
  public void set_then_unset_parent_keep_overridden_rules() {
    RuleDefinitionDto rule1 = createJavaRule();
    RuleDefinitionDto rule2 = createJavaRule();
    QProfileDto profile1 = createProfile(rule1);
    List<ActiveRuleChange> changes = activate(profile1, RuleActivation.create(rule1.getId()));
    assertThat(changes).hasSize(1);

    QProfileDto profile2 = createProfile(rule2);
    changes = activate(profile2, RuleActivation.create(rule2.getId()));
    assertThat(changes).hasSize(1);

    changes = underTest.setParentAndCommit(db.getSession(), profile2, profile1);
    assertThat(changes).hasSize(1);
    assertThatRuleIsActivated(profile2, rule1, changes, rule1.getSeverityString(), INHERITED, emptyMap());
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());

    RuleActivation activation = RuleActivation.create(rule1.getId(), BLOCKER, null);
    changes = activate(profile2, activation);
    assertThat(changes).hasSize(1);
    assertThatRuleIsUpdated(profile2, rule1, BLOCKER, ActiveRuleInheritance.OVERRIDES, emptyMap());
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());

    changes = underTest.removeParentAndCommit(db.getSession(), profile2);
    assertThat(changes).hasSize(1);
    // Not testing changes here since severity is not set in changelog
    assertThatRuleIsActivated(profile2, rule1, null, BLOCKER, null, emptyMap());
    assertThatRuleIsActivated(profile2, rule2, null, rule2.getSeverityString(), null, emptyMap());
  }

  @Test
  public void activation_errors_are_ignored_when_setting_a_parent() {
    RuleDefinitionDto rule1 = createJavaRule();
    RuleDefinitionDto rule2 = createJavaRule();
    QProfileDto parentProfile = createProfile(rule1);
    activate(parentProfile, RuleActivation.create(rule1.getId()));
    activate(parentProfile, RuleActivation.create(rule2.getId()));

    rule1.setStatus(RuleStatus.REMOVED);
    db.rules().update(rule1);

    QProfileDto childProfile = createProfile(rule1);
    List<ActiveRuleChange> changes = underTest.setParentAndCommit(db.getSession(), childProfile, parentProfile);

    assertThatRuleIsNotPresent(childProfile, rule1);
    assertThatRuleIsActivated(childProfile, rule2, changes, rule2.getSeverityString(), INHERITED, emptyMap());
  }

  private List<ActiveRuleChange> activate(QProfileDto profile, RuleActivation activation) {
    return qProfileRules.activateAndCommit(db.getSession(), profile, singleton(activation));
  }

  private QProfileDto createProfile(RuleDefinitionDto rule) {
    return db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(rule.getLanguage()));
  }

  private QProfileDto createChildProfile(QProfileDto parent) {
    return db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p
      .setLanguage(parent.getLanguage())
      .setParentKee(parent.getKee())
      .setName("Child of " + parent.getName()));
  }

  private void assertThatRuleIsActivated(QProfileDto profile, RuleDefinitionDto rule, @Nullable List<ActiveRuleChange> changes,
                                         String expectedSeverity, @Nullable ActiveRuleInheritance expectedInheritance, Map<String, String> expectedParams) {
    OrgActiveRuleDto activeRule = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)
      .stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst()
      .orElseThrow(IllegalStateException::new);

    assertThat(activeRule.getSeverityString()).isEqualTo(expectedSeverity);
    assertThat(activeRule.getInheritance()).isEqualTo(expectedInheritance != null ? expectedInheritance.name() : null);
    assertThat(activeRule.getCreatedAt()).isNotNull();
    assertThat(activeRule.getUpdatedAt()).isNotNull();

    List<ActiveRuleParamDto> params = db.getDbClient().activeRuleDao().selectParamsByActiveRuleId(db.getSession(), activeRule.getId());
    assertThat(params).hasSize(expectedParams.size());

    if (changes != null) {
      ActiveRuleChange change = changes.stream()
        .filter(c -> c.getActiveRule().getId().equals(activeRule.getId()))
        .findFirst().orElseThrow(IllegalStateException::new);
      assertThat(change.getInheritance()).isEqualTo(expectedInheritance);
      assertThat(change.getSeverity()).isEqualTo(expectedSeverity);
      assertThat(change.getType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED);
    }
  }

  private void assertThatRuleIsNotPresent(QProfileDto profile, RuleDefinitionDto rule) {
    Optional<OrgActiveRuleDto> activeRule = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)
      .stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst();

    assertThat(activeRule).isEmpty();
  }

  private void assertThatRuleIsUpdated(QProfileDto profile, RuleDefinitionDto rule,
                                       String expectedSeverity, @Nullable ActiveRuleInheritance expectedInheritance, Map<String, String> expectedParams) {
    OrgActiveRuleDto activeRule = db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile)
      .stream()
      .filter(ar -> ar.getRuleKey().equals(rule.getKey()))
      .findFirst()
      .orElseThrow(IllegalStateException::new);

    assertThat(activeRule.getSeverityString()).isEqualTo(expectedSeverity);
    assertThat(activeRule.getInheritance()).isEqualTo(expectedInheritance != null ? expectedInheritance.name() : null);
    assertThat(activeRule.getCreatedAt()).isNotNull();
    assertThat(activeRule.getUpdatedAt()).isNotNull();

    List<ActiveRuleParamDto> params = db.getDbClient().activeRuleDao().selectParamsByActiveRuleId(db.getSession(), activeRule.getId());
    assertThat(params).hasSize(expectedParams.size());
  }

  private RuleDefinitionDto createRule() {
    return db.rules().insert(r -> r.setSeverity(Severity.MAJOR));
  }

  private RuleDefinitionDto createJavaRule() {
    return db.rules().insert(r -> r.setSeverity(Severity.MAJOR).setLanguage("java"));
  }
}
