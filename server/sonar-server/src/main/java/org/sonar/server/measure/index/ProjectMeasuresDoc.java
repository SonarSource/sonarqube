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
package org.sonar.server.measure.index;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.util.stream.Collectors;
import org.sonar.server.es.BaseDoc;

import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_LANGUAGES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_LANGUAGES_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_LANGUAGES_VALUE;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES_VALUE;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NAME;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_ORGANIZATION_UUID;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_QUALITY_GATE;

public class ProjectMeasuresDoc extends BaseDoc {

  public ProjectMeasuresDoc() {
    super(new HashMap<>(7));
  }

  @Override
  public String getId() {
    return getField("_id");
  }

  @Override
  public String getRouting() {
    return getId();
  }

  @Override
  public String getParent() {
    return getId();
  }

  public ProjectMeasuresDoc setId(String s) {
    setField("_id", s);
    return this;
  }

  public String getOrganizationUuid() {
    return getField(FIELD_ORGANIZATION_UUID);
  }

  public ProjectMeasuresDoc setOrganizationUuid(String s) {
    setField(FIELD_ORGANIZATION_UUID, s);
    return this;
  }

  public String getKey() {
    return getField(FIELD_KEY);
  }

  public ProjectMeasuresDoc setKey(String s) {
    setField(FIELD_KEY, s);
    return this;
  }

  public String getName() {
    return getField(FIELD_NAME);
  }

  public ProjectMeasuresDoc setName(String s) {
    setField(FIELD_NAME, s);
    return this;
  }

  @CheckForNull
  public Date getAnalysedAt() {
    return getNullableField(FIELD_ANALYSED_AT);
  }

  public ProjectMeasuresDoc setAnalysedAt(@Nullable Date d) {
    setField(FIELD_ANALYSED_AT, d);
    return this;
  }

  public Collection<Map<String, Object>> getMeasures() {
    return getField(FIELD_MEASURES);
  }

  public ProjectMeasuresDoc setMeasures(Collection<Map<String, Object>> measures) {
    setField(FIELD_MEASURES, measures);
    return this;
  }

  public ProjectMeasuresDoc setMeasuresFromMap(Map<String, Double> measures) {
    setMeasures(
      measures.entrySet().stream()
        .map(entry -> ImmutableMap.<String, Object>of(
          FIELD_MEASURES_KEY, entry.getKey(),
          FIELD_MEASURES_VALUE, entry.getValue()))
        .collect(Collectors.toList()));
    return this;
  }

  public ProjectMeasuresDoc setLanguages(Map<String, Integer> languageDistribution) {
    setField(FIELD_LANGUAGES,
      languageDistribution.entrySet().stream()
        .map(entry -> ImmutableMap.<String, Object>of(
          FIELD_LANGUAGES_KEY, entry.getKey(),
          FIELD_LANGUAGES_VALUE, entry.getValue()))
        .collect(Collectors.toList()));
    return this;
  }

  @CheckForNull
  public String getQualityGate() {
    return getField(FIELD_QUALITY_GATE);
  }

  public ProjectMeasuresDoc setQualityGate(@Nullable String s) {
    setField(FIELD_QUALITY_GATE, s);
    return this;
  }
}
