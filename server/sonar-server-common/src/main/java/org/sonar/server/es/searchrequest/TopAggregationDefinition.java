/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.es.searchrequest;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

/**
 * Models a first level aggregation in an Elasticsearch request (aka. top-aggregation) with a specific
 * {@link FilterScope} and whether it is to be used to compute data for a sticky facet (see {@link #isSticky()}) or not.
 */
public interface TopAggregationDefinition<S extends TopAggregationDefinition.FilterScope> {
  boolean STICKY = true;
  boolean NON_STICKY = false;

  S getFilterScope();

  boolean isSticky();

  abstract class FilterScope {
    public abstract String getFieldName();

    /**
     * All implementations must implement {@link Object#equals(Object)} and {@link Object#hashCode()} to be used
     * in {@link java.util.Set}.
     */
    public abstract boolean equals(Object other);

    public abstract int hashCode();

    /**
     *
     */
    public abstract boolean intersect(@Nullable FilterScope other);
  }

  /**
   * Filter applies to a regular first level field.
   */
  @Immutable
  final class SimpleFieldFilterScope extends FilterScope {
    private final String fieldName;

    public SimpleFieldFilterScope(String fieldName) {
      this.fieldName = requireNonNull(fieldName, "fieldName can't be null");
    }

    @Override
    public String getFieldName() {
      return fieldName;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SimpleFieldFilterScope that = (SimpleFieldFilterScope) o;
      return fieldName.equals(that.fieldName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(fieldName);
    }

    @Override
    public boolean intersect(@Nullable FilterScope other) {
      if (this == other) {
        return true;
      }
      if (other == null) {
        return false;
      }
      return fieldName.equals(other.getFieldName());
    }
  }

  /**
   * Filter applies to a first level field holding an array of objects (aka. nested fields) used to store various data
   * with similar structure in a single field. Each data is identified by a dedicated field which holds a different value
   * for each data: eg. metric key subfield identifies which metric the other subfield(s) are the value of.
   * <p>
   * This filter scope allows to represent filtering on each type of data in this first-level field, hence the necessity
   * to provide both the name of the subfield and the value which identifies that data.
   */
  @Immutable
  final class NestedFieldFilterScope<T> extends FilterScope {
    private final String fieldName;
    private final String nestedFieldName;
    private final T value;

    public NestedFieldFilterScope(String fieldName, String nestedFieldName, T value) {
      this.fieldName = requireNonNull(fieldName, "fieldName can't be null");
      this.nestedFieldName = requireNonNull(nestedFieldName, "nestedFieldName can't be null");
      this.value = requireNonNull(value, "value can't be null");
    }

    @Override
    public String getFieldName() {
      return fieldName;
    }

    public String getNestedFieldName() {
      return nestedFieldName;
    }

    public T getNestedFieldValue() {
      return value;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      NestedFieldFilterScope<?> that = (NestedFieldFilterScope<?>) o;
      return fieldName.equals(that.fieldName) &&
        nestedFieldName.equals(that.nestedFieldName) &&
        value.equals(that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(fieldName, nestedFieldName, value);
    }

    @Override
    public boolean intersect(@Nullable FilterScope other) {
      if (other instanceof NestedFieldFilterScope) {
        return equals(other);
      }
      return false;
    }
  }
}
