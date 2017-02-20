/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.component.ws;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.component.ws.FilterParser.Criterion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class FilterParserTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void parse_filter_having_operator_and_value() throws Exception {
    List<Criterion> criterion = FilterParser.parse("ncloc > 10 and coverage <= 80");

    assertThat(criterion)
      .extracting(Criterion::getKey, Criterion::getOperator, Criterion::getValue)
      .containsOnly(
        tuple("ncloc", ">", "10"),
        tuple("coverage", "<=", "80"));
  }

  @Test
  public void parse_filter_having_operator_and_value_ignores_white_spaces() throws Exception {
    List<Criterion> criterion = FilterParser.parse("   ncloc    >    10   ");

    assertThat(criterion)
      .extracting(Criterion::getKey, Criterion::getOperator, Criterion::getValue)
      .containsOnly(
        tuple("ncloc", ">", "10"));
  }

  @Test
  public void parse_filter_having_only_key() throws Exception {
    List<Criterion> criterion = FilterParser.parse("isFavorite");

    assertThat(criterion)
      .extracting(Criterion::getKey, Criterion::getOperator, Criterion::getValue)
      .containsOnly(
        tuple("isFavorite", null, null));
  }

  @Test
  public void parse_filter_having_only_key_ignores_white_spaces() throws Exception {
    List<Criterion> criterion = FilterParser.parse("  isFavorite   ");

    assertThat(criterion)
      .extracting(Criterion::getKey, Criterion::getOperator, Criterion::getValue)
      .containsOnly(
        tuple("isFavorite", null, null));
  }

  @Test
  public void parse_filter_having_different_criterion_types() throws Exception {
    List<Criterion> criterion = FilterParser.parse(" ncloc  > 10 and  coverage <= 80 and isFavorite ");

    assertThat(criterion)
      .extracting(Criterion::getKey, Criterion::getOperator, Criterion::getValue)
      .containsOnly(
        tuple("ncloc", ">", "10"),
        tuple("coverage", "<=", "80"),
        tuple("isFavorite", null, null));
  }

  @Test
  public void parse_filter_with_key_having_underscore() throws Exception {
    List<Criterion> criterion = FilterParser.parse(" alert_status = OK");

    assertThat(criterion)
      .extracting(Criterion::getKey, Criterion::getOperator, Criterion::getValue)
      .containsOnly(
        tuple("alert_status", "=", "OK"));
  }

  @Test
  public void accept_empty_query() throws Exception {
    List<Criterion> criterion = FilterParser.parse("");

    assertThat(criterion).isEmpty();
  }

  @Test
  public void search_is_case_insensitive() throws Exception {
    List<Criterion> criterion = FilterParser.parse("ncloc > 10 AnD coverage <= 80 AND debt = 10 AND issues = 20");

    assertThat(criterion).hasSize(4);
  }

  @Test
  public void fail_when_missing_value() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Cannot parse 'ncloc ='");

    FilterParser.parse("ncloc = ");
  }

}
