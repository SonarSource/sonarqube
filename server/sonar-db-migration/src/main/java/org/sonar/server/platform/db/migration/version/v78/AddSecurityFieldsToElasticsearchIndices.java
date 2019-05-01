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
package org.sonar.server.platform.db.migration.version.v78;

import com.google.common.collect.ImmutableMap;
import java.sql.SQLException;
import java.util.Map;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;
import org.sonar.server.platform.db.migration.step.DdlChange;

@SupportsBlueGreen
public class AddSecurityFieldsToElasticsearchIndices extends DdlChange {

  private static final String ISSUE_INDEX = "issues";
  private static final String RULE_INDEX = "rules";
  private static final String KEYWORD_TYPE = "keyword";

  private final MigrationEsClient migrationEsClient;

  public AddSecurityFieldsToElasticsearchIndices(Database db, MigrationEsClient migrationEsClient) {
    super(db);
    this.migrationEsClient = migrationEsClient;
  }

  @Override
  public void execute(Context context) throws SQLException {
    Map<String, String> mappingOptions = ImmutableMap.of("norms", "false");

    migrationEsClient.addMappingToExistingIndex(ISSUE_INDEX, "auth", "sonarsourceSecurity", KEYWORD_TYPE, mappingOptions);
    migrationEsClient.addMappingToExistingIndex(RULE_INDEX, "rule", "cwe", KEYWORD_TYPE, mappingOptions);
    migrationEsClient.addMappingToExistingIndex(RULE_INDEX, "rule", "owaspTop10", KEYWORD_TYPE, mappingOptions);
    migrationEsClient.addMappingToExistingIndex(RULE_INDEX, "rule", "sansTop25", KEYWORD_TYPE, mappingOptions);
    migrationEsClient.addMappingToExistingIndex(RULE_INDEX, "rule", "sonarsourceSecurity", KEYWORD_TYPE, mappingOptions);
  }

}
