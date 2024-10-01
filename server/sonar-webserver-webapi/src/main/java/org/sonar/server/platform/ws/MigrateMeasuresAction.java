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
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.platform.db.migration.adhoc.MigrateBranchesLiveMeasuresToMeasures;
import org.sonar.server.platform.db.migration.adhoc.MigratePortfoliosLiveMeasuresToMeasures;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public class MigrateMeasuresAction implements SystemWsAction {
  public static final String SYSTEM_MEASURES_MIGRATION_ENABLED = "system.measures.migration.enabled";
  public static final String PARAM_SIZE = "size";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final MigrateBranchesLiveMeasuresToMeasures branchesMigration;
  private final MigratePortfoliosLiveMeasuresToMeasures portfoliosMigration;

  public MigrateMeasuresAction(UserSession userSession, DbClient dbClient,
    MigrateBranchesLiveMeasuresToMeasures branchesMigration, MigratePortfoliosLiveMeasuresToMeasures portfoliosMigration) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.branchesMigration = branchesMigration;
    this.portfoliosMigration = portfoliosMigration;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("migrate_measures")
      .setDescription("Prepare the migration to the next major version of SonarQube." +
        "<br/>" +
        "Sending a POST request to this URL will migrate some rows from the 'live_measures' to the 'measures' table. " +
        "Requires system administration permission.")
      .setSince("9.9.8")
      .setPost(true)
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(Resources.getResource(this.getClass(), "example-migrate_measures.json"));

    action.createParam(PARAM_SIZE)
      .setDescription("The number of branches or portfolios to migrate")
      .setDefaultValue(10);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    if (!isMigrationEnabled()) {
      throw new IllegalStateException("Migration is not enabled. Please call the endpoint /api/system/prepare_migration?enable=true and retry.");
    }

    int size = request.mandatoryParamAsInt(PARAM_SIZE);
    if (size <= 0) {
      throw new IllegalArgumentException("Size must be greater than 0");
    }

    int migratedItems = migrateBranches(size);
    if (migratedItems < size) {
      int remainingSize = size - migratedItems;
      migratedItems += migratePortfolios(remainingSize);
    }

    BranchStats statistics = getStatistics();
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject()
        .prop("status", "success")
        .prop("message", format("%s branches or portfolios migrated", migratedItems))
        .prop("remainingBranches", statistics.remainingBranches)
        .prop("totalBranches", statistics.totalBranches)
        .prop("remainingPortfolios", statistics.remainingPortfolios)
        .prop("totalPortfolios", statistics.totalPortfolios)
        .endObject();
    }
  }

  private int migrateBranches(int size) throws SQLException {
    List<String> branchesToMigrate = getBranchesToMigrate(size);
    if (!branchesToMigrate.isEmpty()) {
      branchesMigration.migrate(branchesToMigrate);
    }
    return branchesToMigrate.size();
  }

  private List<String> getBranchesToMigrate(int size) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.branchDao().selectUuidsWithMeasuresMigratedFalse(dbSession, size);
    }
  }

  private int migratePortfolios(int size) throws SQLException {
    List<String> portfoliosToMigrate = getPortfoliosToMigrate(size);
    if (!portfoliosToMigrate.isEmpty()) {
      portfoliosMigration.migrate(portfoliosToMigrate);
    }
    return portfoliosToMigrate.size();
  }

  private List<String> getPortfoliosToMigrate(int size) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.portfolioDao().selectUuidsWithMeasuresMigratedFalse(dbSession, size);
    }
  }

  private boolean isMigrationEnabled() {
    return ofNullable(dbClient.propertiesDao().selectGlobalProperty(SYSTEM_MEASURES_MIGRATION_ENABLED))
      .map(p -> Boolean.parseBoolean(p.getValue()))
      .orElse(false);
  }

  private BranchStats getStatistics() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      int remainingBranches = dbClient.branchDao().countByMeasuresMigratedFalse(dbSession);
      int totalBranches = dbClient.branchDao().countAll(dbSession);
      int remainingPortfolios = dbClient.portfolioDao().countByMeasuresMigratedFalse(dbSession);
      int totalPortfolios = dbClient.portfolioDao().selectAll(dbSession).size();

      return new BranchStats(remainingBranches, totalBranches, remainingPortfolios, totalPortfolios);
    }
  }

  private record BranchStats(int remainingBranches, int totalBranches, int remainingPortfolios, int totalPortfolios) {
  }

}
