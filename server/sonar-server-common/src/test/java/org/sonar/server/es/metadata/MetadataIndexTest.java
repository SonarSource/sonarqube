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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Locale;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.newindex.FakeIndexDefinition;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class MetadataIndexTest {

  @Rule
  public EsTester es = EsTester.createCustom(new MetadataIndexDefinitionBridge(), new FakeIndexDefinition());
  private final MetadataIndex underTest = new MetadataIndexImpl(es.client());
  private final String indexName = randomAlphabetic(20).toLowerCase(Locale.ENGLISH);
  private final Index index = new Random().nextBoolean() ? Index.simple(indexName) : Index.withRelations(indexName);

  @Test
  @UseDataProvider("mainOrRelationType")
  public void type_should_be_not_initialized_by_default(IndexType indexType) {
    assertThat(underTest.getInitialized(indexType)).isFalse();
  }

  @Test
  @UseDataProvider("mainOrRelationType")
  public void type_should_be_initialized_after_explicitly_set_to_initialized(IndexType indexType) {

    underTest.setInitialized(indexType, true);
    assertThat(underTest.getInitialized(indexType)).isTrue();
  }

  @DataProvider
  public static Object[][] mainOrRelationType() {
    IndexMainType mainType = IndexType.main(Index.withRelations("examples"), "example");
    return new Object[][] {
      {mainType},
      {IndexType.relation(mainType, "doo")}
    };
  }

  @Test
  public void hash_should_be_empty_by_default() {
    assertThat(underTest.getHash(index)).isEmpty();
  }

  @Test
  public void hash_should_be_able_to_be_automatically_set() {
    String hash = randomAlphanumeric(20);
    underTest.setHash(index, hash);
    assertThat(underTest.getHash(index)).hasValue(hash);
  }

  @Test
  public void database_metadata_are_empty_if_absent_from_index() {
    assertThat(underTest.getDbVendor()).isNotPresent();
  }

  @Test
  public void database_metadata_are_present_from_index() {
    underTest.setDbMetadata("postgres");

    assertThat(underTest.getDbVendor()).hasValue("postgres");
  }
}
