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
package org.sonar.db.newcodeperiod;

import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.NUMBER_OF_DAYS;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.PREVIOUS_VERSION;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.SPECIFIC_ANALYSIS;

public class NewCodePeriodDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final UuidFactory uuidFactory = new SequenceUuidFactory();
  private final NewCodePeriodDao underTest = new NewCodePeriodDao(System2.INSTANCE, uuidFactory);

  @Test
  public void insert_new_code_period() {
    insert("1", "proj-uuid", "branch-uuid", NUMBER_OF_DAYS, "5");

    Optional<NewCodePeriodDto> resultOpt = underTest.selectByUuid(dbSession, "1");

    assertThat(resultOpt).isNotNull()
      .isNotEmpty();

    NewCodePeriodDto result = resultOpt.get();
    assertThat(result.getUuid()).isEqualTo("1");
    assertThat(result.getProjectUuid()).isEqualTo("proj-uuid");
    assertThat(result.getBranchUuid()).isEqualTo("branch-uuid");
    assertThat(result.getType()).isEqualTo(NUMBER_OF_DAYS);
    assertThat(result.getValue()).isEqualTo("5");
    assertThat(result.getCreatedAt()).isNotZero();
    assertThat(result.getUpdatedAt()).isNotZero();

    db.commit();
    assertNewCodePeriodRowCount(1);
  }

  @Test
  public void reference_branch_new_code_period_accepts_branches_with_long_names() {
    String branchWithLongName = "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabc" +
      "defghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijab" +
      "cdefghijabcdefghijabcdefghijabcdefghijxxxxx";

    insert("1", "proj-uuid", "branch-uuid", REFERENCE_BRANCH, branchWithLongName);

    assertThat(db.select("select uuid as \"UUID\", value as \"VALUE\" from new_code_periods"))
      .extracting(r -> r.get("UUID"), r -> r.get("VALUE"))
      .containsExactly(tuple("1", branchWithLongName));
  }

  @Test
  public void select_global_with_no_value() {
    assertThat(underTest.selectGlobal(dbSession)).isEmpty();
  }

  @Test
  public void update_new_code_period() {
    insert("1", "proj-uuid", "branch-uuid", NUMBER_OF_DAYS, "5");

    underTest.update(dbSession, new NewCodePeriodDto()
      .setUuid("1")
      .setType(SPECIFIC_ANALYSIS)
      .setProjectUuid("proj-uuid")
      .setBranchUuid("branch-uuid")
      .setValue("analysis-uuid"));

    Optional<NewCodePeriodDto> resultOpt = underTest.selectByUuid(dbSession, "1");

    assertThat(resultOpt).isNotNull()
      .isNotEmpty();

    NewCodePeriodDto result = resultOpt.get();
    assertThat(result.getUuid()).isEqualTo("1");
    assertThat(result.getProjectUuid()).isEqualTo("proj-uuid");
    assertThat(result.getBranchUuid()).isEqualTo("branch-uuid");
    assertThat(result.getType()).isEqualTo(SPECIFIC_ANALYSIS);
    assertThat(result.getValue()).isEqualTo("analysis-uuid");
    assertThat(result.getCreatedAt()).isNotZero();
    assertThat(result.getUpdatedAt()).isNotZero();

    db.commit();
    assertNewCodePeriodRowCount(1);
  }

  @Test
  public void insert_with_upsert() {
    insert("1", "proj-uuid", "branch-uuid", NUMBER_OF_DAYS, "5");

    Optional<NewCodePeriodDto> resultOpt = underTest.selectByUuid(dbSession, "1");

    assertThat(resultOpt)
      .isNotNull()
      .isNotEmpty();

    NewCodePeriodDto result = resultOpt.get();
    assertThat(result.getUuid()).isEqualTo("1");
    assertThat(result.getProjectUuid()).isEqualTo("proj-uuid");
    assertThat(result.getBranchUuid()).isEqualTo("branch-uuid");
    assertThat(result.getType()).isEqualTo(NUMBER_OF_DAYS);
    assertThat(result.getValue()).isEqualTo("5");
    assertThat(result.getCreatedAt()).isNotZero();
    assertThat(result.getUpdatedAt()).isNotZero();

    db.commit();
    assertNewCodePeriodRowCount(1);
  }

  @Test
  public void update_with_upsert() {
    insert("1", "proj-uuid", "branch-uuid", NUMBER_OF_DAYS, "5");

    underTest.upsert(dbSession, new NewCodePeriodDto()
      .setUuid("1")
      .setProjectUuid("proj-uuid")
      .setBranchUuid("branch-uuid")
      .setType(SPECIFIC_ANALYSIS)
      .setValue("analysis-uuid"));

    Optional<NewCodePeriodDto> resultOpt = underTest.selectByUuid(dbSession, "1");

    assertThat(resultOpt)
      .isNotNull()
      .isNotEmpty();

    NewCodePeriodDto result = resultOpt.get();
    assertThat(result.getUuid()).isEqualTo("1");
    assertThat(result.getProjectUuid()).isEqualTo("proj-uuid");
    assertThat(result.getBranchUuid()).isEqualTo("branch-uuid");
    assertThat(result.getType()).isEqualTo(SPECIFIC_ANALYSIS);
    assertThat(result.getValue()).isEqualTo("analysis-uuid");
    assertThat(result.getCreatedAt()).isNotZero();
    assertThat(result.getUpdatedAt()).isNotZero();

    db.commit();
    assertNewCodePeriodRowCount(1);
  }

  @Test
  public void select_by_project_and_branch_uuids() {
    insert("1", "proj-uuid", "branch-uuid", NUMBER_OF_DAYS, "5");

    Optional<NewCodePeriodDto> resultOpt = underTest.selectByBranch(dbSession, "proj-uuid", "branch-uuid");
    assertThat(resultOpt)
      .isNotNull()
      .isNotEmpty();

    NewCodePeriodDto result = resultOpt.get();
    assertThat(result.getUuid()).isEqualTo("1");
    assertThat(result.getProjectUuid()).isEqualTo("proj-uuid");
    assertThat(result.getBranchUuid()).isEqualTo("branch-uuid");
    assertThat(result.getType()).isEqualTo(NUMBER_OF_DAYS);
    assertThat(result.getValue()).isEqualTo("5");
    assertThat(result.getCreatedAt()).isNotZero();
    assertThat(result.getUpdatedAt()).isNotZero();
  }

  @Test
  public void select_branches_referencing() {
    ProjectDto project = db.components().insertPrivateProjectDto();
    BranchDto mainBranch = db.getDbClient().branchDao().selectByUuid(dbSession, project.getUuid()).get();
    BranchDto branch1 = db.components().insertProjectBranch(project);
    BranchDto branch2 = db.components().insertProjectBranch(project);
    BranchDto branch3 = db.components().insertProjectBranch(project);

    insert("1", project.getUuid(), null, REFERENCE_BRANCH, mainBranch.getKey());
    insert("2", project.getUuid(), branch1.getUuid(), REFERENCE_BRANCH, mainBranch.getKey());
    insert("3", project.getUuid(), branch2.getUuid(), NUMBER_OF_DAYS, "5");
    insert("4", project.getUuid(), project.getUuid(), PREVIOUS_VERSION, null);
    db.commit();
    assertThat(underTest.selectBranchesReferencing(dbSession, project.getUuid(), mainBranch.getKey())).containsOnly(branch1.getUuid(), branch3.getUuid());
  }

  @Test
  public void select_by_project_uuid() {
    insert("1", "proj-uuid", null, NUMBER_OF_DAYS, "5");

    Optional<NewCodePeriodDto> resultOpt = underTest.selectByProject(dbSession, "proj-uuid");
    assertThat(resultOpt)
      .isNotNull()
      .isNotEmpty();

    NewCodePeriodDto result = resultOpt.get();
    assertThat(result.getUuid()).isEqualTo("1");
    assertThat(result.getProjectUuid()).isEqualTo("proj-uuid");
    assertThat(result.getBranchUuid()).isNull();
    assertThat(result.getType()).isEqualTo(NUMBER_OF_DAYS);
    assertThat(result.getValue()).isEqualTo("5");
    assertThat(result.getCreatedAt()).isNotZero();
    assertThat(result.getUpdatedAt()).isNotZero();
  }

  @Test
  public void select_global() {
    insert("1", null, null, NUMBER_OF_DAYS, "30");

    Optional<NewCodePeriodDto> newCodePeriodDto = underTest.selectGlobal(dbSession);
    assertThat(newCodePeriodDto).isNotEmpty();

    NewCodePeriodDto result = newCodePeriodDto.get();
    assertThat(result.getUuid()).isEqualTo("1");
    assertThat(result.getProjectUuid()).isNull();
    assertThat(result.getBranchUuid()).isNull();
    assertThat(result.getType()).isEqualTo(NUMBER_OF_DAYS);
    assertThat(result.getValue()).isEqualTo("30");
    assertThat(result.getCreatedAt()).isNotZero();
    assertThat(result.getUpdatedAt()).isNotZero();
  }

  @Test
  public void exists_by_project_analysis_is_true() {
    insert("1", "proj-uuid", "branch-uuid", SPECIFIC_ANALYSIS, "analysis-uuid");

    boolean exists = underTest.existsByProjectAnalysisUuid(dbSession, "analysis-uuid");
    assertThat(exists).isTrue();
  }

  @Test
  public void delete_by_project_uuid_and_branch_uuid() {
    insert("1", "proj-uuid", "branch-uuid", SPECIFIC_ANALYSIS, "analysis-uuid");

    underTest.delete(dbSession, "proj-uuid", "branch-uuid");
    db.commit();
    assertNewCodePeriodRowCount(0);
  }

  @Test
  public void delete_by_project_uuid() {
    insert("1", "proj-uuid", null, SPECIFIC_ANALYSIS, "analysis-uuid");

    underTest.delete(dbSession, "proj-uuid", null);
    db.commit();
    assertNewCodePeriodRowCount(0);
  }

  @Test
  public void delete_global() {
    insert("1", null, null, SPECIFIC_ANALYSIS, "analysis-uuid");

    underTest.delete(dbSession, null, null);
    db.commit();
    assertNewCodePeriodRowCount(0);
  }

  @Test
  public void exists_by_project_analysis_is_false() {
    boolean exists = underTest.existsByProjectAnalysisUuid(dbSession, "analysis-uuid");
    assertThat(exists).isFalse();
  }

  @Test
  public void fail_select_by_project_and_branch_uuids_if_project_uuid_not_provided() {
    assertThatThrownBy(() -> underTest.selectByBranch(dbSession, null, "random-uuid"))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Project uuid must be specified.");
  }

  @Test
  public void fail_select_by_project_and_branch_uuids_if_branch_uuid_not_provided() {
    assertThatThrownBy(() -> underTest.selectByBranch(dbSession, "random-uuid", null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Branch uuid must be specified.");
  }

  @Test
  public void fail_select_by_project_uuid_if_project_uuid_not_provided() {
    assertThatThrownBy(() -> underTest.selectByProject(dbSession, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Project uuid must be specified.");
  }

  private void assertNewCodePeriodRowCount(int expected) {
    assertThat(db.countRowsOfTable("new_code_periods"))
      .isEqualTo(expected);
  }

  private void insert(String uuid, @Nullable String projectUuid, @Nullable String branchUuid, NewCodePeriodType type, @Nullable String value) {
    underTest.insert(dbSession, new NewCodePeriodDto()
      .setUuid(uuid)
      .setProjectUuid(projectUuid)
      .setBranchUuid(branchUuid)
      .setType(type)
      .setValue(value));
    db.commit();
  }
}
