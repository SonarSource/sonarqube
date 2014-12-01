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

import org.sonar.server.search.BaseDoc;
import org.sonar.server.search.BaseNormalizer;
import org.sonar.server.search.IndexUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.Map;

public class SourceLineDoc extends BaseDoc {

  public SourceLineDoc(Map<String, Object> fields) {
    super(fields);
  }

  public String projectUuid() {
    return getField(SourceLineIndexDefinition.FIELD_PROJECT_UUID);
  }

  public void setProjectUuid(String projectUuid) {
    setField(SourceLineIndexDefinition.FIELD_PROJECT_UUID, projectUuid);
  }

  public String fileUuid() {
    return getField(SourceLineIndexDefinition.FIELD_FILE_UUID);
  }

  public void setFileUuid(String fileUuid) {
    setField(SourceLineIndexDefinition.FIELD_FILE_UUID, fileUuid);
  }

  public int line() {
    return ((Number)getField(SourceLineIndexDefinition.FIELD_LINE)).intValue();
  }

  public void setLine(int line) {
    setField(SourceLineIndexDefinition.FIELD_LINE, line);
  }

  @CheckForNull
  public String scmRevision() {
    return getNullableField(SourceLineIndexDefinition.FIELD_SCM_REVISION);
  }

  public void setScmRevision(String scmRevision) {
    setField(SourceLineIndexDefinition.FIELD_SCM_REVISION, scmRevision);
  }

  @CheckForNull
  public String scmAuthor() {
    return getNullableField(SourceLineIndexDefinition.FIELD_SCM_AUTHOR);
  }

  public void setScmAuthor(String scmAuthor) {
    setField(SourceLineIndexDefinition.FIELD_SCM_AUTHOR, scmAuthor);
  }

  @CheckForNull
  public Date scmDate() {
    return IndexUtils.parseDateTime(this.<String>getNullableField(SourceLineIndexDefinition.FIELD_SCM_DATE));
  }

  public void setScmDate(@Nullable Date scmDate) {
    setField(SourceLineIndexDefinition.FIELD_SCM_DATE, scmDate);
  }

  @CheckForNull
  public String highlighting() {
    return getNullableField(SourceLineIndexDefinition.FIELD_HIGHLIGHTING);
  }

  public void setHighlighting(String s) {
    setField(SourceLineIndexDefinition.FIELD_HIGHLIGHTING, s);
  }

  public String source() {
    return getField(SourceLineIndexDefinition.FIELD_SOURCE);
  }

  public void setSource(String source) {
    setField(SourceLineIndexDefinition.FIELD_SOURCE, source);
  }

  public Date updateDate() {
    return getFieldAsDate(BaseNormalizer.UPDATED_AT_FIELD);
  }

  public void setUpdateDate(Date updatedAt) {
    setField(BaseNormalizer.UPDATED_AT_FIELD, updatedAt);
  }

  public String key() {
    return String.format("%s_%d", fileUuid(), line());
  }

  @CheckForNull
  public Integer utLineHits() {
    Number lineHits = (Number)getNullableField(SourceLineIndexDefinition.FIELD_UT_LINE_HITS);
    return lineHits == null ? null : lineHits.intValue();
  }

  public void setUtLineHits(@Nullable Integer lineHits) {
    setField(SourceLineIndexDefinition.FIELD_UT_LINE_HITS, lineHits);
  }

  @CheckForNull
  public Integer utConditions() {
    Number conditions = (Number)getNullableField(SourceLineIndexDefinition.FIELD_UT_CONDITIONS);
    return conditions == null ? null : conditions.intValue();
  }

  public void setUtConditions(@Nullable Integer conditions) {
    setField(SourceLineIndexDefinition.FIELD_UT_CONDITIONS, conditions);
  }

  @CheckForNull
  public Integer utCoveredConditions() {
    Number coveredConditions = (Number)getNullableField(SourceLineIndexDefinition.FIELD_UT_COVERED_CONDITIONS);
    return coveredConditions == null ? null : coveredConditions.intValue();
  }

  public void setUtCoveredConditions(@Nullable Integer coveredConditions) {
    setField(SourceLineIndexDefinition.FIELD_UT_COVERED_CONDITIONS, coveredConditions);
  }

  @CheckForNull
  public Integer itLineHits() {
    Number lineHits = (Number)getNullableField(SourceLineIndexDefinition.FIELD_IT_LINE_HITS);
    return lineHits == null ? null : lineHits.intValue();
  }

  public void setItLineHits(@Nullable Integer lineHits) {
    setField(SourceLineIndexDefinition.FIELD_IT_LINE_HITS, lineHits);
  }

  @CheckForNull
  public Integer itConditions() {
    Number conditions = (Number)getNullableField(SourceLineIndexDefinition.FIELD_IT_CONDITIONS);
    return conditions == null ? null : conditions.intValue();
  }

  public void setItConditions(@Nullable Integer conditions) {
    setField(SourceLineIndexDefinition.FIELD_IT_CONDITIONS, conditions);
  }

  @CheckForNull
  public Integer itCoveredConditions() {
    Number coveredConditions = (Number)getNullableField(SourceLineIndexDefinition.FIELD_IT_COVERED_CONDITIONS);
    return coveredConditions == null ? null : coveredConditions.intValue();
  }

  public void setItCoveredConditions(@Nullable Integer coveredConditions) {
    setField(SourceLineIndexDefinition.FIELD_IT_COVERED_CONDITIONS, coveredConditions);
  }

  @CheckForNull
  public Integer overallLineHits() {
    Number lineHits = (Number)getNullableField(SourceLineIndexDefinition.FIELD_OVERALL_LINE_HITS);
    return lineHits == null ? null : lineHits.intValue();
  }

  public void setOverallLineHits(@Nullable Integer lineHits) {
    setField(SourceLineIndexDefinition.FIELD_OVERALL_LINE_HITS, lineHits);
  }

  @CheckForNull
  public Integer overallConditions() {
    Number conditions = (Number)getNullableField(SourceLineIndexDefinition.FIELD_OVERALL_CONDITIONS);
    return conditions == null ? null : conditions.intValue();
  }

  public void setOverallConditions(@Nullable Integer conditions) {
    setField(SourceLineIndexDefinition.FIELD_OVERALL_CONDITIONS, conditions);
  }

  @CheckForNull
  public Integer overallCoveredConditions() {
    Number coveredConditions = (Number)getNullableField(SourceLineIndexDefinition.FIELD_OVERALL_COVERED_CONDITIONS);
    return coveredConditions == null ? null : coveredConditions.intValue();
  }

  public void setOverallCoveredConditions(@Nullable Integer coveredConditions) {
    setField(SourceLineIndexDefinition.FIELD_OVERALL_COVERED_CONDITIONS, coveredConditions);
  }

  @CheckForNull
  public String symbols() {
    return getNullableField(SourceLineIndexDefinition.FIELD_SYMBOLS);
  }

  public void setSymbols(@Nullable String s) {
    setField(SourceLineIndexDefinition.FIELD_SYMBOLS, s);
  }

}
