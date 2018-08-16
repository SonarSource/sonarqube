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
package org.sonar.core.issue;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * PLUGINS MUST NOT USE THIS CLASS, EXCEPT FOR UNIT TESTING.
 *
 * @since 3.6
 */
public class FieldDiffs implements Serializable {

  public static final Splitter FIELDS_SPLITTER = Splitter.on(',').omitEmptyStrings();

  private String issueKey;
  private String userUuid;
  private Date creationDate;

  private final Map<String, Diff> diffs = Maps.newLinkedHashMap();

  public Map<String, Diff> diffs() {
    return diffs;
  }

  public Diff get(String field) {
    return diffs.get(field);
  }

  @CheckForNull
  public String userUuid() {
    return userUuid;
  }

  public FieldDiffs setUserUuid(@Nullable String s) {
    this.userUuid = s;
    return this;
  }

  public Date creationDate() {
    return creationDate;
  }

  public FieldDiffs setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
    return this;
  }

  public String issueKey() {
    return issueKey;
  }

  public FieldDiffs setIssueKey(String issueKey) {
    this.issueKey = issueKey;
    return this;
  }

  @SuppressWarnings("unchecked")
  public FieldDiffs setDiff(String field, @Nullable Serializable oldValue, @Nullable Serializable newValue) {
    Diff diff = diffs.get(field);
    if (diff == null) {
      diff = new Diff(oldValue, newValue);
      diffs.put(field, diff);
    } else {
      diff.setNewValue(newValue);
    }
    return this;
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
    if (isNullOrEmpty(s)) {
      return diffs;
    }
    Iterable<String> fields = FIELDS_SPLITTER.split(s);
    for (String field : fields) {
      String[] keyValues = field.split("=");
      if (keyValues.length == 2) {
        String values = keyValues[1];
        int split = values.indexOf('|');
        if (split > -1) {
          diffs.setDiff(keyValues[0], values.substring(0, split), values.substring(split +1));
        } else {
          diffs.setDiff(keyValues[0], "", emptyToNull(values));
        }
      } else {
        diffs.setDiff(keyValues[0], "", "");
      }
    }
    return diffs;
  }

  public static class Diff<T extends Serializable> implements Serializable {
    private T oldValue;
    private T newValue;

    public Diff(@Nullable T oldValue, @Nullable T newValue) {
      this.oldValue = oldValue;
      this.newValue = newValue;
    }

    @CheckForNull
    public T oldValue() {
      return oldValue;
    }

    @CheckForNull
    public Long oldValueLong() {
      return toLong(oldValue);
    }

    @CheckForNull
    public T newValue() {
      return newValue;
    }

    @CheckForNull
    public Long newValueLong() {
      return toLong(newValue);
    }

    void setNewValue(T t) {
      this.newValue = t;
    }

    @CheckForNull
    private static Long toLong(@Nullable Serializable value) {
      if (value != null && !"".equals(value)) {
        try {
          return Long.valueOf((String) value);
        } catch (ClassCastException e) {
          return (Long) value;
        }
      }
      return null;
    }

    @Override
    public String toString() {
      // TODO escape , and | characters
      StringBuilder sb = new StringBuilder();
      if (oldValue != null) {
        sb.append(oldValue.toString());
        sb.append('|');
      }
      if (newValue != null) {
        sb.append(newValue.toString());
      }
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Diff<?> diff = (Diff<?>) o;
      return Objects.equals(oldValue, diff.oldValue) &&
        Objects.equals(newValue, diff.newValue);
    }

    @Override
    public int hashCode() {
      return Objects.hash(oldValue, newValue);
    }
  }

}
