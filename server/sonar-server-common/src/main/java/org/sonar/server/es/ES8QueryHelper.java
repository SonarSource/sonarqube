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
package org.sonar.server.es;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Helper class for building ES 8 Query objects.
 * Provides methods similar to ES 7's QueryBuilders.
 */
public final class ES8QueryHelper {

  private ES8QueryHelper() {
    // Utility class
  }

  // ========== MATCH ALL ==========

  public static Query matchAllQuery() {
    return Query.of(q -> q.matchAll(m -> m));
  }

  // ========== TERM QUERIES ==========

  public static Query termQuery(String field, String value) {
    return Query.of(q -> q.term(t -> t.field(field).value(value)));
  }

  public static Query termQuery(String field, long value) {
    return Query.of(q -> q.term(t -> t.field(field).value(value)));
  }

  public static Query termQuery(String field, boolean value) {
    return Query.of(q -> q.term(t -> t.field(field).value(value)));
  }

  public static Query termsQuery(String field, String... values) {
    List<FieldValue> fieldValues = Stream.of(values)
      .map(FieldValue::of)
      .toList();
    return Query.of(q -> q.terms(t -> t.field(field).terms(tv -> tv.value(fieldValues))));
  }

  public static Query termsQuery(String field, @Nullable Collection<String> values) {
    if (values == null || values.isEmpty()) {
      return matchAllQuery();
    }
    List<FieldValue> fieldValues = values.stream()
      .map(FieldValue::of)
      .toList();
    return Query.of(q -> q.terms(t -> t.field(field).terms(tv -> tv.value(fieldValues))));
  }

  // ========== BOOL QUERIES ==========

  public static Query boolQuery() {
    return Query.of(q -> q.bool(b -> b));
  }

  public static Query boolQuery(Consumer<BoolQuery.Builder> fn) {
    return Query.of(q -> q.bool(b -> {
      fn.accept(b);
      return b;
    }));
  }

  // ========== MATCH QUERIES ==========

  public static Query matchQuery(String field, String text) {
    return Query.of(q -> q.match(m -> m.field(field).query(text)));
  }

  public static Query matchQuery(String field, String text, Operator operator) {
    return Query.of(q -> q.match(m -> m.field(field).query(text).operator(operator)));
  }

  // ========== RANGE QUERIES ==========

  /**
   * ES 8: Creates a range query for a specific field.
   * Note: In ES8, we use the NumberRangeQuery variant for numeric comparisons.
   */
  public static Query rangeQuery(String field) {
    return Query.of(q -> q.range(r -> r.number(n -> n.field(field))));
  }

  public static Query rangeQueryGte(String field, long value) {
    return Query.of(q -> q.range(r -> r.number(n -> n.field(field).gte((double) value))));
  }

  public static Query rangeQueryLte(String field, long value) {
    return Query.of(q -> q.range(r -> r.number(n -> n.field(field).lte((double) value))));
  }

  public static Query rangeQueryGt(String field, long value) {
    return Query.of(q -> q.range(r -> r.number(n -> n.field(field).gt((double) value))));
  }

  public static Query rangeQueryLt(String field, long value) {
    return Query.of(q -> q.range(r -> r.number(n -> n.field(field).lt((double) value))));
  }

  public static Query rangeQueryBetween(String field, long from, long to) {
    return Query.of(q -> q.range(r -> r.number(n -> n.field(field).gte((double) from).lte((double) to))));
  }

  public static Query rangeQueryGte(String field, double value) {
    return Query.of(q -> q.range(r -> r.number(n -> n.field(field).gte(value))));
  }

  public static Query rangeQueryLte(String field, double value) {
    return Query.of(q -> q.range(r -> r.number(n -> n.field(field).lte(value))));
  }

  public static Query rangeQueryGt(String field, double value) {
    return Query.of(q -> q.range(r -> r.number(n -> n.field(field).gt(value))));
  }

  public static Query rangeQueryLt(String field, double value) {
    return Query.of(q -> q.range(r -> r.number(n -> n.field(field).lt(value))));
  }

  // ========== EXISTS QUERIES ==========

  public static Query existsQuery(String field) {
    return Query.of(q -> q.exists(e -> e.field(field)));
  }

  // ========== PREFIX QUERIES ==========

  public static Query prefixQuery(String field, String prefix) {
    return Query.of(q -> q.prefix(p -> p.field(field).value(prefix)));
  }

  // ========== WILDCARD QUERIES ==========

  public static Query wildcardQuery(String field, String value) {
    return Query.of(q -> q.wildcard(w -> w.field(field).value(value)));
  }

  // ========== NESTED QUERIES ==========

  public static Query nestedQuery(String path, Query query) {
    return Query.of(q -> q.nested(n -> n.path(path).query(query)));
  }

  public static Query nestedQuery(String path, Query query, ChildScoreMode scoreMode) {
    return Query.of(q -> q.nested(n -> n.path(path).query(query).scoreMode(scoreMode)));
  }

  // ========== HAS CHILD/PARENT QUERIES ==========

  public static Query hasChildQuery(String type, Query query) {
    return Query.of(q -> q.hasChild(h -> h.type(type).query(query)));
  }

  public static Query hasChildQuery(String type, Query query, ChildScoreMode scoreMode) {
    return Query.of(q -> q.hasChild(h -> h.type(type).query(query).scoreMode(scoreMode)));
  }

  public static Query hasParentQuery(String parentType, Query query) {
    return Query.of(q -> q.hasParent(h -> h.parentType(parentType).query(query)));
  }

  /**
   * Wraps a query with a boost
   */
  public static Query withBoost(Query query, float boost) {
    return Query.of(q -> q.bool(b -> b.must(query).boost(boost)));
  }
}
