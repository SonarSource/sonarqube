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

import com.google.common.io.Resources;
import java.sql.SQLException;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.platform.db.migration.adhoc.AddMeasuresMigratedColumnToPortfoliosTable;
import org.sonar.server.platform.db.migration.adhoc.AddMeasuresMigratedColumnToProjectBranchesTable;
import org.sonar.server.platform.db.migration.adhoc.CreateIndexOnPortfoliosMeasuresMigrated;
import org.sonar.server.platform.db.migration.adhoc.CreateIndexOnProjectBranchesMeasuresMigrated;
import org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.core.config.CorePropertyDefinitions.SYSTEM_MEASURES_MIGRATION_ENABLED;

/**
 * Implementation of the {@code prepare_migration} action for the System WebService.
 */
public class PrepareMigrationAction implements SystemWsAction {

  public static final String PARAM_ENABLE = "enable";
  private final UserSession userSession;
  private final DbClient dbClient;
  private final CreateMeasuresTable createMeasuresTable;
  private final AddMeasuresMigratedColumnToProjectBranchesTable addMeasuresMigratedColumnToProjectBranchesTable;
  private final AddMeasuresMigratedColumnToPortfoliosTable addMeasuresMigratedColumnToPortfoliosTable;
  private final CreateIndexOnProjectBranchesMeasuresMigrated createIndexOnProjectBranchesMeasuresMigrated;
  private final CreateIndexOnPortfoliosMeasuresMigrated createIndexOnPortfoliosMeasuresMigrated;

  public PrepareMigrationAction(UserSession userSession, DbClient dbClient, CreateMeasuresTable createMeasuresTable,
    AddMeasuresMigratedColumnToProjectBranchesTable addMeasuresMigratedColumnToProjectBranchesTable,
    AddMeasuresMigratedColumnToPortfoliosTable addMeasuresMigratedColumnToPortfoliosTable,
    CreateIndexOnProjectBranchesMeasuresMigrated createIndexOnProjectBranchesMeasuresMigrated,
    CreateIndexOnPortfoliosMeasuresMigrated createIndexOnPortfoliosMeasuresMigrated) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.createMeasuresTable = createMeasuresTable;
    this.addMeasuresMigratedColumnToProjectBranchesTable = addMeasuresMigratedColumnToProjectBranchesTable;
    this.addMeasuresMigratedColumnToPortfoliosTable = addMeasuresMigratedColumnToPortfoliosTable;
    this.createIndexOnProjectBranchesMeasuresMigrated = createIndexOnProjectBranchesMeasuresMigrated;
    this.createIndexOnPortfoliosMeasuresMigrated = createIndexOnPortfoliosMeasuresMigrated;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("prepare_migration")
      .setDescription("Prepare the migration to the next major version of SonarQube." +
        "<br/>" +
        "Sending a POST request to this URL enables the 'live_measures' table migration. " +
        "It is strongly advised to <strong>make a database backup</strong> before invoking this WS. " +
        "Requires system administration permission.")
      .setSince("9.9.8")
      .setPost(true)
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(Resources.getResource(this.getClass(), "example-prepare_migration.json"));

    action.createParam(PARAM_ENABLE)
      .setDescription("Set to true to enable the migration mode. Set to false to disable.")
      .setBooleanPossibleValues()
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    boolean enable = request.mandatoryParamAsBoolean(PARAM_ENABLE);
    if (enable) {
      updateDdl();
    }
    updateProperty(enable);

    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject()
        .prop("message", format("The 'live_measures' migration mode is %s", enable ? "enabled" : "disabled"))
        .endObject();
    }
  }

  private void updateDdl() throws SQLException {
    createMeasuresTable.execute();
    addMeasuresMigratedColumnToProjectBranchesTable.execute();
    addMeasuresMigratedColumnToPortfoliosTable.execute();
    createIndexOnProjectBranchesMeasuresMigrated.execute();
    createIndexOnPortfoliosMeasuresMigrated.execute();
  }

  private void updateProperty(boolean enable) {
    dbClient.propertiesDao().saveProperty(new PropertyDto().setKey(SYSTEM_MEASURES_MIGRATION_ENABLED).setValue(Boolean.toString(enable)));
  }

}
