/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.server.measure.index.ProjectMeasuresQuery;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public class FilterParser {

  private static final String DOUBLE_QUOTES = "\"";
  private static final Splitter CRITERIA_SPLITTER = Splitter.on(Pattern.compile(" and ", Pattern.CASE_INSENSITIVE)).trimResults().omitEmptyStrings();
  private static final Splitter IN_VALUES_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();
  private static final Pattern PATTERN_WITH_COMPARISON_OPERATOR = Pattern.compile("(\\w+)\\s*+(<=?|>=?|=)\\s*+([^<>=]*+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_WITHOUT_OPERATOR = Pattern.compile("(\\w+)\\s*+", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_WITH_IN_OPERATOR = Pattern.compile("(\\w+)\\s+(in)\\s+\\(([^()]*)\\)", Pattern.CASE_INSENSITIVE);

  private FilterParser() {
    // Only static methods
  }

  public static List<Criterion> parse(String filter) {
    return StreamSupport.stream(CRITERIA_SPLITTER.split(filter).spliterator(), false)
      .map(FilterParser::parseCriterion)
      .toList();
  }

  private static Criterion parseCriterion(String rawCriterion) {
    try {
      return Stream.of(
          tryParsingCriterionWithoutOperator(rawCriterion),
          tryParsingCriterionWithInOperator(rawCriterion),
          tryParsingCriterionWithComparisonOperator(rawCriterion)
        )
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Criterion is invalid"));
    } catch (Exception e) {
      throw new IllegalArgumentException(String.format("Cannot parse '%s' : %s", rawCriterion, e.getMessage()), e);
    }
  }

  private static Optional<Criterion> tryParsingCriterionWithoutOperator(String criterion) {
    Matcher matcher = PATTERN_WITHOUT_OPERATOR.matcher(criterion);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    Criterion.Builder builder = new Criterion.Builder();
    builder.setKey(matcher.group(1));
    return Optional.of(builder.build());
  }

  private static Optional<Criterion> tryParsingCriterionWithComparisonOperator(String criterion) {
    Matcher matcher = PATTERN_WITH_COMPARISON_OPERATOR.matcher(criterion);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    Criterion.Builder builder = new Criterion.Builder();
    builder.setKey(matcher.group(1));
    String operatorValue = matcher.group(2);
    String value = matcher.group(3);
    if (!isNullOrEmpty(operatorValue) && !isNullOrEmpty(value)) {
      builder.setOperator(ProjectMeasuresQuery.Operator.getByValue(operatorValue));
      builder.setValue(sanitizeValue(value));
    }
    return Optional.of(builder.build());
  }

  private static Optional<Criterion> tryParsingCriterionWithInOperator(String criterion) {
    Matcher matcher = PATTERN_WITH_IN_OPERATOR.matcher(criterion);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    Criterion.Builder builder = new Criterion.Builder();
    builder.setKey(matcher.group(1));
    builder.setOperator(ProjectMeasuresQuery.Operator.IN);
    builder.setValues(IN_VALUES_SPLITTER.splitToList(matcher.group(3)));
    return Optional.of(builder.build());
  }

  @CheckForNull
  private static String sanitizeValue(@Nullable String value) {
    if (value == null) {
      return null;
    }
    if (value.length() > 2 && value.startsWith(DOUBLE_QUOTES) && value.endsWith(DOUBLE_QUOTES)) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }

  public static class Criterion {
    private final String key;
    private final ProjectMeasuresQuery.Operator operator;
    private final String value;
    private final List<String> values;

    private Criterion(Builder builder) {
      this.key = builder.key;
      this.operator = builder.operator;
      this.value = builder.value;
      this.values = builder.values;
    }

    public String getKey() {
      return key;
    }

    @CheckForNull
    public ProjectMeasuresQuery.Operator getOperator() {
      return operator;
    }

    @CheckForNull
    public String getValue() {
      return value;
    }

    public List<String> getValues() {
      return values;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private String key;
      private ProjectMeasuresQuery.Operator operator;
      private String value;
      private List<String> values = new ArrayList<>();

      public Builder setKey(String key) {
        this.key = key;
        return this;
      }

      public Builder setOperator(@Nullable ProjectMeasuresQuery.Operator operator) {
        this.operator = operator;
        return this;
      }

      public Builder setValue(@Nullable String value) {
        this.value = value;
        return this;
      }

      public Builder setValues(List<String> values) {
        this.values = requireNonNull(values, "Values cannot be null");
        return this;
      }

      public Criterion build() {
        return new Criterion(this);
      }
    }
  }

}
