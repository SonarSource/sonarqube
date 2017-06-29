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
package org.sonar.server.es;

import java.io.IOException;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexCreatorTest {

  @Rule
  public EsTester es = new EsTester();

  @Test
  public void create_index() throws Exception {
    assertThat(mappings()).isEmpty();

    IndexDefinitions registry = new IndexDefinitions(new IndexDefinition[] {new FakeIndexDefinition()}, new MapSettings().asConfig());
    registry.start();
    IndexCreator creator = new IndexCreator(es.client(), registry);
    creator.start();

    // check that index is created with related mapping
    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappings();
    MappingMetaData mapping = mappings.get("fakes").get("fake");
    assertThat(mapping.type()).isEqualTo("fake");
    assertThat(mapping.getSourceAsMap()).isNotEmpty();
    assertThat(countMappingFields(mapping)).isEqualTo(2);
    assertThat(field(mapping, "updatedAt").get("type")).isEqualTo("date");

    assertThat(setting("fakes", "index.sonar_hash")).isNotEmpty();

    // of course do not delete indices on stop
    creator.stop();
    assertThat(mappings()).isNotEmpty();
  }

  @Test
  public void recreate_index_on_definition_changes() throws Exception {
    assertThat(mappings()).isEmpty();

    // v1
    IndexDefinitions registry = new IndexDefinitions(new IndexDefinition[] {new FakeIndexDefinition()}, new MapSettings().asConfig());
    registry.start();
    IndexCreator creator = new IndexCreator(es.client(), registry);
    creator.start();
    creator.stop();
    String hashV1 = setting("fakes", "index.sonar_hash");
    assertThat(hashV1).isNotEmpty();

    // v2
    registry = new IndexDefinitions(new IndexDefinition[] {new FakeIndexDefinitionV2()}, new MapSettings().asConfig());
    registry.start();
    creator = new IndexCreator(es.client(), registry);
    creator.start();
    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappings();
    MappingMetaData mapping = mappings.get("fakes").get("fake");
    assertThat(countMappingFields(mapping)).isEqualTo(3);
    assertThat(field(mapping, "updatedAt").get("type")).isEqualTo("date");
    assertThat(field(mapping, "newField").get("type")).isEqualTo("integer");
    String hashV2 = setting("fakes", "index.sonar_hash");
    assertThat(hashV2).isNotEqualTo(hashV1);
    creator.stop();
  }

  private String setting(String indexName, String settingKey) {
    GetSettingsResponse indexSettings = es.client().nativeClient().admin().indices().prepareGetSettings(indexName).get();
    return indexSettings.getSetting(indexName, settingKey);
  }

  private ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings() {
    return es.client().nativeClient().admin().indices().prepareGetMappings().get().mappings();
  }

  @CheckForNull
  private Map<String, Object> field(MappingMetaData mapping, String field) throws IOException {
    Map<String, Object> props = (Map) mapping.getSourceAsMap().get("properties");
    return (Map<String, Object>) props.get(field);
  }

  private int countMappingFields(MappingMetaData mapping) throws IOException {
    return ((Map) mapping.getSourceAsMap().get("properties")).size();
  }

  public static class FakeIndexDefinition implements IndexDefinition {
    @Override
    public void define(IndexDefinitionContext context) {
      NewIndex index = context.create("fakes");
      NewIndex.NewIndexType mapping = index.createType("fake");
      mapping.stringFieldBuilder("key").build();
      mapping.createDateTimeField("updatedAt");
    }
  }

  public static class FakeIndexDefinitionV2 implements IndexDefinition {
    @Override
    public void define(IndexDefinitionContext context) {
      NewIndex index = context.create("fakes");
      NewIndex.NewIndexType mapping = index.createType("fake");
      mapping.stringFieldBuilder("key").build();
      mapping.createDateTimeField("updatedAt");
      mapping.createIntegerField("newField");
    }
  }
}
