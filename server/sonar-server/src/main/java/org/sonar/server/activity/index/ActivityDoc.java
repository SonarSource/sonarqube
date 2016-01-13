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
package org.sonar.server.activity.index;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.server.search.BaseDoc;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ActivityDoc extends BaseDoc {

  public ActivityDoc(Map<String, Object> fields) {
    super(fields);
  }

  @VisibleForTesting
  ActivityDoc() {
    super(new HashMap<String, Object>());
  }

  public void setCreatedAt(Date date) {
    setField(ActivityIndexDefinition.FIELD_CREATED_AT, date);
  }

  public Date getCreatedAt() {
    return getFieldAsDate(ActivityIndexDefinition.FIELD_CREATED_AT);
  }

  public String getKey() {
    return this.getField(ActivityIndexDefinition.FIELD_KEY);
  }

  public void setKey(String s) {
    setField(ActivityIndexDefinition.FIELD_KEY, s);
  }

  @CheckForNull
  public String getLogin() {
    return this.getNullableField(ActivityIndexDefinition.FIELD_LOGIN);
  }

  public void setLogin(@Nullable String s) {
    setField(ActivityIndexDefinition.FIELD_LOGIN, s);
  }

  public String getType() {
    return (String) getField(ActivityIndexDefinition.FIELD_TYPE);
  }

  public void setType(String s) {
    setField(ActivityIndexDefinition.FIELD_TYPE, s);
  }

  @CheckForNull
  public String getAction() {
    return this.getNullableField(ActivityIndexDefinition.FIELD_ACTION);
  }

  public void setAction(@Nullable String s) {
    setField(ActivityIndexDefinition.FIELD_ACTION, s);
  }

  public Map<String, String> getDetails() {
    return this.getField(ActivityIndexDefinition.FIELD_DETAILS);
  }

  public void setDetails(Map<String, String> details) {
    setField(ActivityIndexDefinition.FIELD_DETAILS, details);
  }

  @CheckForNull
  public String getMessage() {
    return this.getNullableField(ActivityIndexDefinition.FIELD_MESSAGE);
  }

  public void setMessage(@Nullable String s) {
    setField(ActivityIndexDefinition.FIELD_MESSAGE, s);
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
