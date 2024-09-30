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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.db.migration.adhoc.AddMeasuresMigratedColumnToPortfoliosTable;
import org.sonar.server.platform.db.migration.adhoc.AddMeasuresMigratedColumnToProjectBranchesTable;
import org.sonar.server.platform.db.migration.adhoc.CreateIndexOnPortfoliosMeasuresMigrated;
import org.sonar.server.platform.db.migration.adhoc.CreateIndexOnProjectBranchesMeasuresMigrated;
import org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.server.platform.ws.PrepareMigrationAction.SYSTEM_MEASURES_MIGRATION_ENABLED;
import static org.sonar.test.JsonAssert.assertJson;

@RunWith(DataProviderRunner.class)
public class PrepareMigrationActionTest {

  public static final String PARAM_ENABLE = "enable";
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().logIn().setSystemAdministrator();

  @Rule
  public DbTester dbTester = DbTester.create();

  private final CreateMeasuresTable createMeasuresTable = mock();
  private final AddMeasuresMigratedColumnToProjectBranchesTable addMeasuresMigratedColumnToProjectBranchesTable = mock();
  private final AddMeasuresMigratedColumnToPortfoliosTable addMeasuresMigratedColumnToPortfoliosTable = mock();
  private final CreateIndexOnProjectBranchesMeasuresMigrated createIndexOnProjectBranchesMeasuresMigrated = mock();
  private final CreateIndexOnPortfoliosMeasuresMigrated createIndexOnPortfoliosMeasuresMigrated = mock();

  private final PrepareMigrationAction underTest = new PrepareMigrationAction(userSessionRule, dbTester.getDbClient(), createMeasuresTable,
    addMeasuresMigratedColumnToProjectBranchesTable, addMeasuresMigratedColumnToPortfoliosTable, createIndexOnProjectBranchesMeasuresMigrated, createIndexOnPortfoliosMeasuresMigrated);
  private final WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void should_throw_if_enable_parameter_is_missing() {
    TestRequest request = tester.newRequest();

    assertThatIllegalArgumentException()
      .isThrownBy(request::execute)
      .withMessage("The 'enable' parameter is missing");
  }

  @Test
  public void verify_example() {
    TestResponse response = tester.newRequest()
      .setParam(PARAM_ENABLE, "true")
      .execute();

    assertJson(response.getInput()).isSimilarTo(getClass().getResource("example-prepare_migration.json"));
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

  @Test
  @DataProvider(value = {"true", "yes"})
  public void should_enable_migration(String enableParamValue) throws SQLException {
    assertThat(getPropertyValue()).isNull();

    TestResponse response = tester.newRequest()
      .setParam(PARAM_ENABLE, enableParamValue)
      .execute();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(getPropertyValue()).isTrue();

    verify(createMeasuresTable).execute();
    verify(addMeasuresMigratedColumnToProjectBranchesTable).execute();
    verify(addMeasuresMigratedColumnToPortfoliosTable).execute();
    verify(createIndexOnProjectBranchesMeasuresMigrated).execute();
    verify(createIndexOnPortfoliosMeasuresMigrated).execute();

    // reentrant
    response = tester.newRequest()
      .setParam(PARAM_ENABLE, enableParamValue)
      .execute();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(getPropertyValue()).isTrue();
  }

  @Test
  public void property_is_unchanged_if_the_migrations_failed() throws SQLException {
    doThrow(new SQLException("Oops")).when(createMeasuresTable).execute();

    TestRequest request = tester.newRequest()
      .setParam(PARAM_ENABLE, "true");

    assertThatExceptionOfType(RuntimeException.class)
      .isThrownBy(request::execute);

    assertThat(getPropertyValue()).isNull();
  }

  @Test
  @DataProvider(value = {"false", "no"})
  public void should_disable_migration(String disableParamValue) {
    dbTester.getDbClient().propertiesDao().saveProperty(new PropertyDto().setKey(SYSTEM_MEASURES_MIGRATION_ENABLED).setValue("true"));

    TestResponse response = tester.newRequest()
      .setParam(PARAM_ENABLE, disableParamValue)
      .execute();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(getPropertyValue()).isFalse();

    verifyNoInteractions(createMeasuresTable, addMeasuresMigratedColumnToPortfoliosTable, addMeasuresMigratedColumnToProjectBranchesTable,
      createIndexOnProjectBranchesMeasuresMigrated, createIndexOnPortfoliosMeasuresMigrated);

    // reentrant
    response = tester.newRequest()
      .setParam(PARAM_ENABLE, disableParamValue)
      .execute();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(getPropertyValue()).isFalse();
  }

  private Boolean getPropertyValue() {
    PropertyDto propertyDto = dbTester.getDbClient().propertiesDao().selectGlobalProperty(SYSTEM_MEASURES_MIGRATION_ENABLED);
    if (propertyDto == null) {
      return null;
    }
    return Boolean.parseBoolean(propertyDto.getValue());
  }

}
