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
package org.sonar.server.component.es;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.config.Settings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

public class ProjectMeasuresIndexDefinition implements IndexDefinition {

  public static final String INDEX_PROJECT_MEASURES = "projectmeasures";

  public static final String TYPE_PROJECT_MEASURES = "projectmeasures";
  public static final String FIELD_KEY = "key";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_ANALYSED_AT = "analysedAt";
  public static final String FIELD_QUALITY_GATE = "qualityGate";
  public static final String FIELD_MEASURES = "measures";
  public static final String FIELD_MEASURES_KEY = "key";
  public static final String FIELD_MEASURES_VALUE = "value";

  public static final String TYPE_AUTHORIZATION = "authorization";
  public static final String FIELD_AUTHORIZATION_PROJECT_UUID = "project";
  public static final String FIELD_AUTHORIZATION_GROUPS = "groupNames";
  public static final String FIELD_AUTHORIZATION_USERS = "users";
  public static final String FIELD_AUTHORIZATION_UPDATED_AT = "updatedAt";

  private final Settings settings;

  public ProjectMeasuresIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX_PROJECT_MEASURES);
    index.refreshHandledByIndexer();
    index.configureShards(settings, 5);

    // type "projectmeasures"
    NewIndex.NewIndexType mapping = index.createType(TYPE_PROJECT_MEASURES);
    mapping.setAttribute("_parent", ImmutableMap.of("type", TYPE_AUTHORIZATION));
    mapping.setAttribute("_routing", ImmutableMap.of("required", "true"));
    mapping.stringFieldBuilder(FIELD_KEY).disableNorms().build();
    mapping.stringFieldBuilder(FIELD_NAME).enableSorting().enableGramSearch().build();
    mapping.stringFieldBuilder(FIELD_QUALITY_GATE).build();
    mapping.createDateTimeField(FIELD_ANALYSED_AT);
    mapping.nestedFieldBuilder(FIELD_MEASURES)
      .addStringFied(FIELD_MEASURES_KEY)
      .addDoubleField(FIELD_MEASURES_VALUE)
      .build();

    // do not store document but only indexation of information
    mapping.setEnableSource(false);

    // type "authorization"
    NewIndex.NewIndexType authorizationMapping = index.createType(TYPE_AUTHORIZATION);
    authorizationMapping.setAttribute("_routing", ImmutableMap.of("required", "true"));
    authorizationMapping.createDateTimeField(FIELD_AUTHORIZATION_UPDATED_AT);
    authorizationMapping.stringFieldBuilder(FIELD_AUTHORIZATION_PROJECT_UUID).disableNorms().build();
    authorizationMapping.stringFieldBuilder(FIELD_AUTHORIZATION_GROUPS).disableNorms().build();
    authorizationMapping.stringFieldBuilder(FIELD_AUTHORIZATION_USERS).disableNorms().build();
    authorizationMapping.setEnableSource(false);
  }
}
