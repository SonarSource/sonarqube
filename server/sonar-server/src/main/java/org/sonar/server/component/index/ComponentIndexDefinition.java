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
package org.sonar.server.component.index;

import org.sonar.api.config.Configuration;
import org.sonar.server.es.DefaultIndexSettingsElement;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.NewIndex;

import static org.sonar.server.es.DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SEARCH_PREFIX_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SEARCH_PREFIX_CASE_INSENSITIVE_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.es.NewIndex.SettingsConfiguration.MANUAL_REFRESH_INTERVAL;
import static org.sonar.server.es.NewIndex.SettingsConfiguration.newBuilder;

public class ComponentIndexDefinition implements IndexDefinition {

  public static final IndexType INDEX_TYPE_COMPONENT = new IndexType("components", "component");
  public static final String FIELD_UUID = "uuid";
  public static final String FIELD_PROJECT_UUID = "project_uuid";
  public static final String FIELD_ORGANIZATION_UUID = "organization_uuid";
  public static final String FIELD_KEY = "key";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_QUALIFIER = "qualifier";
  public static final String FIELD_LANGUAGE = "language";

  private static final int DEFAULT_NUMBER_OF_SHARDS = 5;

  static final DefaultIndexSettingsElement[] NAME_ANALYZERS = {SORTABLE_ANALYZER, SEARCH_PREFIX_ANALYZER, SEARCH_PREFIX_CASE_INSENSITIVE_ANALYZER, SEARCH_GRAMS_ANALYZER};

  private final Configuration config;

  public ComponentIndexDefinition(Configuration config) {
    this.config = config;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(
      INDEX_TYPE_COMPONENT.getIndex(),
      newBuilder(config)
        .setRefreshInterval(MANUAL_REFRESH_INTERVAL)
        .setDefaultNbOfShards(DEFAULT_NUMBER_OF_SHARDS)
        .build());

    NewIndex.NewIndexType mapping = index.createType(INDEX_TYPE_COMPONENT.getType())
      .requireProjectAuthorization();

    mapping.keywordFieldBuilder(FIELD_UUID).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_PROJECT_UUID).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_KEY).addSubFields(SORTABLE_ANALYZER).build();
    mapping.textFieldBuilder(FIELD_NAME)
      .withFieldData()
      .termVectorWithPositionOffsets()
      .addSubFields(NAME_ANALYZERS)
      .build();

    mapping.keywordFieldBuilder(FIELD_QUALIFIER).build();
    mapping.keywordFieldBuilder(FIELD_LANGUAGE).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ORGANIZATION_UUID).disableNorms().build();
  }
}
