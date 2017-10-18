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
package org.sonar.server.issue.index;

import java.util.TimeZone;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.ExtendedBounds;
import org.joda.time.DateTimeZone;
import org.sonar.api.utils.DateUtils;

class DateHistogramBuilder {

  private String aggregationName;
  private String field;
  private DateHistogramInterval bucketSize;
  private long startTime;
  private long endTime;
  private long timeZoneRawOffsetMillis;

  DateHistogramAggregationBuilder build() {
    return AggregationBuilders.dateHistogram(aggregationName)
      .field(field)
      .dateHistogramInterval(bucketSize)
      .minDocCount(0L)
      .format(DateUtils.DATETIME_FORMAT)
      .timeZone(DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT")))
      .offset(getOffsetInSeconds() + "s")
      // ES dateHistogram bounds are inclusive while createdBefore parameter is exclusive
      .extendedBounds(new ExtendedBounds(getMin(), getMax()));
  }

  /**
   * @return the time difference of this machine's local time to UTC (as seconds)
   */
  private long getOffsetInSeconds() {
    return -timeZoneRawOffsetMillis / 1_000L;
  }

  private long getMin() {
    return startTime;
  }

  private long getMax() {
    return endTime - (getOffsetInSeconds() * 1_000L) - 1L;
  }

  public DateHistogramBuilder setAggregationName(String aggregationName) {
    this.aggregationName = aggregationName;
    return this;
  }

  public DateHistogramBuilder setField(String field) {
    this.field = field;
    return this;
  }

  public DateHistogramBuilder setBucketSize(DateHistogramInterval bucketSize) {
    this.bucketSize = bucketSize;
    return this;
  }

  public DateHistogramBuilder setStartTime(long startTime) {
    this.startTime = startTime;
    return this;
  }

  public DateHistogramBuilder setEndTime(long endTime) {
    this.endTime = endTime;
    return this;
  }

  public DateHistogramBuilder setTimeZoneRawOffsetMillis(long timeZoneRawOffsetMillis) {
    this.timeZoneRawOffsetMillis = timeZoneRawOffsetMillis;
    return this;
  }
}
