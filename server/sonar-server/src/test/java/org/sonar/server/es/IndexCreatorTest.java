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
package org.sonar.server.es;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.es.metadata.MetadataIndexDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.action.support.WriteRequest.RefreshPolicy.IMMEDIATE;
import static org.sonar.server.es.NewIndex.SettingsConfiguration.newBuilder;

public class IndexCreatorTest {

  private static final NewIndex.SettingsConfiguration SETTINGS_CONFIGURATION = newBuilder(new MapSettings().asConfig()).build();
  private static final String LOG_DB_VENDOR_CHANGED = "Delete Elasticsearch indices (DB vendor changed)";
  private static final String LOG_DB_SCHEMA_CHANGED = "Delete Elasticsearch indices (DB schema changed)";

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public EsTester es = new EsTester();

  private MetadataIndexDefinition metadataIndexDefinition = new MetadataIndexDefinition(new MapSettings().asConfig());
  private MetadataIndex metadataIndex = new MetadataIndex(es.client());
  private TestEsDbCompatibility esDbCompatibility = new TestEsDbCompatibility();
  private MapSettings settings = new MapSettings();

  @Test
  public void create_index() throws Exception {
    assertThat(mappings()).isEmpty();

    IndexCreator underTest = startNewCreator(new FakeIndexDefinition());

    // check that index is created with related mapping
    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappings();
    MappingMetaData mapping = mappings.get("fakes").get("fake");
    assertThat(mapping.type()).isEqualTo("fake");
    assertThat(mapping.getSourceAsMap()).isNotEmpty();
    assertThat(countMappingFields(mapping)).isEqualTo(2);
    assertThat(field(mapping, "updatedAt").get("type")).isEqualTo("date");

    // of course do not delete indices on stop
    underTest.stop();
    assertThat(mappings()).isNotEmpty();
  }

  @Test
  public void mark_all_non_existing_index_types_as_uninitialized() {
    startNewCreator(context -> {
      NewIndex i = context.create("i", SETTINGS_CONFIGURATION);
      i.createType("t1");
      i.createType("t2");
    });

    assertThat(metadataIndex.getHash("i")).isNotEmpty();
    assertThat(metadataIndex.getInitialized(new IndexType("i", "t1"))).isFalse();
    assertThat(metadataIndex.getInitialized(new IndexType("i", "t2"))).isFalse();
  }

  @Test
  public void recreate_index_on_definition_changes() throws Exception {
    // v1
    startNewCreator(new FakeIndexDefinition());

    IndexType fakeIndexType = new IndexType("fakes", "fake");
    String id = "1";
    es.client().prepareIndex(fakeIndexType).setId(id).setSource(new FakeDoc().getFields()).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE).get();
    assertThat(es.client().prepareGet(fakeIndexType, id).get().isExists()).isTrue();

    // v2
    startNewCreator(new FakeIndexDefinitionV2());

    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappings();
    MappingMetaData mapping = mappings.get("fakes").get("fake");
    assertThat(countMappingFields(mapping)).isEqualTo(3);
    assertThat(field(mapping, "updatedAt").get("type")).isEqualTo("date");
    assertThat(field(mapping, "newField").get("type")).isEqualTo("integer");

