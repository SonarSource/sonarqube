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
package org.sonar.telemetry.legacy;

import javax.annotation.Nullable;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.qualityprofile.QProfileComparison;

import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.db.qualityprofile.ActiveRuleDto.OVERRIDES;

public class QualityProfileDataProviderIT {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();

  QualityProfileDataProvider underTest = new QualityProfileDataProvider(dbClient, new QProfileComparison(dbClient));

  @Test
  public void retrieveQualityProfilesData_whenDefaultRootProfile_shouldReturnRelevantInformation() {
    QProfileDto qProfile1 = createQualityProfile(false, null);
    dbTester.qualityProfiles().setAsDefault(qProfile1);
    Assertions.assertThat(underTest.retrieveQualityProfilesData())
      .extracting(p -> p.uuid(), p -> p.isDefault(), p -> p.isBuiltIn(), p -> p.builtInParent(),
        p -> p.rulesActivatedCount(), p -> p.rulesDeactivatedCount(), p -> p.rulesOverriddenCount())
      .containsExactlyInAnyOrder(tuple(qProfile1.getKee(), true, false, false, null, null, null));
  }

  @Test
  public void retrieveQualityProfilesData_whenDefaultChildProfile_shouldReturnRelevantInformation() {
    QProfileDto rootProfile = createQualityProfile(false, null);

    QProfileDto childProfile = createQualityProfile(false, rootProfile.getKee());

    dbTester.qualityProfiles().setAsDefault(childProfile);
    Assertions.assertThat(underTest.retrieveQualityProfilesData())
      .extracting(p -> p.uuid(), p -> p.isDefault(), p -> p.isBuiltIn(), p -> p.builtInParent(),
        p -> p.rulesActivatedCount(), p -> p.rulesDeactivatedCount(), p -> p.rulesOverriddenCount())
      .containsExactlyInAnyOrder(
        tuple(rootProfile.getKee(), false, false, false, null, null, null),
        tuple(childProfile.getKee(), true, false, false, null, null, null));
  }

  @Test
  public void retrieveQualityProfilesData_whenProfileAssignedToProject_shouldReturnProfile() {
    ProjectData projectData = dbTester.components().insertPublicProject();

    QProfileDto associatedProfile = createQualityProfile(false, null);

    QProfileDto unassociatedProfile = createQualityProfile(false, null);

    dbTester.qualityProfiles().associateWithProject(projectData.getProjectDto(), associatedProfile);

    Assertions.assertThat(underTest.retrieveQualityProfilesData())
      .extracting(p -> p.uuid(), p -> p.isDefault())
      .containsExactlyInAnyOrder(
        tuple(associatedProfile.getKee(), false),
        tuple(unassociatedProfile.getKee(), false)
      );
  }

  @Test
  public void retrieveQualityProfilesData_whenBuiltInParent_shouldReturnBuiltInParent() {

    QProfileDto rootBuiltinProfile = createQualityProfile(true, null);

    QProfileDto childProfile = createQualityProfile(false, rootBuiltinProfile.getKee());

    QProfileDto grandChildProfile = createQualityProfile(false, childProfile.getKee());

    dbTester.qualityProfiles().setAsDefault(rootBuiltinProfile, childProfile, grandChildProfile);

    Assertions.assertThat(underTest.retrieveQualityProfilesData())
      .extracting(p -> p.uuid(), p -> p.isBuiltIn(), p -> p.builtInParent())
      .containsExactlyInAnyOrder(tuple(rootBuiltinProfile.getKee(), true, null),
        tuple(childProfile.getKee(), false, true),
        tuple(grandChildProfile.getKee(), false, true)
      );
  }

  @Test
  public void retrieveQualityProfilesData_whenBuiltInParent_shouldReturnActiveAndUnactiveRules() {

    QProfileDto rootBuiltinProfile = createQualityProfile(true, null);

    QProfileDto childProfile = createQualityProfile(false, rootBuiltinProfile.getKee());
    RuleDto activatedRule = dbTester.rules().insert();
    RuleDto deactivatedRule = dbTester.rules().insert();

    dbTester.qualityProfiles().activateRule(rootBuiltinProfile, deactivatedRule);
    dbTester.qualityProfiles().activateRule(childProfile, activatedRule);
    dbTester.qualityProfiles().setAsDefault(childProfile);

    Assertions.assertThat(underTest.retrieveQualityProfilesData())
      .extracting(p -> p.uuid(), p -> p.rulesActivatedCount(), p -> p.rulesDeactivatedCount(), p -> p.rulesOverriddenCount())
      .containsExactlyInAnyOrder(
        tuple(rootBuiltinProfile.getKee(), null, null, null),
        tuple(childProfile.getKee(), 1, 1, 0)
      );
  }

  @Test
  public void retrieveQualityProfilesData_whenBuiltInParent_shouldReturnOverriddenRules() {

    QProfileDto rootBuiltinProfile = createQualityProfile(true, null);

    QProfileDto childProfile = createQualityProfile(false, rootBuiltinProfile.getKee());
    RuleDto rule = dbTester.rules().insert();
    RuleParamDto initialRuleParam = dbTester.rules().insertRuleParam(rule, p -> p.setName("key").setDefaultValue("initial"));


    ActiveRuleDto activeRuleDto = dbTester.qualityProfiles().activateRule(rootBuiltinProfile, rule);
    dbTester.getDbClient().activeRuleDao().insertParam(dbTester.getSession(), activeRuleDto, newParam(activeRuleDto, initialRuleParam, "key", "value"));

    ActiveRuleDto childActivateRule = dbTester.qualityProfiles().activateRule(childProfile, rule, ar -> {
      ar.setInheritance(OVERRIDES);
    });
    dbTester.getDbClient().activeRuleDao().insertParam(dbTester.getSession(), activeRuleDto, newParam(childActivateRule, initialRuleParam, "key", "override"));

    dbTester.qualityProfiles().setAsDefault(childProfile);

    Assertions.assertThat(underTest.retrieveQualityProfilesData())
      .extracting(p -> p.uuid(), p -> p.rulesActivatedCount(), p -> p.rulesDeactivatedCount(), p -> p.rulesOverriddenCount())
      .containsExactlyInAnyOrder(
        tuple(rootBuiltinProfile.getKee(), null, null, null),
        tuple(childProfile.getKee(), 0, 0, 1));
  }

  private static ActiveRuleParamDto newParam(ActiveRuleDto activeRuleDto, RuleParamDto initial, String key, String value) {
    return new ActiveRuleParamDto().setActiveRuleUuid(activeRuleDto.getRuleUuid()).setRulesParameterUuid(initial.getUuid()).setKey(key).setValue(value);
  }

  private QProfileDto createQualityProfile(boolean isBuiltIn, @Nullable String parentKee) {
    return dbTester.qualityProfiles().insert(p -> {
      p.setIsBuiltIn(isBuiltIn);
      p.setParentKee(parentKee);
    });
  }
}
