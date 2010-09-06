/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.web.gwt.client.webservices;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.ArrayList;
import java.util.List;

public final class WSMetrics {

  private WSMetrics() {
  }

  private final static List<Metric> DICTIONNARY = new ArrayList<Metric>();

  public static final Metric NCLOC = add(new Metric("ncloc"));
  public static final Metric LINES = add(new Metric("lines"));
  public static final Metric CLASSES = add(new Metric("classes"));
  public static final Metric PACKAGES = add(new Metric("packages"));
  public static final Metric FUNCTIONS = add(new Metric("functions"));
  public static final Metric ACCESSORS = add(new Metric("accessors"));
  public static final Metric FILES = add(new Metric("files"));
  public static final Metric DIRECTORIES = add(new Metric("directories"));
  public static final Metric PUBLIC_API = add(new Metric("public_api"));

  /* complexity */
  public static final Metric COMPLEXITY = add(new Metric("complexity"));
  public static final Metric CLASS_COMPLEXITY = add(new Metric("class_complexity"));
  public static final Metric FUNCTION_COMPLEXITY = add(new Metric("function_complexity"));
  public static final Metric FILE_COMPLEXITY = add(new Metric("file_complexity"));
  public static final Metric STATEMENTS = add(new Metric("statements"));

  public static final Metric CLASS_COMPLEXITY_DISTRIBUTION = add(new Metric("class_complexity_distribution"));
  public static final Metric FUNCTION_COMPLEXITY_DISTRIBUTION = add(new Metric("function_complexity_distribution"));

  /* comments */
  public static final Metric COMMENT_LINES = add(new Metric("comment_lines"));
  public static final Metric COMMENT_LINES_DENSITY = add(new Metric("comment_lines_density"));
  public static final Metric PUBLIC_DOCUMENTED_API_DENSITY = add(new Metric("public_documented_api_density"));
  public static final Metric PUBLIC_UNDOCUMENTED_API = add(new Metric("public_undocumented_api"));
  public static final Metric COMMENTED_OUT_CODE_LINES = add(new Metric("commented_out_code_lines"));

  /* unit tests */
  public static final Metric TESTS = add(new Metric("tests"));
  public static final Metric TESTS_EXECUTION_TIME = add(new Metric("test_execution_time"));
  public static final Metric TEST_ERRORS = add(new Metric("test_errors"));
  public static final Metric SKIPPED_TESTS = add(new Metric("skipped_tests"));
  public static final Metric TEST_FAILURES = add(new Metric("test_failures"));
  public static final Metric TEST_SUCCESS_DENSITY = add(new Metric("test_success_density"));
  public static final Metric TEST_DATA = add(new Metric("test_data"));

  /* coverage */
  public static final Metric COVERAGE = add(new Metric("coverage"));
  public static final Metric LINE_COVERAGE = add(new Metric("line_coverage"));
  public static final Metric UNCOVERED_LINES = add(new Metric("uncovered_lines"));
  public static final Metric BRANCH_COVERAGE = add(new Metric("branch_coverage"));
  public static final Metric UNCOVERED_CONDITIONS = add(new Metric("uncovered_conditions"));
  public static final Metric COVERAGE_LINE_HITS_DATA = add(new Metric("coverage_line_hits_data"));
  public static final Metric BRANCH_COVERAGE_HITS_DATA = add(new Metric("branch_coverage_hits_data"));

  /* duplicated lines */
  public static final Metric DUPLICATED_LINES = add(new Metric("duplicated_lines"));
  public static final Metric DUPLICATED_BLOCKS = add(new Metric("duplicated_blocks"));
  public static final Metric DUPLICATED_FILES = add(new Metric("duplicated_files"));
  public static final Metric DUPLICATED_LINES_DENSITY = add(new Metric("duplicated_lines_density"));
  public static final Metric DUPLICATIONS_DATA = add(new Metric("duplications_data"));

  /* coding rules */
  public static final Metric VIOLATIONS_DENSITY = add(new Metric("violations_density"));
  public static final Metric VIOLATIONS = add(new Metric("violations"));
  public static final Metric WEIGHTED_VIOLATIONS = add(new Metric("weighted_violations"));

  /* design */
  public static final Metric LCOM4 = add(new Metric("lcom4"));
  public static final Metric RFC = add(new Metric("rfc"));

  public static class MetricsList extends ResponsePOJO {

    private List<Metric> metrics = new ArrayList<Metric>();

    public List<Metric> getMetrics() {
      return metrics;
    }
  }

  /**
   * Generates a callback that will update the metrics definitions from the WSMetrics metrics constants list with data
   * received from a MetricsQuery call
   *
   * @param callback
   * @return
   */
  public static QueryCallBack<MetricsList> getUpdateMetricsFromServer(final QueryCallBack<MetricsList> callback) {
    return new QueryCallBack<MetricsList>() {
      public void onResponse(MetricsList response, JavaScriptObject jsonRawResponse) {
        for (Metric metric : response.getMetrics()) {
          Metric WSMetricConstant = get(metric.getKey());
          if (WSMetricConstant != null) {
            WSMetricConstant.updateFrom(metric);
          } else {
            add(metric);
          }
        }
        callback.onResponse(response, jsonRawResponse);
      }

      public void onError(int errorCode, String errorMessage) {
        callback.onError(errorCode, errorMessage);
      }

      public void onTimeout() {
        callback.onTimeout();
      }
    };
  }

  public static class Metric {
    public enum ValueType {
      INT, FLOAT, PERCENT, BOOL, STRING, MILLISEC, DATA, LEVEL, DISTRIB
    }

    private String key;
    private String name;
    private String description;
    private String domain;
    private boolean qualitative;
    private boolean userManaged;
    private int direction;
    private ValueType type;

    public Metric(String key) {
      super();
      this.key = key;
    }

    public Metric(String key, String name, String description, String domain,
                  boolean qualitative, boolean userManaged, int direction, ValueType type) {
      super();
      this.key = key;
      this.name = name;
      this.description = description;
      this.domain = domain;
      this.qualitative = qualitative;
      this.userManaged = userManaged;
      this.direction = direction;
      this.type = type;
    }

    public void updateFrom(Metric metric) {
      this.name = metric.getName();
      this.description = metric.getDescription();
      this.domain = metric.getDomain();
      this.qualitative = metric.isQualitative();
      this.userManaged = metric.isUserManaged();
      this.direction = metric.getDirection();
      this.type = metric.getType();
    }

    public String getName() {
      return name;
    }

    public ValueType getType() {
      return type;
    }

    public String getDescription() {
      return description;
    }

    public String getDomain() {
      return domain;
    }

    public boolean isQualitative() {
      return qualitative;
    }

    public boolean isUserManaged() {
      return userManaged;
    }

    public int getDirection() {
      return direction;
    }

    public String getKey() {
      return key;
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Metric)) {
        return false;
      }
      if (this == obj) {
        return true;
      }
      Metric other = (Metric) obj;
      return key.equals(other.getKey());
    }
  }

  public static Metric add(Metric metric) {
    if (!DICTIONNARY.contains(metric)) {
      DICTIONNARY.add(metric);
    }
    return metric;
  }

  public static Metric get(String metricKey) {
    for (Metric metric : DICTIONNARY) {
      if (metric.getKey().equals(metricKey)) {
        return metric;
      }
    }
    return new Metric(metricKey);
  }

}