    assertThat(es.client().prepareGet(fakeIndexType, id).get().isExists()).isFalse();
  }

  @Test
  public void do_not_recreate_index_on_unchanged_definition() {
    // v1
    startNewCreator(new FakeIndexDefinition());
    IndexType fakeIndexType = new IndexType("fakes", "fake");
    String id = "1";
    es.client().prepareIndex(fakeIndexType).setId(id).setSource(new FakeDoc().getFields()).setRefreshPolicy(IMMEDIATE).get();
    assertThat(es.client().prepareGet(fakeIndexType, id).get().isExists()).isTrue();

    // v1
    startNewCreator(new FakeIndexDefinition());
    assertThat(es.client().prepareGet(fakeIndexType, id).get().isExists()).isTrue();
  }

  @Test
  public void delete_existing_indices_if_db_vendor_changed() {
    testDeleteOnDbChange(LOG_DB_VENDOR_CHANGED,
      c -> c.setHasSameDbVendor(false));
  }

  @Test
  public void delete_existing_indices_if_db_schema_changed() {
    testDeleteOnDbChange(LOG_DB_SCHEMA_CHANGED,
      c -> c.setHasSameDbSchemaVersion(false));
  }

  @Test
  public void do_not_check_db_compatibility_on_fresh_es() {
    // supposed to be ignored
    esDbCompatibility.setHasSameDbVendor(false);
    esDbCompatibility.setHasSameDbSchemaVersion(false);

    startNewCreator(new FakeIndexDefinition());

    assertThat(logTester.logs(LoggerLevel.INFO))
      .doesNotContain(LOG_DB_VENDOR_CHANGED)
      .doesNotContain(LOG_DB_SCHEMA_CHANGED)
      .contains("Create type fakes/fake")
      .contains("Create type metadatas/metadata");
  }

  @Test
  public void do_not_check_db_compatibility_if_disabled_by_configuration() {
    settings.setProperty("sonar.search.disableDropOnDbMigration", true);

    // initial startup, automatic drop may be ignored because indices do not exist
    startNewCreator(new FakeIndexDefinition());
    logTester.clear();

    // second startup, automatic drop can be disabled only by configuration

    // supposed to be ignored
    esDbCompatibility.setHasSameDbVendor(false);
    esDbCompatibility.setHasSameDbSchemaVersion(false);

    startNewCreator(new FakeIndexDefinition());

    assertThat(logTester.logs(LoggerLevel.INFO))
      .doesNotContain(LOG_DB_VENDOR_CHANGED)
      .doesNotContain(LOG_DB_SCHEMA_CHANGED);
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Automatic drop of search indices in turned off (see property sonar.search.disableDropOnDbMigration)");
  }

  private void testDeleteOnDbChange(String expectedLog, Consumer<TestEsDbCompatibility> afterFirstStart) {
    startNewCreator(new FakeIndexDefinition());
    assertThat(logTester.logs(LoggerLevel.INFO))
      .doesNotContain(expectedLog)
      .contains("Create type fakes/fake")
      .contains("Create type metadatas/metadata");
    putFakeDocument();
    assertThat(es.countDocuments(FakeIndexDefinition.INDEX_TYPE)).isEqualTo(1);

    afterFirstStart.accept(esDbCompatibility);
    logTester.clear();
    startNewCreator(new FakeIndexDefinition());

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains(expectedLog)
      .contains("Create type fakes/fake")
      // keep existing metadata
      .doesNotContain("Create type metadatas/metadata");
    // index has been dropped and re-created
    assertThat(es.countDocuments(FakeIndexDefinition.INDEX_TYPE)).isEqualTo(0);
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

  private IndexCreator startNewCreator(IndexDefinition... definitions) {
    IndexDefinitions defs = new IndexDefinitions(definitions, new MapSettings().asConfig());
    defs.start();
    IndexCreator creator = new IndexCreator(es.client(), defs, metadataIndexDefinition, metadataIndex, esDbCompatibility, settings.asConfig());
    creator.start();
    return creator;
  }

  private void putFakeDocument() {
    es.putDocuments(FakeIndexDefinition.INDEX_TYPE, ImmutableMap.of("key", "foo"));
  }

  private static class FakeIndexDefinition implements IndexDefinition {
    private static final IndexType INDEX_TYPE = new IndexType("fakes", "fake");

    @Override
    public void define(IndexDefinitionContext context) {
      NewIndex index = context.create("fakes", SETTINGS_CONFIGURATION);
      NewIndex.NewIndexType mapping = index.createType("fake");
      mapping.keywordFieldBuilder("key").build();
      mapping.createDateTimeField("updatedAt");
    }
  }

  private static class FakeIndexDefinitionV2 implements IndexDefinition {
    @Override
    public void define(IndexDefinitionContext context) {
      NewIndex index = context.create("fakes", SETTINGS_CONFIGURATION);
      NewIndex.NewIndexType mapping = index.createType("fake");
      mapping.keywordFieldBuilder("key").build();
      mapping.createDateTimeField("updatedAt");
      mapping.createIntegerField("newField");
    }
  }
}
