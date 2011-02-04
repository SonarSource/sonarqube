/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.tests.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.*;

import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ViolationsTimeMachineIT {

  private static Sonar sonar;
  private static final String PROJECT = "org.sonar.tests:violations-timemachine";
  private static final String FILE = "org.sonar.tests:violations-timemachine:org.sonar.tests.violationstimemachine.Hello";

  @BeforeClass
  public static void buildServer() {
    sonar = ITUtils.createSonarWsClient();
  }

  @Test
  public void projectIsAnalyzed() {
    assertThat(sonar.find(new ResourceQuery(PROJECT)).getName(), is("Violations timemachine"));
    assertThat(sonar.find(new ResourceQuery(PROJECT)).getVersion(), is("1.0-SNAPSHOT"));
    assertThat(sonar.find(new ResourceQuery(PROJECT)).getDate().getMonth(), is(10)); // November
  }

  @Test
  public void testHistoryOfViolations() {
    TimeMachineQuery query = TimeMachineQuery.createForMetrics(PROJECT,
        CoreMetrics.BLOCKER_VIOLATIONS_KEY,
        CoreMetrics.CRITICAL_VIOLATIONS_KEY,
        CoreMetrics.MAJOR_VIOLATIONS_KEY,
        CoreMetrics.MINOR_VIOLATIONS_KEY,
        CoreMetrics.INFO_VIOLATIONS_KEY);
    TimeMachine timemachine = sonar.find(query);
    assertThat(timemachine.getCells().length, is(2));

    TimeMachineCell cell1 = timemachine.getCells()[0];
    TimeMachineCell cell2 = timemachine.getCells()[1];

    assertThat(cell1.getDate().getMonth(), is(9));
    assertThat(cell1.getValues(), is(new Object[]{0L, 0L, 3L, 4L, 0L}));

    assertThat(cell2.getDate().getMonth(), is(10));
    assertThat(cell2.getValues(), is(new Object[]{0L, 0L, 5L, 4L, 0L}));
  }

  @Test
  public void testHistoryOfMeasures() {
    TimeMachineQuery query = TimeMachineQuery.createForMetrics(PROJECT,
        CoreMetrics.LINES_KEY,//int
        CoreMetrics.VIOLATIONS_DENSITY_KEY // double
        );
    TimeMachine timemachine = sonar.find(query);
    assertThat(timemachine.getCells().length, is(2));

    TimeMachineCell cell1 = timemachine.getCells()[0];
    TimeMachineCell cell2 = timemachine.getCells()[1];

    assertThat(cell1.getDate().getMonth(), is(9));
    assertThat(cell1.getValues(), is(new Object[]{30L, 38.1}));

    assertThat(cell2.getDate().getMonth(), is(10));
    assertThat(cell2.getValues(), is(new Object[]{41L, 34.5}));
  }

  @Test
  public void correctLinesAndDates() {
    ViolationQuery query = ViolationQuery.createForResource(FILE).setSeverities("MAJOR");
    List<Violation> violations = sonar.findAll(query);

    assertThat(violations.get(0).getLine(), is(8));
    assertThat(violations.get(0).getCreatedAt().getMonth(), is(9)); // old violation

    assertThat(violations.get(1).getLine(), is(13));
    assertThat(violations.get(1).getCreatedAt().getMonth(), is(9)); // old violation

    assertThat(violations.get(2).getLine(), is(18));
    assertThat(violations.get(2).getCreatedAt().getMonth(), is(10)); // new violation
  }

  // Specific cases for timemachine web service

  @Test
  public void unknownMetrics() {
    TimeMachine timemachine = sonar.find(TimeMachineQuery.createForMetrics(PROJECT, "notfound"));
    assertThat(timemachine.getCells().length, is(0));

    timemachine = sonar.find(TimeMachineQuery.createForMetrics(PROJECT, CoreMetrics.LINES_KEY, "notfound"));
    assertThat(timemachine.getCells().length, is(2));
    for (TimeMachineCell cell : timemachine.getCells()) {
      assertThat(cell.getValues().length, is(1));
      assertThat(cell.getValues()[0], is(Long.class));
    }

    timemachine = sonar.find(TimeMachineQuery.createForMetrics(PROJECT));
    assertThat(timemachine.getCells().length, is(0));
  }

  @Test
  public void noDataForInterval() {
    Date date = new Date();
    TimeMachine timemachine = sonar.find(TimeMachineQuery.createForMetrics(PROJECT, CoreMetrics.LINES_KEY)
        .setFrom(date)
        .setTo(date));
    assertThat(timemachine.getCells().length, is(0));
  }

  @Test
  public void unknownResource() {
    TimeMachine timemachine = sonar.find(TimeMachineQuery.createForMetrics("notfound:notfound", CoreMetrics.LINES_KEY));
    assertNull(timemachine);
  }
}
