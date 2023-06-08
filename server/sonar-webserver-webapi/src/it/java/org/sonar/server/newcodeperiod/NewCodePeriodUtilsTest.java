/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.newcodeperiod;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.NUMBER_OF_DAYS;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.PREVIOUS_VERSION;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.SPECIFIC_ANALYSIS;
import static org.sonar.server.newcodeperiod.NewCodePeriodUtils.getNewCodeDefinitionValue;
import static org.sonar.server.newcodeperiod.NewCodePeriodUtils.getNewCodeDefinitionValueProjectCreation;
import static org.sonar.server.newcodeperiod.NewCodePeriodUtils.validateType;

public class NewCodePeriodUtilsTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE, true);

  private static final String MAIN_BRANCH = "main";
  private ComponentDbTester componentDb = new ComponentDbTester(db);

  private DbSession dbSession = db.getSession();
  private DbClient dbClient = db.getDbClient();
  @Test
  public void validateType_throw_IAE_if_no_valid_type() {
    assertThatThrownBy(() -> validateType("nonValid", false, false))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid type: nonValid");
  }

  @Test
  public void validateType_throw_IAE_if_type_is_invalid_for_global() {
    assertThatThrownBy(() -> validateType("SPECIFIC_ANALYSIS", true, false))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid type 'SPECIFIC_ANALYSIS'. Overall setting can only be set with types: [PREVIOUS_VERSION, NUMBER_OF_DAYS]");
  }

  @Test
  public void validateType_throw_IAE_if_type_is_invalid_for_project() {
    assertThatThrownBy(() -> validateType("SPECIFIC_ANALYSIS", false, false))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid type 'SPECIFIC_ANALYSIS'. Projects can only be set with types: [PREVIOUS_VERSION, NUMBER_OF_DAYS, REFERENCE_BRANCH]");
  }

  @Test
  public void validateType_return_type_for_branch() {
    assertThat(validateType("REFERENCE_BRANCH", false, true)).isEqualTo(REFERENCE_BRANCH);
  }

  @Test
  public void validateType_return_type_for_project() {
    assertThat(validateType("REFERENCE_BRANCH", false, false)).isEqualTo(REFERENCE_BRANCH);
  }

  @Test
  public void validateType_return_type_for_overall() {
    assertThat(validateType("PREVIOUS_VERSION", true, false)).isEqualTo(PREVIOUS_VERSION);
  }

  @Test
  public void getNCDValue_throw_IAE_if_no_value_for_days() {
    assertThatThrownBy(() -> getNewCodeDefinitionValue(dbSession, dbClient, NUMBER_OF_DAYS, null, null, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("New code definition type 'NUMBER_OF_DAYS' requires a value");
  }

  @Test
  public void getNCDValue_throw_IAE_if_no_value_for_reference_branch() {
    assertThatThrownBy(() -> getNewCodeDefinitionValue(dbSession, dbClient, REFERENCE_BRANCH, null, null, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("New code definition type 'REFERENCE_BRANCH' requires a value");
  }

  @Test
  public void getNCDValue_throw_IAE_if_no_value_for_analysis() {
    assertThatThrownBy(() -> getNewCodeDefinitionValue(dbSession, dbClient, SPECIFIC_ANALYSIS, null, null, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("New code definition type 'SPECIFIC_ANALYSIS' requires a value");
  }

  @Test
  public void getNCDValue_throw_IAE_if_days_is_invalid() {
    assertThatThrownBy(() -> getNewCodeDefinitionValue(dbSession, dbClient, NUMBER_OF_DAYS, null, null, "unknown"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Failed to parse number of days: unknown");
  }

  @Test
  public void getNCDValue_throw_IAE_if_previous_version_type_and_value_provided() {
    assertThatThrownBy(() -> getNewCodeDefinitionValue(dbSession, dbClient, PREVIOUS_VERSION, null, null, "someValue"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unexpected value for type 'PREVIOUS_VERSION'");
  }

  @Test
  public void getNCDValue_return_empty_for_previous_version_type() {
    assertThat(getNewCodeDefinitionValue(dbSession, dbClient, PREVIOUS_VERSION, null, null, null)).isEmpty();
  }

  @Test
  public void getNCDValue_return_days_value_for_number_of_days_type() {
    String numberOfDays = "30";

    assertThat(getNewCodeDefinitionValue(dbSession, dbClient, NUMBER_OF_DAYS, null, null, numberOfDays))
      .isPresent()
      .get()
      .isEqualTo(numberOfDays);
  }

  @Test
  public void getNCDValue_return_specific_analysis_uuid_for_specific_analysis_type() {
    ComponentDto project = componentDb.insertPublicProject().getMainBranchComponent();
    SnapshotDto analysisMaster = db.components().insertSnapshot(project);
    ProjectDto projectDto = new ProjectDto().setUuid(project.uuid());
    BranchDto branchDto = new BranchDto().setUuid(project.uuid());
    String numberOfDays = "30";

    assertThat(getNewCodeDefinitionValue(dbSession, dbClient, SPECIFIC_ANALYSIS, projectDto, branchDto, analysisMaster.getUuid()))
      .isPresent()
      .get()
      .isEqualTo(analysisMaster.getUuid());
  }

  @Test
  public void getNCDValue_return_branch_value_for_reference_branch_type() {
    String branchKey = "main";

    assertThat(getNewCodeDefinitionValue(dbSession, dbClient, REFERENCE_BRANCH, null, null, branchKey))
      .isPresent()
      .get()
      .isEqualTo(branchKey);
  }

  @Test
  public void getNCDValueProjectCreation_throw_IAE_if_no_value_for_days() {
    assertThatThrownBy(() -> getNewCodeDefinitionValueProjectCreation(NUMBER_OF_DAYS, null, MAIN_BRANCH))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("New code definition type 'NUMBER_OF_DAYS' requires a value");
  }

  @Test
  public void getNCDValueProjectCreation_throw_IAE_if_days_is_invalid() {
    assertThatThrownBy(() -> getNewCodeDefinitionValueProjectCreation(NUMBER_OF_DAYS, "unknown", MAIN_BRANCH))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Failed to parse number of days: unknown");
  }

  @Test
  public void getNCDValueProjectCreation_throw_IAE_if_previous_version_type_and_value_provided() {
    assertThatThrownBy(() -> getNewCodeDefinitionValueProjectCreation(PREVIOUS_VERSION, "someValue", MAIN_BRANCH))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unexpected value for type 'PREVIOUS_VERSION'");
  }

  @Test
  public void getNCDValueProjectCreation_return_empty_for_previous_version_type() {
    assertThat(getNewCodeDefinitionValueProjectCreation(PREVIOUS_VERSION,  null, MAIN_BRANCH)).isEmpty();
  }

  @Test
  public void getNCDValueProjectCreation_return_days_value_for_number_of_days_type() {
    String numberOfDays = "30";

    assertThat(getNewCodeDefinitionValueProjectCreation(NUMBER_OF_DAYS, numberOfDays, MAIN_BRANCH))
      .isPresent()
      .get()
      .isEqualTo(numberOfDays);
  }

  @Test
  public void getNCDValueProjectCreation_return_branch_value_for_reference_branch_type() {
    String branchKey = "main";

    assertThat(getNewCodeDefinitionValueProjectCreation(REFERENCE_BRANCH, null, branchKey))
      .isPresent()
      .get()
      .isEqualTo(branchKey);
  }

  @Test
  public void getNCDValueProjectCreation_throw_IAE_ﬁif_reference_branch_type_and_value_provided() {
    assertThatThrownBy(() -> getNewCodeDefinitionValueProjectCreation(REFERENCE_BRANCH, "someValue", MAIN_BRANCH))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unexpected value for type 'REFERENCE_BRANCH'");
  }

}
