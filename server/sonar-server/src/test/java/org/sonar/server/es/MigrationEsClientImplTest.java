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
package org.sonar.server.es;

import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

public class MigrationEsClientImplTest {
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public EsTester es = EsTester.createCustom(
    new SimpleIndexDefinition("as"),
    new SimpleIndexDefinition("bs"),
    new SimpleIndexDefinition("cs"));

  private MigrationEsClient underTest = new MigrationEsClientImpl(es.client());

  @Test
  public void delete_existing_index() {
    underTest.deleteIndexes("as");

    assertThat(loadExistingIndices())
      .toIterable()
      .doesNotContain("as")
      .contains("bs", "cs");
    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("Drop Elasticsearch index [as]");
  }

  @Test
  public void delete_index_that_does_not_exist() {
    underTest.deleteIndexes("as", "xxx", "cs");

    assertThat(loadExistingIndices())
      .toIterable()
      .doesNotContain("as", "cs")
      .contains("bs");
    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("Drop Elasticsearch index [as]", "Drop Elasticsearch index [cs]")
      .doesNotContain("Drop Elasticsearch index [xxx]");
  }

  @Test
  public void addMappingToExistingIndex() {
    Map<String, String> mappingOptions = ImmutableMap.of("norms", "false");
    underTest.addMappingToExistingIndex("as", "s", "new_field", "keyword", mappingOptions);

    assertThat(loadExistingIndices()).toIterable().contains("as");
    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappings();
    MappingMetaData mapping = mappings.get("as").get("s");
    assertThat(countMappingFields(mapping)).isEqualTo(1);
    assertThat(field(mapping, "new_field")).isNotNull();

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("Add mapping [new_field] to Elasticsearch index [as]");
    assertThat(underTest.getUpdatedIndices()).containsExactly("as");
  }

  private Iterator<String> loadExistingIndices() {
    return es.client().nativeClient().admin().indices().prepareGetMappings().get().mappings().keysIt();
  }

  private ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings() {
    return es.client().nativeClient().admin().indices().prepareGetMappings().get().mappings();
  }

  @CheckForNull
  @SuppressWarnings("unchecked")
  private Map<String, Object> field(MappingMetaData mapping, String field) {
    Map<String, Object> props = (Map<String, Object>) mapping.getSourceAsMap().get("properties");
    return (Map<String, Object>) props.get(field);
  }

  private int countMappingFields(MappingMetaData mapping) {
    return ((Map) mapping.getSourceAsMap().get("properties")).size();
  }

  private static class SimpleIndexDefinition implements IndexDefinition {
    private final String indexName;

    public SimpleIndexDefinition(String indexName) {
      this.indexName = indexName;
    }

    @Override
    public void define(IndexDefinitionContext context) {
      IndexType.IndexMainType mainType = IndexType.main(Index.simple(indexName), indexName.substring(1));
      context.create(
        mainType.getIndex(),
        newBuilder(new MapSettings().asConfig()).build())
        .createTypeMapping(mainType);
    }
  }
}
