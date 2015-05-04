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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.sonar.server.search.BaseDoc;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.sonar.server.test.index.TestIndexDefinition.FIELD_COVERED_FILES;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_DURATION_IN_MS;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_MESSAGE;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_NAME;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_PROJECT_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_STACKTRACE;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_STATUS;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_TEST_UUID;

public class TestDoc extends BaseDoc {
  public TestDoc(Map<String, Object> fields) {
    super(fields);
  }

  @VisibleForTesting
  public TestDoc() {
    super(Maps.<String, Object>newHashMapWithExpectedSize(10));
  }

  public String projectUuid() {
    return getField(FIELD_PROJECT_UUID);
  }

  public TestDoc setProjectUuid(String projectUuid) {
    setField(FIELD_PROJECT_UUID, projectUuid);
    return this;
  }

  public String fileUuid() {
    return getField(FIELD_FILE_UUID);
  }

  public TestDoc setFileUuid(String fileUuid) {
    setField(FIELD_FILE_UUID, fileUuid);
    return this;
  }

  public String testUuid() {
    return getField(FIELD_TEST_UUID);
  }

  public TestDoc setUuid(String testUuid) {
    setField(FIELD_TEST_UUID, testUuid);
    return this;
  }

  public String name() {
    return getField(FIELD_NAME);
  }

  public TestDoc setName(String name) {
    setField(FIELD_NAME, name);
    return this;
  }

  public String status() {
    return getField(FIELD_STATUS);
  }

  public TestDoc setStatus(String status) {
    setField(FIELD_STATUS, status);
    return this;
  }

  @CheckForNull
  public String message() {
    return getNullableField(FIELD_MESSAGE);
  }

  public TestDoc setMessage(String message) {
    setField(FIELD_MESSAGE, message);
    return this;
  }

  @CheckForNull
  public String stackTrace() {
    return getNullableField(FIELD_STACKTRACE);
  }

  public TestDoc setStackTrace(String stackTrace) {
    setField(FIELD_STACKTRACE, stackTrace);
    return this;
  }

  @CheckForNull
  public Long durationInMs() {
    Number number =  getNullableField(FIELD_DURATION_IN_MS);
    return number == null ? null : number.longValue();
  }

  public TestDoc setDurationInMs(Long durationInMs) {
    setField(FIELD_DURATION_IN_MS, durationInMs);
    return this;
  }

  public List<CoveredFileDoc> coveredFiles() {
    List<Map<String, Object>> coveredFilesAsMaps = getNullableField(FIELD_COVERED_FILES);
    if (coveredFilesAsMaps == null) {
      return new ArrayList<>();
    }
    List<CoveredFileDoc> coveredFiles = new ArrayList<>();
    for (Map<String, Object> coveredFileMap : coveredFilesAsMaps) {
      coveredFiles.add(new CoveredFileDoc(coveredFileMap));
    }
    return coveredFiles;
  }

  public TestDoc setCoveredFiles(List<CoveredFileDoc> coveredFiles) {
    List<Map<String, Object>> coveredFilesAsMaps = new ArrayList<>();
    for (CoveredFileDoc coveredFile : coveredFiles) {
      coveredFilesAsMaps.add(coveredFile.getFields());
    }
    setField(FIELD_COVERED_FILES, coveredFilesAsMaps);
    return this;
  }
}
