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
package org.sonar.server.test.index;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.config.Configuration;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.NewIndex;

public class TestIndexDefinition implements IndexDefinition {

  public static final IndexType INDEX_TYPE_TEST = new IndexType("tests", "test");
  public static final String FIELD_PROJECT_UUID = "projectUuid";
  public static final String FIELD_FILE_UUID = "fileUuid";
  public static final String FIELD_TEST_UUID = "testUuid";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_STATUS = "status";
  public static final String FIELD_DURATION_IN_MS = "durationInMs";
  public static final String FIELD_MESSAGE = "message";
  public static final String FIELD_STACKTRACE = "stacktrace";
  public static final String FIELD_COVERED_FILES = "coveredFiles";
  public static final String FIELD_COVERED_FILE_UUID = "sourceFileUuid";
  public static final String FIELD_COVERED_FILE_LINES = "coveredLines";
  public static final String FIELD_UPDATED_AT = "updatedAt";

  private final Configuration config;

  public TestIndexDefinition(Configuration config) {
    this.config = config;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX_TYPE_TEST.getIndex());

    index.refreshHandledByIndexer();
    index.configureShards(config, 5);

    NewIndex.NewIndexType mapping = index.createType(INDEX_TYPE_TEST.getType());
    mapping.setAttribute("_routing", ImmutableMap.of("required", true));
    mapping.stringFieldBuilder(FIELD_PROJECT_UUID).disableNorms().build();
    mapping.stringFieldBuilder(FIELD_FILE_UUID).disableNorms().build();
    mapping.stringFieldBuilder(FIELD_TEST_UUID).disableNorms().build();
    mapping.stringFieldBuilder(FIELD_NAME).disableNorms().disableSearch().build();
    mapping.stringFieldBuilder(FIELD_STATUS).disableNorms().disableSearch().build();
    mapping.createLongField(FIELD_DURATION_IN_MS);
    mapping.stringFieldBuilder(FIELD_MESSAGE).disableNorms().disableSearch().build();
    mapping.stringFieldBuilder(FIELD_STACKTRACE).disableNorms().disableSearch().build();
    mapping.setProperty(FIELD_COVERED_FILES, ImmutableMap.of("type", "nested", "properties", ImmutableMap.of(
      FIELD_COVERED_FILE_UUID, ImmutableMap.of("type", "string", "index", "not_analyzed"),
      FIELD_COVERED_FILE_LINES, ImmutableMap.of("type", "integer"))));
    mapping.createDateTimeField(FIELD_UPDATED_AT);
  }
}
