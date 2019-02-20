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
package org.sonar.server.view.index;

import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.newindex.NewRegularIndex;
import org.sonar.server.es.newindex.TypeMapping;

import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

/**
 * Definition of ES index "views", including settings and fields.
 */
public class ViewIndexDefinition implements IndexDefinition {

  public static final Index DESCRIPTOR = Index.simple("views");
  public static final IndexMainType TYPE_VIEW = IndexType.main(DESCRIPTOR, "view");
  public static final String FIELD_UUID = "uuid";
  public static final String FIELD_PROJECTS = "projects";

  private final Configuration config;

  public ViewIndexDefinition(Configuration config) {
    this.config = config;
  }

  /**
   * Keep the document sources in index so that indexer tests can verify content
   * of indexed documents.
   */
  public static ViewIndexDefinition createForTest() {
    return new ViewIndexDefinition(new MapSettings().asConfig());
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewRegularIndex index = context.create(
      DESCRIPTOR,
      newBuilder(config)
        .setDefaultNbOfShards(5)
        .build())
      // storing source is required because some search queries on issue index use terms lookup query onto the view index
      // and this requires source to be stored (https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-terms-query.html#query-dsl-terms-lookup)
      .setEnableSource(true);

    // type "view"
    TypeMapping mapping = index.createTypeMapping(TYPE_VIEW);
    mapping.keywordFieldBuilder(FIELD_UUID).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_PROJECTS).disableNorms().build();
  }
}
