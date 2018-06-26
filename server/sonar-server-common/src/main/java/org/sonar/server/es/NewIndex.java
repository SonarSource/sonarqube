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
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.sonar.api.config.Configuration;
import org.sonar.server.permission.index.AuthorizationTypeSupport;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Objects.requireNonNull;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.SEARCH_REPLICAS;
import static org.sonar.server.es.DefaultIndexSettings.ANALYZER;
import static org.sonar.server.es.DefaultIndexSettings.FIELDDATA_ENABLED;
import static org.sonar.server.es.DefaultIndexSettings.FIELD_FIELDDATA;
import static org.sonar.server.es.DefaultIndexSettings.FIELD_TERM_VECTOR;
import static org.sonar.server.es.DefaultIndexSettings.FIELD_TYPE_KEYWORD;
import static org.sonar.server.es.DefaultIndexSettings.FIELD_TYPE_TEXT;
import static org.sonar.server.es.DefaultIndexSettings.INDEX;
import static org.sonar.server.es.DefaultIndexSettings.INDEX_NOT_SEARCHABLE;
import static org.sonar.server.es.DefaultIndexSettings.INDEX_SEARCHABLE;
import static org.sonar.server.es.DefaultIndexSettings.NORMS;
import static org.sonar.server.es.DefaultIndexSettings.STORE;
import static org.sonar.server.es.DefaultIndexSettings.TYPE;
import static org.sonar.server.es.DefaultIndexSettingsElement.UUID_MODULE_ANALYZER;

public class NewIndex {

  private final String indexName;
  private final Settings.Builder settings = DefaultIndexSettings.defaults();
  private final Map<String, NewIndexType> types = new LinkedHashMap<>();

  NewIndex(String indexName, SettingsConfiguration settingsConfiguration) {
    checkArgument(StringUtils.isAllLowerCase(indexName), "Index name must be lower-case: " + indexName);
    this.indexName = indexName;
    applySettingsConfiguration(settingsConfiguration);
  }

