/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.JsonpSerializable;
import co.elastic.clients.json.jackson.JacksonJsonProvider;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.es.metadata.MetadataIndexDefinition;
import org.sonar.server.es.metadata.MetadataIndexImpl;
import org.sonar.server.es.newindex.NewRegularIndex;
import org.sonar.server.es.newindex.SettingsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.es.IndexType.main;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

class IndexCreatorTest {

  private static final SettingsConfiguration SETTINGS_CONFIGURATION = newBuilder(new MapSettings().asConfig()).build();
  private static final String LOG_DB_VENDOR_CHANGED = "Delete Elasticsearch indices (DB vendor changed)";
  private static final String LOG_DB_SCHEMA_CHANGED = "Delete Elasticsearch indices (DB schema changed)";

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();
  @RegisterExtension
  public EsTester es = EsTester.createCustom();

  private final MetadataIndexDefinition metadataIndexDefinition = new MetadataIndexDefinition(new MapSettings().asConfig());
  private final MetadataIndex metadataIndex = new MetadataIndexImpl(es.client());
  private final TestEsDbCompatibility esDbCompatibility = new TestEsDbCompatibility();

  @Test
  void create_index() {
    IndexCreator underTest = run(new FakeIndexDefinition());

    // check that index is created with related mapping
    verifyFakeIndexV1();

    // of course do not delete indices on stop
    underTest.stop();
    assertThat(mappings()).isNotEmpty();
  }

  @Test
  void recreate_index_on_definition_changes() {
    // v1
    run(new FakeIndexDefinition());

    IndexMainType fakeIndexType = main(Index.simple("fakes"), "fake");
    String id = "1";
    es.client().indexV2(ir -> ir.index(fakeIndexType.getIndex().getName())
      .id(id)
      .document(new FakeDoc().getFields())
      .refresh(Refresh.True));

    assertThat(es.client().getV2(req -> req.index(fakeIndexType.getIndex().getName()).id(id), Map.class).found()).isTrue();

    // v2
    run(new FakeIndexDefinitionV2());

    Map<String, MappingMetadata> mappings = mappings();
    MappingMetadata mapping = mappings.get("fakes");
    assertThat(countMappingFields(mapping)).isEqualTo(3);
    assertThat(field(mapping, "updatedAt")).containsEntry("type", "date");
    assertThat(field(mapping, "newField")).containsEntry("type", "integer");

    assertThat(es.client().getV2(req -> req.index(fakeIndexType.getIndex().getName()).id(id), Map.class).found()).isFalse();
  }

  @Test
  void mark_all_non_existing_index_types_as_uninitialized() {
    Index fakesIndex = Index.simple("fakes");
    Index fakersIndex = Index.simple("fakers");
    run(context -> {
      context.create(fakesIndex, SETTINGS_CONFIGURATION)
        .createTypeMapping(IndexType.main(fakesIndex, "fake"));
      context.create(fakersIndex, SETTINGS_CONFIGURATION)
        .createTypeMapping(IndexType.main(fakersIndex, "faker"));
    });

    assertThat(metadataIndex.getHash(fakesIndex)).isNotEmpty();
    assertThat(metadataIndex.getHash(fakersIndex)).isNotEmpty();
    assertThat(metadataIndex.getInitialized(main(fakesIndex, "fake"))).isFalse();
    assertThat(metadataIndex.getInitialized(main(fakersIndex, "faker"))).isFalse();
  }

  @Test
  void delete_existing_indices_if_db_vendor_changed() {
    testDeleteOnDbChange(LOG_DB_VENDOR_CHANGED,
      c -> c.setHasSameDbVendor(false));
  }

  @Test
  void do_not_check_db_compatibility_on_fresh_es() {
    // supposed to be ignored
    esDbCompatibility.setHasSameDbVendor(false);

    run(new FakeIndexDefinition());

    assertThat(logTester.logs(Level.INFO))
      .doesNotContain(LOG_DB_VENDOR_CHANGED)
      .doesNotContain(LOG_DB_SCHEMA_CHANGED)
      .contains("Create mapping fakes")
      .contains("Create mapping metadatas");
  }

  @Test
  void start_makes_metadata_index_read_write_if_read_only() {
    run(new FakeIndexDefinition());

    IndexMainType mainType = MetadataIndexDefinition.TYPE_METADATA;
    makeReadOnly(mainType);

    run(new FakeIndexDefinition());

    assertThat(isNotReadOnly(mainType)).isTrue();
  }

  @Test
  void start_makes_index_read_write_if_read_only() {
    FakeIndexDefinition fakeIndexDefinition = new FakeIndexDefinition();
    IndexMainType fakeIndexMainType = FakeIndexDefinition.INDEX_TYPE.getMainType();
    run(fakeIndexDefinition);

    IndexMainType mainType = MetadataIndexDefinition.TYPE_METADATA;
    makeReadOnly(mainType);
    makeReadOnly(fakeIndexMainType);

    run(fakeIndexDefinition);

    assertThat(isNotReadOnly(mainType)).isTrue();
    assertThat(isNotReadOnly(fakeIndexMainType)).isTrue();
  }

