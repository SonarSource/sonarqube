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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.Uuids;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SelectImpl;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.step.Upsert;

import static java.util.stream.Collectors.joining;

public class PopulateLiveMeasures extends DataChange {
  private static final Logger LOG = Loggers.get(PopulateLiveMeasures.class);

  private final System2 system2;
  private final Database db;

  public PopulateLiveMeasures(Database db, System2 system2) {
    super(db);
    this.db = db;
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    boolean firstAttempt = context.prepareSelect("select count(1) from live_measures_p")
      .get(t -> t.getLong(1)) == 0;
    if (!firstAttempt) {
      LOG.info("Retry detected (non empty table live_measures_p). Handle it");
    }

    long now = system2.now();
    int projectBatchSize = 10;
    String statement = "" +
      " select" +
      "   s.uuid, s.component_uuid" +
      " from snapshots s" +
      " where" +
      "   s.islast = ?" +
      (firstAttempt ? "" : "   and not exists (select 1 from live_measures_p lmp where lmp.project_uuid=s.component_uuid)");
    try (Connection connection = createReadUncommittedConnection();
      Select select = SelectImpl.create(db, connection, statement).setBoolean(1, true)) {
      List<Row> rows = new ArrayList<>(projectBatchSize);
      select.scroll(t -> {
        rows.add(new Row(t.getString(1), t.getString(2)));
        if (rows.size() == projectBatchSize) {
          processProjectBatch(context, rows, firstAttempt, now);

          rows.clear();
        }
      });

      if (!rows.isEmpty()) {
        processProjectBatch(context, rows, firstAttempt, now);
      }
    }
  }

  private static void processProjectBatch(Context context, List<Row> rows, boolean firstAttempt, long now) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.rowPluralName("live measures");
    setSelect(rows, massUpdate);
    massUpdate.update("delete from live_measures where project_uuid=?");
    massUpdate.update(" insert into live_measures" +
      "   (uuid, component_uuid, project_uuid, metric_id, value, text_value, variation, measure_data, created_at, updated_at)" +
      " values" +
      "   (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    massUpdate.update("insert into live_measures_p (project_uuid) values (?)");
    LiveMeasurePopulationMultiHandler handler = new LiveMeasurePopulationMultiHandler(firstAttempt, rows, now);
    massUpdate.execute(handler);

    Set<String> notCommittedProjectUuids = handler.notCommittedProjectUuids;
    if (!notCommittedProjectUuids.isEmpty()) {
      Upsert upsert = context.prepareUpsert("insert into live_measures_p (project_uuid) values (?)");
      for (String projectUuid : notCommittedProjectUuids) {
        upsert.setString(1, projectUuid);
        upsert.execute();
      }
      upsert.commit();
    }
  }

  private static void setSelect(List<Row> rows, MassUpdate massUpdate) throws SQLException {
    String questionMarks = rows.stream().map(t -> "?").collect(joining(",", "(", ")"));
    SqlStatement select = massUpdate.select("" +
      " select" +
      "   p.project_uuid, pm.component_uuid, pm.metric_id, pm.value, pm.text_value, pm.variation_value_1, pm.measure_data" +
      " from project_measures pm" +
      " inner join projects p on p.uuid = pm.component_uuid and p.project_uuid in " + questionMarks +
      " where" +
      "   pm.analysis_uuid in " + questionMarks +
      " order by" +
      "   p.project_uuid");
    int i = 1;
    for (Row row : rows) {
      select.setString(i, row.getProjectUuid());
      i++;
    }
    for (Row row : rows) {
      select.setString(i, row.getAnalysisUuid());
      i++;
    }
  }

  private static class LiveMeasurePopulationMultiHandler implements MassUpdate.MultiHandler {
    private final boolean firstAttempt;
    private final long now;
    private final Set<String> notCommittedProjectUuids;
    private String deletePreviousProjectUuid = null;
    private String currentProjectUuid = null;

    private LiveMeasurePopulationMultiHandler(boolean firstAttempt, List<Row> rows, long now) {
      this.firstAttempt = firstAttempt;
      this.now = now;
      this.notCommittedProjectUuids = rows.stream().map(Row::getProjectUuid).collect(Collectors.toSet());
    }

    @Override
    public boolean handle(Select.Row row, SqlStatement update, int updateIndex) throws SQLException {
      String projectUuid = row.getString(1);
      switch (updateIndex) {
        case 0:
          return doRentranceDelete(update, projectUuid);
        case 1:
          return doLiveMeasureInsert(row, update, projectUuid);
        case 2:
          return doComponentDoneInsert(update, projectUuid);
        default:
          throw new IllegalStateException("Unsupported update index" + updateIndex);
      }
    }

    private boolean doRentranceDelete(SqlStatement update, String projectUuid) throws SQLException {
      if (firstAttempt) {
        return false;
      }

      if (deletePreviousProjectUuid == null || !deletePreviousProjectUuid.equals(projectUuid)) {
        update.setString(1, projectUuid);
        deletePreviousProjectUuid = projectUuid;
        return true;
      }
      return false;
    }

    private boolean doLiveMeasureInsert(Select.Row row, SqlStatement update, String projectUuid) throws SQLException {
      update.setString(1, Uuids.create());
      update.setString(2, row.getString(2));
      update.setString(3, projectUuid);
      update.setInt(4, row.getInt(3));
      update.setDouble(5, row.getNullableDouble(4));
      update.setString(6, row.getNullableString(5));
      update.setDouble(7, row.getNullableDouble(6));
      update.setBytes(8, row.getNullableBytes(7));
      update.setLong(9, now);
      update.setLong(10, now);
      return true;
    }

    /**
     * When currentProjectUuid changes, we know we are done will all the measures for this component and therefor
     * can insert it into live_measures_p.
     * <p>
     * <strong>This requires statement selecting measures to be sorted by project_uuid and then component_uuid.</strong>
     */
    private boolean doComponentDoneInsert(SqlStatement update, String projectUuid) throws SQLException {
      if (currentProjectUuid == null || currentProjectUuid.equals(projectUuid)) {
        this.currentProjectUuid = projectUuid;
        return false;
      }
      update.setString(1, currentProjectUuid);
      this.notCommittedProjectUuids.remove(currentProjectUuid);
      this.currentProjectUuid = projectUuid;
      return true;
    }

  }

  @Immutable
  private static final class Row {
    private final String analysisUuid;
    private final String projectUuid;

    private Row(String analysisUuid, String projectUuid) {
      this.analysisUuid = analysisUuid;
      this.projectUuid = projectUuid;
    }

    public String getAnalysisUuid() {
      return analysisUuid;
    }

    public String getProjectUuid() {
      return projectUuid;
    }

    @Override
    public String toString() {
      return "Row{" +
        "analysisUuid='" + analysisUuid + '\'' +
        ", projectUuid='" + projectUuid + '\'' +
        '}';
    }
  }
}
