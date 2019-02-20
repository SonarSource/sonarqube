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
package org.sonar.server.es.metadata;

import org.sonar.api.config.Configuration;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexDefinition.IndexDefinitionContext;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.newindex.NewRegularIndex;

import static org.sonar.server.es.newindex.SettingsConfiguration.MANUAL_REFRESH_INTERVAL;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

public class MetadataIndexDefinition {

  public static final Index DESCRIPTOR = Index.simple("metadatas");
  public static final IndexMainType TYPE_METADATA = IndexType.main(DESCRIPTOR, "metadata");
  public static final String FIELD_VALUE = "value";

  private static final int DEFAULT_NUMBER_OF_SHARDS = 1;

  private final Configuration configuration;

  public MetadataIndexDefinition(Configuration configuration) {
    this.configuration = configuration;
  }

  public void define(IndexDefinitionContext context) {
    NewRegularIndex index = context.create(
      DESCRIPTOR,
      newBuilder(configuration)
        .setRefreshInterval(MANUAL_REFRESH_INTERVAL)
        .setDefaultNbOfShards(DEFAULT_NUMBER_OF_SHARDS)
        .build());

    index.createTypeMapping(TYPE_METADATA)
      .keywordFieldBuilder(FIELD_VALUE).disableSearch().store().build();
  }
}