  private void applySettingsConfiguration(SettingsConfiguration settingsConfiguration) {
    settings.put("index.mapper.dynamic", valueOf(false));
    settings.put("index.refresh_interval", refreshInterval(settingsConfiguration));

    Configuration config = settingsConfiguration.getConfiguration();
    boolean clusterMode = config.getBoolean(CLUSTER_ENABLED.getKey()).orElse(false);
    int shards = config.getInt(format("sonar.search.%s.shards", indexName))
      .orElse(settingsConfiguration.getDefaultNbOfShards());
    int replicas = clusterMode ? config.getInt(SEARCH_REPLICAS.getKey()).orElse(1) : 0;

    settings.put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, shards);
    settings.put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, replicas);
  }

  private static String refreshInterval(SettingsConfiguration settingsConfiguration) {
    int refreshInterval = settingsConfiguration.getRefreshInterval();
    if (refreshInterval == -1) {
      return "-1";
    }
    return refreshInterval + "s";
  }

  public static class SettingsConfiguration {
    public static final int MANUAL_REFRESH_INTERVAL = -1;

    private final Configuration configuration;
    private final int defaultNbOfShards;
    private final int refreshInterval;

    private SettingsConfiguration(Builder builder) {
      this.configuration = builder.configuration;
      this.defaultNbOfShards = builder.defaultNbOfShards;
      this.refreshInterval = builder.refreshInterval;
    }

    public static Builder newBuilder(Configuration configuration) {
      return new Builder(configuration);
    }

    public Configuration getConfiguration() {
      return configuration;
    }

    public int getDefaultNbOfShards() {
      return defaultNbOfShards;
    }

    public int getRefreshInterval() {
      return refreshInterval;
    }

    public static class Builder {
      private final Configuration configuration;
      private int defaultNbOfShards = 1;
      private int refreshInterval = 30;

      public Builder(Configuration configuration) {
        this.configuration = requireNonNull(configuration, "configuration can't be null");
      }

      public Builder setDefaultNbOfShards(int defaultNbOfShards) {
        checkArgument(defaultNbOfShards >= 1, "defaultNbOfShards must be >= 1");
        this.defaultNbOfShards = defaultNbOfShards;
        return this;
      }

      public Builder setRefreshInterval(int refreshInterval) {
        checkArgument(refreshInterval == -1 || refreshInterval > 0,
          "refreshInterval must be either -1 or strictly positive");
        this.refreshInterval = refreshInterval;
        return this;
      }

      public SettingsConfiguration build() {
        return new SettingsConfiguration(this);
      }
    }

  }

  public String getName() {
    return indexName;
  }

  public Settings.Builder getSettings() {
    return settings;
  }

  public NewIndexType createType(String typeName) {
    NewIndexType type = new NewIndexType(this, typeName);
    types.put(typeName, type);
    return type;
  }

  public Map<String, NewIndexType> getTypes() {
    return types;
  }

  public static class NewIndexType {
    private final NewIndex index;
    private final String name;
    private final Map<String, Object> attributes = new TreeMap<>();
    private final Map<String, Object> properties = new TreeMap<>();

    private NewIndexType(NewIndex index, String typeName) {
      this.index = index;
      this.name = typeName;
      // defaults
      attributes.put("dynamic", false);
      attributes.put("_all", ImmutableSortedMap.of("enabled", false));
      attributes.put("_source", ImmutableSortedMap.of("enabled", true));
      attributes.put("properties", properties);
    }

    public NewIndexType requireProjectAuthorization() {
      AuthorizationTypeSupport.enableProjectAuthorization(this);
      return this;
    }

    public NewIndex getIndex() {
      return index;
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

    public KeywordFieldBuilder keywordFieldBuilder(String fieldName) {
      return new KeywordFieldBuilder(this, fieldName);
    }

    public TextFieldBuilder textFieldBuilder(String fieldName) {
      return new TextFieldBuilder(this, fieldName);
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
      Map<String, String> hash = new TreeMap<>();
      hash.put("type", "date");
      hash.put("format", "date_time||epoch_second");
      return setProperty(fieldName, hash);
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
        TYPE, FIELD_TYPE_TEXT,
        INDEX, DefaultIndexSettings.INDEX_SEARCHABLE,
        ANALYZER, UUID_MODULE_ANALYZER.getName()));
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
  public abstract static class StringFieldBuilder<T extends StringFieldBuilder<T>> {
    private final NewIndexType indexType;
    private final String fieldName;
    private boolean disableSearch = false;
    private boolean disableNorms = false;
    private boolean termVectorWithPositionOffsets = false;
    private SortedMap<String, Object> subFields = Maps.newTreeMap();
    private boolean store = false;
    protected boolean disabledDocValues = false;

    private StringFieldBuilder(NewIndexType indexType, String fieldName) {
      this.indexType = indexType;
      this.fieldName = fieldName;
    }

    /**
     * Add a sub-field. A {@code SortedMap} is required for consistency of the index settings hash.
     * @see IndexDefinitionHash
     */
    private T addSubField(String fieldName, SortedMap<String, String> fieldDefinition) {
      subFields.put(fieldName, fieldDefinition);
      return castThis();
    }

    /**
     * Add subfields, one for each analyzer.
     */
    public T addSubFields(DefaultIndexSettingsElement... analyzers) {
      Arrays.stream(analyzers)
        .forEach(analyzer -> addSubField(analyzer.getSubFieldSuffix(), analyzer.fieldMapping()));
      return castThis();
    }

    /**
     * Norms consume useless memory if string field is used for filtering or aggregations.
     *
     * https://www.elastic.co/guide/en/elasticsearch/reference/2.3/norms.html
     * https://www.elastic.co/guide/en/elasticsearch/guide/current/scoring-theory.html#field-norm
     */
    public T disableNorms() {
      this.disableNorms = true;
      return castThis();
    }

    /**
     * Position offset term vectors are required for the fast_vector_highlighter (fvh).
     */
    public T termVectorWithPositionOffsets() {
      this.termVectorWithPositionOffsets = true;
      return castThis();
    }

    /**
     * "index: false" -> Make this field not searchable.
     * By default field is "true": it is searchable, but index the value exactly
     * as specified.
     */
    public T disableSearch() {
      this.disableSearch = true;
      return castThis();
    }

    public T store() {
      this.store = true;
      return castThis();
    }

    @SuppressWarnings("unchecked")
    private T castThis() {
      return (T) this;
    }

    public NewIndexType build() {
      if (subFields.isEmpty()) {
        return buildWithoutSubfields();
      }
      return buildWithSubfields();
    }

    private NewIndexType buildWithoutSubfields() {
      Map<String, Object> hash = new TreeMap<>();
      hash.put("type", getFieldType());
      hash.put(INDEX, disableSearch ? INDEX_NOT_SEARCHABLE : INDEX_SEARCHABLE);
      hash.put(NORMS, valueOf(!disableNorms));
      hash.put(STORE, valueOf(store));
      if (FIELD_TYPE_KEYWORD.equals(getFieldType())) {
        hash.put("doc_values", valueOf(!disabledDocValues));
      }
      if (getFieldData()) {
        hash.put(FIELD_FIELDDATA, FIELDDATA_ENABLED);
      }
      return indexType.setProperty(fieldName, hash);
    }

    private NewIndexType buildWithSubfields() {
      Map<String, Object> hash = new TreeMap<>();
      hash.put("type", getFieldType());
      hash.put(INDEX, disableSearch ? INDEX_NOT_SEARCHABLE : INDEX_SEARCHABLE);
      hash.put(NORMS, "false");
      hash.put(STORE, valueOf(store));
      if (FIELD_TYPE_KEYWORD.equals(getFieldType())) {
        hash.put("doc_values", valueOf(!disabledDocValues));
      }
      if (getFieldData()) {
        hash.put(FIELD_FIELDDATA, FIELDDATA_ENABLED);
      }
      if (termVectorWithPositionOffsets) {
        hash.put(FIELD_TERM_VECTOR, "with_positions_offsets");
      }
      hash.put("fields", configureSubFields());
      return indexType.setProperty(fieldName, hash);
    }

    private Map<String, Object> configureSubFields() {
      Map<String, Object> multiFields = new TreeMap<>(subFields);

      // apply this fields configuration to all subfields
      multiFields.entrySet().forEach(entry -> {
        Object subFieldMapping = entry.getValue();
        if (subFieldMapping instanceof Map) {
          entry.setValue(configureSubField((Map<String, String>) subFieldMapping));
        }
      });
      return multiFields;
    }

    private Map<String, String> configureSubField(Map<String, String> subFieldMapping) {
      Map<String, String> subHash = new TreeMap<>(subFieldMapping);
      subHash.put(INDEX, INDEX_SEARCHABLE);
      subHash.put(NORMS, "false");
      subHash.put(STORE, valueOf(store));
      if (termVectorWithPositionOffsets) {
        subHash.put(FIELD_TERM_VECTOR, "with_positions_offsets");
      }
      return subHash;
    }

    protected abstract boolean getFieldData();

    protected abstract String getFieldType();
  }

  public static class KeywordFieldBuilder extends StringFieldBuilder<KeywordFieldBuilder> {

    private KeywordFieldBuilder(NewIndexType indexType, String fieldName) {
      super(indexType, fieldName);
    }

    @Override
    protected boolean getFieldData() {
      return false;
    }

    protected String getFieldType() {
      return FIELD_TYPE_KEYWORD;
    }

    /**
     * By default, field is stored on disk in a column-stride fashion, so that it can later be used for sorting,
     * aggregations, or scripting.
     * Disabling this reduces the size of the index and drop the constraint of single term max size of
     * 32766 bytes (which, if there is no tokenizing enabled on the field, equals the size of the whole data).
     */
    public KeywordFieldBuilder disableSortingAndAggregating() {
      this.disabledDocValues = true;
      return this;
    }
  }

  public static class TextFieldBuilder extends StringFieldBuilder<TextFieldBuilder> {

    private boolean fieldData = false;

    private TextFieldBuilder(NewIndexType indexType, String fieldName) {
      super(indexType, fieldName);
    }

    protected String getFieldType() {
      return FIELD_TYPE_TEXT;
    }

    /**
     * Required to enable sorting, aggregation and access to field data on fields of type "text".
     * <p>Disabled by default as this can have significant memory cost</p>
     */
    public StringFieldBuilder withFieldData() {
      this.fieldData = true;
      return this;
    }

    @Override
    protected boolean getFieldData() {
      return fieldData;
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

    public NestedFieldBuilder addKeywordField(String fieldName) {
      return setProperty(fieldName, ImmutableSortedMap.of(
        "type", FIELD_TYPE_KEYWORD,
        INDEX, INDEX_SEARCHABLE));
    }

    public NestedFieldBuilder addDoubleField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "double"));
    }

    public NestedFieldBuilder addIntegerField(String fieldName) {
      return setProperty(fieldName, ImmutableMap.of("type", "integer"));
    }

    public NewIndexType build() {
      checkArgument(!properties.isEmpty(), "At least one sub-field must be declared in nested property '%s'", fieldName);

      return indexType.setProperty(fieldName, ImmutableSortedMap.of(
        "type", "nested",
        "properties", properties));
    }
  }

}
