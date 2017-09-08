/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.es.metadata;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.server.platform.db.migration.history.MigrationHistory;

public class EsDbCompatibilityImpl implements EsDbCompatibility {

  private static final String METADATA_KEY_DB_VENDOR = "dbVendor";
  private static final String METADATA_KEY_DB_SCHEMA_VERSION = "dbSchemaVersion";

  private final DbClient dbClient;
  private final MetadataIndex metadataIndex;
  private final MigrationHistory dbMigrationHistory;

  public EsDbCompatibilityImpl(DbClient dbClient, MetadataIndex metadataIndex, MigrationHistory dbMigrationHistory) {
    this.dbClient = dbClient;
    this.metadataIndex = metadataIndex;
    this.dbMigrationHistory = dbMigrationHistory;
  }

  @Override
  public boolean hasSameDbVendor() {
    Optional<String> registeredDbVendor = metadataIndex.getMetadata(METADATA_KEY_DB_VENDOR);
    return registeredDbVendor.isPresent() && registeredDbVendor.get().equals(getDbVendor());
  }

  @Override
  public boolean hasSameDbSchemaVersion() {
    Optional<Long> registeredVersion = metadataIndex.getMetadata(METADATA_KEY_DB_SCHEMA_VERSION).map(Long::parseLong);
    if (!registeredVersion.isPresent()) {
      return false;
    }
    return getDbSchemaVersion()
      .filter(effectiveVersion -> Objects.equals(registeredVersion.get(), effectiveVersion))
      .isPresent();
  }

  @Override
  public void markAsCompatible() {
    metadataIndex.setMetadata(METADATA_KEY_DB_VENDOR, getDbVendor());
    metadataIndex.setMetadata(METADATA_KEY_DB_SCHEMA_VERSION, getDbSchemaVersion()
      .map(String::valueOf)
      .orElseThrow(() -> new IllegalStateException("DB schema version is not present in database")));
  }

  private String getDbVendor() {
    return dbClient.getDatabase().getDialect().getId().toLowerCase(Locale.ENGLISH);
  }

  private Optional<Long> getDbSchemaVersion() {
    return dbMigrationHistory.getLastMigrationNumber();
  }
}
