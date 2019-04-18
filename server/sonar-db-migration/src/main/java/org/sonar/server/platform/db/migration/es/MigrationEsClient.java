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
package org.sonar.server.platform.db.migration.es;

public interface MigrationEsClient {

  /**
   * This method is re-entrant and does not fail if indexName or otherIndexNames do not exist
   */
  void deleteIndexes(String name, String... otherNames);

  /**
   * Adds a new mapping to an existing elasticsearch index
   *
   * @param index name of the index that the mapping is added to
   * @param type document type in the index
   * @param mappingName name of the new mapping
   * @param mappingType type of the new mapping
   */
  void addMappingToExistingIndex(String index, String type, String mappingName, String mappingType);
}
