/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.FakeIndexDefinition;
import org.sonar.server.platform.db.migration.history.MigrationHistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EsDbCompatibilityImplTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public EsTester es = new EsTester(new FakeIndexDefinition());
  private DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
  private MigrationHistory migrationHistory = mock(MigrationHistory.class);
  private MetadataIndex metadataIndex = new MetadataIndex(es.client());
  private EsDbCompatibilityImpl underTest = new EsDbCompatibilityImpl(dbClient, metadataIndex, migrationHistory);

  @Test
  public void hasSameDbVendor_is_true_if_values_match() {
    prepareDb("mysql", 1_800L);
    prepareEs("mysql", 2_000L);

    assertThat(underTest.hasSameDbVendor()).isTrue();
  }

  @Test
  public void hasSameDbVendor_is_false_if_values_dont_match() {
    prepareDb("mysql", 1_800L);
    prepareEs("postgres", 1_800L);

    assertThat(underTest.hasSameDbVendor()).isFalse();
  }

  @Test
  public void hasSameDbVendor_is_false_if_value_is_absent_from_es() {
    prepareDb("mysql", 1_800L);

    assertThat(underTest.hasSameDbVendor()).isFalse();
  }

  @Test
  public void hasSameDbSchemaVersion_is_true_if_values_match() {
    prepareDb("mysql", 1_800L);
    prepareEs("postgres", 1_800L);

    assertThat(underTest.hasSameDbSchemaVersion()).isTrue();
  }

  @Test
  public void hasSameDbSchemaVersion_is_false_if_values_dont_match() {
    prepareDb("mysql", 1_800L);
    prepareEs("postgres", 2_000L);

    assertThat(underTest.hasSameDbSchemaVersion()).isFalse();
  }

  @Test
  public void hasSameDbSchemaVersion_is_false_if_value_is_absent_from_db() {
    prepareDb("mysql", null);
    prepareEs("postgres", 1_800L);

    assertThat(underTest.hasSameDbSchemaVersion()).isFalse();
  }

  @Test
  public void hasSameDbSchemaVersion_is_false_if_value_is_absent_from_es() {
    prepareDb("mysql", 1_800L);

    assertThat(underTest.hasSameDbSchemaVersion()).isFalse();
  }

  @Test
  public void store_db_metadata_in_es() {
    prepareDb("mysql", 1_800L);

    underTest.markAsCompatible();

    assertThat(metadataIndex.getDbVendor()).hasValue("mysql");
    assertThat(metadataIndex.getDbSchemaVersion()).hasValue(1_800L);
  }

  @Test
  public void store_updates_db_metadata_in_es() {
    prepareEs("mysql", 1_800L);
    prepareDb("postgres", 2_000L);

    underTest.markAsCompatible();

    assertThat(metadataIndex.getDbVendor()).hasValue("postgres");
    assertThat(metadataIndex.getDbSchemaVersion()).hasValue(2_000L);
  }

  @Test
  public void store_throws_ISE_if_metadata_cant_be_loaded_from_db() {
    prepareDb("postgres", null);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("DB schema version is not present in database");

    underTest.markAsCompatible();
  }

  @Test
  public void store_marks_es_as_compatible_with_db() {
    prepareDb("postgres", 1_800L);

    underTest.markAsCompatible();

    assertThat(underTest.hasSameDbSchemaVersion()).isTrue();
    assertThat(underTest.hasSameDbVendor()).isTrue();
  }

  private void prepareDb(String dbVendor, @Nullable Long dbSchemaVersion) {
    when(dbClient.getDatabase().getDialect().getId()).thenReturn(dbVendor);
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.ofNullable(dbSchemaVersion));
  }

  private void prepareEs(String dbVendor, long dbSchemaVersion) {
    metadataIndex.setDbMetadata(dbVendor, dbSchemaVersion);
  }
}
