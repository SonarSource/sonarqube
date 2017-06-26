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
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.es.metadata.MetadataIndexDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class IndexCreatorTest {

  @Rule
  public EsTester es = new EsTester();
  private MetadataIndexDefinition metadataIndexDefinition = new MetadataIndexDefinition(new MapSettings().asConfig());
  private MetadataIndex metadataIndex = new MetadataIndex(es.client());

  @Test
  public void create_index() throws Exception {
    assertThat(mappings()).isEmpty();

    IndexDefinitions registry = new IndexDefinitions(new IndexDefinition[] {new FakeIndexDefinition()}, new MapSettings().asConfig());
    registry.start();
    IndexCreator creator = new IndexCreator(es.client(), registry, metadataIndexDefinition, metadataIndex);
    creator.start();

    // check that index is created with related mapping
    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappings();
    MappingMetaData mapping = mappings.get("fakes").get("fake");
    assertThat(mapping.type()).isEqualTo("fake");
    assertThat(mapping.getSourceAsMap()).isNotEmpty();
    assertThat(countMappingFields(mapping)).isEqualTo(2);
    assertThat(field(mapping, "updatedAt").get("type")).isEqualTo("date");

    // of course do not delete indices on stop
    creator.stop();
    assertThat(mappings()).isNotEmpty();
  }

  @Test
  public void mark_all_non_existing_index_types_as_uninitialized() throws Exception {
    MetadataIndex metadataIndexMock = mock(MetadataIndex.class);
    IndexDefinitions registry = new IndexDefinitions(new IndexDefinition[] {context -> {
      NewIndex i = context.create("i");
      i.createType("t1");
      i.createType("t2");
    }}, new MapSettings().asConfig());
    registry.start();
    IndexCreator creator = new IndexCreator(es.client(), registry, metadataIndexDefinition, metadataIndexMock);
    creator.start();

    verify(metadataIndexMock).setHash(eq("i"), anyString());
    verify(metadataIndexMock).setInitialized(eq(new IndexType("i", "t1")), eq(false));
    verify(metadataIndexMock).setInitialized(eq(new IndexType("i", "t2")), eq(false));
    verifyNoMoreInteractions(metadataIndexMock);
  }

  @Test
  public void recreate_index_on_definition_changes() throws Exception {
    assertThat(mappings()).isEmpty();

    // v1
    IndexDefinitions registry = new IndexDefinitions(new IndexDefinition[] {new FakeIndexDefinition()}, new MapSettings().asConfig());
    registry.start();
    IndexCreator creator = new IndexCreator(es.client(), registry, metadataIndexDefinition, metadataIndex);
    creator.start();
    creator.stop();

    IndexType fakeIndexType = new IndexType("fakes", "fake");
    String id = "1";
    es.client().prepareIndex(fakeIndexType).setId(id).setSource(new FakeDoc().getFields()).setRefresh(true).get();
    assertThat(es.client().prepareGet(fakeIndexType, id).get().isExists()).isTrue();

    // v2
    registry = new IndexDefinitions(new IndexDefinition[] {new FakeIndexDefinitionV2()}, new MapSettings().asConfig());
    registry.start();
    creator = new IndexCreator(es.client(), registry, metadataIndexDefinition, metadataIndex);
    creator.start();
    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = mappings();
    MappingMetaData mapping = mappings.get("fakes").get("fake");
    assertThat(countMappingFields(mapping)).isEqualTo(3);
    assertThat(field(mapping, "updatedAt").get("type")).isEqualTo("date");
    assertThat(field(mapping, "newField").get("type")).isEqualTo("integer");
    creator.stop();

    assertThat(es.client().prepareGet(fakeIndexType, id).get().isExists()).isFalse();
  }

  @Test
  public void do_not_recreate_index_on_unchanged_definition() throws Exception {
    assertThat(mappings()).isEmpty();

    // v1
    IndexDefinitions registry = new IndexDefinitions(new IndexDefinition[] {new FakeIndexDefinition()}, new MapSettings().asConfig());
    registry.start();
    IndexCreator creator = new IndexCreator(es.client(), registry, metadataIndexDefinition, metadataIndex);
    creator.start();
    creator.stop();

    IndexType fakeIndexType = new IndexType("fakes", "fake");
    String id = "1";
    es.client().prepareIndex(fakeIndexType).setId(id).setSource(new FakeDoc().getFields()).setRefresh(true).get();
    assertThat(es.client().prepareGet(fakeIndexType, id).get().isExists()).isTrue();

    // v1
    registry = new IndexDefinitions(new IndexDefinition[] {new FakeIndexDefinition()}, new MapSettings().asConfig());
    registry.start();
    creator = new IndexCreator(es.client(), registry, metadataIndexDefinition, metadataIndex);
    creator.start();
    creator.stop();

    assertThat(es.client().prepareGet(fakeIndexType, id).get().isExists()).isTrue();
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
