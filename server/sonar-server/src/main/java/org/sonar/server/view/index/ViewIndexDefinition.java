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
package org.sonar.server.view.index;

import org.sonar.api.config.Configuration;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.NewIndex;

import static org.sonar.server.es.NewIndex.SettingsConfiguration.newBuilder;

/**
 * Definition of ES index "views", including settings and fields.
 */
public class ViewIndexDefinition implements IndexDefinition {

  public static final IndexType INDEX_TYPE_VIEW = new IndexType("views", "view");
  public static final String FIELD_UUID = "uuid";
  public static final String FIELD_PROJECTS = "projects";

  private final Configuration config;

  public ViewIndexDefinition(Configuration config) {
    this.config = config;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(
      INDEX_TYPE_VIEW.getIndex(),
      newBuilder(config)
        .setDefaultNbOfShards(5)
        .build());

    // type "view"
    NewIndex.NewIndexType mapping = index.createType(INDEX_TYPE_VIEW.getType());
    mapping.keywordFieldBuilder(FIELD_UUID).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_PROJECTS).disableNorms().build();
  }
}
