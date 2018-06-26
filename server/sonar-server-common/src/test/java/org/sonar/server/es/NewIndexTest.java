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
import java.util.Map;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assert.fail;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.SEARCH_REPLICAS;
import static org.sonar.server.es.NewIndex.SettingsConfiguration.newBuilder;

public class NewIndexTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings();
  private NewIndex.SettingsConfiguration defaultSettingsConfiguration = newBuilder(settings.asConfig()).build();

  @Test
  public void getName_returns_constructor_argument() {
    assertThat(new NewIndex("foo", defaultSettingsConfiguration).getName()).isEqualTo("foo");
  }

  @Test
  public void no_types_of_none_are_specified() {
    assertThat(new NewIndex("foo", defaultSettingsConfiguration).getTypes()).isEmpty();
  }

  @Test
  public void verify_default_index_settings_in_standalone() {
    Settings underTest = new NewIndex("issues", defaultSettingsConfiguration).getSettings().build();

    assertThat(underTest.get("index.number_of_shards")).isNotEmpty();
    assertThat(underTest.get("index.mapper.dynamic")).isEqualTo("false");
    assertThat(underTest.get("index.refresh_interval")).isEqualTo("30s");
    assertThat(underTest.get("index.number_of_shards")).isEqualTo("1");
    assertThat(underTest.get("index.number_of_replicas")).isEqualTo("0");
  }

  @Test
  public void verify_default_index_settings_in_cluster() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "true");
    Settings underTest = new NewIndex("issues", defaultSettingsConfiguration).getSettings().build();

    assertThat(underTest.get("index.number_of_shards")).isNotEmpty();
    assertThat(underTest.get("index.mapper.dynamic")).isEqualTo("false");
    assertThat(underTest.get("index.refresh_interval")).isEqualTo("30s");
    assertThat(underTest.get("index.number_of_shards")).isEqualTo("1");
    assertThat(underTest.get("index.number_of_replicas")).isEqualTo("1");
  }

  @Test
  public void index_name_is_lower_case() {
    try {
      new NewIndex("Issues", defaultSettingsConfiguration);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Index name must be lower-case: Issues");
    }
  }

  @Test
  public void define_fields() {
    NewIndex index = new NewIndex("issues", defaultSettingsConfiguration);
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
    NewIndex index = new NewIndex("issues", defaultSettingsConfiguration);
    NewIndex.NewIndexType mapping = index.createType("issue");
    mapping.keywordFieldBuilder("basic_field").build();
    mapping.keywordFieldBuilder("not_searchable_field").disableSearch().build();
    mapping.keywordFieldBuilder("all_capabilities_field")
      .addSubFields(
        DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER,
        DefaultIndexSettingsElement.SEARCH_WORDS_ANALYZER,
        DefaultIndexSettingsElement.SORTABLE_ANALYZER)
      .build();
    mapping.keywordFieldBuilder("dumb_text_storage")
      .disableSearch()
      .disableNorms()
      .disableSortingAndAggregating()
      .build();

    Map<String, Object> props = (Map) mapping.getProperty("basic_field");
    assertThat(props.get("type")).isEqualTo("keyword");
    assertThat(props.get("index")).isEqualTo("true");
    assertThat(props.get("fields")).isNull();

    props = (Map) mapping.getProperty("not_searchable_field");
    assertThat(props.get("type")).isEqualTo("keyword");
    assertThat(props.get("index")).isEqualTo("false");
    assertThat(props.get("norms")).isEqualTo("true");
    assertThat(props.get("store")).isEqualTo("false");
    assertThat(props.get("doc_values")).isEqualTo("true");
    assertThat(props.get("fields")).isNull();

    props = (Map) mapping.getProperty("all_capabilities_field");
    assertThat(props.get("type")).isEqualTo("keyword");
    // no need to test values, it's not the scope of this test
    assertThat((Map) props.get("fields")).isNotEmpty();

    props = (Map) mapping.getProperty("dumb_text_storage");
    assertThat(props.get("type")).isEqualTo("keyword");
    assertThat(props.get("index")).isEqualTo("false");
    assertThat(props.get("norms")).isEqualTo("false");
    assertThat(props.get("store")).isEqualTo("false");
    assertThat(props.get("doc_values")).isEqualTo("false");
    assertThat(props.get("fields")).isNull();
  }

  @Test
  public void define_nested_field() {
    NewIndex index = new NewIndex("projectmeasures", defaultSettingsConfiguration);
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

    NewIndex index = new NewIndex("projectmeasures", defaultSettingsConfiguration);
    NewIndex.NewIndexType mapping = index.createType("project_measures");

    mapping.nestedFieldBuilder("measures").build();
  }

  @Test
  public void use_doc_values_by_default() {
    NewIndex index = new NewIndex("issues", defaultSettingsConfiguration);
    NewIndex.NewIndexType mapping = index.createType("issue");
    mapping.keywordFieldBuilder("the_doc_value").build();

    Map<String, Object> props = (Map) mapping.getProperty("the_doc_value");
    assertThat(props.get("type")).isEqualTo("keyword");
    assertThat(props.get("doc_values")).isEqualTo("true");
  }

  @Test
  public void default_shards_and_replicas() {
    NewIndex index = new NewIndex("issues", newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo("5");
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  public void five_shards_and_one_replica_by_default_on_cluster() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "true");
    NewIndex index = new NewIndex("issues", newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo("5");
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("1");
  }

  @Test
  public void customize_number_of_shards() {
    settings.setProperty("sonar.search.issues.shards", "3");
    NewIndex index = new NewIndex("issues", newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo("3");
    // keep default value
    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  public void default_number_of_replicas_on_standalone_instance_must_be_0() {
    NewIndex index = new NewIndex("issues", newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  public void default_number_of_replicas_on_non_enabled_cluster_must_be_0() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "false");
    NewIndex index = new NewIndex("issues", newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  public void default_number_of_replicas_on_cluster_instance_must_be_1() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "true");
    NewIndex index = new NewIndex("issues", newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("1");
  }

  @Test
  public void when_number_of_replicas_on_cluster_is_specified_to_zero_default_value_must_not_be_used() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "true");
    settings.setProperty(SEARCH_REPLICAS.getKey(), "0");
    NewIndex index = new NewIndex("issues", newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  public void index_defined_with_specified_number_of_replicas_when_cluster_enabled() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "true");
    settings.setProperty(SEARCH_REPLICAS.getKey(), "3");
    NewIndex index = new NewIndex("issues", newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(index.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("3");
  }

  @Test
  public void fail_when_replica_customization_cant_be_parsed() {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "true");
    settings.setProperty(SEARCH_REPLICAS.getKey(), "ꝱꝲꝳପ");
    NewIndex.SettingsConfiguration settingsConfiguration = newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("The property 'sonar.search.replicas' is not an int value: For input string: \"ꝱꝲꝳପ\"");

    new NewIndex("issues", settingsConfiguration);
  }

  @Test
  public void in_standalone_searchReplicas_is_not_overridable() {
    settings.setProperty(SEARCH_REPLICAS.getKey(), "5");
    NewIndex index = new NewIndex("issues", defaultSettingsConfiguration);

    assertThat(index.getSettings().get("index.number_of_replicas")).isEqualTo("0");
  }

  @Test
  public void index_with_source() {
    NewIndex index = new NewIndex("issues", defaultSettingsConfiguration);
    NewIndex.NewIndexType mapping = index.createType("issue");
    mapping.setEnableSource(true);

    mapping = index.getTypes().get("issue");
    assertThat(mapping).isNotNull();
    assertThat(getAttributeAsMap(mapping, "_source")).containsExactly(entry("enabled", true));
  }

  @Test
  public void index_without_source() {
    NewIndex index = new NewIndex("issues", defaultSettingsConfiguration);
    NewIndex.NewIndexType mapping = index.createType("issue");
    mapping.setEnableSource(false);

    mapping = index.getTypes().get("issue");
    assertThat(mapping).isNotNull();
    assertThat(getAttributeAsMap(mapping, "_source")).containsExactly(entry("enabled", false));
  }

  @Test
  public void index_requires_project_authorization() {
    NewIndex index = new NewIndex("issues", defaultSettingsConfiguration);
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
