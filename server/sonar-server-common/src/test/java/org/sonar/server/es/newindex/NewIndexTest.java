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
package org.sonar.server.es.newindex;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Map;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexType.IndexMainType;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.SEARCH_REPLICAS;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

@RunWith(DataProviderRunner.class)
public class NewIndexTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String someIndexName = randomAlphabetic(5).toLowerCase();
  private MapSettings settings = new MapSettings();
  private SettingsConfiguration defaultSettingsConfiguration = newBuilder(settings.asConfig()).build();

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void getRelations_returns_empty_if_no_relation_added(Index index) {
    NewIndex<?> newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), defaultSettingsConfiguration);

    assertThat(newIndex.getRelations()).isEmpty();
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void does_not_enable_all_field(Index index) {
    SimplestNewIndex newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), defaultSettingsConfiguration);

    // _all field is deprecated in 6.X and will be removed in 7.x and should not be used
    assertThat(newIndex.getAttributes().get("_all")).isNull();
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void verify_default_index_settings_in_standalone(Index index) {
    Settings underTest = new SimplestNewIndex(IndexType.main(index, "foo"), defaultSettingsConfiguration)
      .getSettings().build();

    assertThat(underTest.get("index.number_of_shards")).isNotEmpty();
    // index.mapper.dynamic is deprecated and should not be set anymore
    assertThat(underTest.get("index.mapper.dynamic")).isNull();
    assertThat(underTest.get("index.refresh_interval")).isEqualTo("30s");
    // setting "mapping.single_type" has been dropped in 6.X because multi type indices are not supported anymore
    assertThat(underTest.get("mapping.single_type")).isNull();
    assertThat(underTest.get("index.number_of_shards")).isEqualTo("1");
    assertThat(underTest.get("index.number_of_replicas")).isEqualTo("0");
    assertThat(underTest.get("index.max_ngram_diff")).isEqualTo("13");
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void verify_default_index_settings_in_cluster(Index index) {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "true");
    Settings underTest = new SimplestNewIndex(IndexType.main(index, "foo"), defaultSettingsConfiguration).getSettings().build();

    // _all field is deprecated in ES 6.X and will be removed in 7.X
    assertThat(underTest.get("_all")).isNull();
    assertThat(underTest.get("index.number_of_shards")).isNotEmpty();
    assertThat(underTest.get("index.refresh_interval")).isEqualTo("30s");
    // setting "mapping.single_type" has been dropped in 6.X because multi type indices are not supported anymore
    assertThat(underTest.get("mapping.single_type")).isNull();
    assertThat(underTest.get("index.number_of_shards")).isEqualTo("1");
    assertThat(underTest.get("index.number_of_replicas")).isEqualTo("1");
    assertThat(underTest.get("index.max_ngram_diff")).isEqualTo("13");
  }

  @Test
  @UseDataProvider("indexAndTypeMappings")
  public void define_fields(NewIndex<?> newIndex, TypeMapping typeMapping) {
    typeMapping.setField("foo_field", ImmutableMap.of("type", "keyword"));
    typeMapping.createBooleanField("boolean_field");
    typeMapping.createByteField("byte_field");
    typeMapping.createDateTimeField("dt_field");
    typeMapping.createDoubleField("double_field");
    typeMapping.createIntegerField("int_field");
    typeMapping.createLongField("long_field");
    typeMapping.createShortField("short_field");
    typeMapping.createUuidPathField("uuid_path_field");

    assertThat(newIndex.getProperty("foo_field")).isInstanceOf(Map.class);
    assertThat((Map) newIndex.getProperty("foo_field")).containsEntry("type", "keyword");
    assertThat((Map) newIndex.getProperty("byte_field")).isNotEmpty();
    assertThat((Map) newIndex.getProperty("double_field")).isNotEmpty();
    assertThat((Map) newIndex.getProperty("dt_field")).isNotEmpty();
    assertThat((Map) newIndex.getProperty("int_field")).containsEntry("type", "integer");
    assertThat((Map) newIndex.getProperty("long_field")).isNotEmpty();
    assertThat((Map) newIndex.getProperty("short_field")).isNotEmpty();
    assertThat((Map) newIndex.getProperty("uuid_path_field")).isNotEmpty();
    assertThat((Map) newIndex.getProperty("unknown")).isNull();
  }

  @Test
  @UseDataProvider("indexAndTypeMappings")
  public void define_string_field(NewIndex<?> newIndex, TypeMapping typeMapping) {
    typeMapping.keywordFieldBuilder("basic_field").build();
    typeMapping.keywordFieldBuilder("not_searchable_field").disableSearch().build();
    typeMapping.keywordFieldBuilder("all_capabilities_field")
      .addSubFields(
        DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER,
        DefaultIndexSettingsElement.SEARCH_WORDS_ANALYZER,
        DefaultIndexSettingsElement.SORTABLE_ANALYZER)
      .build();
    typeMapping.keywordFieldBuilder("dumb_text_storage")
      .disableSearch()
      .disableNorms()
      .disableSortingAndAggregating()
      .build();

    Map<String, Object> props = (Map) newIndex.getProperty("basic_field");
    assertThat(props.get("type")).isEqualTo("keyword");
    assertThat(props.get("index")).isEqualTo("true");
    assertThat(props.get("fields")).isNull();

    props = (Map) newIndex.getProperty("not_searchable_field");
    assertThat(props.get("type")).isEqualTo("keyword");
    assertThat(props.get("index")).isEqualTo("false");
    assertThat(props.get("norms")).isEqualTo("true");
    assertThat(props.get("store")).isEqualTo("false");
    assertThat(props.get("doc_values")).isEqualTo("true");
    assertThat(props.get("fields")).isNull();

    props = (Map) newIndex.getProperty("all_capabilities_field");
    assertThat(props.get("type")).isEqualTo("keyword");
    // no need to test values, it's not the scope of this test
    assertThat((Map) props.get("fields")).isNotEmpty();

    props = (Map) newIndex.getProperty("dumb_text_storage");
    assertThat(props.get("type")).isEqualTo("keyword");
    assertThat(props.get("index")).isEqualTo("false");
    assertThat(props.get("norms")).isEqualTo("false");
    assertThat(props.get("store")).isEqualTo("false");
    assertThat(props.get("doc_values")).isEqualTo("false");
    assertThat(props.get("fields")).isNull();
  }

  @Test
  @UseDataProvider("indexAndTypeMappings")
  public void define_nested_field(NewIndex<?> newIndex, TypeMapping typeMapping) {
    typeMapping.nestedFieldBuilder("measures")
      .addKeywordField("key")
      .addDoubleField("value")
      .build();
    Map<String, Object> result = (Map) newIndex.getProperty("measures");

    assertThat(result.get("type")).isEqualTo("nested");
    Map<String, Map<String, Object>> subProperties = (Map) result.get("properties");
    assertThat(subProperties.get("key").get("type")).isEqualTo("keyword");
    assertThat(subProperties.get("value").get("type")).isEqualTo("double");
  }

  @Test
  @UseDataProvider("indexAndTypeMappings")
  public void fail_when_nested_with_no_field(NewIndex<?> newIndex, TypeMapping typeMapping) {
    NestedFieldBuilder<TypeMapping> nestedFieldBuilder = typeMapping.nestedFieldBuilder("measures");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("At least one sub-field must be declared in nested property 'measures'");

    nestedFieldBuilder.build();
  }

  @Test
  @UseDataProvider("indexAndTypeMappings")
  public void use_doc_values_by_default(NewIndex<?> newIndex, TypeMapping typeMapping) {
    typeMapping.keywordFieldBuilder("the_doc_value").build();

    Map<String, Object> props = (Map) newIndex.getProperty("the_doc_value");
    assertThat(props.get("type")).isEqualTo("keyword");
    assertThat(props.get("doc_values")).isEqualTo("true");
  }

  @DataProvider
  public static Object[][] indexAndTypeMappings() {
    String indexName = randomAlphabetic(5).toLowerCase();
    MapSettings settings = new MapSettings();
    SettingsConfiguration defaultSettingsConfiguration = newBuilder(settings.asConfig()).build();
    Index index = Index.withRelations(indexName);
    IndexMainType mainType = IndexType.main(index, "foo");
    SimplestNewIndex newIndex = new SimplestNewIndex(mainType, defaultSettingsConfiguration);
    TypeMapping mainMapping = newIndex.createTypeMapping(mainType);
    TypeMapping relationMapping = newIndex.createTypeMapping(IndexType.relation(mainType, "bar"));
    return new Object[][] {
      {newIndex, mainMapping},
      {newIndex, relationMapping},
    };
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void default_shards_and_replicas(Index index) {
    NewIndex newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(newIndex.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo("5");
    assertThat(newIndex.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void five_shards_and_one_replica_by_default_on_cluster(Index index) {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "true");
    NewIndex newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(newIndex.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo("5");
    assertThat(newIndex.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("1");
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void customize_number_of_shards(Index index) {
    settings.setProperty("sonar.search." + index.getName() + ".shards", "3");
    NewIndex newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(newIndex.getSetting(IndexMetaData.SETTING_NUMBER_OF_SHARDS)).isEqualTo("3");
    // keep default value
    assertThat(newIndex.getSetting(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void default_number_of_replicas_on_standalone_instance_must_be_0(Index index) {
    NewIndex newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(newIndex.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void default_number_of_replicas_on_non_enabled_cluster_must_be_0(Index index) {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "false");
    NewIndex newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(newIndex.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void default_number_of_replicas_on_cluster_instance_must_be_1(Index index) {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "true");
    NewIndex newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(newIndex.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("1");
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void when_number_of_replicas_on_cluster_is_specified_to_zero_default_value_must_not_be_used(Index index) {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "true");
    settings.setProperty(SEARCH_REPLICAS.getKey(), "0");
    NewIndex newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(newIndex.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("0");
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void index_defined_with_specified_number_of_replicas_when_cluster_enabled(Index index) {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "true");
    settings.setProperty(SEARCH_REPLICAS.getKey(), "3");
    NewIndex newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build());

    assertThat(newIndex.getSettings().get(IndexMetaData.SETTING_NUMBER_OF_REPLICAS)).isEqualTo("3");
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void fail_when_replica_customization_cant_be_parsed(Index index) {
    settings.setProperty(CLUSTER_ENABLED.getKey(), "true");
    settings.setProperty(SEARCH_REPLICAS.getKey(), "ꝱꝲꝳପ");
    SettingsConfiguration settingsConfiguration = newBuilder(settings.asConfig()).setDefaultNbOfShards(5).build();
    IndexMainType mainType = IndexType.main(index, "foo");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("The property 'sonar.search.replicas' is not an int value: For input string: \"ꝱꝲꝳପ\"");

    new SimplestNewIndex(mainType, settingsConfiguration);
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void in_standalone_searchReplicas_is_not_overridable(Index index) {
    settings.setProperty(SEARCH_REPLICAS.getKey(), "5");
    NewIndex newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), defaultSettingsConfiguration);

    assertThat(newIndex.getSettings().get("index.number_of_replicas")).isEqualTo("0");
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void index_with_source(Index index) {
    NewIndex newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), defaultSettingsConfiguration);
    newIndex.setEnableSource(true);

    assertThat(newIndex).isNotNull();
    assertThat(getAttributeAsMap(newIndex, "_source")).containsExactly(entry("enabled", true));
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void index_without_source(Index index) {
    NewIndex newIndex = new SimplestNewIndex(IndexType.main(index, "foo"), defaultSettingsConfiguration);
    newIndex.setEnableSource(false);

    assertThat(getAttributeAsMap(newIndex, "_source")).containsExactly(entry("enabled", false));
  }

  @Test
  public void createTypeMapping_with_IndexRelationType_fails_with_ISE_if_index_does_not_allow_relations() {
    IndexType.IndexRelationType indexRelationType = IndexType.relation(IndexType.main(Index.withRelations(someIndexName), "bar"), "bar");

    Index index = Index.simple(someIndexName);
    IndexMainType mainType = IndexType.main(index, "foo");
    NewIndex underTest = new NewIndex(index, defaultSettingsConfiguration) {
      @Override
      public IndexMainType getMainType() {
        return mainType;
      }

      @Override
      public BuiltIndex build() {
        throw new UnsupportedOperationException("build not implemented");
      }
    };

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Index is not configured to accept relations. Update IndexDefinition.Descriptor instance for this index");

    underTest.createTypeMapping(indexRelationType);
  }

  @DataProvider
  public static Object[][] indexWithAndWithoutRelations() {
    return new Object[][] {
      {Index.simple(someIndexName)},
      {Index.withRelations(someIndexName)}
    };
  }

  private static Map<String, Object> getAttributeAsMap(NewIndex newIndex, String attributeKey) {
    return (Map<String, Object>) newIndex.getAttributes().get(attributeKey);
  }

  private static final class SimplestNewIndex extends NewIndex<SimplestNewIndex> {
    private final IndexMainType mainType;

    public SimplestNewIndex(IndexMainType mainType, SettingsConfiguration settingsConfiguration) {
      super(mainType.getIndex(), settingsConfiguration);
      this.mainType = mainType;
    }

    @Override
    public IndexMainType getMainType() {
      return mainType;
    }

    @Override
    public BuiltIndex<SimplestNewIndex> build() {
      return new BuiltIndex<>(this);
    }
  }
}
