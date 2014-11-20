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
    return getField(SourceLineIndexDefinition.FIELD_LINE);
  }

  public void setLine(int line) {
    setField(SourceLineIndexDefinition.FIELD_LINE, line);
  }

  public String scmRevision() {
    return getField(SourceLineIndexDefinition.FIELD_SCM_REVISION);
  }

  public void setScmRevision(String scmRevision) {
    setField(SourceLineIndexDefinition.FIELD_SCM_REVISION, scmRevision);
  }

  public String scmAuthor() {
    return getField(SourceLineIndexDefinition.FIELD_SCM_AUTHOR);
  }

  public void setScmAuthor(String scmAuthor) {
    setField(SourceLineIndexDefinition.FIELD_SCM_AUTHOR, scmAuthor);
  }

  public Date scmDate() {
    return getField(SourceLineIndexDefinition.FIELD_SCM_DATE);
  }

  public void setScmDate(Date scmDate) {
    setField(SourceLineIndexDefinition.FIELD_SCM_DATE, scmDate);
  }

  public String highlighting() {
    return getField(SourceLineIndexDefinition.FIELD_HIGHLIGHTING);
  }

  public void setHighlighting(String fileUuid) {
    setField(SourceLineIndexDefinition.FIELD_HIGHLIGHTING, fileUuid);
  }

  public String source() {
    return getField(SourceLineIndexDefinition.FIELD_SOURCE);
  }

  public void setSource(String source) {
    setField(SourceLineIndexDefinition.FIELD_SOURCE, source);
  }

  public Date updateDate() {
    return getField(BaseNormalizer.UPDATED_AT_FIELD);
  }

  public void setUpdateDate(Date updatedAt) {
    setField(BaseNormalizer.UPDATED_AT_FIELD, updatedAt);
  }

  public String key() {
    return String.format("%s_%d", fileUuid(), line());
  }
}
