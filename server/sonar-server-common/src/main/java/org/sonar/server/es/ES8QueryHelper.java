/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

  // ========== MATCH ALL / MATCH NONE ==========

  public static Query matchAllQuery() {
    return Query.of(q -> q.matchAll(m -> m));
  }

  public static Query matchNoneQuery() {
    return Query.of(q -> q.matchNone(m -> m));
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

  /**
   * Builds a terms query that matches no document when {@code values} is null or empty
   * (parity with the ES7 {@code QueryBuilders.termsQuery} behavior). Use this when an
   * empty filter set should narrow results to nothing — e.g. an anonymous user filtering
   * on their own favorites.
   */
  public static Query termsQuery(String field, @Nullable Collection<String> values) {
    if (values == null || values.isEmpty()) {
      return matchNoneQuery();
    }
    List<FieldValue> fieldValues = values.stream()
      .map(FieldValue::of)
      .toList();
    return Query.of(q -> q.terms(t -> t.field(field).terms(tv -> tv.value(fieldValues))));
  }

  /**
   * Builds a terms query that degenerates into {@code match_all} when {@code values} is
   * null or empty. Use this when an empty filter set means "no constraint" — i.e. the
   * filter should be a no-op rather than excluding everything.
   */
  public static Query termsQueryOrMatchAll(String field, @Nullable Collection<String> values) {
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

  // ========== UNTYPED RANGE QUERIES ==========
  // For date fields stored as epoch_millis, or any field where the type cannot be inferred,
  // the untyped variant accepts an opaque value.

  public static Query untypedRangeQueryGte(String field, long value) {
    return Query.of(q -> q.range(r -> r.untyped(u -> u.field(field).gte(co.elastic.clients.json.JsonData.of(value)))));
  }

  public static Query untypedRangeQueryGt(String field, long value) {
    return Query.of(q -> q.range(r -> r.untyped(u -> u.field(field).gt(co.elastic.clients.json.JsonData.of(value)))));
  }

  public static Query untypedRangeQueryLt(String field, long value) {
    return Query.of(q -> q.range(r -> r.untyped(u -> u.field(field).lt(co.elastic.clients.json.JsonData.of(value)))));
  }

  public static Query untypedRangeQueryLte(String field, long value) {
    return Query.of(q -> q.range(r -> r.untyped(u -> u.field(field).lte(co.elastic.clients.json.JsonData.of(value)))));
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
