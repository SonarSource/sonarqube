/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.permission.index.AuthorizationDoc;

import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_CREATED_AT;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_LANGUAGES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NAME;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NCLOC_DISTRIBUTION;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_ORGANIZATION_UUID;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_QUALITY_GATE_STATUS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_TAGS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_UUID;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.SUB_FIELD_DISTRIB_LANGUAGE;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.SUB_FIELD_DISTRIB_NCLOC;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.SUB_FIELD_MEASURES_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.SUB_FIELD_MEASURES_VALUE;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;

public class ProjectMeasuresDoc extends BaseDoc {

  public static final Map<String, Integer> QUALITY_GATE_STATUS = Map.of(OK.name(), 1, ERROR.name(), 3);

  public ProjectMeasuresDoc() {
    super(TYPE_PROJECT_MEASURES, new HashMap<>(8));
  }

  @Override
  public String getId() {
    return getField(FIELD_UUID);
  }

  public ProjectMeasuresDoc setId(String s) {
    setField(FIELD_UUID, s);
    setParent(AuthorizationDoc.idOf(s));
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

  public String getQualifier() {
    return getField(FIELD_QUALIFIER);
  }

  public ProjectMeasuresDoc setQualifier(String s) {
    setField(FIELD_QUALIFIER, s);
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

  public ProjectMeasuresDoc setCreatedAt(Date d) {
    setField(FIELD_CREATED_AT, d);
    return this;
  }

  public Date getCreatedAt() {
    return getField(FIELD_CREATED_AT);
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
        .map(entry -> Map.<String, Object>of(
          SUB_FIELD_MEASURES_KEY, entry.getKey(),
          SUB_FIELD_MEASURES_VALUE, entry.getValue()))
        .toList());
    return this;
  }

  public ProjectMeasuresDoc setLanguages(List<String> languages) {
    setField(FIELD_LANGUAGES, languages);
    return this;
  }

  public Collection<Map<String, Object>> getNclocLanguageDistribution() {
    return getField(FIELD_NCLOC_DISTRIBUTION);
  }

  public ProjectMeasuresDoc setNclocLanguageDistribution(Collection<Map<String, Object>> distribution) {
    setField(FIELD_NCLOC_DISTRIBUTION, distribution);
    return this;
  }

  public ProjectMeasuresDoc setNclocLanguageDistributionFromMap(Map<String, Integer> distribution) {
    setNclocLanguageDistribution(
      distribution.entrySet().stream()
        .map(entry -> Map.<String, Object>of(
          SUB_FIELD_DISTRIB_LANGUAGE, entry.getKey(),
          SUB_FIELD_DISTRIB_NCLOC, entry.getValue()))
        .toList());
    return this;
  }

  public ProjectMeasuresDoc setQualityGateStatus(@Nullable String s) {
    setField(FIELD_QUALITY_GATE_STATUS, s != null ? QUALITY_GATE_STATUS.get(s) : null);
    return this;
  }

  public ProjectMeasuresDoc setTags(List<String> tags) {
    setField(FIELD_TAGS, tags);
    return this;
  }

}
