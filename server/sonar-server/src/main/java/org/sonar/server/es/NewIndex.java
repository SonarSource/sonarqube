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
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.sonar.server.search.IndexField;

import javax.annotation.CheckForNull;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class NewIndex {

  public static class NewMapping {
    private final Map<String, Object> attributes = new TreeMap<String, Object>();
    private final Map<String, Object> properties = new TreeMap<String, Object>();

    private NewMapping() {
      // defaults
      attributes.put("dynamic", false);
      attributes.put("_all", ImmutableSortedMap.of("enabled", false));

      attributes.put("properties", properties);
    }

    /**
     * Complete the root json hash of mapping type, for example to set the attribute "_id"
     */
    public NewMapping setAttribute(String key, Object value) {
      attributes.put(key, value);
      return this;
    }

    /**
     * Complete the json hash named "properties" in mapping type, usually to declare fields
     */
    public NewMapping setProperty(String key, Object value) {
      properties.put(key, value);
      return this;
    }

    public StringFieldBuilder stringFieldBuilder(String fieldName) {
      return new StringFieldBuilder(this, fieldName);
    }

    public NewMapping createBooleanField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "boolean"));
    }

    public NewMapping createByteField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "byte"));
    }

    public NewMapping createDateTimeField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "date", "format", "date_time"));
    }

    public NewMapping createDoubleField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "double"));
    }

    public NewMapping createIntegerField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "integer"));
    }

    public NewMapping createLongField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "long"));
    }

    public NewMapping createShortField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "short"));
    }

    public NewMapping createUuidPathField(String fieldName) {
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
    private static final ImmutableMap<String, String> NOT_ANALYZED = ImmutableSortedMap.of(
      "type", "string",
      "index", "not_analyzed");

    private final NewMapping newMapping;
    private final String fieldName;
    private boolean sortable = false, wordSearch = false, gramSearch = false;

    private StringFieldBuilder(NewMapping newMapping, String fieldName) {
      this.newMapping = newMapping;
      this.fieldName = fieldName;
    }

    /**
     * Create a inner-field named "sort" with analyzer "sortable"
     */
    public StringFieldBuilder enableSorting() {
      this.sortable = true;
      return this;
    }

    public StringFieldBuilder enableWordSearch() {
      this.wordSearch = true;
      return this;
    }

    public StringFieldBuilder enableGramSearch() {
      this.gramSearch = true;
      return this;
    }

    public void build() {
      Map<String, Object> hash = new TreeMap<String, Object>();
      if (wordSearch || sortable || gramSearch) {
        hash.put("type", "multi_field");
        Map<String, Object> multiFields = new TreeMap<String, Object>();

        if (sortable) {
          multiFields.put(IndexField.SORT_SUFFIX, ImmutableSortedMap.of(
            "type", "string",
            "index", "analyzed",
            "analyzer", "sortable"));
        }
        if (wordSearch) {
          multiFields.put(IndexField.SEARCH_WORDS_SUFFIX, ImmutableSortedMap.of(
            "type", "string",
            "index", "analyzed",
            "index_analyzer", "index_words",
            "search_analyzer", "search_words"));
        }
        if (gramSearch) {
          multiFields.put(IndexField.SEARCH_PARTIAL_SUFFIX, ImmutableSortedMap.of(
            "type", "string",
            "index", "analyzed",
            "index_analyzer", "index_grams",
            "search_analyzer", "search_grams"));
        }
        multiFields.put(fieldName, NOT_ANALYZED);
        hash.put("fields", multiFields);
      } else {
        hash.putAll(NOT_ANALYZED);
      }

      newMapping.setProperty(fieldName, hash);
    }
  }

  private final String indexName;
  private final ImmutableSettings.Builder settings = DefaultMappingSettings.defaults();
  private final SortedMap<String, NewMapping> mappings = new TreeMap<String, NewMapping>();

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

  public NewMapping createMapping(String typeName) {
    NewMapping type = new NewMapping();
    mappings.put(typeName, type);
    return type;
  }

  public SortedMap<String, NewMapping> getMappings() {
    return mappings;
  }
}
