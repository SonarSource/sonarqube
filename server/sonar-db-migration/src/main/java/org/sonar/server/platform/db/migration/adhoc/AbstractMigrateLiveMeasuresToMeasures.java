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
package org.sonar.server.platform.db.migration.adhoc;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.MurmurHash3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.step.Upsert;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractMigrateLiveMeasuresToMeasures extends DataChange {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMigrateLiveMeasuresToMeasures.class);

  private static final Set<String> TEXT_VALUE_TYPES = Set.of("STRING", "LEVEL", "DATA", "DISTRIB");
  private static final Gson GSON = new Gson();

  private static final String CLEANUP_QUERY = """
    DELETE FROM measures
    WHERE branch_uuid = ?
    """;

  private static final String SELECT_QUERY = """
    SELECT lm.component_uuid,
      m.name,
      m.val_type,
      lm.value,
      lm.text_value,
      lm.measure_data
    FROM live_measures lm
    INNER JOIN metrics m ON m.uuid = lm.metric_uuid
    WHERE lm.project_uuid = ?
    AND (lm.measure_data is null OR %s(lm.measure_data) < 1000000)
    AND m.name NOT IN ('executable_lines_data', 'ncloc_data')
    ORDER BY lm.component_uuid
    """;

  private static final String INSERT_QUERY = """
    insert into measures (component_uuid, branch_uuid, json_value, json_value_hash, created_at, updated_at)
    values ( ?, ?, ?, ?, ?, ?)
    """;

  private final String tableName;
  private final String item;
  private final System2 system2;
  private List<String> uuids = List.of();
  private int migrated = 0;

  protected AbstractMigrateLiveMeasuresToMeasures(Database db, System2 system2, String tableName, String item) {
    super(db);
    this.system2 = system2;
    this.tableName = tableName;
    this.item = item;
  }

  private String getUpdateFlagQuery() {
    return format("""
    UPDATE %s
    SET measures_migrated = ?
    WHERE uuid = ?
    """, tableName);
  }

  // This is a special entry point for the case of a configurable migration
  public void migrate(List<String> uuids) throws SQLException {
    try (Connection readConnection = createDdlConnection();
         Connection writeConnection = createDdlConnection()) {
      Context context = new Context(db, readConnection, writeConnection);
      this.uuids = uuids;
      execute(context);
    }
  }

  @Override
  protected void execute(Context context) throws SQLException {
    LOGGER.info("Starting the migration of {} {}s", uuids.size(), item);

    String selectQuery = String.format(SELECT_QUERY, getByteLengthFunction());

    for (String uuid : uuids) {
      migrateItem(uuid, context, selectQuery);

      migrated++;
      if (migrated % 100 == 0) {
        LOGGER.info("{} {}s migrated", migrated, item);
      }
    }
    LOGGER.info("Finished migration of {} {}s", uuids.size(), item);
  }

  private String getByteLengthFunction() {
    return switch (getDialect().getId()) {
      case PostgreSql.ID -> "OCTET_LENGTH";
      case MsSql.ID -> "DATALENGTH";
      case Oracle.ID -> "DBMS_LOB.GETLENGTH";
      case H2.ID -> "LENGTH";
      default -> throw new IllegalStateException("Unsupported dialect: " + getDialect().getId());
    };
  }

  private void migrateItem(String uuid, Context context, String selectQuery) throws SQLException {
    LOGGER.debug("Cleaning leftovers from a previous attempt for {} {}...", item, uuid);

    context.prepareUpsert(CLEANUP_QUERY)
      .setString(1, uuid)
      .execute()
      .commit();

    LOGGER.debug("Migrating {} {}...", item, uuid);

    Map<String, Object> measureValues = new HashMap<>();
    AtomicReference<String> componentUuid = new AtomicReference<>(null);

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(selectQuery).setString(1, uuid);
    massUpdate.update(INSERT_QUERY);
    massUpdate.execute((row, update) -> {
      boolean shouldUpdate = false;
      String rowComponentUuid = row.getString(1);
      if (componentUuid.get() == null || !rowComponentUuid.equals(componentUuid.get())) {
        if (!measureValues.isEmpty()) {
          preparePersistMeasure(uuid, update, componentUuid, measureValues);
          shouldUpdate = true;
        }

        LOGGER.debug("Starting processing of component {}...", rowComponentUuid);
        componentUuid.set(rowComponentUuid);
        measureValues.clear();
        readMeasureValue(row, measureValues);
      } else {
        readMeasureValue(row, measureValues);
      }
      return shouldUpdate;
    });
    // insert the last component
    if (!measureValues.isEmpty()) {
      Upsert measureInsert = context.prepareUpsert(INSERT_QUERY);
      preparePersistMeasure(uuid, measureInsert, componentUuid, measureValues);
      measureInsert
        .execute()
        .commit();
    }

    LOGGER.debug("Flagging migration done for {} {}...", item, uuid);

    context.prepareUpsert(getUpdateFlagQuery())
      .setBoolean(1, true)
      .setString(2, uuid)
      .execute()
      .commit();

    LOGGER.debug("Migration finished for {} {}", item, uuid);
  }

  private void preparePersistMeasure(String uuid, SqlStatement<?> update, AtomicReference<String> componentUuid, Map<String, Object> measureValues) throws SQLException {
    LOGGER.debug("Persisting measures for component {}...", componentUuid.get());
    String jsonValue = GSON.toJson(measureValues);

    long jsonHash = MurmurHash3.hash128(jsonValue.getBytes(UTF_8))[0];

    update.setString(1, componentUuid.get());
    update.setString(2, uuid);
    update.setString(3, jsonValue);
    update.setLong(4, jsonHash);
    update.setLong(5, system2.now());
    update.setLong(6, system2.now());
  }

  private static void readMeasureValue(Select.Row row, Map<String, Object> measureValues) throws SQLException {
    String metricName = row.getString(2);
    String valueType = row.getString(3);
    Double numericValue = row.getDouble(4);
    String textValue = row.getString(5);
    byte[] data = row.getBytes(6);

    Object metricValue = getMetricValue(data, textValue, valueType, numericValue);
    if (metricValue != null) {
      measureValues.put(metricName, metricValue);
    }
  }

  private static Object getMetricValue(@Nullable byte[] data, @Nullable String textValue, String valueType, Double numericValue) {
    return TEXT_VALUE_TYPES.contains(valueType) ? getTextValue(data, textValue) : numericValue;
  }

  private static String getTextValue(@Nullable byte[] data, @Nullable String textValue) {
    return data != null ? new String(data, UTF_8) : textValue;
  }
}
