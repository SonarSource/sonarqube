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
package org.sonar.db.newcodeperiod;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NewCodePeriodDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbSession dbSession = db.getSession();
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private NewCodePeriodDao underTest = new NewCodePeriodDao(System2.INSTANCE, uuidFactory);

  @Test
  public void insert_new_code_period() {
    when(uuidFactory.create()).thenReturn("uuid-1");
    underTest.insert(dbSession, new NewCodePeriodDto()
      .setProjectUuid("proj-uuid")
      .setBranchUuid("branch-uuid")
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("5"));

    Optional<NewCodePeriodDto> resultOpt = underTest.selectByUuid(dbSession, "uuid-1");

    assertThat(resultOpt).isNotNull();
    assertThat(resultOpt).isNotEmpty();

    NewCodePeriodDto result = resultOpt.get();
    assertThat(result.getUuid()).isEqualTo("uuid-1");
    assertThat(result.getProjectUuid()).isEqualTo("proj-uuid");
    assertThat(result.getBranchUuid()).isEqualTo("branch-uuid");
    assertThat(result.getType()).isEqualTo(NewCodePeriodType.NUMBER_OF_DAYS);
    assertThat(result.getValue()).isEqualTo("5");
    assertThat(result.getCreatedAt()).isNotEqualTo(0);
    assertThat(result.getUpdatedAt()).isNotEqualTo(0);
  }

  @Test
  public void insert_with_upsert() {
    when(uuidFactory.create()).thenReturn("uuid-1");

    underTest.upsert(dbSession, new NewCodePeriodDto()
      .setProjectUuid("proj-uuid")
      .setBranchUuid("branch-uuid")
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("5"));

    Optional<NewCodePeriodDto> resultOpt = underTest.selectByUuid(dbSession, "uuid-1");

    assertThat(resultOpt).isNotNull();
    assertThat(resultOpt).isNotEmpty();

    NewCodePeriodDto result = resultOpt.get();
    assertThat(result.getUuid()).isEqualTo("uuid-1");
    assertThat(result.getProjectUuid()).isEqualTo("proj-uuid");
    assertThat(result.getBranchUuid()).isEqualTo("branch-uuid");
    assertThat(result.getType()).isEqualTo(NewCodePeriodType.NUMBER_OF_DAYS);
    assertThat(result.getValue()).isEqualTo("5");
    assertThat(result.getCreatedAt()).isNotEqualTo(0);
    assertThat(result.getUpdatedAt()).isNotEqualTo(0);
  }

  @Test
  public void update_with_upsert() {
    when(uuidFactory.create()).thenReturn("uuid-1");

    db.newCodePeriods().insert(new NewCodePeriodDto()
      .setProjectUuid("proj-uuid")
      .setBranchUuid("branch-uuid")
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("5"));

    underTest.upsert(dbSession, new NewCodePeriodDto()
      .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
      .setProjectUuid("proj-uuid")
      .setBranchUuid("branch-uuid")
      .setValue("analysis-uuid"));

    Optional<NewCodePeriodDto> resultOpt = underTest.selectByUuid(dbSession, "uuid-1");

    assertThat(resultOpt).isNotNull();
    assertThat(resultOpt).isNotEmpty();

    NewCodePeriodDto result = resultOpt.get();
    assertThat(result.getUuid()).isEqualTo("uuid-1");
    assertThat(result.getProjectUuid()).isEqualTo("proj-uuid");
    assertThat(result.getBranchUuid()).isEqualTo("branch-uuid");
    assertThat(result.getType()).isEqualTo(NewCodePeriodType.SPECIFIC_ANALYSIS);
    assertThat(result.getValue()).isEqualTo("analysis-uuid");
    assertThat(result.getCreatedAt()).isNotEqualTo(0);
    assertThat(result.getUpdatedAt()).isNotEqualTo(0);
  }

  @Test
  public void select_by_project_and_branch_uuids() {
    when(uuidFactory.create()).thenReturn("uuid-1");

    NewCodePeriodDto dto = db.newCodePeriods().insert(new NewCodePeriodDto()
      .setProjectUuid("proj-uuid")
      .setBranchUuid("branch-uuid")
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("5"));

    Optional<NewCodePeriodDto> resultOpt = underTest.selectByBranch(dbSession, dto.getProjectUuid(), dto.getBranchUuid());
    assertThat(resultOpt).isNotNull();
    assertThat(resultOpt).isNotEmpty();

    NewCodePeriodDto result = resultOpt.get();
    assertThat(result.getUuid()).isEqualTo("1");
    assertThat(result.getProjectUuid()).isEqualTo("proj-uuid");
    assertThat(result.getBranchUuid()).isEqualTo("branch-uuid");
    assertThat(result.getType()).isEqualTo(NewCodePeriodType.NUMBER_OF_DAYS);
    assertThat(result.getValue()).isEqualTo("5");
    assertThat(result.getCreatedAt()).isNotEqualTo(0);
    assertThat(result.getUpdatedAt()).isNotEqualTo(0);
  }

  @Test
  public void select_by_project_uuid() {
    when(uuidFactory.create()).thenReturn("uuid-1");

    NewCodePeriodDto dto = db.newCodePeriods().insert(new NewCodePeriodDto()
      .setProjectUuid("proj-uuid")
      .setBranchUuid(null)
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("5"));

    Optional<NewCodePeriodDto> resultOpt = underTest.selectByProject(dbSession, dto.getProjectUuid());
    assertThat(resultOpt).isNotNull();
    assertThat(resultOpt).isNotEmpty();

    NewCodePeriodDto result = resultOpt.get();
    assertThat(result.getUuid()).isEqualTo("1");
    assertThat(result.getProjectUuid()).isEqualTo("proj-uuid");
    assertThat(result.getBranchUuid()).isNull();
    assertThat(result.getType()).isEqualTo(NewCodePeriodType.NUMBER_OF_DAYS);
    assertThat(result.getValue()).isEqualTo("5");
    assertThat(result.getCreatedAt()).isNotEqualTo(0);
    assertThat(result.getUpdatedAt()).isNotEqualTo(0);
  }

  @Test
  public void select_global() {
    when(uuidFactory.create()).thenReturn("uuid-1");

    db.newCodePeriods().insert(new NewCodePeriodDto()
      .setProjectUuid(null)
      .setBranchUuid(null)
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("30"));

    NewCodePeriodDto result = underTest.selectGlobal(dbSession);
    assertThat(result.getUuid()).isEqualTo("1");
    assertThat(result.getProjectUuid()).isNull();
    assertThat(result.getBranchUuid()).isNull();
    assertThat(result.getType()).isEqualTo(NewCodePeriodType.NUMBER_OF_DAYS);
    assertThat(result.getValue()).isEqualTo("30");
    assertThat(result.getCreatedAt()).isNotEqualTo(0);
    assertThat(result.getUpdatedAt()).isNotEqualTo(0);
  }

  @Test
  public void fail_select_by_project_and_branch_uuids_if_project_uuid_not_provided() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Project uuid must be specified.");
    underTest.selectByBranch(dbSession, null, "random-uuid");
  }

  @Test
  public void fail_select_by_project_and_branch_uuids_if_branch_uuid_not_provided() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Branch uuid must be specified.");
    underTest.selectByBranch(dbSession, "random-uuid", null);
  }

  @Test
  public void fail_select_by_project_uuid_if_project_uuid_not_provided() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Project uuid must be specified.");
    underTest.selectByProject(dbSession, null);
  }

}
