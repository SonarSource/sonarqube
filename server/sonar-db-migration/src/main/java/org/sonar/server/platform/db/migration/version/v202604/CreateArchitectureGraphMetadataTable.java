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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.DynamoStyleTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code arch_graph_metadata} table holding the metadata of the unified architecture capability's
 * graphs. It is the relational form of the SonarQube Cloud DynamoDB {@code Graph} table: partition key
 * {@code organization_id}, sort key {@code uuid}, the {@code UuidIndex} GSI on {@code uuid}, and the
 * {@code AnalysisIdIndex}/{@code ProjectIdIndex}/{@code BranchIdIndex} LSIs (which share the partition key) on
 * {@code analysis_id}/{@code project_id}/{@code branch_id} — expressed through {@link DynamoStyleTableBuilder}
 * so the DynamoDB shape maps one-to-one. {@code created_at} is stored as epoch millis.
 *
 * <p>The name is intentionally distinct from the legacy {@code architecture_graphs} table of the (no longer
 * deployed) {@code core-extension-architecture} feature, which is left untouched. The capability reads/writes
 * this table via the host MyBatis mapper it registers (see the architecture-server module); this table is
 * community/global, not edition-gated.
 */
public class CreateArchitectureGraphMetadataTable extends CreateTableChange {

  static final String TABLE_NAME = "arch_graph_metadata";
  static final String COLUMN_ORGANIZATION_ID = "organization_id";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_ANALYSIS_ID = "analysis_id";
  static final String COLUMN_PROJECT_ID = "project_id";
  static final String COLUMN_BRANCH_ID = "branch_id";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_ECOSYSTEM = "ecosystem";
  static final String COLUMN_TYPE = "type";
  static final String COLUMN_PERSPECTIVE_KEY = "perspective_key";
  static final String COLUMN_CONTENT_TYPE = "content_type";
  static final String COLUMN_VERSION = "version";
  static final String INDEX_UUID = "arch_graph_metadata_uuid";
  static final String INDEX_ANALYSIS = "arch_graph_metadata_analysis";
  static final String INDEX_PROJECT = "arch_graph_metadata_project";
  static final String INDEX_BRANCH = "arch_graph_metadata_branch";

  static final int SHORT_TEXT_SIZE = 255;

  protected CreateArchitectureGraphMetadataTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new DynamoStyleTableBuilder(getDialect(), tableName)
      .withPartitionKey(newVarcharColumnDefBuilder().setColumnName(COLUMN_ORGANIZATION_ID).setLimit(UUID_SIZE).setIsNullable(false).build())
      .withSortKey(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_ANALYSIS_ID).setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_ID).setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_BRANCH_ID).setLimit(UUID_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_ECOSYSTEM).setLimit(SHORT_TEXT_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_TYPE).setLimit(SHORT_TEXT_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_PERSPECTIVE_KEY).setLimit(SHORT_TEXT_SIZE).setIsNullable(true).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_CONTENT_TYPE).setLimit(SHORT_TEXT_SIZE).setIsNullable(false).build())
      .withAttribute(newVarcharColumnDefBuilder().setColumnName(COLUMN_VERSION).setLimit(SHORT_TEXT_SIZE).setIsNullable(false).build())
      .withAttribute(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .withGlobalSecondaryIndex(INDEX_UUID, COLUMN_UUID)
      .withLocalSecondaryIndex(INDEX_ANALYSIS, COLUMN_ANALYSIS_ID)
      .withLocalSecondaryIndex(INDEX_PROJECT, COLUMN_PROJECT_ID)
      .withLocalSecondaryIndex(INDEX_BRANCH, COLUMN_BRANCH_ID)
      .build());
  }
}
