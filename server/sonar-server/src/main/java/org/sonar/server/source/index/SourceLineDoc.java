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

import com.google.common.collect.ImmutableList;
import org.sonar.server.search.BaseDoc;
import org.sonar.server.search.IndexUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SourceLineDoc extends BaseDoc {

  public SourceLineDoc(Map<String, Object> fields) {
    super(fields);
  }

  public SourceLineDoc() {
    this(new HashMap<String, Object>(20));
  }

  public String projectUuid() {
    return getField(SourceLineIndexDefinition.FIELD_PROJECT_UUID);
  }

  public SourceLineDoc setProjectUuid(String projectUuid) {
    setField(SourceLineIndexDefinition.FIELD_PROJECT_UUID, projectUuid);
    return this;
  }

  public String fileUuid() {
    return getField(SourceLineIndexDefinition.FIELD_FILE_UUID);
  }

  public SourceLineDoc setFileUuid(String fileUuid) {
    setField(SourceLineIndexDefinition.FIELD_FILE_UUID, fileUuid);
    return this;
  }

  public int line() {
    return ((Number) getField(SourceLineIndexDefinition.FIELD_LINE)).intValue();
  }

  public SourceLineDoc setLine(int line) {
    setField(SourceLineIndexDefinition.FIELD_LINE, line);
    return this;
  }

  @CheckForNull
  public String scmRevision() {
    return getNullableField(SourceLineIndexDefinition.FIELD_SCM_REVISION);
  }

  public SourceLineDoc setScmRevision(@Nullable String scmRevision) {
    setField(SourceLineIndexDefinition.FIELD_SCM_REVISION, scmRevision);
    return this;
  }

  @CheckForNull
  public String scmAuthor() {
    return getNullableField(SourceLineIndexDefinition.FIELD_SCM_AUTHOR);
  }

  public SourceLineDoc setScmAuthor(@Nullable String scmAuthor) {
    setField(SourceLineIndexDefinition.FIELD_SCM_AUTHOR, scmAuthor);
    return this;
  }

  @CheckForNull
  public Date scmDate() {
    return IndexUtils.parseDateTime(this.<String>getNullableField(SourceLineIndexDefinition.FIELD_SCM_DATE));
  }

  public SourceLineDoc setScmDate(@Nullable Date scmDate) {
    setField(SourceLineIndexDefinition.FIELD_SCM_DATE, scmDate);
    return this;
  }

  @CheckForNull
  public String highlighting() {
    return getNullableField(SourceLineIndexDefinition.FIELD_HIGHLIGHTING);
  }

  public SourceLineDoc setHighlighting(@Nullable String s) {
    setField(SourceLineIndexDefinition.FIELD_HIGHLIGHTING, s);
    return this;
  }

  public String source() {
    return getField(SourceLineIndexDefinition.FIELD_SOURCE);
  }

  public SourceLineDoc setSource(@Nullable String source) {
    setField(SourceLineIndexDefinition.FIELD_SOURCE, source);
    return this;
  }

  public Date updateDate() {
    return getFieldAsDate(SourceLineIndexDefinition.FIELD_UPDATED_AT);
  }

  public SourceLineDoc setUpdateDate(@Nullable Date updatedAt) {
    setField(SourceLineIndexDefinition.FIELD_UPDATED_AT, updatedAt);
    return this;
  }

  @CheckForNull
  public Integer utLineHits() {
    Number lineHits = getNullableField(SourceLineIndexDefinition.FIELD_UT_LINE_HITS);
    return lineHits == null ? null : lineHits.intValue();
  }

  public SourceLineDoc setUtLineHits(@Nullable Integer lineHits) {
    setField(SourceLineIndexDefinition.FIELD_UT_LINE_HITS, lineHits);
    return this;
  }

  @CheckForNull
  public Integer utConditions() {
    Number conditions = getNullableField(SourceLineIndexDefinition.FIELD_UT_CONDITIONS);
    return conditions == null ? null : conditions.intValue();
  }

  public SourceLineDoc setUtConditions(@Nullable Integer conditions) {
    setField(SourceLineIndexDefinition.FIELD_UT_CONDITIONS, conditions);
    return this;
  }

  @CheckForNull
  public Integer utCoveredConditions() {
    Number coveredConditions = getNullableField(SourceLineIndexDefinition.FIELD_UT_COVERED_CONDITIONS);
    return coveredConditions == null ? null : coveredConditions.intValue();
  }

  public SourceLineDoc setUtCoveredConditions(@Nullable Integer coveredConditions) {
    setField(SourceLineIndexDefinition.FIELD_UT_COVERED_CONDITIONS, coveredConditions);
    return this;
  }

  @CheckForNull
  public Integer itLineHits() {
    Number lineHits = getNullableField(SourceLineIndexDefinition.FIELD_IT_LINE_HITS);
    return lineHits == null ? null : lineHits.intValue();
  }

  public SourceLineDoc setItLineHits(@Nullable Integer lineHits) {
    setField(SourceLineIndexDefinition.FIELD_IT_LINE_HITS, lineHits);
    return this;
  }

  @CheckForNull
  public Integer itConditions() {
    Number conditions = getNullableField(SourceLineIndexDefinition.FIELD_IT_CONDITIONS);
    return conditions == null ? null : conditions.intValue();
  }

  public SourceLineDoc setItConditions(@Nullable Integer conditions) {
    setField(SourceLineIndexDefinition.FIELD_IT_CONDITIONS, conditions);
    return this;
  }

  @CheckForNull
  public Integer itCoveredConditions() {
    Number coveredConditions = (Number) getNullableField(SourceLineIndexDefinition.FIELD_IT_COVERED_CONDITIONS);
    return coveredConditions == null ? null : coveredConditions.intValue();
  }

  public SourceLineDoc setItCoveredConditions(@Nullable Integer coveredConditions) {
    setField(SourceLineIndexDefinition.FIELD_IT_COVERED_CONDITIONS, coveredConditions);
    return this;
  }

  @CheckForNull
  public Integer overallLineHits() {
    Number lineHits = getNullableField(SourceLineIndexDefinition.FIELD_OVERALL_LINE_HITS);
    return lineHits == null ? null : lineHits.intValue();
  }

  public SourceLineDoc setOverallLineHits(@Nullable Integer lineHits) {
    setField(SourceLineIndexDefinition.FIELD_OVERALL_LINE_HITS, lineHits);
    return this;
  }

  @CheckForNull
  public Integer overallConditions() {
    Number conditions = getNullableField(SourceLineIndexDefinition.FIELD_OVERALL_CONDITIONS);
    return conditions == null ? null : conditions.intValue();
  }

  public SourceLineDoc setOverallConditions(@Nullable Integer conditions) {
    setField(SourceLineIndexDefinition.FIELD_OVERALL_CONDITIONS, conditions);
    return this;
  }

  @CheckForNull
  public Integer overallCoveredConditions() {
    Number coveredConditions = getNullableField(SourceLineIndexDefinition.FIELD_OVERALL_COVERED_CONDITIONS);
    return coveredConditions == null ? null : coveredConditions.intValue();
  }

  public SourceLineDoc setOverallCoveredConditions(@Nullable Integer coveredConditions) {
    setField(SourceLineIndexDefinition.FIELD_OVERALL_COVERED_CONDITIONS, coveredConditions);
    return this;
  }

  @CheckForNull
  public String symbols() {
    return getNullableField(SourceLineIndexDefinition.FIELD_SYMBOLS);
  }

  public SourceLineDoc setSymbols(@Nullable String s) {
    setField(SourceLineIndexDefinition.FIELD_SYMBOLS, s);
    return this;
  }

  public Collection<Integer> duplications() {
    Collection<Integer> duplications = getNullableField(SourceLineIndexDefinition.FIELD_DUPLICATIONS);
    return duplications == null ? ImmutableList.<Integer>of() : duplications;
  }

  public SourceLineDoc setDuplications(@Nullable Collection<Integer> dups) {
    setField(SourceLineIndexDefinition.FIELD_DUPLICATIONS, dups == null ? ImmutableList.<Integer>of() : dups);
    return this;
  }
}
