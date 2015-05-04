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
    nestedMapping.stringFieldBuilder(FIELD_COVERED_FILE_UUID).build();
    nestedMapping.createIntegerField(FIELD_COVERED_FILE_LINES);

    NewIndex.NewIndexType mapping = index.createType(TYPE);
    mapping.stringFieldBuilder(FIELD_PROJECT_UUID).build();
    mapping.stringFieldBuilder(FIELD_FILE_UUID).build();
    mapping.stringFieldBuilder(FIELD_TEST_UUID).build();
    mapping.stringFieldBuilder(FIELD_NAME).disableSearch().build();
    mapping.stringFieldBuilder(FIELD_STATUS).disableSearch().build();
    mapping.createLongField(FIELD_DURATION_IN_MS);
    mapping.stringFieldBuilder(FIELD_MESSAGE).disableSearch().build();
    mapping.stringFieldBuilder(FIELD_STACKTRACE).disableSearch().build();
    mapping.nestedObjectBuilder(FIELD_COVERED_FILES, nestedMapping).build();
    mapping.createDateTimeField(FIELD_UPDATED_AT);
  }
}
