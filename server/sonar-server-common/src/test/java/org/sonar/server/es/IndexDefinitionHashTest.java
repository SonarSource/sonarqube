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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.newindex.SettingsConfiguration;
import org.sonar.server.es.newindex.TestNewIndex;
import org.sonar.server.es.newindex.TypeMapping;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;

public class IndexDefinitionHashTest {
  private final SettingsConfiguration settingsConfiguration = settingsConfigurationOf(new MapSettings());

  @Test
  public void hash_changes_if_mainType_is_different() {
    Index simpleIndex = Index.simple("foo");
    Index withRelationsIndex = Index.withRelations("foo");
    IndexMainType mainTypeBar = IndexMainType.main(simpleIndex, "bar");
    TestNewIndex indexSimpleBar = new TestNewIndex(mainTypeBar, settingsConfiguration);
    TestNewIndex indexSimpleDonut = new TestNewIndex(IndexMainType.main(simpleIndex, "donut"), settingsConfiguration);
    TestNewIndex indexWithRelationsBar = new TestNewIndex(IndexMainType.main(withRelationsIndex, "bar"), settingsConfiguration);

    assertThat(hashOf(indexSimpleBar))
      .isEqualTo(hashOf(new TestNewIndex(mainTypeBar, settingsConfiguration)))
      .isNotEqualTo(hashOf(indexSimpleDonut))
      .isNotEqualTo(hashOf(indexWithRelationsBar));
    assertThat(hashOf(indexSimpleDonut))
      .isNotEqualTo(hashOf(indexWithRelationsBar));
  }

  @Test
  public void hash_changes_if_relations_are_different() {
    Index index = Index.withRelations("foo");
    IndexMainType mainType = IndexMainType.main(index, "bar");
    TestNewIndex indexNoRelation = new TestNewIndex(mainType, settingsConfiguration);
    TestNewIndex indexOneRelation = new TestNewIndex(mainType, settingsConfiguration)
      .addRelation("donut1");
    TestNewIndex indexOneOtherRelation = new TestNewIndex(mainType, settingsConfiguration)
      .addRelation("donut2");
    TestNewIndex indexTwoRelations = new TestNewIndex(mainType, settingsConfiguration)
      .addRelation("donut1")
      .addRelation("donut2");
    TestNewIndex indexTwoOtherRelations = new TestNewIndex(mainType, settingsConfiguration)
      .addRelation("donut1")
      .addRelation("donut3");

    assertThat(hashOf(indexNoRelation))
      .isEqualTo(hashOf(new TestNewIndex(mainType, settingsConfiguration)))
      .isNotEqualTo(hashOf(indexOneRelation))
      .isNotEqualTo(hashOf(indexOneOtherRelation))
      .isNotEqualTo(hashOf(indexTwoRelations))
      .isNotEqualTo(hashOf(indexTwoOtherRelations));
    assertThat(hashOf(indexOneRelation))
      .isEqualTo(hashOf(new TestNewIndex(mainType, settingsConfiguration).addRelation("donut1")))
      .isNotEqualTo(hashOf(indexOneOtherRelation))
      .isNotEqualTo(hashOf(indexTwoRelations))
      .isNotEqualTo(hashOf(indexTwoOtherRelations));
    assertThat(hashOf(indexTwoRelations))
      .isEqualTo(hashOf(new TestNewIndex(mainType, settingsConfiguration)
        .addRelation("donut1")
        .addRelation("donut2")))
      .isNotEqualTo(hashOf(indexOneRelation))
      .isNotEqualTo(hashOf(indexOneOtherRelation))
      .isNotEqualTo(hashOf(indexTwoOtherRelations));
  }

