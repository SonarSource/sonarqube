/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * PLUGINS MUST NOT USE THIS CLASS, EXCEPT FOR UNIT TESTING.
 *
 * @since 3.6
 */
public class FieldDiffs implements Serializable {
  private static final String CHAR_TO_ESCAPE = "|,{}=:";
  private static final String ASSIGNEE = "assignee";
  private static final String ENCODING_PREFIX = "{base64:";
  private static final String ENCODING_SUFFIX = "}";
  private static final String WEBHOOK_SOURCE = "webhookSource";
  private static final String EXTERNAL_USER_KEY = "externalUser";

  private final Map<String, Diff> diffs = new LinkedHashMap<>();

  private String issueKey = null;
  private String userUuid = null;
  private Date creationDate = null;
  private String externalUser = null;
  private String webhookSource = null;

  public Map<String, Diff> diffs() {
    if (diffs.containsKey(ASSIGNEE)) {
      Map<String, Diff> result = new LinkedHashMap<>(diffs);
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

  public Optional<String> userUuid() {
    return Optional.ofNullable(userUuid);
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

  public Optional<String> issueKey() {
    return Optional.ofNullable(issueKey);
  }

  public FieldDiffs setIssueKey(@Nullable String issueKey) {
    this.issueKey = issueKey;
    return this;
  }

  public Optional<String> externalUser() {
    return Optional.ofNullable(externalUser);
  }

  public FieldDiffs setExternalUser(@Nullable String externalUser) {
    this.externalUser = externalUser;
    return this;
  }

  public Optional<String> webhookSource() {
    return Optional.ofNullable(webhookSource);
  }

  public FieldDiffs setWebhookSource(@Nullable String webhookSource) {
    this.webhookSource = webhookSource;
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
    List<String> serializedStrings = new ArrayList<>();
    if (isNotBlank(webhookSource)) {
      serializedStrings.add(WEBHOOK_SOURCE + "=" + webhookSource);
    }
    if (isNotBlank(externalUser)) {
      serializedStrings.add(EXTERNAL_USER_KEY + "=" + externalUser);
    }
    diffs.entrySet().stream()
      .map(entry -> serializeKeyValuePair(shouldEncode, entry.getKey(), entry.getValue()))
      .forEach(serializedStrings::add);
    return StringUtils.join(serializedStrings, ",");
  }

  private static String serializeKeyValuePair(boolean shouldEncode, String key, Diff values) {
    String serializedValues = shouldEncode ? values.toEncodedString() : values.toString();
    return key + "=" + serializedValues;
  }

  public static FieldDiffs parse(@Nullable String s) {
    FieldDiffs diffs = new FieldDiffs();
    if (isNullOrEmpty(s)) {
      return diffs;
    }

    for (String field : s.split(",")) {
      if (field.isEmpty()) {
        continue;
      }

      String[] keyValues = field.split("=", 2);
      String key = keyValues[0];
      if (keyValues.length == 2) {
        String values = keyValues[1];
        if (EXTERNAL_USER_KEY.equals(key)) {
          diffs.setExternalUser(trimToNull(values));
        } else if (WEBHOOK_SOURCE.equals(key)) {
          diffs.setWebhookSource(trimToNull(values));
        } else {
          addDiff(diffs, key, values);
        }
      } else {
        diffs.setDiff(key, null, null);
      }
    }
    return diffs;
  }

  private static void addDiff(FieldDiffs diffs, String key, String values) {
    int split = values.indexOf('|');
    if (split > -1) {
      diffs.setDiff(key, emptyToNull(values.substring(0, split)), emptyToNull(values.substring(split + 1)));
    } else {
      diffs.setDiff(key, null, emptyToNull(values));
    }
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
