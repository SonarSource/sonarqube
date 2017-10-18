package org.sonar.server.issue.index;

import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AT;

public class DateHistogramBuilderTest {

  @Test
  public void bounds_for_a_single_day() throws Exception {
    DateHistogramAggregationBuilder dateHistogram = new DateHistogramBuilder()
      .setAggregationName(PARAM_CREATED_AT)
      .setField(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT)
      .setBucketSize(DateHistogramInterval.DAY)
      .setStartTime(startOfLocalDay("2017-01-01"))
      .setEndTime(startOfLocalDay("2017-01-02"))
      .setTimeZoneRawOffsetMillis(startOfUtcDay("2017-01-01") - startOfLocalDay("2017-01-01"))
      .build();
    dateAssert(dateHistogram.extendedBounds().getMin(), "min", startOfUtcDay("2017-01-01"));
    dateAssert(dateHistogram.extendedBounds().getMax(), "max", endOfUtcDay("2017-01-01"));
  }

  private void dateAssert(long actual, String name, long expected) {
    String description = name + ":\n" +
      "Expected :" + DateUtils.formatDateTime(expected) + "\n" +
      "Actual   :" + DateUtils.formatDateTime(actual) + "\n";
    assertThat(actual).as(description).isEqualTo(expected);
  }

  @Test
  public void test_this_test() throws Exception {
    assertThat(startOfLocalDay("2017-01-01")).isEqualTo(1_483_225_200_000L);
    assertThat(startOfUtcDay("2017-01-01")).isEqualTo(1_483_228_800_000L);
    assertThat(endOfUtcDay("2017-01-01")).isEqualTo(1_483_315_199_999L);
  }

  private static long startOfLocalDay(String day) {
    return DateUtils.parseDateTime(day +"T00:00:00+0100").getTime();
  }

  private static long startOfUtcDay(String day) {
    return DateUtils.parseDateTime(day +"T00:00:00+0000").getTime();
  }

  private static long endOfUtcDay(String day) {
    return DateUtils.parseDateTime(day +"T23:59:59+0000").getTime() + 999L;
  }
}
