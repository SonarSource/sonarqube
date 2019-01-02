/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * PLUGINS MUST NOT USE THIS CLASS, EXCEPT FOR UNIT TESTING.
 *
 * @since 3.6
 */
public class FieldDiffs implements Serializable {

  public static final Splitter FIELDS_SPLITTER = Splitter.on(',').omitEmptyStrings();
  private static final String CHAR_TO_ESCAPE = "|,{}=:";

  private String issueKey;
  private String userUuid;
  private Date creationDate;
  public static final String ASSIGNEE = "assignee";
  public static final String ENCODING_PREFIX = "{base64:";
  public static final String ENCODING_SUFFIX = "}";

  private final Map<String, Diff> diffs = Maps.newLinkedHashMap();

  public Map<String, Diff> diffs() {
    if (diffs.containsKey(ASSIGNEE)) {
      Map<String, Diff> result = Maps.newLinkedHashMap(diffs);
      result.put(ASSIGNEE, decode(result.get(ASSIGNEE)));
      return result;
    }
    return diffs;
  }

  public Diff get(String field) {
    if (field.equals(ASSIGNEE)) {
      return decode(diffs.get(ASSIGNEE));
    }
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
    if (field.equals(ASSIGNEE)) {
      oldValue = encodeField(oldValue);
      newValue = encodeField(newValue);
    }
    if (diff == null) {
      diff = new Diff(oldValue, newValue);
      diffs.put(field, diff);
    } else {
      diff.setNewValue(newValue);
    }
    return this;
  }

  public String toEncodedString() {
    return serialize(true);
  }

  @Override
  public String toString() {
    return serialize(false);
  }

  private String serialize(boolean shouldEncode) {
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
      if (shouldEncode) {
        sb.append(entry.getValue().toEncodedString());
      } else {
        sb.append(entry.getValue().toString());
      }
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
      String[] keyValues = field.split("=", 2);
      if (keyValues.length == 2) {
        String values = keyValues[1];
        int split = values.indexOf('|');
        if (split > -1) {
          diffs.setDiff(keyValues[0], emptyToNull(values.substring(0, split)), emptyToNull(values.substring(split + 1)));
        } else {
          diffs.setDiff(keyValues[0], null, emptyToNull(values));
        }
      } else {
        diffs.setDiff(keyValues[0], null, null);
      }
    }
    return diffs;
  }

  @SuppressWarnings("unchecked")
  Diff decode(Diff encoded) {
    return new Diff(
        decodeField(encoded.oldValue),
        decodeField(encoded.newValue)
    );
  }

  private static Serializable decodeField(@Nullable Serializable field) {
    if (field == null || !isEncoded(field)) {
      return field;
    }

    String encodedField = field.toString();
    String value = encodedField.substring(ENCODING_PREFIX.length(), encodedField.indexOf(ENCODING_SUFFIX));
    return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
  }

  private static Serializable encodeField(@Nullable Serializable field) {
    if (field == null || !shouldEncode(field.toString())) {
      return field;
    }

    return ENCODING_PREFIX + Base64.getEncoder().encodeToString(field.toString().getBytes(StandardCharsets.UTF_8)) + ENCODING_SUFFIX;
  }

  private static boolean shouldEncode(String field) {
    return !field.isEmpty() && !isEncoded(field) && containsCharToEscape(field);
  }

  private static boolean containsCharToEscape(Serializable s) {
    return StringUtils.containsAny(s.toString(), CHAR_TO_ESCAPE);
  }

  private static boolean isEncoded(Serializable field) {
    return field.toString().startsWith(ENCODING_PREFIX) && field.toString().endsWith(ENCODING_SUFFIX);
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

    void setNewValue(@Nullable T t) {
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

    private String toEncodedString() {
      return serialize(true);
    }

    @Override
    public String toString() {
      return serialize(false);
    }

    private String serialize(boolean shouldEncode) {
      StringBuilder sb = new StringBuilder();
      if (oldValue != null) {
        sb.append(shouldEncode ? oldValue : decodeField(oldValue));
        sb.append('|');
      }
      if (newValue != null) {
        sb.append(shouldEncode ? newValue : decodeField(newValue));
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
