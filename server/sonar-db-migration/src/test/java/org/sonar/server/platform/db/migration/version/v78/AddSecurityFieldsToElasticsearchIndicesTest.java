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
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AddSecurityFieldsToElasticsearchIndicesTest {

  private MigrationEsClient esClient = mock(MigrationEsClient.class);
  private Database database = mock(Database.class, Mockito.RETURNS_DEEP_STUBS);
  private DdlChange underTest = new AddSecurityFieldsToElasticsearchIndices(database, esClient);

  @Test
  public void migration_adds_new_issues_mapping() throws SQLException {
    underTest.execute();

    verify(esClient).addMappingToExistingIndex("issues", "auth", "sonarsourceSecurity", "keyword",
      ImmutableMap.of("norms", "false"));
    verify(esClient).addMappingToExistingIndex("rules", "rule", "cwe", "keyword",
      ImmutableMap.of("norms", "false"));
    verify(esClient).addMappingToExistingIndex("rules", "rule", "owaspTop10", "keyword",
      ImmutableMap.of("norms", "false"));
    verify(esClient).addMappingToExistingIndex("rules", "rule", "sansTop25", "keyword",
      ImmutableMap.of("norms", "false"));
    verify(esClient).addMappingToExistingIndex("rules", "rule", "sonarsourceSecurity", "keyword",
      ImmutableMap.of("norms", "false"));
    verifyNoMoreInteractions(esClient);
  }
}
