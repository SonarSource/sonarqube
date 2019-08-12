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
package org.sonar.server.es.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.db.DbClient;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexType;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EsDbCompatibilityImplTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
  private MetadataIndex metadataIndex = spy(new TestMetadataIndex());
  private EsDbCompatibilityImpl underTest = new EsDbCompatibilityImpl(dbClient, metadataIndex);

  @Test
  public void hasSameDbVendor_is_true_if_values_match() {
    prepareDb("mssql");
    prepareEs("mssql");

    assertThat(underTest.hasSameDbVendor()).isTrue();
  }

  @Test
  public void hasSameDbVendor_is_false_if_values_dont_match() {
    prepareDb("mssql");
    prepareEs("postgres");

    assertThat(underTest.hasSameDbVendor()).isFalse();
  }

  @Test
  public void hasSameDbVendor_is_false_if_value_is_absent_from_es() {
    prepareDb("mssql");

    assertThat(underTest.hasSameDbVendor()).isFalse();
  }

  @Test
  public void markAsCompatible_db_metadata_in_es() {
    prepareDb("mssql");

    underTest.markAsCompatible();

    assertThat(metadataIndex.getDbVendor()).hasValue("mssql");
  }

  @Test
  public void markAsCompatible_updates_db_metadata_in_es() {
    prepareEs("mssql");
    prepareDb("postgres");

    underTest.markAsCompatible();

    assertThat(metadataIndex.getDbVendor()).hasValue("postgres");
  }

  @Test
  public void markAsCompatible_marks_es_as_compatible_with_db() {
    prepareDb("postgres");

    underTest.markAsCompatible();

    assertThat(underTest.hasSameDbVendor()).isTrue();
  }

  @Test
  public void markAsCompatible_has_no_effect_if_vendor_is_the_same() {
    String vendor = randomAlphabetic(12);
    prepareEs(vendor);
    prepareDb(vendor);

    underTest.markAsCompatible();

    assertThat(underTest.hasSameDbVendor()).isTrue();
    verify(metadataIndex, times(0)).setDbMetadata(anyString());
  }

  private void prepareDb(String dbVendor) {
    when(dbClient.getDatabase().getDialect().getId()).thenReturn(dbVendor);
  }

  private void prepareEs(String dbVendor) {
    metadataIndex.setDbMetadata(dbVendor);
    // reset spy to not perturbate assertions on spy from verified code
    reset(metadataIndex);
  }

  private static class TestMetadataIndex implements MetadataIndex {
    private final Map<Index, String> hashes = new HashMap<>();
    private final Map<IndexType, Boolean> initializeds = new HashMap<>();
    @CheckForNull
    private String dbVendor = null;

    @Override
    public Optional<String> getHash(Index index) {
      return Optional.ofNullable(hashes.get(index));
    }

    @Override
    public void setHash(Index index, String hash) {
      hashes.put(index, hash);
    }

    @Override
    public boolean getInitialized(IndexType indexType) {
      return initializeds.getOrDefault(indexType, false);
    }

    @Override
    public void setInitialized(IndexType indexType, boolean initialized) {
      initializeds.put(indexType, initialized);
    }

    @Override
    public Optional<String> getDbVendor() {
      return Optional.ofNullable(dbVendor);
    }

    @Override
    public void setDbMetadata(String vendor) {
      this.dbVendor = vendor;
    }
  }
}