  @Test
  public void hash_is_the_same_if_only_relations_order_changes() {
    Index index = Index.withRelations("foo");
    IndexMainType mainType = IndexMainType.main(index, "bar");
    TestNewIndex indexTwoRelations = new TestNewIndex(mainType, settingsConfiguration)
      .addRelation("donut1")
      .addRelation("donut2")
      .addRelation("donut3");
    TestNewIndex indexTwoRelationsOtherOrder = new TestNewIndex(mainType, settingsConfiguration)
      .addRelation("donut2")
      .addRelation("donut1")
      .addRelation("donut3");
    TestNewIndex indexTwoRelationsOtherOrder2 = new TestNewIndex(mainType, settingsConfiguration)
      .addRelation("donut2")
      .addRelation("donut3")
      .addRelation("donut1");

    assertThat(hashOf(indexTwoRelations))
      .isEqualTo(hashOf(indexTwoRelationsOtherOrder))
      .isEqualTo(hashOf(indexTwoRelationsOtherOrder2));
  }

  @Test
  public void hash_changes_if_fields_on_main_type_mapping_are_different() {
    Index index = Index.withRelations("foo");
    IndexMainType mainType = IndexMainType.main(index, "bar");
    TestNewIndex indexNoField = new TestNewIndex(mainType, settingsConfiguration);
    TestNewIndex indexOneField = new TestNewIndex(mainType, settingsConfiguration);
    indexOneField.getMainTypeMapping()
      .createIntegerField("field1");
    TestNewIndex indexOneFieldAgain = new TestNewIndex(mainType, settingsConfiguration);
    indexOneFieldAgain.getMainTypeMapping()
      .createIntegerField("field1");
    TestNewIndex indexOneOtherField = new TestNewIndex(mainType, settingsConfiguration);
    indexOneOtherField.getMainTypeMapping()
      .createIntegerField("field2");
    TestNewIndex indexTwoFields = new TestNewIndex(mainType, settingsConfiguration);
    indexTwoFields.getMainTypeMapping()
      .createIntegerField("field1")
      .createIntegerField("field2");
    TestNewIndex indexTwoFieldsAgain = new TestNewIndex(mainType, settingsConfiguration);
    indexTwoFieldsAgain.getMainTypeMapping()
      .createIntegerField("field1")
      .createIntegerField("field2");
    TestNewIndex indexTwoOtherFields = new TestNewIndex(mainType, settingsConfiguration);
    indexTwoOtherFields.getMainTypeMapping()
      .createIntegerField("field1")
      .createIntegerField("field3");

    assertThat(hashOf(indexNoField))
      .isEqualTo(hashOf(new TestNewIndex(mainType, settingsConfiguration)))
      .isNotEqualTo(hashOf(indexOneField))
      .isNotEqualTo(hashOf(indexOneOtherField))
      .isNotEqualTo(hashOf(indexTwoFields))
      .isNotEqualTo(hashOf(indexTwoOtherFields));
    assertThat(hashOf(indexOneField))
      .isEqualTo(hashOf(indexOneFieldAgain))
      .isNotEqualTo(hashOf(indexOneOtherField))
      .isNotEqualTo(hashOf(indexTwoFields))
      .isNotEqualTo(hashOf(indexTwoOtherFields));
    assertThat(hashOf(indexTwoFields))
      .isEqualTo(hashOf(indexTwoFieldsAgain))
      .isNotEqualTo(hashOf(indexOneField))
      .isNotEqualTo(hashOf(indexOneOtherField))
      .isNotEqualTo(hashOf(indexTwoOtherFields));
  }

  @Test
  public void hash_is_the_same_if_only_fields_order_changes() {
    Index index = Index.withRelations("foo");
    IndexMainType mainType = IndexMainType.main(index, "bar");
    TestNewIndex indexTwoFields = new TestNewIndex(mainType, settingsConfiguration);
    indexTwoFields.getMainTypeMapping()
      .createBooleanField("donut1")
      .createBooleanField("donut2")
      .createBooleanField("donut3");
    TestNewIndex indexTwoFieldsOtherOrder = new TestNewIndex(mainType, settingsConfiguration);
    indexTwoFieldsOtherOrder.getMainTypeMapping()
      .createBooleanField("donut2")
      .createBooleanField("donut1")
      .createBooleanField("donut3");
    TestNewIndex indexTwoFieldsOtherOrder2 = new TestNewIndex(mainType, settingsConfiguration);
    indexTwoFieldsOtherOrder2.getMainTypeMapping()
      .createBooleanField("donut2")
      .createBooleanField("donut3")
      .createBooleanField("donut1");

    assertThat(hashOf(indexTwoFields))
      .isEqualTo(hashOf(indexTwoFieldsOtherOrder))
      .isEqualTo(hashOf(indexTwoFieldsOtherOrder2));
  }

