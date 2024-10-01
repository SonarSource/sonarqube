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
package org.sonar.server.platform.ws;

import com.google.gson.Gson;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.sql.SQLException;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.Database;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.db.migration.adhoc.AddMeasuresMigratedColumnToPortfoliosTable;
import org.sonar.server.platform.db.migration.adhoc.AddMeasuresMigratedColumnToProjectBranchesTable;
import org.sonar.server.platform.db.migration.adhoc.MigrateBranchesLiveMeasuresToMeasures;
import org.sonar.server.platform.db.migration.adhoc.MigratePortfoliosLiveMeasuresToMeasures;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.server.platform.ws.MigrateMeasuresAction.SYSTEM_MEASURES_MIGRATION_ENABLED;
import static org.sonar.test.JsonAssert.assertJson;

@RunWith(DataProviderRunner.class)
public class MigrateMeasuresActionTest {
  private static final Gson GSON = new Gson();

  public static final String PARAM_SIZE = "size";
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().logIn().setSystemAdministrator();

  @Rule
  public DbTester dbTester = DbTester.create();

  private final MigrateBranchesLiveMeasuresToMeasures measuresMigration = mock();
  private final MigratePortfoliosLiveMeasuresToMeasures portfoliosMigration = mock();
  private final MigrateMeasuresAction underTest = new MigrateMeasuresAction(userSessionRule, dbTester.getDbClient(), measuresMigration, portfoliosMigration);
  private final WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void should_throw_if_migration_is_not_enabled() {
    TestRequest request = tester.newRequest();

    assertThatIllegalStateException()
      .isThrownBy(request::execute)
      .withMessage("Migration is not enabled. Please call the endpoint /api/system/prepare_migration?enable=true and retry.");
  }

  @Test
  @DataProvider(value = {"0", "-1", "-100"})
  public void should_throws_IAE_if_size_in_invalid(int size) throws SQLException {
    enableMigration();

    TestRequest request = tester
      .newRequest()
      .setParam(PARAM_SIZE, Integer.toString(size));

    assertThatIllegalArgumentException()
      .isThrownBy(request::execute)
      .withMessage("Size must be greater than 0");
  }

