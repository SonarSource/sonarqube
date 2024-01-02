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
package org.sonar.server.newcodeperiod;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.server.component.ComponentCreationData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.NUMBER_OF_DAYS;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.PREVIOUS_VERSION;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.SPECIFIC_ANALYSIS;

public class NewCodeDefinitionResolverTest {

  private static final String MAIN_BRANCH_UUID = "main-branch-uuid";
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private static final String DEFAULT_PROJECT_ID = "12345";

  private static final String MAIN_BRANCH = "main";

  private DbSession dbSession = db.getSession();
  private DbClient dbClient = db.getDbClient();
  private PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private NewCodeDefinitionResolver newCodeDefinitionResolver = new NewCodeDefinitionResolver(db.getDbClient(), editionProvider);

  @Test
  public void createNewCodeDefinition_throw_IAE_if_no_valid_type() {
    assertThatThrownBy(() -> newCodeDefinitionResolver.createNewCodeDefinition(dbSession, DEFAULT_PROJECT_ID, MAIN_BRANCH_UUID, MAIN_BRANCH, "nonValid", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid type: nonValid");
  }

  @Test
  public void createNewCodeDefinition_throw_IAE_if_type_is_not_allowed() {
    assertThatThrownBy(() -> newCodeDefinitionResolver.createNewCodeDefinition(dbSession, DEFAULT_PROJECT_ID, MAIN_BRANCH_UUID, MAIN_BRANCH, SPECIFIC_ANALYSIS.name(), null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid type 'SPECIFIC_ANALYSIS'. `newCodeDefinitionType` can only be set with types: [PREVIOUS_VERSION, NUMBER_OF_DAYS, REFERENCE_BRANCH]");
  }

  @Test
  public void createNewCodeDefinition_throw_IAE_if_no_value_for_days() {
    assertThatThrownBy(() -> newCodeDefinitionResolver.createNewCodeDefinition(dbSession, DEFAULT_PROJECT_ID, MAIN_BRANCH_UUID, MAIN_BRANCH, NUMBER_OF_DAYS.name(), null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("New code definition type 'NUMBER_OF_DAYS' requires a newCodeDefinitionValue");
  }

  @Test
  public void createNewCodeDefinition_throw_IAE_if_days_is_invalid() {
    assertThatThrownBy(() -> newCodeDefinitionResolver.createNewCodeDefinition(dbSession, DEFAULT_PROJECT_ID, MAIN_BRANCH_UUID,  MAIN_BRANCH, NUMBER_OF_DAYS.name(), "unknown"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Failed to parse number of days: unknown");
  }

  @Test
  public void createNewCodeDefinition_throw_IAE_if_value_is_set_for_reference_branch() {
    assertThatThrownBy(() -> newCodeDefinitionResolver.createNewCodeDefinition(dbSession, DEFAULT_PROJECT_ID, MAIN_BRANCH_UUID, MAIN_BRANCH, REFERENCE_BRANCH.name(), "feature/zw"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unexpected value for newCodeDefinitionType 'REFERENCE_BRANCH'");
  }

  @Test
  public void createNewCodeDefinition_throw_IAE_if_previous_version_type_and_value_provided() {
    assertThatThrownBy(() -> newCodeDefinitionResolver.createNewCodeDefinition(dbSession, DEFAULT_PROJECT_ID, MAIN_BRANCH_UUID, MAIN_BRANCH, PREVIOUS_VERSION.name(), "10.2.3"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unexpected value for newCodeDefinitionType 'PREVIOUS_VERSION'");
  }

  @Test
  public void createNewCodeDefinition_persist_previous_version_type() {
    newCodeDefinitionResolver.createNewCodeDefinition(dbSession, DEFAULT_PROJECT_ID, MAIN_BRANCH_UUID, MAIN_BRANCH, PREVIOUS_VERSION.name(), null);

    Optional<NewCodePeriodDto> newCodePeriodDto = dbClient.newCodePeriodDao().selectByProject(dbSession, DEFAULT_PROJECT_ID);
    assertThat(newCodePeriodDto).map(NewCodePeriodDto::getType).hasValue(PREVIOUS_VERSION);
  }

  @Test
  public void createNewCodeDefinition_return_days_value_for_number_of_days_type() {
    String numberOfDays = "30";

    newCodeDefinitionResolver.createNewCodeDefinition(dbSession, DEFAULT_PROJECT_ID, MAIN_BRANCH_UUID, MAIN_BRANCH, NUMBER_OF_DAYS.name(), numberOfDays);

    Optional<NewCodePeriodDto> newCodePeriodDto = dbClient.newCodePeriodDao().selectByProject(dbSession, DEFAULT_PROJECT_ID);

    assertThat(newCodePeriodDto)
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue)
      .containsExactly(NUMBER_OF_DAYS, numberOfDays);
  }

  @Test
  public void createNewCodeDefinition_return_branch_value_for_reference_branch_type() {
    newCodeDefinitionResolver.createNewCodeDefinition(dbSession, DEFAULT_PROJECT_ID, MAIN_BRANCH_UUID, MAIN_BRANCH, REFERENCE_BRANCH.name(), null);

    Optional<NewCodePeriodDto> newCodePeriodDto = dbClient.newCodePeriodDao().selectByProject(dbSession, DEFAULT_PROJECT_ID);

    assertThat(newCodePeriodDto)
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue, NewCodePeriodDto::getBranchUuid, NewCodePeriodDto::getProjectUuid)
      .containsExactly(REFERENCE_BRANCH, MAIN_BRANCH, null, DEFAULT_PROJECT_ID);
  }

  @Test
  public void checkNewCodeDefinitionParam_throw_IAE_if_newCodeDefinitionValue_is_provided_without_newCodeDefinitionType() {
    assertThatThrownBy(() -> newCodeDefinitionResolver.checkNewCodeDefinitionParam(null, "anyvalue"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("New code definition type is required when new code definition value is provided");
  }

  @Test
  public void checkNewCodeDefinitionParam_do_not_throw_when_both_value_and_type_are_provided() {
    assertThatNoException()
      .isThrownBy(() -> newCodeDefinitionResolver.checkNewCodeDefinitionParam("PREVIOUS_VERSION", "anyvalue"));
  }

}
