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

import java.util.List;
import java.util.Map;

import static org.sonar.server.test.index.TestIndexDefinition.*;

public class TestDoc extends BaseDoc {
  public TestDoc(Map<String, Object> fields) {
    super(fields);
  }

  @VisibleForTesting
  TestDoc() {
    super(Maps.<String, Object>newHashMapWithExpectedSize(7));
  }

  public String uuid() {
    return getField(FIELD_UUID);
  }

  public TestDoc setUuid(String uuid) {
    setField(FIELD_UUID, uuid);
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

  public String message() {
    return getField(FIELD_MESSAGE);
  }

  public TestDoc setMessage(String message) {
    setField(FIELD_MESSAGE, message);
    return this;
  }

  public String stackTrace() {
    return getField(FIELD_STACKTRACE);
  }

  public TestDoc setStackTrace(String stackTrace) {
    setField(FIELD_STACKTRACE, stackTrace);
    return this;
  }

  public String type() {
    return getField(FIELD_TYPE);
  }

  public TestDoc setType(String type) {
    setField(FIELD_TYPE, type);
    return this;
  }

  public Long durationInMs() {
    return getField(FIELD_DURATION_IN_MS);
  }

  public TestDoc setDurationInMs(Long durationInMs) {
    setField(FIELD_DURATION_IN_MS, durationInMs);
    return this;
  }

  // TODO TBE - it should be a CoverageBlockDoc list
  public List<Map<String, Object>> coverageBlocks() {
    return getField(FIELD_COVERAGE_BLOCKS);
  }

  public TestDoc setCoverageBlocks(List<Map<String, Object>> coverageBlocks) {
    setField(FIELD_COVERAGE_BLOCKS, coverageBlocks);
    return this;
  }
}
