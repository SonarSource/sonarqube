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

package org.sonar.server.test.index;

import org.sonar.api.config.Settings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

public class TestIndexDefinition implements IndexDefinition {
  public static final String INDEX = "tests";
  public static final String TYPE = "test";
  public static final String FIELD_UUID = "uuid";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_STATUS = "status";
  public static final String FIELD_TYPE = "type";
  public static final String FIELD_DURATION_IN_MS = "durationInMs";
  public static final String FIELD_MESSAGE = "message";
  public static final String FIELD_STACKTRACE = "stacktrace";
  public static final String FIELD_COVERAGE_BLOCKS = "coverageBlocks";
  public static final String FIELD_COVERAGE_BLOCK_UUID = "uuid";
  public static final String FIELD_COVERAGE_BLOCK_LINES = "lines";

  private final Settings settings;

  public TestIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX);

    index.refreshHandledByIndexer();
    index.setShards(settings);

    NewIndex.NewIndexType nestedMapping = index.createType(TYPE);
    nestedMapping.stringFieldBuilder(FIELD_COVERAGE_BLOCK_UUID).build();
    nestedMapping.createIntegerField(FIELD_COVERAGE_BLOCK_LINES);

    NewIndex.NewIndexType mapping = index.createType(TYPE);
    mapping.stringFieldBuilder(FIELD_UUID).build();
    mapping.stringFieldBuilder(FIELD_NAME).build();
    mapping.stringFieldBuilder(FIELD_STATUS).disableSearch().build();
    mapping.stringFieldBuilder(FIELD_TYPE).disableSearch().build();
    mapping.createLongField(FIELD_DURATION_IN_MS);
    mapping.stringFieldBuilder(FIELD_MESSAGE).disableSearch().build();
    mapping.stringFieldBuilder(FIELD_STACKTRACE).disableSearch().build();
    mapping.nestedObjectBuilder(FIELD_COVERAGE_BLOCKS, nestedMapping).build();
  }
}
