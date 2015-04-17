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
package org.sonar.server.source.index;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.config.Settings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

public class SourceLineIndexDefinition implements IndexDefinition {

  public static final String INDEX = "sourcelines";
  public static final String TYPE = "sourceline";
  public static final String FIELD_PROJECT_UUID = "projectUuid";
  public static final String FIELD_FILE_UUID = "fileUuid";
  public static final String FIELD_LINE = "line";
  public static final String FIELD_SCM_REVISION = "scmRevision";
  public static final String FIELD_SCM_AUTHOR = "scmAuthor";
  public static final String FIELD_SCM_DATE = "scmDate";
  public static final String FIELD_HIGHLIGHTING = "highlighting";
  public static final String FIELD_SOURCE = "source";
  public static final String FIELD_UT_LINE_HITS = "utLineHits";
  public static final String FIELD_UT_CONDITIONS = "utConditions";
  public static final String FIELD_UT_COVERED_CONDITIONS = "utCoveredConditions";
  public static final String FIELD_IT_LINE_HITS = "itLineHits";
  public static final String FIELD_IT_CONDITIONS = "itConditions";
  public static final String FIELD_IT_COVERED_CONDITIONS = "itCoveredConditions";
  public static final String FIELD_OVERALL_LINE_HITS = "overallLineHits";
  public static final String FIELD_OVERALL_CONDITIONS = "overallConditions";
  public static final String FIELD_OVERALL_COVERED_CONDITIONS = "overallCoveredConditions";
  public static final String FIELD_SYMBOLS = "symbols";
  public static final String FIELD_DUPLICATIONS = "duplications";
  public static final String FIELD_UPDATED_AT = "updatedAt";

  private final Settings settings;

  public SourceLineIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX);

    index.refreshHandledByIndexer();
    index.setShards(settings);

    // type "sourceline"
    NewIndex.NewIndexType mapping = index.createType(TYPE);
    mapping.setAttribute("_routing", ImmutableMap.of("required", true, "path", FIELD_PROJECT_UUID));
    mapping.stringFieldBuilder(FIELD_PROJECT_UUID).build();
    mapping.stringFieldBuilder(FIELD_FILE_UUID).build();
    mapping.createIntegerField(FIELD_LINE);
    mapping.stringFieldBuilder(FIELD_SCM_REVISION).disableSearch().build();
    mapping.stringFieldBuilder(FIELD_SCM_AUTHOR).disableSearch().build();
    mapping.createDateTimeField(FIELD_SCM_DATE);
    mapping.stringFieldBuilder(FIELD_HIGHLIGHTING).disableSearch().build();
    mapping.stringFieldBuilder(FIELD_SOURCE).disableSearch().build();
    mapping.createIntegerField(FIELD_UT_LINE_HITS);
    mapping.createIntegerField(FIELD_UT_CONDITIONS);
    mapping.createIntegerField(FIELD_UT_COVERED_CONDITIONS);
    mapping.createIntegerField(FIELD_IT_LINE_HITS);
    mapping.createIntegerField(FIELD_IT_CONDITIONS);
    mapping.createIntegerField(FIELD_IT_COVERED_CONDITIONS);
    mapping.createIntegerField(FIELD_OVERALL_LINE_HITS);
    mapping.createIntegerField(FIELD_OVERALL_CONDITIONS);
    mapping.createIntegerField(FIELD_OVERALL_COVERED_CONDITIONS);
    mapping.stringFieldBuilder(FIELD_SYMBOLS).disableSearch().build();
    mapping.createShortField(FIELD_DUPLICATIONS);
    mapping.createDateTimeField(FIELD_UPDATED_AT);
  }

  public static String docKey(String fileUuid, int line) {
    return String.format("%s_%d", fileUuid, line);
  }
}
