/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.sonar.process.ProcessProperties;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.server.es.BaseIndex.SEARCH_PARTIAL_SUFFIX;
import static org.sonar.server.es.BaseIndex.SEARCH_WORDS_SUFFIX;
import static org.sonar.server.es.BaseIndex.SORT_SUFFIX;

public class NewIndex {

  private final String indexName;
  private final Settings.Builder settings = DefaultIndexSettings.defaults();
  private final Map<String, NewIndexType> types = new LinkedHashMap<>();

  NewIndex(String indexName) {
    Preconditions.checkArgument(StringUtils.isAllLowerCase(indexName), "Index name must be lower-case: " + indexName);
    this.indexName = indexName;
  }

  public void refreshHandledByIndexer() {
    getSettings().put("index.refresh_interval", "-1");
  }

  public String getName() {
    return indexName;
  }

  public Settings.Builder getSettings() {
    return settings;
  }

  public NewIndexType createType(String typeName) {
    NewIndexType type = new NewIndexType(typeName);
    types.put(typeName, type);
    return type;
  }

  public Map<String, NewIndexType> getTypes() {
    return types;
  }

  public void configureShards(org.sonar.api.config.Settings settings, int defaultNbOfShards) {
    boolean clusterMode = settings.getBoolean(ProcessProperties.CLUSTER_ENABLED);
    int shards = settings.getInt(format("sonar.search.%s.shards", indexName));
    if (shards == 0) {
      shards = defaultNbOfShards;
    }
    int replicas = settings.getInt(format("sonar.search.%s.replicas", indexName));
    if (replicas == 0) {
      replicas = clusterMode ? 1 : 0;
    }
    getSettings().put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, shards);
    getSettings().put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, replicas);
  }

  public static class NewIndexType {
    private final String name;
    private final Map<String, Object> attributes = new TreeMap<>();
    private final Map<String, Object> properties = new TreeMap<>();

    private NewIndexType(String typeName) {
      this.name = typeName;
      // defaults
      attributes.put("dynamic", false);
      attributes.put("_all", ImmutableSortedMap.of("enabled", false));
      attributes.put("_source", ImmutableSortedMap.of("enabled", true));
      attributes.put("properties", properties);
    }

    public String getName() {
      return name;
    }

    /**
     * Complete the root json hash of mapping type, for example to set the attribute "_id"
     */
    public NewIndexType setAttribute(String key, Object value) {
      attributes.put(key, value);
      return this;
    }

    /**
     * Complete the json hash named "properties" in mapping type, usually to declare fields
     */
    public NewIndexType setProperty(String key, Object value) {
      properties.put(key, value);
      return this;
    }

    public NewIndexType setEnableSource(boolean enableSource) {
      attributes.put("_source", ImmutableSortedMap.of("enabled", enableSource));
      return this;
    }

    public StringFieldBuilder stringFieldBuilder(String fieldName) {
      return new StringFieldBuilder(this, fieldName);
    }

    public NestedFieldBuilder nestedFieldBuilder(String fieldName) {
      return new NestedFieldBuilder(this, fieldName);
    }

    public NewIndexType createBooleanField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "boolean"));
    }

    public NewIndexType createByteField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "byte"));
    }

    public NewIndexType createDateTimeField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "date", "format", "date_time"));
    }

    public NewIndexType createDoubleField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "double"));
    }

    public NewIndexType createIntegerField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "integer"));
    }

    public NewIndexType createLongField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "long"));
    }

    public NewIndexType createShortField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "short"));
    }

    public NewIndexType createUuidPathField(String fieldName) {
      return setProperty(fieldName, ImmutableSortedMap.of(
        "type", "string",
        "index", "analyzed",
        "analyzer", "uuid_analyzer"));
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }

    @CheckForNull
    public Object getProperty(String key) {
      return properties.get(key);
    }
  }

  /**
   * Helper to define a string field in mapping of index type
   */
  public static class StringFieldBuilder {
    private final NewIndexType indexType;
    private final String fieldName;
    private boolean disableSearch = false;
    private boolean disableNorms = false;
    private SortedMap<String, Object> subFields = Maps.newTreeMap();

    private StringFieldBuilder(NewIndexType indexType, String fieldName) {
      this.indexType = indexType;
      this.fieldName = fieldName;
    }

    /**
     * Add a sub-field. A {@code SortedMap} is required for consistency of the index settings hash.
     * @see IndexDefinitionHash
     */
    public StringFieldBuilder addSubField(String fieldName, SortedMap<String, String> fieldDefinition) {
      subFields.put(fieldName, fieldDefinition);
      return this;
    }

    /**
     * Create an inner-field named "sort" with analyzer "sortable"
     */
    public StringFieldBuilder enableSorting() {
      addSubField(SORT_SUFFIX, ImmutableSortedMap.of(
        "type", "string",
        "index", "analyzed",
        "analyzer", "sortable"));
      return this;
    }

    /**
     * Create an inner-field named "words" with analyzer "words"
     */
    public StringFieldBuilder enableWordSearch() {
      addSubField(SEARCH_WORDS_SUFFIX, ImmutableSortedMap.of(
        "type", "string",
        "index", "analyzed",
        "analyzer", "index_words",
        "search_analyzer", "search_words"));
      return this;
    }

    /**
     * Create a inner-field named "grams" with analyzer "grams"
     */
    public StringFieldBuilder enableGramSearch() {
      addSubField(SEARCH_PARTIAL_SUFFIX, ImmutableSortedMap.of(
        "type", "string",
        "index", "analyzed",
        "analyzer", "index_grams",
        "search_analyzer", "search_grams"));
      return this;
    }

    /**
     * Norms consume useless memory if string field is used for filtering or aggregations.
     *
     * https://www.elastic.co/guide/en/elasticsearch/reference/2.3/norms.html
     * https://www.elastic.co/guide/en/elasticsearch/guide/current/scoring-theory.html#field-norm
     */
    public StringFieldBuilder disableNorms() {
      this.disableNorms = true;
      return this;
    }

    /**
     * "index: no" -> Don’t index this field at all. This field will not be searchable.
     * By default field is "not_analyzed": it is searchable, but index the value exactly
     * as specified.
     */
    public StringFieldBuilder disableSearch() {
      this.disableSearch = true;
      return this;
    }

    public NewIndexType build() {
      Map<String, Object> hash = new TreeMap<>();
      if (subFields.isEmpty()) {
        hash.putAll(ImmutableMap.of(
          "type", "string",
          "index", disableSearch ? "no" : "not_analyzed",
          "norms", ImmutableMap.of("enabled", String.valueOf(!disableNorms))));
      } else {
        hash.put("type", "multi_field");
        Map<String, Object> multiFields = new TreeMap<>(subFields);
        multiFields.put(fieldName, ImmutableMap.of(
          "type", "string",
          "index", "not_analyzed",
          "norms", ImmutableMap.of("enabled", "false")));
        hash.put("fields", multiFields);
      }

      return indexType.setProperty(fieldName, hash);
    }
  }

  public static class NestedFieldBuilder {
    private final NewIndexType indexType;
    private final String fieldName;
    private final Map<String, Object> properties = new TreeMap<>();

    private NestedFieldBuilder(NewIndexType indexType, String fieldName) {
      this.indexType = indexType;
      this.fieldName = fieldName;
    }

    private NestedFieldBuilder setProperty(String fieldName, Object value) {
      properties.put(fieldName, value);

      return this;
    }

    public NestedFieldBuilder addStringFied(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "string"));
    }

    public NestedFieldBuilder addDoubleField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "double"));
    }

    public NewIndexType build() {
      checkArgument(!properties.isEmpty(), "At least one sub-field must be declared in nested property '%s'", fieldName);
      Map<String, Object> hash = new TreeMap<>();
      hash.put("type", "nested");
      hash.put("properties", properties);

      return indexType.setProperty(fieldName, hash);
    }
  }


}
