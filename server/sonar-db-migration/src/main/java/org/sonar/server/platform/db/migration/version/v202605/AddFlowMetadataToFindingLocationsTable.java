/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.db.DatabaseUtils.tableColumnExists;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Adds flow metadata to {@code finding_locations}. A flow is the ordered path the detection agent
 * walked to establish a finding — untrusted input reaching a sink, say — and it is stored as one
 * {@code SECONDARY} row per step rather than in a table of its own.
 *
 * <p>{@code flow_index} and {@code location_index} are load-bearing rather than decoration: row order
 * in a table guarantees nothing and that order <em>is</em> the path, so each step's position is stored
 * instead of inferred. Without them a two-step flow can come back reversed and read as a sink flowing
 * into a source.
 *
 * <p>{@code flow_description} is repeated on every step of its flow, as SonarCloud does — there is no
 * flow table to hang it off, and one extra text per step is cheaper than a table whose only column is a
 * label. {@code flow_type} has no producer yet: the agent emits a description and locations only, so
 * the column exists to be filled without another migration the day it starts classifying flows.
 *
 * <p>All four nullable: a {@code PRIMARY} location belongs to no flow, and neither does a
 * {@code SECONDARY} one that came from a plain secondary location rather than an evidence path.
 */
public class AddFlowMetadataToFindingLocationsTable extends DdlChange {

  static final String TABLE_NAME = "finding_locations";

  static final String COLUMN_FLOW_INDEX = "flow_index";
  static final String COLUMN_LOCATION_INDEX = "location_index";
  static final String COLUMN_FLOW_DESCRIPTION = "flow_description";
  static final String COLUMN_FLOW_TYPE = "flow_type";

  // Same as the message column on this table: a flow description is prose of the same kind.
  static final int FLOW_DESCRIPTION_SIZE = 4000;
  static final int FLOW_TYPE_SIZE = 100;

  public AddFlowMetadataToFindingLocationsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, COLUMN_FLOW_INDEX)) {
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
          .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_FLOW_INDEX).setIsNullable(true).build())
          .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_LOCATION_INDEX).setIsNullable(true).build())
          .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_FLOW_DESCRIPTION).setIsNullable(true)
            .setLimit(FLOW_DESCRIPTION_SIZE).build())
          .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_FLOW_TYPE).setIsNullable(true)
            .setLimit(FLOW_TYPE_SIZE).build())
          .build());
      }
    }
  }
}