  private boolean isNotReadOnly(IndexMainType mainType) {
    String indexName = mainType.getIndex().getName();
    co.elastic.clients.elasticsearch.indices.IndexSettings indexSettings = es.client().getSettingsV2(req -> req.index(indexName))
      .get(indexName)
      .settings();

    String readOnly = indexSettings.otherSettings().containsKey("index.blocks.read_only_allow_delete")
      ? indexSettings.otherSettings().get("index.blocks.read_only_allow_delete").to(String.class)
      : null;
    return readOnly == null;
  }

  private void makeReadOnly(IndexMainType mainType) {
    String indexName = mainType.getIndex().getName();

    es.client().putSettingsV2(req -> req
      .index(indexName)
      .settings(s -> s.otherSettings(java.util.Map.of("index.blocks.read_only_allow_delete", co.elastic.clients.json.JsonData.of(true))))
    );
  }

  private void testDeleteOnDbChange(String expectedLog, Consumer<TestEsDbCompatibility> afterFirstStart) {
    run(new FakeIndexDefinition());
    assertThat(logTester.logs(Level.INFO))
      .doesNotContain(expectedLog)
      .contains("Create mapping fakes")
      .contains("Create mapping metadatas");
    putFakeDocument();
    assertThat(es.countDocuments(FakeIndexDefinition.INDEX_TYPE)).isOne();

    afterFirstStart.accept(esDbCompatibility);
    logTester.clear();
    run(new FakeIndexDefinition());

    assertThat(logTester.logs(Level.INFO))
      .contains(expectedLog)
      .contains("Create mapping fakes")
      // keep existing metadata
      .doesNotContain("Create mapping metadatas");
    // index has been dropped and re-created
    assertThat(es.countDocuments(FakeIndexDefinition.INDEX_TYPE)).isZero();
  }

  private Map<String, MappingMetadata> mappings() {
    // This allows to maintain the retro compatibility with the MappingMetadata class used with ES client v7
    return es.client().getMappingV2(req -> req.index("*")).result().entrySet()
      .stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        this::toMappingMetadata
      ));
  }

  private MappingMetadata toMappingMetadata(Map.Entry<String, IndexMappingRecord> entry) {
    try {
      Map<String, Object> propertiesMap = new HashMap<>();
      entry.getValue().mappings().properties().forEach((key, value) -> {
        propertiesMap.put(key, serializeToMap(value));
      });
      Map<String, Object> sourceMap = Map.of("properties", propertiesMap);
      return new MappingMetadata("_doc", sourceMap);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> serializeToMap(JsonpSerializable value) {
    StringWriter writer = new StringWriter();
    JsonProvider provider = new JacksonJsonProvider();
    JsonGenerator generator = provider.createGenerator(writer);
    JsonpMapper mapper = new JacksonJsonpMapper();
    value.serialize(generator, mapper);
    generator.close();
    // Parse the JSON string back to a Map
    return new com.google.gson.Gson().fromJson(writer.toString(), Map.class);
  }

  @CheckForNull
  @SuppressWarnings("unchecked")
  private Map<String, Object> field(MappingMetadata mapping, String field) {
    Map<String, Object> props = (Map<String, Object>) mapping.getSourceAsMap().get("properties");
    return (Map<String, Object>) props.get(field);
  }

  private int countMappingFields(MappingMetadata mapping) {
    return ((Map) mapping.getSourceAsMap().get("properties")).size();
  }

  private IndexCreator run(IndexDefinition... definitions) {
    IndexDefinitions defs = new IndexDefinitions(definitions, new MapSettings().asConfig());
    defs.start();
    IndexCreator creator = new IndexCreator(es.client(), defs, metadataIndexDefinition, metadataIndex, esDbCompatibility);
    creator.start();
    return creator;
  }

  private void putFakeDocument() {
    es.putDocuments(FakeIndexDefinition.INDEX_TYPE, Map.of("key", "foo"));
  }

  private void verifyFakeIndexV1() {
    Map<String, MappingMetadata> mappings = mappings();
    MappingMetadata mapping = mappings.get("fakes");
    assertThat(mapping.getSourceAsMap()).isNotEmpty();
    assertThat(countMappingFields(mapping)).isEqualTo(2);
    assertThat(field(mapping, "updatedAt")).containsEntry("type", "date");
  }

  private static class FakeIndexDefinition implements IndexDefinition {
    static final IndexMainType INDEX_TYPE = main(Index.simple("fakes"), "fake");

    @Override
    public void define(IndexDefinitionContext context) {
      Index index = Index.simple("fakes");
      NewRegularIndex newIndex = context.create(index, SETTINGS_CONFIGURATION);
      newIndex.createTypeMapping(IndexType.main(index, "fake"))
        .keywordFieldBuilder("key").build()
        .createDateTimeField("updatedAt");
    }
  }

  private static class FakeIndexDefinitionV2 implements IndexDefinition {
    @Override
    public void define(IndexDefinitionContext context) {
      Index index = Index.simple("fakes");
      NewRegularIndex newIndex = context.create(index, SETTINGS_CONFIGURATION);
      newIndex.createTypeMapping(IndexType.main(index, "fake"))
        .keywordFieldBuilder("key").build()
        .createDateTimeField("updatedAt")
        .createIntegerField("newField");
    }
  }
}