  @Test
  public void verify_example() throws SQLException {
    enableMigration();
    // 3 branches, 2 migrated
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto branch1 = dbTester.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH));
    dbTester.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH));
    dbTester.getDbClient().branchDao().updateMeasuresMigrated(dbTester.getSession(), project.branchUuid(), true);
    dbTester.getDbClient().branchDao().updateMeasuresMigrated(dbTester.getSession(), branch1.branchUuid(), true);
    // 2 portfolios, 1 migrated
    PortfolioDto portfolio1 = dbTester.components().insertPrivatePortfolioDto("name1");
    dbTester.components().insertPrivatePortfolioDto("name2");
    dbTester.getDbClient().portfolioDao().updateMeasuresMigrated(dbTester.getSession(), portfolio1.getUuid(), true);
    dbTester.getSession().commit();

    TestResponse response = tester.newRequest()
      .execute();

    assertJson(response.getInput()).isSimilarTo(getClass().getResource("example-migrate_measures.json"));
  }

  @Test
  public void does_not_migrate_portfolios_if_measures_are_not_finished() throws SQLException {
    enableMigration();
    // 2 branches
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto branch = dbTester.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH));
    dbTester.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH));

    TestResponse response = tester.newRequest()
      .setParam(PARAM_SIZE, "2")
      .execute();

    assertThat(GSON.fromJson(response.getInput(), ActionResponse.class))
      .isEqualTo(new ActionResponse("success", "2 branches or portfolios migrated", 3, 3, 0, 0));
    verify(measuresMigration).migrate(List.of(project.uuid(), branch.uuid()));
    verifyNoInteractions(portfoliosMigration);
  }

  @Test
  public void migrate_portfolios_to_reach_the_requested_size() throws SQLException {
    enableMigration();

    // 1 branch
    ComponentDto project = dbTester.components().insertPrivateProject();
    // 2 portfolios
    PortfolioDto portfolio1 = dbTester.components().insertPrivatePortfolioDto("name1");
    dbTester.components().insertPrivatePortfolioDto("name2");

    TestResponse response = tester.newRequest()
      .setParam(PARAM_SIZE, "2")
      .execute();

    assertThat(GSON.fromJson(response.getInput(), ActionResponse.class))
      .isEqualTo(new ActionResponse("success", "2 branches or portfolios migrated", 1, 1, 2, 2));
    verify(measuresMigration).migrate(List.of(project.uuid()));
    verify(portfoliosMigration).migrate(List.of(portfolio1.getUuid()));
  }

  @Test
  public void migrate_portfolios_only_if_measures_are_done() throws SQLException {
    enableMigration();
    // 2 branches, all migrated
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto branch1 = dbTester.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH));
    dbTester.getDbClient().branchDao().updateMeasuresMigrated(dbTester.getSession(), project.branchUuid(), true);
    dbTester.getDbClient().branchDao().updateMeasuresMigrated(dbTester.getSession(), branch1.branchUuid(), true);
    // 2 portfolios, 1 migrated
    PortfolioDto portfolio1 = dbTester.components().insertPrivatePortfolioDto("name1");
    PortfolioDto portfolio2 = dbTester.components().insertPrivatePortfolioDto("name2");
    dbTester.getDbClient().portfolioDao().updateMeasuresMigrated(dbTester.getSession(), portfolio1.getUuid(), true);
    dbTester.commit();

    TestResponse response = tester.newRequest()
      .setParam(PARAM_SIZE, "2")
      .execute();

    assertThat(GSON.fromJson(response.getInput(), ActionResponse.class))
      .isEqualTo(new ActionResponse("success", "1 branches or portfolios migrated", 0, 2, 1, 2));
    verifyNoInteractions(measuresMigration);
    verify(portfoliosMigration).migrate(List.of(portfolio2.getUuid()));
  }

  @Test
  public void does_nothing_if_migration_is_finished() throws SQLException {
    enableMigration();
    // 2 branches, all migrated
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto branch1 = dbTester.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH));
    dbTester.getDbClient().branchDao().updateMeasuresMigrated(dbTester.getSession(), project.branchUuid(), true);
    dbTester.getDbClient().branchDao().updateMeasuresMigrated(dbTester.getSession(), branch1.branchUuid(), true);
    // 2 portfolios, all migrated
    PortfolioDto portfolio1 = dbTester.components().insertPrivatePortfolioDto("name1");
    PortfolioDto portfolio2 = dbTester.components().insertPrivatePortfolioDto("name2");
    dbTester.getDbClient().portfolioDao().updateMeasuresMigrated(dbTester.getSession(), portfolio1.getUuid(), true);
    dbTester.getDbClient().portfolioDao().updateMeasuresMigrated(dbTester.getSession(), portfolio2.getUuid(), true);
    dbTester.commit();

    TestResponse response = tester.newRequest()
      .setParam(PARAM_SIZE, "2")
      .execute();

    assertThat(GSON.fromJson(response.getInput(), ActionResponse.class))
      .isEqualTo(new ActionResponse("success", "0 branches or portfolios migrated", 0, 2, 0, 2));
    verifyNoInteractions(measuresMigration, portfoliosMigration);
  }

  private void enableMigration() throws SQLException {
    Database database = dbTester.getDbClient().getDatabase();
    new AddMeasuresMigratedColumnToProjectBranchesTable(database).execute();
    new AddMeasuresMigratedColumnToPortfoliosTable(database).execute();
    dbTester.getDbClient().propertiesDao().saveProperty(new PropertyDto().setKey(SYSTEM_MEASURES_MIGRATION_ENABLED).setValue("true"));
  }

  @Test
  public void throws_ForbiddenException_if_user_is_not_logged_in() {
    userSessionRule.anonymous();

    TestRequest request = tester.newRequest();

    assertThatExceptionOfType(ForbiddenException.class)
      .isThrownBy(request::execute);
  }

  @Test
  public void throws_ForbiddenException_if_user_is_not_system_admin() {
    userSessionRule.logIn();

    TestRequest request = tester.newRequest();

    assertThatExceptionOfType(ForbiddenException.class)
      .isThrownBy(request::execute);
  }

  private record ActionResponse(String status, String message, int remainingBranches, int totalBranches, int remainingPortfolios,
                                int totalPortfolios) {
  }
}
