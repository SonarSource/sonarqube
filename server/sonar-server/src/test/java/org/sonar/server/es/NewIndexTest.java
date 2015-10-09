/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import java.util.Map;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class NewIndexTest {

  @Test
  public void most_basic_index() {
    NewIndex index = new NewIndex("issues");
    assertThat(index.getName()).isEqualTo("issues");
    assertThat(index.getTypes()).isEmpty();
    Settings settings = index.getSettings().build();
    // test some basic settings
    assertThat(settings.get("index.number_of_shards")).isNotEmpty();
  }

  @Test
  public void index_name_is_lower_case() {
    try {
      new NewIndex("Issues");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Index name must be lower-case: Issues");
    }
  }

  @Test
  public void define_fields() {
    NewIndex index = new NewIndex("issues");
    NewIndex.NewIndexType mapping = index.createType("issue");
    mapping.setAttribute("dynamic", "true");
    mapping.setProperty("foo_field", ImmutableMap.of("type", "string"));
    mapping.createBooleanField("boolean_field");
    mapping.createByteField("byte_field");
    mapping.createDateTimeField("dt_field");
    mapping.createDoubleField("double_field");
    mapping.createIntegerField("int_field");
    mapping.createLongField("long_field");
    mapping.createShortField("short_field");
    mapping.createUuidPathField("uuid_path_field");

    mapping = index.getTypes().get("issue");
    assertThat(mapping.getAttributes().get("dynamic")).isEqualTo("true");
    assertThat(mapping).isNotNull();
    assertThat(mapping.getProperty("foo_field")).isInstanceOf(Map.class);
    assertThat((Map) mapping.getProperty("foo_field")).containsEntry("type", "string");
    assertThat((Map) mapping.getProperty("byte_field")).isNotEmpty();
    assertThat((Map) mapping.getProperty("double_field")).isNotEmpty();
    assertThat((Map) mapping.getProperty("dt_field")).isNotEmpty();
    assertThat((Map) mapping.getProperty("int_field")).containsEntry("type", "integer");
    assertThat((Map) mapping.getProperty("long_field")).isNotEmpty();
    assertThat((Map) mapping.getProperty("short_field")).isNotEmpty();
    assertThat((Map) mapping.getProperty("uuid_path_field")).isNotEmpty();
    assertThat((Map) mapping.getProperty("unknown")).isNull();
  }

  @Test
  public void define_string_field() {
    NewIndex index = new NewIndex("issues");
    NewIndex.NewIndexType mapping = index.createType("issue");
    mapping.stringFieldBuilder("basic_field").build();
    mapping.stringFieldBuilder("not_searchable_field").disableSearch().build();
    mapping.stringFieldBuilder("all_capabilities_field")
      .enableGramSearch()
      .enableWordSearch()
      .enableSorting()
      .build();

    Map<String, Object> props = (Map) mapping.getProperty("basic_field");
    assertThat(props.get("type")).isEqualTo("string");
    assertThat(props.get("index")).isEqualTo("not_analyzed");
    assertThat(props.get("fields")).isNull();

    props = (Map) mapping.getProperty("not_searchable_field");
    assertThat(props.get("type")).isEqualTo("string");
    assertThat(props.get("index")).isEqualTo("no");
    assertThat(props.get("fields")).isNull();

    props = (Map) mapping.getProperty("all_capabilities_field");
    assertThat(props.get("type")).isEqualTo("multi_field");
    // no need to test values, it's not the scope of this test
    assertThat((Map) props.get("fields")).isNotEmpty();
  }

  @Test
  public void string_doc_values() {
    NewIndex index = new NewIndex("issues");
    NewIndex.NewIndexType mapping = index.createType("issue");
    mapping.stringFieldBuilder("the_doc_value").docValues().build();

    Map<String, Object> props = (Map) mapping.getProperty("the_doc_value");
    assertThat(props.get("type")).isEqualTo("string");
    assertThat(props.get("index")).isEqualTo("not_analyzed");
    assertThat(props.get("doc_values")).isEqualTo(Boolean.TRUE);
    assertThat(props.get("fields")).isNull();
  }

  @Test
  public void analyzed_strings_must_not_be_doc_values() {
    NewIndex index = new NewIndex("issues");
    NewIndex.NewIndexType mapping = index.createType("issue");
    try {
      mapping.stringFieldBuilder("the_doc_value").enableGramSearch().docValues().build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Doc values are not supported on analyzed strings of field: the_doc_value");
    }
  }

  @Test
  public void do_not_disable_search_on_searchable_fields() {
    NewIndex index = new NewIndex("issues");
    NewIndex.NewIndexType mapping = index.createType("issue");
    try {
      mapping.stringFieldBuilder("my_field").enableGramSearch().disableSearch().build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Can't mix searchable and non-searchable arguments on field: my_field");
    }
  }

  @Test
  public void default_shards_and_replicas() {
    NewIndex index = new NewIndex("issues");
    index.setShards(new org.sonar.api.config.Settings());
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo(String.valueOf(NewIndex.DEFAULT_NUMBER_OF_SHARDS));
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  public void five_shards_and_one_replica_by_default_on_cluster() {
    NewIndex index = new NewIndex("issues");
    org.sonar.api.config.Settings settings = new org.sonar.api.config.Settings();
    settings.setProperty("sonar.cluster.activate", "true");
    index.setShards(settings);
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo(String.valueOf(NewIndex.DEFAULT_NUMBER_OF_SHARDS));
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("1");
  }

  @Test
  public void customize_number_of_shards() {
    NewIndex index = new NewIndex("issues");
    org.sonar.api.config.Settings settings = new org.sonar.api.config.Settings();
    settings.setProperty("sonar.search.issues.shards", "3");
    index.setShards(settings);
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo("3");
    // keep default value
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  public void customize_number_of_shards_and_replicas() {
    NewIndex index = new NewIndex("issues");
    org.sonar.api.config.Settings settings = new org.sonar.api.config.Settings();
    settings.setProperty("sonar.search.issues.shards", "3");
    settings.setProperty("sonar.search.issues.replicas", "1");
    index.setShards(settings);
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo("3");
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("1");
  }
}
