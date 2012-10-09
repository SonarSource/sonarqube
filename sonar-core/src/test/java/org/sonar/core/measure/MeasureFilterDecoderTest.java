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

import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeasureFilterDecoderTest {

  private MetricFinder metricFinder;

  @Before
  public void before() {
    metricFinder = mock(MetricFinder.class);
    when(metricFinder.findByKey(anyString())).thenAnswer(new Answer<Metric>() {
      public Metric answer(InvocationOnMock invocationOnMock) throws Throwable {
        return new Metric((String) invocationOnMock.getArguments()[0]);
      }
    });
  }

  @Test
  public void should_decode() throws ParseException {
    String json = "{\"base\": \"org.struts\", \"onBaseChildren\": true, \"scopes\": [\"PRJ\"], " +
      "\"qualifiers\": [\"TRK\",\"CLA\"], " +
      "\"languages\": [\"java\", \"php\"], \"name\": \"Struts\", \"fromDate\": \"2012-12-25\", " +
      "\"toDate\": \"2013-01-31\", " +
      "\"favourites\": true, " +
      "\"sortAsc\": true, \"sortField\": \"METRIC\", \"sortMetric\": \"ncloc\", \"sortPeriod\":5, " +
      "\"conditions\":[{\"metric\":\"lines\", \"op\":\">\", \"val\":123.0}]}";

    MeasureFilter filter = new MeasureFilterDecoder(metricFinder).decode(json);

    assertThat(filter.getBaseResourceKey()).isEqualTo("org.struts");
    assertThat(filter.isOnBaseResourceChildren()).isTrue();
    assertThat(filter.getResourceScopes()).containsExactly("PRJ");
    assertThat(filter.getResourceQualifiers()).containsExactly("TRK", "CLA");
    assertThat(filter.getResourceLanguages()).containsExactly("java", "php");
    assertThat(filter.getResourceName()).isEqualTo("Struts");
    assertThat(filter.getFromDate().getYear()).isEqualTo(2012 - 1900);
    assertThat(filter.getToDate().getYear()).isEqualTo(2013 - 1900);
    assertThat(filter.isOnFavourites()).isTrue();
    assertThat(filter.sort().metric().getKey()).isEqualTo("ncloc");
    assertThat(filter.sort().isAsc()).isTrue();
    MeasureFilterCondition condition = filter.getMeasureConditions().get(0);
    assertThat(condition.metric().getKey()).isEqualTo("lines");
    assertThat(condition.operator()).isEqualTo(">");
    assertThat(condition.value()).isEqualTo(123.0);
  }

  @Test
  public void should_set_max_date_by_number_of_days() throws ParseException {
    String json = "{\"beforeDays\": 5}";

    MeasureFilter filter = new MeasureFilterDecoder(metricFinder).decode(json);

    assertThat(filter.getFromDate()).isNull();
    assertThat(filter.getToDate().before(new Date())).isTrue();
  }

  @Test
  public void should_set_min_date_by_number_of_days() throws ParseException {
    String json = "{\"afterDays\": 5}";

    MeasureFilter filter = new MeasureFilterDecoder(metricFinder).decode(json);

    assertThat(filter.getToDate()).isNull();
    assertThat(filter.getFromDate().before(new Date())).isTrue();
  }

  @Test
  public void test_default_values() throws ParseException {
    MeasureFilter filter = new MeasureFilterDecoder(metricFinder).decode("{}");

    assertThat(filter.getBaseResourceKey()).isNull();
    assertThat(filter.isOnBaseResourceChildren()).isFalse();
    assertThat(filter.getResourceScopes()).isEmpty();
    assertThat(filter.getResourceQualifiers()).isEmpty();
    assertThat(filter.getResourceLanguages()).isEmpty();
    assertThat(filter.getResourceName()).isNull();
    assertThat(filter.getFromDate()).isNull();
    assertThat(filter.getToDate()).isNull();
    assertThat(filter.isOnFavourites()).isFalse();
    assertThat(filter.sort().metric()).isNull();
    assertThat(filter.sort().getPeriod()).isNull();
    assertThat(filter.sort().onMeasures()).isFalse();
    assertThat(filter.sort().field()).isEqualTo(MeasureFilterSort.Field.NAME);
    assertThat(filter.sort().isAsc()).isTrue();
    assertThat(filter.getMeasureConditions()).isEmpty();

  }
}
