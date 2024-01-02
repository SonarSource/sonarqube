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
  package org.sonar.server.newcodeperiod.ws;

  import java.time.Instant;
  import java.util.Optional;
  import org.junit.Rule;
  import org.junit.Test;
  import org.sonar.api.server.ws.WebService;
  import org.sonar.api.utils.DateUtils;
  import org.sonar.api.utils.System2;
  import org.sonar.api.web.UserRole;
  import org.sonar.core.util.UuidFactoryFast;
  import org.sonar.db.DbClient;
  import org.sonar.db.DbTester;
  import org.sonar.db.component.BranchType;
  import org.sonar.db.component.ComponentDbTester;
  import org.sonar.db.component.ComponentDto;
  import org.sonar.db.component.SnapshotDto;
  import org.sonar.db.newcodeperiod.NewCodePeriodDao;
  import org.sonar.db.newcodeperiod.NewCodePeriodDbTester;
  import org.sonar.db.newcodeperiod.NewCodePeriodDto;
  import org.sonar.db.newcodeperiod.NewCodePeriodType;
  import org.sonar.server.component.ComponentFinder;
  import org.sonar.server.component.TestComponentFinder;
  import org.sonar.server.exceptions.ForbiddenException;
  import org.sonar.server.exceptions.NotFoundException;
  import org.sonar.server.tester.UserSessionRule;
  import org.sonar.server.ws.WsActionTester;
  import org.sonarqube.ws.NewCodePeriods;
  import org.sonarqube.ws.NewCodePeriods.ListWSResponse;
  import org.sonarqube.ws.NewCodePeriods.ShowWSResponse;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;
  import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
  import static org.sonar.db.component.SnapshotTesting.newAnalysis;

  public class ListActionTest {
    @Rule
    public UserSessionRule userSession = UserSessionRule.standalone();
    @Rule
    public DbTester db = DbTester.create(System2.INSTANCE);

    private ComponentDbTester componentDb = new ComponentDbTester(db);
    private DbClient dbClient = db.getDbClient();
    private ComponentFinder componentFinder = TestComponentFinder.from(db);
    private NewCodePeriodDao dao = new NewCodePeriodDao(System2.INSTANCE, UuidFactoryFast.getInstance());
    private NewCodePeriodDbTester tester = new NewCodePeriodDbTester(db);
    private ListAction underTest = new ListAction(dbClient, userSession, componentFinder, dao);
    private WsActionTester ws = new WsActionTester(underTest);

    @Test
    public void test_definition() {
      WebService.Action definition = ws.getDef();

      assertThat(definition.key()).isEqualTo("list");
      assertThat(definition.isInternal()).isFalse();
      assertThat(definition.since()).isEqualTo("8.0");
      assertThat(definition.isPost()).isFalse();

      assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("project");
      assertThat(definition.param("project").isRequired()).isTrue();
    }

    @Test
    public void throw_NFE_if_project_not_found() {
      assertThatThrownBy(() -> ws.newRequest()
        .setParam("project", "unknown")
        .execute())
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Project 'unknown' not found");
    }

    @Test
    public void throw_FE_if_no_project_permission() {
      ComponentDto project = componentDb.insertPublicProject();

      assertThatThrownBy(() -> ws.newRequest()
        .setParam("project", project.getKey())
        .execute())
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("Insufficient privileges");
    }

    @Test
    public void list_only_branches() {
      ComponentDto project = componentDb.insertPublicProject();

      createBranches(project, 5, BranchType.BRANCH);
      createBranches(project, 3, BranchType.PULL_REQUEST);

      logInAsProjectAdministrator(project);

      ListWSResponse response = ws.newRequest()
        .setParam("project", project.getKey())
        .executeProtobuf(ListWSResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getNewCodePeriodsCount()).isEqualTo(6);
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getBranchKey)
        .contains(DEFAULT_MAIN_BRANCH_NAME, "BRANCH_0", "BRANCH_1", "BRANCH_2", "BRANCH_3", "BRANCH_4");

      //check if global default is set
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getType)
        .contains(NewCodePeriods.NewCodePeriodType.PREVIOUS_VERSION);
    }

    @Test
    public void list_inherited_global_settings() {
      ComponentDto project = componentDb.insertPublicProject();
      tester.insert(new NewCodePeriodDto().setType(NewCodePeriodType.SPECIFIC_ANALYSIS).setValue("uuid"));

      createBranches(project, 5, BranchType.BRANCH);

      logInAsProjectAdministrator(project);

      ListWSResponse response = ws.newRequest()
        .setParam("project", project.getKey())
        .executeProtobuf(ListWSResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getNewCodePeriodsCount()).isEqualTo(6);
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getBranchKey)
        .contains(DEFAULT_MAIN_BRANCH_NAME, "BRANCH_0", "BRANCH_1", "BRANCH_2", "BRANCH_3", "BRANCH_4");

      //check if global default is set
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getType)
        .contains(NewCodePeriods.NewCodePeriodType.SPECIFIC_ANALYSIS);
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getValue)
        .contains("uuid");
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getInherited)
        .contains(true);
    }

    @Test
    public void list_inherited_project_settings() {
      ComponentDto projectWithOwnSettings = componentDb.insertPublicProject();
      ComponentDto projectWithGlobalSettings = componentDb.insertPublicProject();
      tester.insert(new NewCodePeriodDto()
        .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
        .setValue("global_uuid"));
      tester.insert(new NewCodePeriodDto()
        .setProjectUuid(projectWithOwnSettings.uuid())
        .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
        .setValue("project_uuid"));

      createBranches(projectWithOwnSettings, 5, BranchType.BRANCH);

      logInAsProjectAdministrator(projectWithOwnSettings, projectWithGlobalSettings);

      ListWSResponse response = ws.newRequest()
        .setParam("project", projectWithOwnSettings.getKey())
        .executeProtobuf(ListWSResponse.class);

      //verify project with project level settings
      assertThat(response).isNotNull();
      assertThat(response.getNewCodePeriodsCount()).isEqualTo(6);
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getBranchKey)
        .contains(DEFAULT_MAIN_BRANCH_NAME, "BRANCH_0", "BRANCH_1", "BRANCH_2", "BRANCH_3", "BRANCH_4");

      //check if project setting is set
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getType)
        .contains(NewCodePeriods.NewCodePeriodType.SPECIFIC_ANALYSIS);
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getValue)
        .containsOnly("project_uuid");
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getInherited)
        .containsOnly(true);

      //verify project with global level settings
      response = ws.newRequest()
        .setParam("project", projectWithGlobalSettings.getKey())
        .executeProtobuf(ListWSResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getNewCodePeriodsCount()).isOne();
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getBranchKey)
        .containsOnly(DEFAULT_MAIN_BRANCH_NAME);

      //check if global setting is set
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getType)
        .contains(NewCodePeriods.NewCodePeriodType.SPECIFIC_ANALYSIS);
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getValue)
        .contains("global_uuid");
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getInherited)
        .containsOnly(true);
    }

    @Test
    public void list_branch_and_inherited_global_settings() {
      ComponentDto project = componentDb.insertPublicProject();
      ComponentDto branchWithOwnSettings = componentDb.insertProjectBranch(project, branchDto -> branchDto.setKey("OWN_SETTINGS"));
      componentDb.insertProjectBranch(project, branchDto -> branchDto.setKey("GLOBAL_SETTINGS"));

      tester.insert(new NewCodePeriodDto()
        .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
        .setValue("global_uuid"));

      tester.insert(new NewCodePeriodDto()
        .setProjectUuid(project.uuid())
        .setBranchUuid(branchWithOwnSettings.uuid())
        .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
        .setValue("branch_uuid"));

      logInAsProjectAdministrator(project);

      ListWSResponse response = ws.newRequest()
        .setParam("project", project.getKey())
        .executeProtobuf(ListWSResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getNewCodePeriodsCount()).isEqualTo(3);
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getBranchKey)
        .contains(DEFAULT_MAIN_BRANCH_NAME, "OWN_SETTINGS", "GLOBAL_SETTINGS");

      Optional<ShowWSResponse> ownSettings = response.getNewCodePeriodsList().stream()
        .filter(s -> !s.getInherited())
        .findFirst();

      assertThat(ownSettings)
        .isNotNull()
        .isNotEmpty();
      assertThat(ownSettings.get().getProjectKey()).isEqualTo(project.getKey());
      assertThat(ownSettings.get().getBranchKey()).isEqualTo("OWN_SETTINGS");
      assertThat(ownSettings.get().getType()).isEqualTo(NewCodePeriods.NewCodePeriodType.SPECIFIC_ANALYSIS);
      assertThat(ownSettings.get().getValue()).isEqualTo("branch_uuid");
      assertThat(ownSettings.get().getInherited()).isFalse();

      //check if global default is set
      assertThat(response.getNewCodePeriodsList())
        .filteredOn(ShowWSResponse::getInherited)
        .extracting(ShowWSResponse::getType)
        .contains(NewCodePeriods.NewCodePeriodType.SPECIFIC_ANALYSIS);
      assertThat(response.getNewCodePeriodsList())
        .filteredOn(ShowWSResponse::getInherited)
        .extracting(ShowWSResponse::getValue)
        .contains("global_uuid");
    }

    @Test
    public void list_branch_and_inherited_project_settings() {
      ComponentDto project = componentDb.insertPublicProject();
      ComponentDto branchWithOwnSettings = componentDb.insertProjectBranch(project, branchDto -> branchDto.setKey("OWN_SETTINGS"));
      componentDb.insertProjectBranch(project, branchDto -> branchDto.setKey("PROJECT_SETTINGS"));

      tester.insert(new NewCodePeriodDto()
        .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
        .setValue("global_uuid"));

      tester.insert(new NewCodePeriodDto()
        .setProjectUuid(project.uuid())
        .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
        .setValue("project_uuid"));

      tester.insert(new NewCodePeriodDto()
        .setProjectUuid(project.uuid())
        .setBranchUuid(branchWithOwnSettings.uuid())
        .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
        .setValue("branch_uuid"));

      logInAsProjectAdministrator(project);

      ListWSResponse response = ws.newRequest()
        .setParam("project", project.getKey())
        .executeProtobuf(ListWSResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getNewCodePeriodsCount()).isEqualTo(3);
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getBranchKey)
        .contains(DEFAULT_MAIN_BRANCH_NAME, "OWN_SETTINGS", "PROJECT_SETTINGS");

      Optional<ShowWSResponse> ownSettings = response.getNewCodePeriodsList().stream()
        .filter(s -> !s.getInherited())
        .findFirst();

      assertThat(ownSettings)
        .isNotNull()
        .isNotEmpty();
      assertThat(ownSettings.get().getProjectKey()).isEqualTo(project.getKey());
      assertThat(ownSettings.get().getBranchKey()).isEqualTo("OWN_SETTINGS");
      assertThat(ownSettings.get().getType()).isEqualTo(NewCodePeriods.NewCodePeriodType.SPECIFIC_ANALYSIS);
      assertThat(ownSettings.get().getValue()).isEqualTo("branch_uuid");
      assertThat(ownSettings.get().getInherited()).isFalse();

      //check if global default is set
      assertThat(response.getNewCodePeriodsList())
        .filteredOn(ShowWSResponse::getInherited)
        .extracting(ShowWSResponse::getType)
        .contains(NewCodePeriods.NewCodePeriodType.SPECIFIC_ANALYSIS);
      assertThat(response.getNewCodePeriodsList())
        .filteredOn(ShowWSResponse::getInherited)
        .extracting(ShowWSResponse::getValue)
        .contains("project_uuid");
    }

    @Test
    public void verify_specific_analysis_effective_value() {
      ComponentDto project = componentDb.insertPublicProject();
      ComponentDto branch = componentDb.insertProjectBranch(project, branchDto -> branchDto.setKey("PROJECT_BRANCH"));

      SnapshotDto analysis = componentDb.insertSnapshot(newAnalysis(project)
        .setUuid("A1")
        .setCreatedAt(Instant.now().toEpochMilli())
        .setProjectVersion("1.2")
        .setBuildString("1.2.0.322")
        .setRevision("bfe36592eb7f9f2708b5d358b5b5f33ed535c8cf")
      );

      componentDb.insertSnapshot(newAnalysis(project)
        .setUuid("A2")
        .setCreatedAt(Instant.now().toEpochMilli())
        .setProjectVersion("1.2")
        .setBuildString("1.2.0.322")
        .setRevision("2d6d5d8d5fabe2223f07aa495e794d0401ff4b04")
      );

      tester.insert(new NewCodePeriodDto()
        .setProjectUuid(project.uuid())
        .setBranchUuid(branch.uuid())
        .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
        .setValue(analysis.getUuid()));

      logInAsProjectAdministrator(project);

      ListWSResponse response = ws.newRequest()
        .setParam("project", project.getKey())
        .executeProtobuf(ListWSResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getNewCodePeriodsCount()).isEqualTo(2);
      assertThat(response.getNewCodePeriodsList()).extracting(ShowWSResponse::getBranchKey)
        .containsOnly(DEFAULT_MAIN_BRANCH_NAME, "PROJECT_BRANCH");

      ShowWSResponse result = response.getNewCodePeriodsList().get(0);
      assertThat(result.getType()).isEqualTo(NewCodePeriods.NewCodePeriodType.SPECIFIC_ANALYSIS);
      assertThat(result.getValue()).isEqualTo("A1");
      assertThat(result.getProjectKey()).isEqualTo(project.getKey());
      assertThat(result.getBranchKey()).isEqualTo("PROJECT_BRANCH");
      assertThat(result.getEffectiveValue()).isEqualTo(DateUtils.formatDateTime(analysis.getCreatedAt()));
    }

    private void createBranches(ComponentDto project, int numberOfBranches, BranchType branchType) {
      for (int branchCount = 0; branchCount < numberOfBranches; branchCount++) {
        String branchKey = String.format("%s_%d", branchType.name(), branchCount);
        componentDb.insertProjectBranch(project, branchDto -> branchDto.setKey(branchKey).setBranchType(branchType));
      }
    }

    private void logInAsProjectAdministrator(ComponentDto... project) {
      userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    }
  }
