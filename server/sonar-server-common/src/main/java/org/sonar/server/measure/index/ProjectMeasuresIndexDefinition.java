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
package org.sonar.server.measure.index;

import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexType.IndexRelationType;
import org.sonar.server.es.newindex.NewAuthorizedIndex;
import org.sonar.server.es.newindex.TypeMapping;
import org.sonar.server.permission.index.IndexAuthorizationConstants;

import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.es.newindex.SettingsConfiguration.MANUAL_REFRESH_INTERVAL;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

public class ProjectMeasuresIndexDefinition implements IndexDefinition {

  public static final Index DESCRIPTOR = Index.withRelations("projectmeasures");
  public static final IndexType.IndexMainType TYPE_AUTHORIZATION = IndexType.main(DESCRIPTOR, IndexAuthorizationConstants.TYPE_AUTHORIZATION);
  public static final IndexRelationType TYPE_PROJECT_MEASURES = IndexType.relation(TYPE_AUTHORIZATION, "projectmeasure");

  public static final String FIELD_UUID = "uuid";
  public static final String FIELD_ORGANIZATION_UUID = "organizationUuid";

  /**
   * Project key. Only projects (qualifier=TRK)
   */
  public static final String FIELD_KEY = "key";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_ANALYSED_AT = "analysedAt";
  public static final String FIELD_QUALITY_GATE_STATUS = "qualityGateStatus";
  public static final String FIELD_TAGS = "tags";
  public static final String FIELD_MEASURES = "measures";
  public static final String FIELD_MEASURES_KEY = "key";
  public static final String FIELD_MEASURES_VALUE = "value";
  public static final String FIELD_LANGUAGES = "languages";
  public static final String FIELD_NCLOC_LANGUAGE_DISTRIBUTION = "nclocLanguageDistribution";
  public static final String FIELD_DISTRIB_LANGUAGE = "language";
  public static final String FIELD_DISTRIB_NCLOC = "ncloc";

  private final Configuration config;
  private final boolean enableSource;

  private ProjectMeasuresIndexDefinition(Configuration config, boolean enableSource) {
    this.config = config;
    this.enableSource = enableSource;
  }

  public ProjectMeasuresIndexDefinition(Configuration config) {
    this(config, false);
  }

  /**
   * Keep the document sources in index so that indexer tests can verify content
   * of indexed documents.
   */
  public static ProjectMeasuresIndexDefinition createForTest() {
    return new ProjectMeasuresIndexDefinition(new MapSettings().asConfig(), true);
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewAuthorizedIndex index = context.createWithAuthorization(
      DESCRIPTOR,
      newBuilder(config)
        .setRefreshInterval(MANUAL_REFRESH_INTERVAL)
        .setDefaultNbOfShards(5)
        .build())
      .setEnableSource(enableSource);

    TypeMapping mapping = index.createTypeMapping(TYPE_PROJECT_MEASURES);
    mapping.keywordFieldBuilder(FIELD_UUID).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ORGANIZATION_UUID).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_KEY).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_NAME).addSubFields(SORTABLE_ANALYZER, SEARCH_GRAMS_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_QUALITY_GATE_STATUS).build();
    mapping.keywordFieldBuilder(FIELD_TAGS).build();
    mapping.keywordFieldBuilder(FIELD_LANGUAGES).build();
    mapping.nestedFieldBuilder(FIELD_MEASURES)
      .addKeywordField(FIELD_MEASURES_KEY)
      .addDoubleField(FIELD_MEASURES_VALUE)
      .build();
    mapping.nestedFieldBuilder(FIELD_NCLOC_LANGUAGE_DISTRIBUTION)
      .addKeywordField(FIELD_DISTRIB_LANGUAGE)
      .addIntegerField(FIELD_DISTRIB_NCLOC)
      .build();
    mapping.createDateTimeField(FIELD_ANALYSED_AT);
  }
}
