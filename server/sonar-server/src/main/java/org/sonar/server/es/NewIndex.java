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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessProperties;
import org.sonar.server.search.IndexField;

import javax.annotation.CheckForNull;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class NewIndex {

  public void refreshHandledByIndexer() {
    getSettings().put("index.refresh_interval", "-1");
  }

  public void setShards(Settings settings) {
    boolean clusterMode = settings.getBoolean(ProcessProperties.CLUSTER_ACTIVATE);
    if (clusterMode) {
      getSettings().put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 4);
      getSettings().put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1);
      // else keep defaults (one shard)
    }
  }

  public static class NewIndexType {
    private final String name;
    private final Map<String, Object> attributes = new TreeMap<String, Object>();
    private final Map<String, Object> properties = new TreeMap<String, Object>();

    private NewIndexType(String typeName) {
      this.name = typeName;
      // defaults
      attributes.put("dynamic", false);
      attributes.put("_all", ImmutableSortedMap.of("enabled", false));

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

    public StringFieldBuilder stringFieldBuilder(String fieldName) {
      return new StringFieldBuilder(this, fieldName);
    }

    public NestedObjectBuilder nestedObjectBuilder(String fieldName, NewIndexType nestedMapping) {
      return new NestedObjectBuilder(this, nestedMapping, fieldName);
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

    public NewIndexType createDynamicNestedField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "nested", "dynamic", "true"));
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

  public static class NestedObjectBuilder {
    private final NewIndexType indexType;
    private final NewIndexType nestedType;
    private final String fieldName;
    private boolean dynamic = false;

    public NestedObjectBuilder(NewIndexType indexType, NewIndexType nestedType, String fieldName) {
      this.indexType = indexType;
      this.nestedType = nestedType;
      this.fieldName = fieldName;
    }

    public NestedObjectBuilder dynamic() {
      this.dynamic = true;
      return this;
    }

    public void build() {
      if (dynamic) {
        indexType.setProperty(fieldName, ImmutableMap.of("type", "nested", "dynamic", "true"));
      } else {
        nestedType.setAttribute("type", "nested");
        indexType.setProperty(fieldName, nestedType.attributes);
      }
    }
  }

  /**
   * Helper to define a string field in mapping of index type
   */
  public static class StringFieldBuilder {
    private final NewIndexType indexType;
    private final String fieldName;
    private boolean docValues = false, disableSearch = false, hasAnalyzedField = false;
    private SortedMap<String, Object> subFields = Maps.newTreeMap();

    private StringFieldBuilder(NewIndexType indexType, String fieldName) {
      this.indexType = indexType;
      this.fieldName = fieldName;
    }

    public StringFieldBuilder docValues() {
      this.docValues = true;
      return this;
    }

    /**
     * Add a sub-field. A {@code SortedMap} is required for consistency of the index settings hash.
     * @see IndexDefinitionHash
     */
    public StringFieldBuilder addSubField(String fieldName, SortedMap<String, String> fieldDefinition) {
      subFields.put(fieldName, fieldDefinition);
      hasAnalyzedField |= "analyzed".equals(fieldDefinition.get("index"));
      return this;
    }

    /**
     * Create an inner-field named "sort" with analyzer "sortable"
     */
    public StringFieldBuilder enableSorting() {
      addSubField(IndexField.SORT_SUFFIX, ImmutableSortedMap.of(
        "type", "string",
        "index", "analyzed",
        "analyzer", "sortable"));
      return this;
    }

    /**
     * Create an inner-field named "words" with analyzer "words"
     */
    public StringFieldBuilder enableWordSearch() {
      addSubField(IndexField.SEARCH_WORDS_SUFFIX, ImmutableSortedMap.of(
        "type", "string",
        "index", "analyzed",
        "index_analyzer", "index_words",
        "search_analyzer", "search_words"));
      return this;
    }

    /**
     * Create a inner-field named "grams" with analyzer "grams"
     */
    public StringFieldBuilder enableGramSearch() {
      addSubField(IndexField.SEARCH_PARTIAL_SUFFIX, ImmutableSortedMap.of(
        "type", "string",
        "index", "analyzed",
        "index_analyzer", "index_grams",
        "search_analyzer", "search_grams"));
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

    public void build() {
      validate();
      Map<String, Object> hash = new TreeMap<>();
      if (!subFields.isEmpty()) {
        hash.put("type", "multi_field");
        Map<String, Object> multiFields = new TreeMap<>(subFields);
        multiFields.put(fieldName, ImmutableMap.of(
          "type", "string",
          "index", "not_analyzed",
          "omit_norms", "true",
          "doc_values", docValues));
        hash.put("fields", multiFields);
      } else {
        hash.putAll(ImmutableMap.of(
          "type", "string",
          "index", disableSearch ? "no" : "not_analyzed",
          "omit_norms", "true",
          "doc_values", docValues));
      }

      indexType.setProperty(fieldName, hash);
    }

    private void validate() {
      if (docValues && hasAnalyzedField) {
        throw new IllegalStateException("Doc values are not supported on analyzed strings of field: " + fieldName);
      }
      if (disableSearch && hasAnalyzedField) {
        throw new IllegalStateException("Can't mix searchable and non-searchable arguments on field: " + fieldName);
      }
    }
  }

  private final String indexName;
  private final ImmutableSettings.Builder settings = DefaultIndexSettings.defaults();
  private final SortedMap<String, NewIndexType> types = new TreeMap<String, NewIndexType>();

  NewIndex(String indexName) {
    Preconditions.checkArgument(StringUtils.isAllLowerCase(indexName), "Index name must be lower-case: " + indexName);
    this.indexName = indexName;
  }

  public String getName() {
    return indexName;
  }

  public ImmutableSettings.Builder getSettings() {
    return settings;
  }

  public NewIndexType createType(String typeName) {
    NewIndexType type = new NewIndexType(typeName);
    types.put(typeName, type);
    return type;
  }

  public SortedMap<String, NewIndexType> getTypes() {
    return types;
  }
}
