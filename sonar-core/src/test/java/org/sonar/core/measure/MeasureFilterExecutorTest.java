/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.core.measure;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.SnapshotDto;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class MeasureFilterExecutorTest extends AbstractDaoTestCase {
  private static final long JAVA_PROJECT_ID = 1L;
  private static final long JAVA_FILE_BIG_ID = 3L;
  private static final long JAVA_FILE_TINY_ID = 4L;
  private static final long JAVA_PROJECT_SNAPSHOT_ID = 101L;
  private static final long JAVA_FILE_BIG_SNAPSHOT_ID = 103L;
  private static final long JAVA_FILE_TINY_SNAPSHOT_ID = 104L;
  private static final long JAVA_PACKAGE_SNAPSHOT_ID = 102L;
  private static final long PHP_PROJECT_ID = 10L;
  private static final long PHP_SNAPSHOT_ID = 110L;
  private static final Metric METRIC_LINES = new Metric.Builder("lines", "Lines", Metric.ValueType.INT).create().setId(1);
  private static final Metric METRIC_PROFILE = new Metric.Builder("profile", "Profile", Metric.ValueType.STRING).create().setId(2);
  private static final Metric METRIC_COVERAGE = new Metric.Builder("coverage", "Coverage", Metric.ValueType.FLOAT).create().setId(3);

  private MeasureFilterExecutor executor;

  @Before
  public void before() {
    executor = new MeasureFilterExecutor(getMyBatis(), getDatabase(), new ResourceDao(getMyBatis()));
    setupData("shared");
  }

  @Test
  public void invalid_filter_should_not_return_results() {
    MeasureFilter filter = new MeasureFilter();
    // no qualifiers
    assertThat(executor.execute(filter, null)).isEmpty();
  }

  @Test
  public void filter_is_not_valid_if_missing_base_snapshot() {
    MesasureFilterContext context = new MesasureFilterContext();
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK").setOnBaseResourceChildren(true);
    assertThat(MeasureFilterExecutor.isValid(filter, context)).isFalse();

    context.setBaseSnapshot(new SnapshotDto().setId(123L));
    assertThat(MeasureFilterExecutor.isValid(filter, context)).isTrue();
  }

  @Test
  public void filter_is_not_valid_if_anonymous_favourites() {
    MesasureFilterContext context = new MesasureFilterContext();
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK").setUserFavourites(true);
    assertThat(MeasureFilterExecutor.isValid(filter, context)).isFalse();

    context.setUserId(123L);
    assertThat(MeasureFilterExecutor.isValid(filter, context)).isTrue();
  }

  @Test
  public void projects_without_measure_conditions() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK").setSortOn(MeasureFilterSort.Field.LANGUAGE);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(2);
    verifyJavaProject(rows.get(0));
    verifyPhpProject(rows.get(1));
  }

  @Test
  public void test_default_sort() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("CLA");

    assertThat(filter.sort().isAsc()).isTrue();
    assertThat(filter.sort().field()).isEqualTo(MeasureFilterSort.Field.NAME);
    assertThat(filter.sort().metric()).isNull();
  }

  @Test
  public void sort_by_ascending_resource_name() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("CLA").setSortAsc(true);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    // Big -> Tiny
    assertThat(rows).hasSize(2);
    verifyJavaBigFile(rows.get(0));
    verifyJavaTinyFile(rows.get(1));
  }

  @Test
  public void sort_by_descending_resource_name() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("CLA").setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    // Tiny -> Big
    assertThat(rows).hasSize(2);
    verifyJavaTinyFile(rows.get(0));
    verifyJavaBigFile(rows.get(1));
  }

  @Test
  public void sort_by_ascending_text_measure() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK").setSortOnMetric(METRIC_PROFILE);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(2);
    verifyPhpProject(rows.get(0));//php way
    verifyJavaProject(rows.get(1));// Sonar way
  }

  @Test
  public void sort_by_descending_text_measure() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK").setSortOnMetric(METRIC_PROFILE).setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(2);
    verifyJavaProject(rows.get(0));// Sonar way
    verifyPhpProject(rows.get(1));//php way
  }

  @Test
  public void sort_by_missing_text_measure() {
    // the metric 'profile' is not set on files
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("CLA").setSortOnMetric(METRIC_PROFILE);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(2);//2 files randomly sorted
  }

  @Test
  public void sort_by_ascending_numeric_measure() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("CLA").setSortOnMetric(METRIC_LINES);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    // Tiny -> Big
    assertThat(rows).hasSize(2);
    verifyJavaTinyFile(rows.get(0));
    verifyJavaBigFile(rows.get(1));
  }

  @Test
  public void sort_by_descending_numeric_measure() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("CLA").setSortOnMetric(METRIC_LINES).setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    // Big -> Tiny
    assertThat(rows).hasSize(2);
    verifyJavaBigFile(rows.get(0));
    verifyJavaTinyFile(rows.get(1));
  }

  @Test
  public void sort_by_missing_numeric_measure() {
    // coverage measures are not computed
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("CLA").setSortOnMetric(METRIC_COVERAGE);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    // 2 files, random order
    assertThat(rows).hasSize(2);
  }

  @Test
  public void sort_by_ascending_variation() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK").setSortOnMetric(METRIC_LINES).setSortOnPeriod(5);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(2);
    verifyJavaProject(rows.get(0));// +400
    verifyPhpProject(rows.get(1));// +4900
  }

  @Test
  public void sort_by_descending_variation() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK")
      .setSortOnMetric(METRIC_LINES).setSortOnPeriod(5).setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(2);
    verifyPhpProject(rows.get(0));// +4900
    verifyJavaProject(rows.get(1));// +400
  }

  @Test
  public void sort_by_ascending_date() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK").setSortOn(MeasureFilterSort.Field.DATE);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    verifyJavaProject(rows.get(0));// 2008
    verifyPhpProject(rows.get(1));// 2012
  }

  @Test
  public void sort_by_descending_date() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK").setSortOn(MeasureFilterSort.Field.DATE).setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    verifyPhpProject(rows.get(0));// 2012
    verifyJavaProject(rows.get(1));// 2008
  }

  @Test
  public void condition_on_numeric_measure() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("CLA")
      .setSortOnMetric(METRIC_LINES)
      .addCondition(new MeasureFilterValueCondition(METRIC_LINES, MeasureFilterValueCondition.Operator.GREATER, 200));
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(1);
    verifyJavaBigFile(rows.get(0));
  }

  @Test
  public void condition_on_measure_variation() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK")
      .setSortOnMetric(METRIC_LINES)
      .addCondition(new MeasureFilterValueCondition(METRIC_LINES, MeasureFilterValueCondition.Operator.GREATER, 1000).setPeriod(5));
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(1);
    verifyPhpProject(rows.get(0));
  }

  @Test
  public void multiple_conditions_on_numeric_measures() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("CLA")
      .setSortOnMetric(METRIC_LINES)
      .addCondition(new MeasureFilterValueCondition(METRIC_LINES, MeasureFilterValueCondition.Operator.GREATER, 2))
      .addCondition(new MeasureFilterValueCondition(METRIC_LINES, MeasureFilterValueCondition.Operator.LESS_OR_EQUALS, 50));
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(1);
    verifyJavaTinyFile(rows.get(0));
  }

  @Test
  public void filter_by_language() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK").setResourceLanguages("java", "cobol");
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(1);
    verifyJavaProject(rows.get(0));
  }

  @Test
  public void filter_by_min_date() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK").setFromDate(DateUtils.parseDate("2012-12-13"));
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    // php has been analyzed in 2012-12-13, whereas java project has been analyzed in 2008
    assertThat(rows).hasSize(1);
    verifyPhpProject(rows.get(0));
  }

  @Test
  public void filter_by_range_of_dates() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK")
      .setFromDate(DateUtils.parseDate("2007-01-01"))
      .setToDate(DateUtils.parseDate("2010-01-01"));
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    // php has been analyzed in 2012-12-13, whereas java project has been analyzed in 2008
    assertThat(rows).hasSize(1);
    verifyJavaProject(rows.get(0));
  }

  @Test
  public void filter_by_resource_name() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK").setResourceName("PHP Proj");
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(1);
    verifyPhpProject(rows.get(0));
  }

  @Test
  public void filter_by_base_resource() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("CLA").setBaseResourceKey("java_project");
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(2);
    // default sort is on resource name
    verifyJavaBigFile(rows.get(0));
    verifyJavaTinyFile(rows.get(1));
  }

  @Test
  public void filter_by_parent_resource() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK", "PAC", "CLA").setBaseResourceKey("java_project").setOnBaseResourceChildren(true);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).hasSize(1);// the package org.sonar.foo
    assertThat(rows.get(0).getSnapshotId()).isEqualTo(JAVA_PACKAGE_SNAPSHOT_ID);
  }

  @Test
  public void filter_by_parent_without_children() {
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers("TRK", "PAC", "CLA").setBaseResourceKey("java_project:org.sonar.foo.Big").setOnBaseResourceChildren(true);
    List<MeasureFilterRow> rows = executor.execute(filter, null);

    assertThat(rows).isEmpty();
  }

  @Test
  public void filter_by_user_favourites() {
    MeasureFilter filter = new MeasureFilter().setUserFavourites(true);
    List<MeasureFilterRow> rows = executor.execute(filter, 50L);

    assertThat(rows).hasSize(2);
    verifyJavaBigFile(rows.get(0));
    verifyPhpProject(rows.get(1));
  }

  private void verifyJavaProject(MeasureFilterRow row) {
    assertThat(row.getSnapshotId()).isEqualTo(JAVA_PROJECT_SNAPSHOT_ID);
    assertThat(row.getResourceId()).isEqualTo(JAVA_PROJECT_ID);
    assertThat(row.getResourceRootId()).isEqualTo(JAVA_PROJECT_ID);
  }

  private void verifyJavaBigFile(MeasureFilterRow row) {
    assertThat(row.getSnapshotId()).isEqualTo(JAVA_FILE_BIG_SNAPSHOT_ID);
    assertThat(row.getResourceId()).isEqualTo(JAVA_FILE_BIG_ID);
    assertThat(row.getResourceRootId()).isEqualTo(JAVA_PROJECT_ID);
  }

  private void verifyJavaTinyFile(MeasureFilterRow row) {
    assertThat(row.getSnapshotId()).isEqualTo(JAVA_FILE_TINY_SNAPSHOT_ID);
    assertThat(row.getResourceId()).isEqualTo(JAVA_FILE_TINY_ID);
    assertThat(row.getResourceRootId()).isEqualTo(JAVA_PROJECT_ID);
  }

  private void verifyPhpProject(MeasureFilterRow row) {
    assertThat(row.getSnapshotId()).isEqualTo(PHP_SNAPSHOT_ID);
    assertThat(row.getResourceId()).isEqualTo(PHP_PROJECT_ID);
    assertThat(row.getResourceRootId()).isEqualTo(PHP_PROJECT_ID);
  }
}
