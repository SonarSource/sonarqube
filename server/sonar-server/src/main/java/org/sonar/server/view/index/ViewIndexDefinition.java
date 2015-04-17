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

package org.sonar.server.view.index;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.config.Settings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

/**
 * Definition of ES index "views", including settings and fields.
 */
public class ViewIndexDefinition implements IndexDefinition {

  public static final String INDEX = "views";

  public static final String TYPE_VIEW = "view";

  public static final String FIELD_UUID = "uuid";
  public static final String FIELD_PROJECTS = "projects";

  private final Settings settings;

  public ViewIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX);

    index.setShards(settings);

    // type "view"
    NewIndex.NewIndexType mapping = index.createType(TYPE_VIEW);
    mapping.setAttribute("_id", ImmutableMap.of("path", FIELD_UUID));
    mapping.stringFieldBuilder(FIELD_UUID).build();
    mapping.stringFieldBuilder(FIELD_PROJECTS).build();
  }
}
