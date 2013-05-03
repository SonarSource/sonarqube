/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.issue;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class FieldDiffs implements Serializable {

  public static final Splitter FIELDS_SPLITTER = Splitter.on(',').omitEmptyStrings();

  private String userLogin;
  private Date createdAt, updatedAt;
  private final Map<String, Diff> diffs = Maps.newLinkedHashMap();

  public Map<String, Diff> diffs() {
    return diffs;
  }

  public Diff get(String field) {
    return diffs.get(field);
  }

  @CheckForNull
  public String userLogin() {
    return userLogin;
  }

  public FieldDiffs setUserLogin(@Nullable String s) {
    this.userLogin = s;
    return this;
  }

  public Date createdAt() {
    return createdAt;
  }

  public FieldDiffs setCreatedAt(Date d) {
    this.createdAt = d;
    return this;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public FieldDiffs setUpdatedAt(Date d) {
    this.updatedAt = d;
    return this;
  }


  @SuppressWarnings("unchecked")
  public void setDiff(String field, @Nullable Serializable oldValue, @Nullable Serializable newValue) {
    Diff diff = diffs.get(field);
    if (diff == null) {
      diff = new Diff(oldValue, newValue);
      diffs.put(field, diff);
    } else {
      diff.setNewValue(newValue);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    boolean notFirst = false;
    for (Map.Entry<String, Diff> entry : diffs.entrySet()) {
      if (notFirst) {
        sb.append(',');
      } else {
        notFirst = true;
      }
      sb.append(entry.getKey());
      sb.append('=');
      sb.append(entry.getValue().toString());
    }
    return sb.toString();
  }

  public static FieldDiffs parse(@Nullable String s) {
    FieldDiffs diffs = new FieldDiffs();
    if (!Strings.isNullOrEmpty(s)) {
      Iterable<String> fields = FIELDS_SPLITTER.split(s);
      for (String field : fields) {
        String[] keyValues = field.split("=");
        if (keyValues.length == 2) {
          String[] values = keyValues[1].split("\\|");
          String oldValue = "";
          String newValue = "";
          if (values.length > 0) {
            oldValue = Strings.nullToEmpty(values[0]);
          }
          if (values.length > 1) {
            newValue = Strings.nullToEmpty(values[1]);
          }
          diffs.setDiff(keyValues[0], oldValue, newValue);
        }
      }
    }
    return diffs;
  }

  public static class Diff<T extends Serializable> implements Serializable {
    private T oldValue, newValue;

    public Diff(@Nullable T oldValue, @Nullable T newValue) {
      this.oldValue = oldValue;
      this.newValue = newValue;
    }

    public T oldValue() {
      return oldValue;
    }

    public T newValue() {
      return newValue;
    }

    void setNewValue(T t) {
      this.newValue = t;
    }

    @Override
    public String toString() {
      //TODO escape , and | characters
      StringBuilder sb = new StringBuilder();
      if (oldValue != null) {
        sb.append(oldValue.toString());
      }
      sb.append('|');
      if (newValue != null) {
        sb.append(newValue.toString());
      }
      return sb.toString();
    }
  }
}
