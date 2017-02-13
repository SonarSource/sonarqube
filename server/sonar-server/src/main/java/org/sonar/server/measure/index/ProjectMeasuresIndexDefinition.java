/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import org.sonar.api.config.Settings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

public class ProjectMeasuresIndexDefinition implements IndexDefinition {

  public static final String INDEX_PROJECT_MEASURES = "projectmeasures";

  public static final String TYPE_PROJECT_MEASURE = "projectmeasure";
  public static final String FIELD_ORGANIZATION_UUID = "organizationUuid";
  public static final String FIELD_KEY = "key";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_ANALYSED_AT = "analysedAt";
  public static final String FIELD_QUALITY_GATE = "qualityGate";
  public static final String FIELD_MEASURES = "measures";
  public static final String FIELD_MEASURES_KEY = "key";
  public static final String FIELD_MEASURES_VALUE = "value";

  private final Settings settings;

  public ProjectMeasuresIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX_PROJECT_MEASURES);
    index.refreshHandledByIndexer();
    index.configureShards(settings, 5);

    NewIndex.NewIndexType mapping = index.createType(TYPE_PROJECT_MEASURE)
      .requireProjectAuthorization();

    mapping.stringFieldBuilder(FIELD_ORGANIZATION_UUID).build();
    mapping.stringFieldBuilder(FIELD_KEY).disableNorms().build();
    mapping.stringFieldBuilder(FIELD_NAME).enableSorting().build();
    mapping.stringFieldBuilder(FIELD_QUALITY_GATE).build();
    mapping.createDateTimeField(FIELD_ANALYSED_AT);
    mapping.nestedFieldBuilder(FIELD_MEASURES)
      .addStringFied(FIELD_MEASURES_KEY)
      .addDoubleField(FIELD_MEASURES_VALUE)
      .build();
    mapping.setEnableSource(false);
  }
}
