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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.process.ProcessProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assert.fail;

public class NewIndexTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
    mapping.setProperty("foo_field", ImmutableMap.of("type", "keyword"));
    mapping.createBooleanField("boolean_field");
    mapping.createByteField("byte_field");
    mapping.createDateTimeField("dt_field");
    mapping.createDoubleField("double_field");
    mapping.createIntegerField("int_field");
    mapping.createLongField("long_field");
    mapping.createShortField("short_field");
    mapping.createUuidPathField("uuid_path_field");

    mapping = index.getTypes().get("issue");
    assertThat(mapping).isNotNull();
    assertThat(mapping.getAttributes().get("dynamic")).isEqualTo("true");
    assertThat(mapping.getProperty("foo_field")).isInstanceOf(Map.class);
    assertThat((Map) mapping.getProperty("foo_field")).containsEntry("type", "keyword");
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
    mapping.keywordFieldBuilder("basic_field").build();
    mapping.keywordFieldBuilder("not_searchable_field").disableSearch().build();
    mapping.keywordFieldBuilder("all_capabilities_field")
      .addSubFields(
        DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER,
        DefaultIndexSettingsElement.SEARCH_WORDS_ANALYZER,
        DefaultIndexSettingsElement.SORTABLE_ANALYZER)
      .build();

    Map<String, Object> props = (Map) mapping.getProperty("basic_field");
    assertThat(props.get("type")).isEqualTo("keyword");
    assertThat(props.get("index")).isEqualTo("true");
    assertThat(props.get("fields")).isNull();

    props = (Map) mapping.getProperty("not_searchable_field");
    assertThat(props.get("type")).isEqualTo("keyword");
    assertThat(props.get("index")).isEqualTo("false");
    assertThat(props.get("fields")).isNull();

    props = (Map) mapping.getProperty("all_capabilities_field");
    assertThat(props.get("type")).isEqualTo("multi_field");
    // no need to test values, it's not the scope of this test
    assertThat((Map) props.get("fields")).isNotEmpty();
  }

  @Test
  public void define_nested_field() {
    NewIndex index = new NewIndex("projectmeasures");
    NewIndex.NewIndexType mapping = index.createType("projectmeasures");

    mapping.nestedFieldBuilder("measures")
      .addKeywordField("key")
      .addDoubleField("value")
      .build();
    Map<String, Object> result = (Map) mapping.getProperty("measures");

    assertThat(result.get("type")).isEqualTo("nested");
    Map<String, Map<String, Object>> subProperties = (Map) result.get("properties");
    assertThat(subProperties.get("key").get("type")).isEqualTo("keyword");
    assertThat(subProperties.get("value").get("type")).isEqualTo("double");
  }

  @Test
  public void fail_when_nested_with_no_field() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("At least one sub-field must be declared in nested property 'measures'");

    NewIndex index = new NewIndex("projectmeasures");
    NewIndex.NewIndexType mapping = index.createType("project_measures");

    mapping.nestedFieldBuilder("measures").build();
  }

  @Test
  public void use_default_doc_values() {
    NewIndex index = new NewIndex("issues");
    NewIndex.NewIndexType mapping = index.createType("issue");
    mapping.keywordFieldBuilder("the_doc_value").build();

    Map<String, Object> props = (Map) mapping.getProperty("the_doc_value");
    assertThat(props.get("type")).isEqualTo("keyword");
    assertThat(props.get("doc_values")).isNull();
  }

  @Test
  public void default_shards_and_replicas() {
    NewIndex index = new NewIndex("issues");
    index.configureShards(new MapSettings().asConfig(), 5);
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo("5");
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  public void five_shards_and_one_replica_by_default_on_cluster() {
    NewIndex index = new NewIndex("issues");
    MapSettings settings = new MapSettings();
    settings.setProperty(ProcessProperties.CLUSTER_ENABLED, "true");
    index.configureShards(settings.asConfig(), 5);
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo("5");
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("1");
  }

  @Test
  public void customize_number_of_shards() {
    NewIndex index = new NewIndex("issues");
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.search.issues.shards", "3");
    index.configureShards(settings.asConfig(), 5);
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo("3");
    // keep default value
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  public void default_number_of_replicas_on_standalone_instance_must_be_0() {
    NewIndex index = new NewIndex("issues");
    MapSettings settings = new MapSettings();
    index.configureShards(settings.asConfig(), 5);
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  public void default_number_of_replicas_on_non_enabled_cluster_must_be_0() {
    NewIndex index = new NewIndex("issues");
    MapSettings settings = new MapSettings();
    settings.setProperty(ProcessProperties.CLUSTER_ENABLED, "false");
    index.configureShards(settings.asConfig(), 5);
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  public void default_number_of_replicas_on_cluster_instance_must_be_1() {
    NewIndex index = new NewIndex("issues");
    MapSettings settings = new MapSettings();
    settings.setProperty(ProcessProperties.CLUSTER_ENABLED, "true");
    index.configureShards(settings.asConfig(), 5);
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("1");
  }

  @Test
  public void when_number_of_replicas_on_cluster_is_specified_to_zero_default_value_must_not_be_used() {
    NewIndex index = new NewIndex("issues");
    MapSettings settings = new MapSettings();
    settings.setProperty(ProcessProperties.CLUSTER_ENABLED, "true");
    settings.setProperty(ProcessProperties.SEARCH_REPLICAS, "0");
    index.configureShards(settings.asConfig(), 5);
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  public void customize_number_of_replicas() {
    NewIndex index = new NewIndex("issues");
    MapSettings settings = new MapSettings();
    settings.setProperty(ProcessProperties.SEARCH_REPLICAS, "3");
    index.configureShards(settings.asConfig(), 5);
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("3");
  }

  @Test
  public void index_with_source() {
    NewIndex index = new NewIndex("issues");
    NewIndex.NewIndexType mapping = index.createType("issue");
    mapping.setEnableSource(true);

    mapping = index.getTypes().get("issue");
    assertThat(mapping).isNotNull();
    assertThat(getAttributeAsMap(mapping, "_source")).containsExactly(entry("enabled", true));
  }

  @Test
  public void index_without_source() {
    NewIndex index = new NewIndex("issues");
    NewIndex.NewIndexType mapping = index.createType("issue");
    mapping.setEnableSource(false);

    mapping = index.getTypes().get("issue");
    assertThat(mapping).isNotNull();
    assertThat(getAttributeAsMap(mapping, "_source")).containsExactly(entry("enabled", false));
  }

  @Test
  public void index_requires_project_authorization() {
    NewIndex index = new NewIndex("issues");
    index.createType("issue")
      // creates a second type "authorization" and configures _parent and _routing fields
      .requireProjectAuthorization();

    // issue type
    NewIndex.NewIndexType issueType = index.getTypes().get("issue");
    assertThat(getAttributeAsMap(issueType, "_parent")).containsExactly(entry("type", "authorization"));
    assertThat(getAttributeAsMap(issueType, "_routing")).containsExactly(entry("required", true));

    // authorization type
    NewIndex.NewIndexType authorizationType = index.getTypes().get("authorization");
    assertThat(getAttributeAsMap(authorizationType, "_parent")).isNull();
    assertThat(getAttributeAsMap(authorizationType, "_routing")).containsExactly(entry("required", true));
  }

  private static Map<String, Object> getAttributeAsMap(NewIndex.NewIndexType type, String attributeKey) {
    return (Map<String, Object>) type.getAttributes().get(attributeKey);
  }
}
