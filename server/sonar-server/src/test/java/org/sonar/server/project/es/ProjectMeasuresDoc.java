/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.project.es;

import java.util.Date;
import java.util.HashMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.server.es.BaseDoc;

public class ProjectMeasuresDoc extends BaseDoc {

  public ProjectMeasuresDoc() {
    super(new HashMap<>(4));
  }

  @Override
  public String getId() {
    return getField("_id");
  }

  @Override
  public String getRouting() {
    return null;
  }

  @Override
  public String getParent() {
    return null;
  }

  public ProjectMeasuresDoc setId(String s) {
    setField("_id", s);
    return this;
  }

  public String getKey() {
    return getField(ProjectMeasuresIndexDefinition.FIELD_KEY);
  }

  public ProjectMeasuresDoc setKey(String s) {
    setField(ProjectMeasuresIndexDefinition.FIELD_KEY, s);
    return this;
  }

  public String getName() {
    return getField(ProjectMeasuresIndexDefinition.FIELD_NAME);
  }

  public ProjectMeasuresDoc setName(String s) {
    setField(ProjectMeasuresIndexDefinition.FIELD_NAME, s);
    return this;
  }

  @CheckForNull
  public Date getAnalysedAt() {
    return getFieldAsDate(ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT);
  }

  public ProjectMeasuresDoc setAnalysedAt(@Nullable Date d) {
    setField(ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT, d);
    return this;
  }
}