  @Test
  public void hash_changes_if_field_type_changes() {
    Index index = Index.withRelations("foo");
    IndexMainType mainType = IndexMainType.main(index, "bar");
    String fieldName = "field1";

    computeAndVerifyAllDifferentHashesOnMapping(mainType,
      (mapping) -> mapping.createBooleanField(fieldName),
      (mapping) -> mapping.createIntegerField(fieldName),
      (mapping) -> mapping.createByteField(fieldName),
      (mapping) -> mapping.createDateTimeField(fieldName),
      (mapping) -> mapping.createDoubleField(fieldName),
      (mapping) -> mapping.createLongField(fieldName),
      (mapping) -> mapping.createShortField(fieldName),
      (mapping) -> mapping.createUuidPathField(fieldName),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).build(),
      (mapping) -> mapping.textFieldBuilder(fieldName).build(),
      (mapping) -> mapping.nestedFieldBuilder(fieldName).addKeywordField("bar").build());
  }

  @Test
  public void hash_changes_if_keyword_options_change() {
    Index index = Index.withRelations("foo");
    IndexMainType mainType = IndexMainType.main(index, "bar");
    String fieldName = "field1";

    computeAndVerifyAllDifferentHashesOnMapping(mainType,
      (mapping) -> mapping.keywordFieldBuilder(fieldName).build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableSortingAndAggregating().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableSortingAndAggregating().disableNorms().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableSortingAndAggregating().disableSearch().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableSortingAndAggregating().disableNorms().disableSearch().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableNorms().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableNorms().disableSearch().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableSearch().build());
  }

  @Test
  public void hash_is_the_same_if_only_order_of_keyword_options_change() {
    Index index = Index.withRelations("foo");
    IndexMainType mainType = IndexMainType.main(index, "bar");
    String fieldName = "field1";

    computeAndVerifyAllSameHashesOnMapping(mainType,
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableSortingAndAggregating().disableNorms().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableNorms().disableSortingAndAggregating().build());
    computeAndVerifyAllSameHashesOnMapping(mainType,
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableSortingAndAggregating().disableSearch().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableSearch().disableSortingAndAggregating().build());
    computeAndVerifyAllSameHashesOnMapping(mainType,
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableSearch().disableNorms().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableNorms().disableSearch().build());
    computeAndVerifyAllSameHashesOnMapping(mainType,
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableSortingAndAggregating().disableSearch().disableNorms().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableSearch().disableNorms().disableSortingAndAggregating().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableNorms().disableSearch().disableSortingAndAggregating().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableNorms().disableSortingAndAggregating().disableSearch().build(),
      (mapping) -> mapping.keywordFieldBuilder(fieldName).disableSearch().disableSortingAndAggregating().disableNorms().build());
  }

  @Test
  public void hash_changes_if_textFieldBuilder_options_change() {
    Index index = Index.withRelations("foo");
    IndexMainType mainType = IndexMainType.main(index, "bar");
    String fieldName = "field1";

    computeAndVerifyAllDifferentHashesOnMapping(mainType,
      (mapping) -> mapping.textFieldBuilder(fieldName).build(),
      (mapping) -> mapping.textFieldBuilder(fieldName).disableSearch().build(),
      (mapping) -> mapping.textFieldBuilder(fieldName).disableNorms().build(),
      (mapping) -> mapping.textFieldBuilder(fieldName).disableNorms().disableSearch().build());
  }

  @Test
  public void hash_is_the_same_if_only_order_of_textFieldBuilder_options_change() {
    Index index = Index.withRelations("foo");
    IndexMainType mainType = IndexMainType.main(index, "bar");
    String fieldName = "field1";

    computeAndVerifyAllSameHashesOnMapping(mainType,
      (mapping) -> mapping.textFieldBuilder(fieldName).disableSearch().disableNorms().build(),
      (mapping) -> mapping.textFieldBuilder(fieldName).disableNorms().disableSearch().build());
  }

  @SafeVarargs
  private final void computeAndVerifyAllSameHashesOnMapping(IndexMainType mainType, Consumer<TypeMapping>... fieldTypes) {
    List<Consumer<TypeMapping>> fieldTypes1 = Arrays.asList(fieldTypes);
    List<TestNewIndex> mainIndices = fieldTypes1.stream()
      .map(consumer -> {
        TestNewIndex mainTypeMapping = new TestNewIndex(mainType, settingsConfiguration);
        consumer.accept(mainTypeMapping.getMainTypeMapping());
        return mainTypeMapping;
      })
      .collect(toList());
    List<TestNewIndex> relationIndices = fieldTypes1.stream()
      .map(consumer -> {
        TestNewIndex relationTypeMapping = new TestNewIndex(mainType, settingsConfiguration);
        consumer.accept(relationTypeMapping.createRelationMapping("donut"));
        return relationTypeMapping;
      })
      .collect(toList());

    Set<String> mainHashes = mainIndices.stream()
      .map(IndexDefinitionHashTest::hashOf)
      .collect(toSet());
    Set<String> relationHashes = relationIndices.stream()
      .map(IndexDefinitionHashTest::hashOf)
      .collect(toSet());

    assertThat(mainHashes)
      // verify hashing is stable
      .isEqualTo(mainIndices.stream().map(IndexDefinitionHashTest::hashOf).collect(toSet()))
      .doesNotContainAnyElementsOf(relationHashes)
      .hasSize(1);
    assertThat(relationHashes)
      // verify hashing is stable
      .isEqualTo(relationIndices.stream().map(IndexDefinitionHashTest::hashOf).collect(toSet()))
      .doesNotContainAnyElementsOf(mainHashes)
      .hasSize(1);
  }

  @SafeVarargs
  private final void computeAndVerifyAllDifferentHashesOnMapping(IndexMainType mainType, Consumer<TypeMapping>... fieldTypes) {
    List<TestNewIndex> mainIndices = Arrays.stream(fieldTypes)
      .map(consumer -> {
        TestNewIndex mainTypeMapping = new TestNewIndex(mainType, settingsConfiguration);
        consumer.accept(mainTypeMapping.getMainTypeMapping());
        return mainTypeMapping;
      })
      .collect(toList());
    List<TestNewIndex> relationIndices = Arrays.stream(fieldTypes)
      .map(consumer -> {
        TestNewIndex relationTypeMapping = new TestNewIndex(mainType, settingsConfiguration);
        consumer.accept(relationTypeMapping.createRelationMapping("donut"));
        return relationTypeMapping;
      })
      .collect(toList());

    Set<String> mainHashes = mainIndices.stream()
      .map(IndexDefinitionHashTest::hashOf)
      .collect(toSet());
    Set<String> relationHashes = relationIndices.stream()
      .map(IndexDefinitionHashTest::hashOf)
      .collect(toSet());

    assertThat(mainHashes)
      // verify hashing is stable
      .isEqualTo(mainIndices.stream().map(IndexDefinitionHashTest::hashOf).collect(toSet()))
      .doesNotContainAnyElementsOf(relationHashes)
      .hasSize(fieldTypes.length);
    assertThat(relationHashes)
      // verify hashing is stable
      .isEqualTo(relationIndices.stream().map(IndexDefinitionHashTest::hashOf).collect(toSet()))
      .doesNotContainAnyElementsOf(mainHashes)
      .hasSize(fieldTypes.length);
  }

  @Test
  public void hash_changes_if_clustering_is_enabled_or_not() {
    Index index = Index.simple("foo");
    IndexMainType mainType = IndexMainType.main(index, "bar");
    MapSettings empty = new MapSettings();
    MapSettings clusterDisabled = new MapSettings().setProperty(CLUSTER_ENABLED.getKey(), false);
    MapSettings clusterEnabled = new MapSettings().setProperty(CLUSTER_ENABLED.getKey(), true);

    assertThat(hashOf(new TestNewIndex(mainType, settingsConfigurationOf(empty))))
      .isEqualTo(hashOf(new TestNewIndex(mainType, settingsConfigurationOf(empty))))
      .isEqualTo(hashOf(new TestNewIndex(mainType, settingsConfigurationOf(clusterDisabled))))
      .isNotEqualTo(hashOf(new TestNewIndex(mainType, settingsConfigurationOf(clusterEnabled))));
  }

  @Test
  public void hash_changes_if_number_of_shards_changes() {
    Index index = Index.simple("foo");
    IndexMainType mainType = IndexMainType.main(index, "bar");
    Configuration emptySettings = new MapSettings().asConfig();
    SettingsConfiguration defaultNbOfShards = SettingsConfiguration.newBuilder(emptySettings)
      .build();
    SettingsConfiguration specifiedDefaultNbOfShards = SettingsConfiguration.newBuilder(emptySettings)
      .setDefaultNbOfShards(5)
      .build();
    SettingsConfiguration specifyDefaultNbOfShards = SettingsConfiguration.newBuilder(new MapSettings()
      .setProperty("sonar.search." + index.getName() + ".shards", 1)
      .asConfig())
      .setDefaultNbOfShards(1)
      .build();
    SettingsConfiguration specifiedNbOfShards = SettingsConfiguration.newBuilder(new MapSettings()
      .setProperty("sonar.search." + index.getName() + ".shards", 10)
      .asConfig())
      .setDefaultNbOfShards(5)
      .build();

    assertThat(hashOf(new TestNewIndex(mainType, defaultNbOfShards)))
      // verify hash is stable
      .isEqualTo(hashOf(new TestNewIndex(mainType, defaultNbOfShards)))
      .isEqualTo(hashOf(new TestNewIndex(mainType, specifyDefaultNbOfShards)))
      .isNotEqualTo(hashOf(new TestNewIndex(mainType, specifiedDefaultNbOfShards)))
      .isNotEqualTo(hashOf(new TestNewIndex(mainType, specifiedNbOfShards)));
    assertThat(hashOf(new TestNewIndex(mainType, specifiedDefaultNbOfShards)))
      // verify hash is stable
      .isEqualTo(hashOf(new TestNewIndex(mainType, specifiedDefaultNbOfShards)))
      .isNotEqualTo(hashOf(new TestNewIndex(mainType, specifyDefaultNbOfShards)));
  }

  @Test
  public void hash_changes_if_refreshInterval_changes() {
    Index index = Index.simple("foo");
    IndexMainType mainType = IndexMainType.main(index, "bar");
    Configuration emptySettings = new MapSettings().asConfig();
    SettingsConfiguration defaultRefreshInterval = SettingsConfiguration.newBuilder(emptySettings)
      .build();
    SettingsConfiguration noRefreshInterval = SettingsConfiguration.newBuilder(emptySettings)
      .setRefreshInterval(-1)
      .build();
    SettingsConfiguration refreshInterval30 = SettingsConfiguration.newBuilder(emptySettings)
      .setRefreshInterval(30)
      .build();
    SettingsConfiguration someRefreshInterval = SettingsConfiguration.newBuilder(emptySettings)
      .setRefreshInterval(56)
      .build();

    assertThat(hashOf(new TestNewIndex(mainType, defaultRefreshInterval)))
      // verify hash is stable
      .isEqualTo(hashOf(new TestNewIndex(mainType, defaultRefreshInterval)))
      .isEqualTo(hashOf(new TestNewIndex(mainType, refreshInterval30)))
      .isNotEqualTo(hashOf(new TestNewIndex(mainType, noRefreshInterval)))
      .isNotEqualTo(hashOf(new TestNewIndex(mainType, someRefreshInterval)));
    assertThat(hashOf(new TestNewIndex(mainType, noRefreshInterval)))
      .isNotEqualTo(hashOf(new TestNewIndex(mainType, someRefreshInterval)));
  }

  private static SettingsConfiguration settingsConfigurationOf(MapSettings settings) {
    return SettingsConfiguration.newBuilder(settings.asConfig()).build();
  }

  private static String hashOf(TestNewIndex newIndex) {
    return IndexDefinitionHash.of(newIndex.build());
  }
}
