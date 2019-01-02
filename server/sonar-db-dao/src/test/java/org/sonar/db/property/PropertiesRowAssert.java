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
package org.sonar.db.property;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;
import org.sonar.db.DbTester;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;

final class PropertiesRowAssert extends AbstractAssert<PropertiesRowAssert, PropertiesRow> {

  PropertiesRowAssert(DbTester dbTester, String propertyKey, @Nullable Integer userId, @Nullable Integer componentId) {
    super(
      asInternalProperty(
        dbTester,
        () -> " where prop_key='" + propertyKey + "'" +
          " and user_id" + (userId == null ? " is null" : "='" + userId + "'") +
          " and resource_id" + (componentId == null ? " is null" : "='" + componentId + "'")),
      PropertiesRowAssert.class);
  }

  PropertiesRowAssert(DbTester dbTester, String key) {
    super(asInternalProperty(dbTester, () -> " where prop_key='" + key + "'"), PropertiesRowAssert.class);
  }

  PropertiesRowAssert(DbTester dbTester, long id) {
    super(asInternalProperty(dbTester, () -> " where id=" + id), PropertiesRowAssert.class);
  }

  @CheckForNull
  private static PropertiesRow asInternalProperty(DbTester dbTester, Supplier<String> whereClauseSupplier) {
    String whereClause = whereClauseSupplier.get();
    List<Map<String, Object>> rows = dbTester.select(
      "select" +
        " prop_key as \"key\", user_id as \"userId\", resource_id as \"resourceId\", is_empty as \"isEmpty\", text_value as \"textValue\", clob_value as \"clobValue\", created_at as \"createdAt\""
        +
        " from properties" +
        whereClause);
    checkState(rows.size() < 2, "More than one property found for where clause \"" + whereClause + "\"");
    if (rows.isEmpty()) {
      return null;
    } else {
      Map<String, Object> row = rows.iterator().next();
      Long userId = (Long) row.get("userId");
      return new PropertiesRow(
        (String) row.get("key"),
        userId == null ? null : userId.intValue(),
        (Long) row.get("resourceId"),
        toBoolean(row.get("isEmpty")),
        (String) row.get("textValue"),
        (String) row.get("clobValue"),
        (Long) row.get("createdAt"));
    }
  }

  private static Boolean toBoolean(Object flag) {
    if (flag instanceof Boolean) {
      return (Boolean) flag;
    }
    if (flag instanceof Long) {
      Long longBoolean = (Long) flag;
      return longBoolean.equals(1L);
    }
    throw new IllegalArgumentException("Unsupported object type returned for column \"isEmpty\": " + flag.getClass());
  }

  public void doesNotExist() {
    isNull();
  }

  public PropertiesRowAssert hasKey(String expected) {
    isNotNull();

    if (!Objects.equals(actual.getKey(), expected)) {
      failWithMessage("Expected PropertiesRow to have column PROP_KEY to be <%s> but was <%s>", expected, actual.getKey());
    }

    return this;
  }

  public PropertiesRowAssert hasNoUserId() {
    isNotNull();

    if (actual.getUserId() != null) {
      failWithMessage("Expected PropertiesRow to have column USER_ID to be null but was <%s>", actual.getUserId());
    }

    return this;
  }

  public PropertiesRowAssert hasUserId(int expected) {
    isNotNull();

    if (!Objects.equals(actual.getUserId(), expected)) {
      failWithMessage("Expected PropertiesRow to have column USER_ID to be <%s> but was <%s>", true, actual.getUserId());
    }

    return this;
  }

  public PropertiesRowAssert hasNoResourceId() {
    isNotNull();

    if (actual.getResourceId() != null) {
      failWithMessage("Expected PropertiesRow to have column RESOURCE_ID to be null but was <%s>", actual.getResourceId());
    }

    return this;
  }

  public PropertiesRowAssert hasResourceId(long expected) {
    isNotNull();

    if (!Objects.equals(actual.getResourceId(), expected)) {
      failWithMessage("Expected PropertiesRow to have column RESOURCE_ID to be <%s> but was <%s>", true, actual.getResourceId());
    }

    return this;
  }

  public PropertiesRowAssert isEmpty() {
    isNotNull();

    if (!Objects.equals(actual.getEmpty(), TRUE)) {
      failWithMessage("Expected PropertiesRow to have column IS_EMPTY to be <%s> but was <%s>", true, actual.getEmpty());
    }
    if (actual.getTextValue() != null) {
      failWithMessage("Expected PropertiesRow to have column TEXT_VALUE to be null but was <%s>", actual.getTextValue());
    }
    if (actual.getClobValue() != null) {
      failWithMessage("Expected PropertiesRow to have column CLOB_VALUE to be null but was <%s>", actual.getClobValue());
    }

    return this;
  }

  public PropertiesRowAssert hasTextValue(String expected) {
    isNotNull();

    if (!Objects.equals(actual.getTextValue(), requireNonNull(expected))) {
      failWithMessage("Expected PropertiesRow to have column TEXT_VALUE to be <%s> but was <%s>", expected, actual.getTextValue());
    }
    if (actual.getClobValue() != null) {
      failWithMessage("Expected PropertiesRow to have column CLOB_VALUE to be null but was <%s>", actual.getClobValue());
    }
    if (!Objects.equals(actual.getEmpty(), FALSE)) {
      failWithMessage("Expected PropertiesRow to have column IS_EMPTY to be <%s> but was <%s>", false, actual.getEmpty());
    }

    return this;
  }

  public PropertiesRowAssert hasClobValue(String expected) {
    isNotNull();

    if (!Objects.equals(actual.getClobValue(), requireNonNull(expected))) {
      failWithMessage("Expected PropertiesRow to have column CLOB_VALUE to be <%s> but was <%s>", true, actual.getClobValue());
    }
    if (actual.getTextValue() != null) {
      failWithMessage("Expected PropertiesRow to have column TEXT_VALUE to be null but was <%s>", actual.getTextValue());
    }
    if (!Objects.equals(actual.getEmpty(), FALSE)) {
      failWithMessage("Expected PropertiesRow to have column IS_EMPTY to be <%s> but was <%s>", false, actual.getEmpty());
    }

    return this;
  }

  public PropertiesRowAssert hasCreatedAt(long expected) {
    isNotNull();

    if (!Objects.equals(actual.getCreatedAt(), expected)) {
      failWithMessage("Expected PropertiesRow to have column CREATED_AT to be <%s> but was <%s>", expected, actual.getCreatedAt());
    }

    return this;
  }
}
