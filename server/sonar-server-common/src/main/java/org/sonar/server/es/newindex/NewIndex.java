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

import com.google.common.collect.ImmutableSortedMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.sonar.api.config.Configuration;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.IndexType.IndexRelationType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.valueOf;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.SEARCH_REPLICAS;

public abstract class NewIndex<T extends NewIndex<T>> {

  private static final String ENABLED = "enabled";
  private final Index index;
  private final Map<String, IndexRelationType> relations = new LinkedHashMap<>();
  private final Settings.Builder settings = DefaultIndexSettings.defaults();
  private final Map<String, Object> attributes = new TreeMap<>();
  private final Map<String, Object> properties = new TreeMap<>();

  public NewIndex(Index index, SettingsConfiguration settingsConfiguration) {
    this.index = index;
    applySettingsConfiguration(settingsConfiguration);
    configureDefaultAttributes();
  }

  private void applySettingsConfiguration(SettingsConfiguration settingsConfiguration) {
    settings.put("index.refresh_interval", refreshInterval(settingsConfiguration));

    Configuration config = settingsConfiguration.getConfiguration();
    boolean clusterMode = config.getBoolean(CLUSTER_ENABLED.getKey()).orElse(false);
    int shards = config.getInt(String.format("sonar.search.%s.shards", index.getName()))
      .orElse(settingsConfiguration.getDefaultNbOfShards());
    int replicas = clusterMode ? config.getInt(SEARCH_REPLICAS.getKey()).orElse(1) : 0;

    settings.put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, shards);
    settings.put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, replicas);
    settings.put("index.max_ngram_diff", DefaultIndexSettings.MAXIMUM_NGRAM_LENGTH - DefaultIndexSettings.MINIMUM_NGRAM_LENGTH);
  }

  private void configureDefaultAttributes() {
    attributes.put("dynamic", valueOf(false));
    attributes.put("_source", ImmutableSortedMap.of(ENABLED, true));
  }

  private static String refreshInterval(SettingsConfiguration settingsConfiguration) {
    int refreshInterval = settingsConfiguration.getRefreshInterval();
    if (refreshInterval == -1) {
      return "-1";
    }
    return refreshInterval + "s";
  }

  protected Index getIndex() {
    return index;
  }

  public abstract IndexMainType getMainType();

  Collection<IndexRelationType> getRelations() {
    return relations.values();
  }

  /**
   * Public, read-only version of {@link #getRelations()}
   */
  public Stream<IndexRelationType> getRelationsStream() {
    return relations.values().stream();
  }

  Settings.Builder getSettings() {
    return settings;
  }

  @CheckForNull
  public String getSetting(String key) {
    return settings.get(key);
  }

  protected TypeMapping createTypeMapping(IndexMainType mainType) {
    checkArgument(mainType.getIndex().equals(index), "Main type must belong to index %s", index);
    return new TypeMapping(this);
  }

  protected TypeMapping createTypeMapping(IndexRelationType relationType) {
    checkAcceptsRelations();
    IndexMainType mainType = getMainType();
    checkArgument(relationType.getMainType().equals(mainType), "mainType of relation must be %s", mainType);
    String relationName = relationType.getName();
    checkArgument(!relations.containsKey(relationName), "relation %s already exists", relationName);
    relations.put(relationName, relationType);
    return new TypeMapping(this);
  }

  private void checkAcceptsRelations() {
    checkState(getMainType().getIndex().acceptsRelations(), "Index is not configured to accept relations. Update IndexDefinition.Descriptor instance for this index");
  }

  /**
   * Complete the json hash named "properties" in mapping type, usually to declare fields
   */
  void setFieldImpl(String fieldName, Object attributes) {
    properties.put(fieldName, attributes);
  }

  public T setEnableSource(boolean enableSource) {
    attributes.put("_source", ImmutableSortedMap.of(ENABLED, enableSource));
    return castThis();
  }

  @SuppressWarnings("unchecked")
  private T castThis() {
    return (T) this;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  Map<String, Object> getProperties() {
    return properties;
  }

  @CheckForNull
  public Object getProperty(String key) {
    return properties.get(key);
  }

  public abstract BuiltIndex<T> build();

}
