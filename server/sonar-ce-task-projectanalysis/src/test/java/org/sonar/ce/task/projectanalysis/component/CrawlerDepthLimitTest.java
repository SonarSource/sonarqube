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
package org.sonar.ce.task.projectanalysis.component;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.ce.task.projectanalysis.component.Component.Type;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class CrawlerDepthLimitTest {
  private static final Set<Type> REPORT_TYPES = from(asList(Type.values())).filter(new Predicate<Type>() {
    @Override
    public boolean apply(Type input) {
      return input.isReportType();
    }
  }).toSet();
  private static final Set<Type> VIEWS_TYPES = from(asList(Type.values())).filter(new Predicate<Type>() {
    @Override
    public boolean apply(Type input) {
      return input.isViewsType();
    }
  }).toSet();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void PROJECT_isSameAs_only_PROJECT_type() {
    assertIsSameAs(CrawlerDepthLimit.PROJECT, Type.PROJECT);
  }

  @Test
  public void PROJECT_isDeeper_than_no_type() {
    for (Type type : Type.values()) {
      assertThat(CrawlerDepthLimit.PROJECT.isDeeperThan(type)).as("isHigherThan(%s)", type).isFalse();
    }
  }

  @Test
  public void PROJECT_isHigher_than_all_report_types_but_PROJECT() {
    assertThat(CrawlerDepthLimit.PROJECT.isHigherThan(Type.PROJECT)).isFalse();
    for (Type reportType : from(REPORT_TYPES).filter(not(equalTo(Type.PROJECT)))) {
      assertThat(CrawlerDepthLimit.PROJECT.isHigherThan(reportType)).as("isHigherThan(%s)", reportType).isTrue();
    }
  }

  @Test
  public void PROJECT_isDeeper_than_no_views_types() {
    for (Type viewsType : VIEWS_TYPES) {
      assertThat(CrawlerDepthLimit.PROJECT.isDeeperThan(viewsType)).as("isDeeperThan(%s)", viewsType).isFalse();
    }
  }

  @Test
  public void PROJECT_isHigher_than_no_views_types() {
    assertIsHigherThanViewsType(CrawlerDepthLimit.PROJECT);
  }

  @Test
  public void DIRECTORY_isSameAs_only_DIRECTORY_type() {
    assertIsSameAs(CrawlerDepthLimit.DIRECTORY, Type.DIRECTORY);
  }

  @Test
  public void DIRECTORY_isDeeper_than_no_views_types() {
    assertIsDeeperThanViewsType(CrawlerDepthLimit.DIRECTORY);
  }

  @Test
  public void DIRECTORY_isDeeper_than_only_PROJECT_report_type() {
    assertIsDeeperThanReportType(CrawlerDepthLimit.DIRECTORY, Type.PROJECT);
  }

  @Test
  public void DIRECTORY_isHigher_than_only_FILE() {
    assertIsHigherThanReportType(CrawlerDepthLimit.DIRECTORY, Type.FILE);
  }

  @Test
  public void DIRECTORY_isHigher_than_no_views_type() {
    assertIsHigherThanViewsType(CrawlerDepthLimit.DIRECTORY);
  }

  @Test
  public void FILE_isSameAs_only_FILE_type() {
    assertIsSameAs(CrawlerDepthLimit.FILE, Type.FILE);
  }

  @Test
  public void FILE_isDeeper_than_no_views_types() {
    for (Type viewsType : VIEWS_TYPES) {
      assertThat(CrawlerDepthLimit.FILE.isDeeperThan(viewsType)).as("isDeeperThan(%s)", viewsType).isFalse();
    }
  }

  @Test
  public void FILE_isHigher_than_no_views_types() {
    assertIsHigherThanViewsType(CrawlerDepthLimit.FILE);
  }

  @Test
  public void FILE_isHigher_than_no_report_types() {
    assertIsHigherThanReportType(CrawlerDepthLimit.FILE);
  }

  @Test
  public void FILE_isDeeper_than_only_PROJECT_MODULE_and_DIRECTORY_report_types() {
    assertIsDeeperThanReportType(CrawlerDepthLimit.FILE, Type.PROJECT, Type.DIRECTORY);
  }

  @Test
  public void VIEW_isSameAs_only_VIEW_type() {
    assertIsSameAs(CrawlerDepthLimit.VIEW, Type.VIEW);
  }

  @Test
  public void VIEW_isDeeper_than_no_type() {
    for (Type type : Type.values()) {
      assertThat(CrawlerDepthLimit.VIEW.isDeeperThan(type)).as("isDeeperThan(%s)", type).isFalse();
    }
  }

  @Test
  public void VIEW_isHigher_than_all_views_types_but_VIEW() {
    assertThat(CrawlerDepthLimit.VIEW.isHigherThan(Type.VIEW)).isFalse();
    for (Type viewsType : from(VIEWS_TYPES).filter(not(equalTo(Type.VIEW)))) {
      assertThat(CrawlerDepthLimit.VIEW.isHigherThan(viewsType)).as("isHigherThan(%s)", viewsType).isTrue();
    }
  }

  @Test
  public void VIEW_isHigher_than_no_report_types() {
    assertIsHigherThanReportType(CrawlerDepthLimit.VIEW);
  }

  @Test
  public void VIEW_isDeeper_than_no_report_types() {
    assertIsDeeperThanReportType(CrawlerDepthLimit.VIEW);
  }

  @Test
  public void VIEW_isDeeper_than_no_views_types() {
    assertIsDeeperThanViewsType(CrawlerDepthLimit.VIEW);
  }

  @Test
  public void SUBVIEW_isSameAs_only_SUBVIEW_type() {
    assertIsSameAs(CrawlerDepthLimit.SUBVIEW, Type.SUBVIEW);
  }

  @Test
  public void SUBVIEW_isHigher_than_no_report_types() {
    assertIsHigherThanReportType(CrawlerDepthLimit.SUBVIEW);
  }

  @Test
  public void SUBVIEW_isDeeper_than_no_report_types() {
    assertIsDeeperThanReportType(CrawlerDepthLimit.SUBVIEW);
  }

  @Test
  public void SUBVIEW_isDeeper_than_only_VIEW_views_types() {
    assertIsDeeperThanReportType(CrawlerDepthLimit.SUBVIEW, Type.VIEW);
  }

  @Test
  public void PROJECT_VIEW_isSameAs_only_PROJECT_VIEW_type() {
    assertIsSameAs(CrawlerDepthLimit.PROJECT_VIEW, Type.PROJECT_VIEW);
  }

  @Test
  public void PROJECT_VIEW_isHigher_than_no_report_types() {
    assertIsHigherThanReportType(CrawlerDepthLimit.PROJECT_VIEW);
  }

  @Test
  public void PROJECT_VIEW_isDeeper_than_no_report_types() {
    assertIsDeeperThanReportType(CrawlerDepthLimit.PROJECT_VIEW);
  }

  @Test
  public void PROJECT_VIEW_isDeeper_than_VIEWS_and_SUBVIEWS_views_types() {
    assertIsDeeperThanViewsType(CrawlerDepthLimit.PROJECT_VIEW, Type.VIEW, Type.SUBVIEW);
  }

  @Test
  public void LEAVES_is_same_as_FILE_and_PROJECT_VIEW() {
    assertThat(CrawlerDepthLimit.LEAVES.isSameAs(Type.FILE)).isTrue();
    assertThat(CrawlerDepthLimit.LEAVES.isSameAs(Type.PROJECT_VIEW)).isTrue();
    for (Type type : from(asList(Type.values())).filter(not(in(ImmutableSet.of(Type.FILE, Type.PROJECT_VIEW))))) {
      assertThat(CrawlerDepthLimit.LEAVES.isSameAs(type)).isFalse();
    }
  }

  @Test
  public void LEAVES_isDeeper_than_PROJECT_MODULE_and_DIRECTORY_report_types() {
    assertIsDeeperThanReportType(CrawlerDepthLimit.LEAVES, Type.PROJECT, Type.DIRECTORY);
  }

  @Test
  public void LEAVES_isDeeper_than_VIEW_and_SUBVIEW_views_types() {
    assertIsDeeperThanViewsType(CrawlerDepthLimit.LEAVES, Type.VIEW, Type.SUBVIEW);
  }

  @Test
  public void LEAVES_isHigher_than_no_report_types() {
    assertIsHigherThanReportType(CrawlerDepthLimit.LEAVES);
  }

  @Test
  public void LEAVES_isHigher_than_no_views_types() {
    assertIsHigherThanViewsType(CrawlerDepthLimit.LEAVES);
  }

  private void assertIsSameAs(CrawlerDepthLimit crawlerDepthLimit, Type expectedType) {
    assertThat(crawlerDepthLimit.isSameAs(expectedType)).isTrue();
    for (Type type : from(asList(Type.values())).filter(not(equalTo(expectedType)))) {
      assertThat(crawlerDepthLimit.isSameAs(type)).isFalse();
    }
  }

  private void assertIsHigherThanReportType(CrawlerDepthLimit depthLimit, Type... types) {
    for (Type type : types) {
      assertThat(depthLimit.isHigherThan(type)).as("isHigherThan(%s)", type).isTrue();
    }
    for (Type reportType : from(REPORT_TYPES).filter(not(in(Arrays.asList(types))))) {
      assertThat(depthLimit.isHigherThan(reportType)).as("isHigherThan(%s)", reportType).isFalse();
    }
  }

  private void assertIsHigherThanViewsType(CrawlerDepthLimit depthLimit, Type... types) {
    for (Type type : types) {
      assertThat(depthLimit.isHigherThan(type)).as("isHigherThan(%s)", type).isTrue();
    }
    for (Type reportType : from(VIEWS_TYPES).filter(not(in(Arrays.asList(types))))) {
      assertThat(depthLimit.isHigherThan(reportType)).as("isHigherThan(%s)", reportType).isFalse();
    }
  }

  private void assertIsDeeperThanReportType(CrawlerDepthLimit depthLimit, Type... types) {
    for (Type type : types) {
      assertThat(depthLimit.isDeeperThan(type)).as("isDeeperThan(%s)", type).isTrue();
    }
    for (Type reportType : from(REPORT_TYPES).filter(not(in(Arrays.asList(types))))) {
      assertThat(depthLimit.isDeeperThan(reportType)).as("isDeeperThan(%s)", reportType).isFalse();
    }
  }

  private void assertIsDeeperThanViewsType(CrawlerDepthLimit depthLimit, Type... types) {
    for (Type type : types) {
      assertThat(depthLimit.isDeeperThan(type)).as("isDeeperThan(%s)", type).isTrue();
    }
    for (Type reportType : from(VIEWS_TYPES).filter(not(in(Arrays.asList(types))))) {
      assertThat(depthLimit.isDeeperThan(reportType)).as("isDeeperThan(%s)", reportType).isFalse();
    }
  }

  @Test
  @UseDataProvider("viewsTypes")
  public void reportMaxDepth_throws_IAE_if_type_is_views(Type viewsType) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A Report max depth must be a report type");

    CrawlerDepthLimit.reportMaxDepth(viewsType);
  }

  @Test
  @UseDataProvider("reportTypes")
  public void reportMaxDepth_accepts_type_if_report_type(Type reportType) {
    CrawlerDepthLimit.reportMaxDepth(reportType);
  }

  @Test
  @UseDataProvider("reportTypes")
  public void withViewsMaxDepth_throws_IAE_if_type_is_report(Type reportType) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A Views max depth must be a views type");

    CrawlerDepthLimit.reportMaxDepth(reportType).withViewsMaxDepth(reportType);
  }

  @DataProvider
  public static Object[][] viewsTypes() {
    return from(VIEWS_TYPES).transform(new Function<Type, Object[]>() {
      @Nullable
      @Override
      public Object[] apply(Type input) {
        return new Object[] {input};
      }
    }).toArray(Object[].class);
  }

  @DataProvider
  public static Object[][] reportTypes() {
    return from(REPORT_TYPES).transform(new Function<Type, Object[]>() {
      @Nullable
      @Override
      public Object[] apply(Type input) {
        return new Object[] {input};
      }
    }).toArray(Object[].class);
  }
}
