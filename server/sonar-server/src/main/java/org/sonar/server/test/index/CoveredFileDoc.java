/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.sonar.server.es.BaseDoc;

import static org.sonar.server.test.index.TestIndexDefinition.FIELD_COVERED_FILE_LINES;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_COVERED_FILE_UUID;

public class CoveredFileDoc extends BaseDoc {
  public CoveredFileDoc(Map<String, Object> fields) {
    super(fields);
  }

  @VisibleForTesting
  public CoveredFileDoc() {
    super(Maps.newHashMapWithExpectedSize(2));
  }

  @Override
  public String getId() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getRouting() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getParent() {
    throw new UnsupportedOperationException();
  }

  public String fileUuid() {
    return getField(FIELD_COVERED_FILE_UUID);
  }

  public CoveredFileDoc setFileUuid(String fileUuid) {
    setField(FIELD_COVERED_FILE_UUID, fileUuid);
    return this;
  }

  public List<Integer> coveredLines() {
    return getField(FIELD_COVERED_FILE_LINES);
  }

  public CoveredFileDoc setCoveredLines(List<Integer> coveredLines) {
    setField(FIELD_COVERED_FILE_LINES, coveredLines);
    return this;
  }

}
