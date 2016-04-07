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
package org.sonar.server.activity.index;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.config.Settings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

/**
 * Definition of ES index "activities", including settings and fields.
 */
public class ActivityIndexDefinition implements IndexDefinition {

  public static final String INDEX = "activities";
  public static final String TYPE = "activity";
  public static final String FIELD_KEY = "key";
  public static final String FIELD_TYPE = "type";
  public static final String FIELD_ACTION = "action";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_LOGIN = "login";
  public static final String FIELD_DETAILS = "details";
  public static final String FIELD_MESSAGE = "message";

  private final Settings settings;

  public ActivityIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX);
    index.getSettings().put("analysis.analyzer.default.type", "keyword");
    index.configureShards(settings);
    index.refreshHandledByIndexer();

    // type "activity"
    NewIndex.NewIndexType mapping = index.createType(TYPE);
    mapping.setAttribute("_id", ImmutableMap.of("path", FIELD_KEY));
    mapping.stringFieldBuilder(FIELD_KEY).build();
    mapping.stringFieldBuilder(FIELD_TYPE).build();
    mapping.stringFieldBuilder(FIELD_ACTION).build();
    mapping.stringFieldBuilder(FIELD_LOGIN).build();
    mapping.createDynamicNestedField(FIELD_DETAILS);
    mapping.stringFieldBuilder(FIELD_MESSAGE).build();
    mapping.createDateTimeField(FIELD_CREATED_AT);
  }
}
