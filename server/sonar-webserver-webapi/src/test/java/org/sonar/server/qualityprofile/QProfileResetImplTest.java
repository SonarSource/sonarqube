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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TypeValidations;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;

public class QProfileResetImplTest {

  private static final String LANGUAGE = "xoo";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private System2 system2 = new AlwaysIncreasingSystem2();
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private TypeValidations typeValidations = new TypeValidations(asList(new StringTypeValidation(), new IntegerTypeValidation()));
  private RuleActivator ruleActivator = new RuleActivator(system2, db.getDbClient(), typeValidations, userSession);
  private QProfileTree qProfileTree = new QProfileTreeImpl(db.getDbClient(), ruleActivator, system2, activeRuleIndexer);
  private QProfileRules qProfileRules = new QProfileRulesImpl(db.getDbClient(), ruleActivator, null, activeRuleIndexer);
  private QProfileResetImpl underTest = new QProfileResetImpl(db.getDbClient(), ruleActivator, activeRuleIndexer);

  @Test
  public void reset() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE));
    RuleDefinitionDto existingRule = db.rules().insert(r -> r.setLanguage(LANGUAGE));
    qProfileRules.activateAndCommit(db.getSession(), profile, singleton(RuleActivation.create(existingRule.getId())));
    RuleDefinitionDto newRule = db.rules().insert(r -> r.setLanguage(LANGUAGE));

    BulkChangeResult result = underTest.reset(db.getSession(), profile, singletonList(RuleActivation.create(newRule.getId())));

    assertThat(db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), profile))
      .extracting(OrgActiveRuleDto::getRuleKey)
      .containsExactlyInAnyOrder(newRule.getKey());
    // Only activated rules are returned in the result
    assertThat(result.getChanges())
      .extracting(ActiveRuleChange::getKey, ActiveRuleChange::getType)
      .containsExactlyInAnyOrder(tuple(ActiveRuleKey.of(profile, newRule.getKey()), ACTIVATED));
  }

  @Test
  public void inherited_rules_are_not_disabled() {
    QProfileDto parentProfile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE));
    QProfileDto childProfile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE));
    qProfileTree.setParentAndCommit(db.getSession(), childProfile, parentProfile);
    RuleDefinitionDto existingRule = db.rules().insert(r -> r.setLanguage(LANGUAGE));
    qProfileRules.activateAndCommit(db.getSession(), parentProfile, singleton(RuleActivation.create(existingRule.getId())));
    qProfileRules.activateAndCommit(db.getSession(), childProfile, singleton(RuleActivation.create(existingRule.getId())));
    RuleDefinitionDto newRule = db.rules().insert(r -> r.setLanguage(LANGUAGE));

    underTest.reset(db.getSession(), childProfile, singletonList(RuleActivation.create(newRule.getId())));

    assertThat(db.getDbClient().activeRuleDao().selectByProfile(db.getSession(), childProfile))
      .extracting(OrgActiveRuleDto::getRuleKey)
      .containsExactlyInAnyOrder(newRule.getKey(), existingRule.getKey());
  }

  @Test
  public void fail_when_profile_is_built_in() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(LANGUAGE).setIsBuiltIn(true));
    RuleDefinitionDto defaultRule = db.rules().insert(r -> r.setLanguage(LANGUAGE));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(String.format("Operation forbidden for built-in Quality Profile '%s'", profile.getKee()));

    underTest.reset(db.getSession(), profile, singletonList(RuleActivation.create(defaultRule.getId())));
  }

  @Test
  public void fail_when_profile_is_not_persisted() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto().setLanguage(LANGUAGE);
    RuleDefinitionDto defaultRule = db.rules().insert(r -> r.setLanguage(LANGUAGE));

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Quality profile must be persisted");

    underTest.reset(db.getSession(), profile, singletonList(RuleActivation.create(defaultRule.getId())));
  }
}
