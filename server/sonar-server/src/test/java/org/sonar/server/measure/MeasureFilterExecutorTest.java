/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.measure;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class MeasureFilterExecutorTest {

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
  private static final Metric METRIC_UNKNOWN = new Metric.Builder("unknown", "Unknown", Metric.ValueType.FLOAT).create().setId(4);
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private MeasureFilterExecutor executor;

  @Before
  public void before() {
    executor = new MeasureFilterExecutor(db.myBatis(), db.database(), new ResourceDao(db.myBatis(), System2.INSTANCE));
  }

  @Test
  public void should_return_empty_results_if_empty_filter() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter();
    assertThat(filter.isEmpty()).isTrue();

    assertThat(executor.execute(filter, new MeasureFilterContext())).isEmpty();
  }

  @Test
  public void invalid_filter_should_not_return_results() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setUserFavourites(true);
    // anonymous user does not have favourites
    assertThat(executor.execute(filter, new MeasureFilterContext())).isEmpty();
  }

  @Test
  public void filter_is_not_valid_if_missing_base_snapshot() {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilterContext context = new MeasureFilterContext();
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setOnBaseResourceChildren(true);
    assertThat(MeasureFilterExecutor.isValid(filter, context)).isFalse();

    context.setBaseSnapshot(new SnapshotDto().setId(123L));
    assertThat(MeasureFilterExecutor.isValid(filter, context)).isTrue();
  }

  @Test
  public void filter_is_not_valid_if_condition_on_unknown_metric() {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilterContext context = new MeasureFilterContext();
    MeasureFilter filter = new MeasureFilter().addCondition(new MeasureFilterCondition(null, MeasureFilterCondition.Operator.LESS, 3.0));
    assertThat(MeasureFilterExecutor.isValid(filter, context)).isFalse();
  }

  @Test
  public void filter_is_not_valid_if_sorting_on_unknown_metric() {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilterContext context = new MeasureFilterContext();
    MeasureFilter filter = new MeasureFilter().setSortOnMetric(null);
    assertThat(MeasureFilterExecutor.isValid(filter, context)).isFalse();
  }

  @Test
  public void filter_is_not_valid_if_anonymous_favourites() {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilterContext context = new MeasureFilterContext();
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setUserFavourites(true);
    assertThat(MeasureFilterExecutor.isValid(filter, context)).isFalse();

    context.setUserId(123L);
    assertThat(MeasureFilterExecutor.isValid(filter, context)).isTrue();
  }

  @Test
  public void projects_without_measure_conditions() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortOn(MeasureFilterSort.Field.DATE);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(2);
    verifyJavaProject(rows.get(0));
    verifyPhpProject(rows.get(1));
  }

  @Test
  public void should_prevent_sql_injection_through_parameters() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter()
      .setResourceQualifiers(Arrays.asList("'"))
      .setBaseResourceKey("'")
      .setResourceKey("'")
      .setResourceName("'")
      .setResourceName("'")
      .setResourceScopes(Arrays.asList("'"));
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());
    // an exception would be thrown if SQL is not valid
    assertThat(rows).isEmpty();
  }

  @Test
  public void test_default_sort() {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("CLA"));

    assertThat(filter.sort().isAsc()).isTrue();
    assertThat(filter.sort().field()).isEqualTo(MeasureFilterSort.Field.NAME);
    assertThat(filter.sort().metric()).isNull();
  }

  @Test
  public void sort_by_ascending_resource_name() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("CLA")).setSortAsc(true);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // Big -> Tiny
    assertThat(rows).hasSize(2);
    verifyJavaBigFile(rows.get(0));
    verifyJavaTinyFile(rows.get(1));
  }

  @Test
  public void sort_by_ascending_resource_key() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("CLA")).setSortAsc(true).setSortOn(MeasureFilterSort.Field.KEY);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // Big -> Tiny
    assertThat(rows).hasSize(2);
    verifyJavaBigFile(rows.get(0));
    verifyJavaTinyFile(rows.get(1));
  }

  @Test
  public void sort_by_ascending_resource_version() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortAsc(true).setSortOn(MeasureFilterSort.Field.VERSION);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // Java Project 1.0 then Php Project 3.0
    assertThat(rows).hasSize(2);
    verifyJavaProject(rows.get(0));
    verifyPhpProject(rows.get(1));
  }

  @Test
  public void sort_by_descending_resource_name() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("CLA")).setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // Tiny -> Big
    assertThat(rows).hasSize(2);
    verifyJavaTinyFile(rows.get(0));
    verifyJavaBigFile(rows.get(1));
  }

  @Test
  public void sort_by_ascending_text_measure() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortOnMetric(METRIC_PROFILE);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(2);
    verifyPhpProject(rows.get(0));// php way
    verifyJavaProject(rows.get(1));// Sonar way
  }

  @Test
  public void sort_by_descending_text_measure() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortOnMetric(METRIC_PROFILE).setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(2);
    verifyJavaProject(rows.get(0));// Sonar way
    verifyPhpProject(rows.get(1));// php way
  }

  @Test
  public void sort_by_missing_text_measure() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    // the metric 'profile' is not set on files
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("CLA")).setSortOnMetric(METRIC_PROFILE);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(2);// 2 files randomly sorted
  }

  @Test
  public void sort_by_ascending_numeric_measure() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("CLA")).setSortOnMetric(METRIC_LINES);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // Tiny -> Big
    assertThat(rows).hasSize(2);
    verifyJavaTinyFile(rows.get(0));
    verifyJavaBigFile(rows.get(1));
  }

  @Test
  public void sort_by_descending_numeric_measure() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("CLA")).setSortOnMetric(METRIC_LINES).setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // Big -> Tiny
    assertThat(rows).hasSize(2);
    verifyJavaBigFile(rows.get(0));
    verifyJavaTinyFile(rows.get(1));
  }

  @Test
  public void null_measures_are_ordered_after_descending_numeric_measures() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK"))
      .setSortOnMetric(METRIC_COVERAGE).setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // Java project has coverage but not PHP
    assertThat(rows).hasSize(2);
    verifyJavaProject(rows.get(0));
    verifyPhpProject(rows.get(1));
  }

  @Test
  public void null_measures_are_ordered_after_ascending_numeric_measures() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK"))
      .setSortOnMetric(METRIC_COVERAGE).setSortAsc(true);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // Java project has coverage but not PHP
    assertThat(rows).hasSize(2);
    verifyJavaProject(rows.get(0));
    verifyPhpProject(rows.get(1));
  }

  @Test
  public void sort_by_missing_numeric_measure() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    // coverage measures are not computed
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("CLA")).setSortOnMetric(METRIC_UNKNOWN);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // 2 files, random order
    assertThat(rows).hasSize(2);
  }

  @Test
  public void sort_by_ascending_variation() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortOnMetric(METRIC_LINES).setSortOnPeriod(5);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(2);
    verifyJavaProject(rows.get(0));// +400
    verifyPhpProject(rows.get(1));// +4900
  }

  @Test
  public void sort_by_descending_variation() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK"))
      .setSortOnMetric(METRIC_LINES).setSortOnPeriod(5).setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(2);
    verifyPhpProject(rows.get(0));// +4900
    verifyJavaProject(rows.get(1));// +400
  }

  @Test
  public void sort_by_ascending_date() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortOn(MeasureFilterSort.Field.DATE);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    verifyJavaProject(rows.get(0));// 2008
    verifyPhpProject(rows.get(1));// 2012
  }

  @Test
  public void sort_by_descending_date() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortOn(MeasureFilterSort.Field.DATE).setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    verifyPhpProject(rows.get(0));// 2012
    verifyJavaProject(rows.get(1));// 2008
  }

  @Test
  public void sort_by_ascending_created_at() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortOn(MeasureFilterSort.Field.PROJECT_CREATION_DATE);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    verifyJavaProject(rows.get(0));// 2008
    assertThat(DateUtils.formatDate(new Date(rows.get(0).getSortDate()))).isEqualTo("2008-12-19");
    verifyPhpProject(rows.get(1));// 2012
    assertThat(DateUtils.formatDate(new Date(rows.get(1).getSortDate()))).isEqualTo("2012-12-12");
  }

  @Test
  public void sort_by_descending_created_at() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortOn(MeasureFilterSort.Field.PROJECT_CREATION_DATE).setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    verifyPhpProject(rows.get(0));// 2012
    assertThat(DateUtils.formatDate(new Date(rows.get(0).getSortDate()))).isEqualTo("2012-12-12");
    verifyJavaProject(rows.get(1));// 2008
    assertThat(DateUtils.formatDate(new Date(rows.get(1).getSortDate()))).isEqualTo("2008-12-19");
  }

  @Test
  public void sort_by_ascending_alert() throws SQLException {
    db.prepareDbUnit(getClass(), "sort_by_alert.xml");

    Metric alert = new Metric.Builder(CoreMetrics.ALERT_STATUS_KEY, "Alert", Metric.ValueType.LEVEL).create().setId(5);
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortOnMetric(alert);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // Php Project OK, Java Project WARN then Js Project ERROR
    assertThat(rows).hasSize(3);
    verifyPhpProject(rows.get(0));
    verifyJavaProject(rows.get(1));
    verifyProject(rows.get(2), 120L, 20L, 20L);
  }

  @Test
  public void sort_by_descending_alert() throws SQLException {
    db.prepareDbUnit(getClass(), "sort_by_alert.xml");

    Metric alert = new Metric.Builder(CoreMetrics.ALERT_STATUS_KEY, "Alert", Metric.ValueType.LEVEL).create().setId(5);
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortOnMetric(alert).setSortAsc(false);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // Js Project ERROR, Java Project WARN, then Php Project OK
    assertThat(rows).hasSize(3);
    verifyProject(rows.get(0), 120L, 20L, 20L);
    verifyJavaProject(rows.get(1));
    verifyPhpProject(rows.get(2));
  }

  @Test
  public void condition_on_numeric_measure() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("CLA"))
      .setSortOnMetric(METRIC_LINES)
      .addCondition(new MeasureFilterCondition(METRIC_LINES, MeasureFilterCondition.Operator.GREATER, 200));
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(1);
    verifyJavaBigFile(rows.get(0));
  }

  @Test
  public void condition_on_measure_variation() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK"))
      .setSortOnMetric(METRIC_LINES)
      .addCondition(new MeasureFilterCondition(METRIC_LINES, MeasureFilterCondition.Operator.GREATER, 1000).setPeriod(5));
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(1);
    verifyPhpProject(rows.get(0));
  }

  @Test
  public void multiple_conditions_on_numeric_measures() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("CLA"))
      .setSortOnMetric(METRIC_LINES)
      .addCondition(new MeasureFilterCondition(METRIC_LINES, MeasureFilterCondition.Operator.GREATER, 2))
      .addCondition(new MeasureFilterCondition(METRIC_LINES, MeasureFilterCondition.Operator.LESS_OR_EQUALS, 50));
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(1);
    verifyJavaTinyFile(rows.get(0));
  }

  @Test
  public void filter_by_min_date() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setFromDate(DateUtils.parseDateTime("2012-12-13T00:00:00+0000"));
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // php has been analyzed in 2012-12-13, whereas java project has been analyzed in 2008
    assertThat(rows).hasSize(1);
    verifyPhpProject(rows.get(0));
  }

  @Test
  public void filter_by_range_of_dates() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK"))
      .setFromDate(DateUtils.parseDate("2007-01-01"))
      .setToDate(DateUtils.parseDate("2010-01-01"));
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    // php has been analyzed in 2012-12-13, whereas java project has been analyzed in 2008
    assertThat(rows).hasSize(1);
    verifyJavaProject(rows.get(0));
  }

  @Test
  public void filter_by_component_name() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setResourceName("PHP Proj");
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(1);
    verifyPhpProject(rows.get(0));
  }

  @Test
  public void filter_by_component_key() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setResourceKey("Va_proje");
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(1);
    verifyJavaProject(rows.get(0));
  }

  /**
   * see SONAR-4195
   */
  @Test
  public void filter_by_upper_case_component_key() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("CLA")).setResourceKey("big");
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(1);
    verifyJavaBigFile(rows.get(0));
  }

  /**
   * see SONAR-4796
   */
  @Test
  public void escape_percent_and_underscore_when_filter_by_component_name_or_key() throws SQLException {
    db.prepareDbUnit(getClass(), "escape_percent_and_underscore_when_filter_by_component_name_or_key.xml");

    assertThat(executor.execute(
      new MeasureFilter().setResourceQualifiers(newArrayList("CLA")).setResourceKey("java_"),
      new MeasureFilterContext())).hasSize(2);

    assertThat(executor.execute(
      new MeasureFilter().setResourceQualifiers(newArrayList("CLA")).setResourceName("java%"),
      new MeasureFilterContext())).hasSize(2);
  }

  @Test
  public void filter_by_base_resource() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("CLA")).setBaseResourceKey("java_project");
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(2);
    // default sort is on resource name
    verifyJavaBigFile(rows.get(0));
    verifyJavaTinyFile(rows.get(1));
  }

  @Test
  public void filter_by_parent_resource() throws SQLException {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setBaseResourceKey("java_project").setOnBaseResourceChildren(true);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).hasSize(1);// the package org.sonar.foo
    assertThat(rows.get(0).getSnapshotId()).isEqualTo(JAVA_PACKAGE_SNAPSHOT_ID);
  }

  @Test
  public void filter_by_parent_without_children() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK", "PAC", "CLA")).setBaseResourceKey("java_project:org.sonar.foo.Big")
      .setOnBaseResourceChildren(true);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext());

    assertThat(rows).isEmpty();
  }

  @Test
  public void filter_by_user_favourites() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK", "FIL")).setUserFavourites(true);
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext().setUserId(50L));

    assertThat(rows).hasSize(2);
    verifyJavaBigFile(rows.get(0));
    verifyPhpProject(rows.get(1));
  }

  @Test
  public void ignore_person_measures_in_condition() throws Exception {
    db.prepareDbUnit(getClass(), "ignore_person_measures.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).addCondition(
      new MeasureFilterCondition(new Metric("ncloc").setId(1), MeasureFilterCondition.Operator.GREATER, 0.0)
      );
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext().setUserId(50L));

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getSnapshotId()).isEqualTo(101L);
  }

  @Test
  public void ignore_person_measures_in_sort() throws Exception {
    db.prepareDbUnit(getClass(), "ignore_person_measures.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortOnMetric(new Metric("ncloc").setId(1));
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext().setUserId(50L));

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getSnapshotId()).isEqualTo(101L);
  }

  @Test
  public void ignore_quality_model_measures_in_condition() throws Exception {
    db.prepareDbUnit(getClass(), "ignore_quality_model_measures.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).addCondition(
      new MeasureFilterCondition(new Metric("ncloc").setId(1), MeasureFilterCondition.Operator.GREATER, 0.0)
      );
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext().setUserId(50L));

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getSnapshotId()).isEqualTo(101L);
  }

  @Test
  public void ignore_quality_model_measures_in_sort() throws Exception {
    db.prepareDbUnit(getClass(), "ignore_quality_model_measures.xml");
    MeasureFilter filter = new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).setSortOnMetric(new Metric("ncloc").setId(1));
    List<MeasureFilterRow> rows = executor.execute(filter, new MeasureFilterContext().setUserId(50L));

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getSnapshotId()).isEqualTo(101L);
  }

  private void verifyJavaProject(MeasureFilterRow row) {
    verifyProject(row, JAVA_PROJECT_SNAPSHOT_ID, JAVA_PROJECT_ID, JAVA_PROJECT_ID);
  }

  private void verifyJavaBigFile(MeasureFilterRow row) {
    verifyProject(row, JAVA_FILE_BIG_SNAPSHOT_ID, JAVA_FILE_BIG_ID, JAVA_PROJECT_ID);
  }

  private void verifyJavaTinyFile(MeasureFilterRow row) {
    verifyProject(row, JAVA_FILE_TINY_SNAPSHOT_ID, JAVA_FILE_TINY_ID, JAVA_PROJECT_ID);
  }

  private void verifyPhpProject(MeasureFilterRow row) {
    verifyProject(row, PHP_SNAPSHOT_ID, PHP_PROJECT_ID, PHP_PROJECT_ID);
  }

  private void verifyProject(MeasureFilterRow row, Long snashotId, Long resourceId, Long resourceRootId) {
    assertThat(row.getSnapshotId()).isEqualTo(snashotId);
    assertThat(row.getResourceId()).isEqualTo(resourceId);
    assertThat(row.getResourceRootId()).isEqualTo(resourceRootId);
  }
}
