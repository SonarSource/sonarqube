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
package org.sonar.batch;

import org.junit.Test;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.indexer.DefaultSonarIndex;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultTimeMachineTest extends AbstractDbUnitTestCase {

  @Test(timeout = 3000)
  public void loadMeasureFieldsFromDate() throws ParseException {
    setupData("loadMeasuresFromDate");
    DefaultTimeMachine timeMachine = initTimeMachine();

    TimeMachineQuery query = new TimeMachineQuery(null).setFrom(date("2008-02-01")).setMetrics(Arrays.asList(CoreMetrics.NCLOC));
    List<Object[]> measures = timeMachine.getMeasuresFields(query);

    assertThat(measures.size(), is(3));
    for (Object[] measure : measures) {
      assertThat(measure.length, is(3)); // 3 fields
      assertThat(measure[1], is((Object) CoreMetrics.NCLOC));
    }
    assertThat(measures.get(0)[2], is((Object) 200d));
    assertThat(measures.get(1)[2], is((Object) 230d));
    assertThat(measures.get(2)[2], is((Object) 180d));
  }

  private DefaultTimeMachine initTimeMachine() {
    DefaultSonarIndex index = mock(DefaultSonarIndex.class);
    when(index.getResource((Resource) anyObject())).thenReturn(new Project("group:artifact").setId(1));
    DefaultTimeMachine timeMachine = new DefaultTimeMachine(getSession(), index, new MeasuresDao(getSession()));
    return timeMachine;
  }

  @Test(timeout = 3000)
  public void loadMeasuresFromDate() throws ParseException {
    setupData("loadMeasuresFromDate");
    DefaultTimeMachine timeMachine = initTimeMachine();


    TimeMachineQuery query = new TimeMachineQuery(null).setFrom(date("2008-02-01")).setMetrics(Arrays.asList(CoreMetrics.NCLOC));
    List<Measure> measures = timeMachine.getMeasures(query);

    assertThat(measures.size(), is(3));
    long previous = 0;
    for (Measure measure : measures) {
      assertThat(measure.getMetric(), is(CoreMetrics.NCLOC));
      assertThat(measure.getDate().getTime(), greaterThan(previous));
      previous = measure.getDate().getTime();
    }
    assertThat(measures.get(0).getValue(), is(200d));
    assertThat(measures.get(1).getValue(), is(230d));
    assertThat(measures.get(2).getValue(), is(180d));
  }

  @Test(timeout = 3000)
  public void loadMeasuresFromDateInterval() throws ParseException {
    setupData("loadMeasuresFromDate");
    DefaultTimeMachine timeMachine = initTimeMachine();


    TimeMachineQuery query = new TimeMachineQuery(null).setFrom(date("2008-01-01")).setTo(date("2008-12-25")).setMetrics(Arrays.asList(CoreMetrics.NCLOC));
    List<Measure> measures = timeMachine.getMeasures(query);
    assertThat(measures.size(), is(1));
    assertThat(measures.get(0).getValue(), is(200d));
  }

  private Date date(String date) throws ParseException {
    return new SimpleDateFormat("yyyy-MM-dd").parse(date);
  }
}
