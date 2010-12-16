package org.sonar.tests.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
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
  public void timemachine() {
    TimeMachineQuery query = TimeMachineQuery.createForMetrics(PROJECT,
        CoreMetrics.BLOCKER_VIOLATIONS_KEY,
        CoreMetrics.CRITICAL_VIOLATIONS_KEY,
        CoreMetrics.MAJOR_VIOLATIONS_KEY,
        CoreMetrics.MINOR_VIOLATIONS_KEY,
        CoreMetrics.INFO_VIOLATIONS_KEY);
    List<TimeMachineData> snapshots = sonar.findAll(query);
    assertThat(snapshots.size(), is(2));

    TimeMachineData snapshot1 = snapshots.get(0);
    TimeMachineData snapshot2 = snapshots.get(1);

    assertThat(snapshot1.getDate().getMonth(), is(9));
    assertThat(snapshot1.getValues(), is(Arrays.asList("0.0", "0.0", "3.0", "4.0", "0.0")));

    assertThat(snapshot2.getDate().getMonth(), is(10));
    assertThat(snapshot2.getValues(), is(Arrays.asList("0.0", "0.0", "5.0", "4.0", "0.0")));
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
    List<TimeMachineData> snapshots = sonar.findAll(TimeMachineQuery.createForMetrics(PROJECT, "notfound"));
    assertThat(snapshots.size(), is(2));
    for (TimeMachineData data : snapshots) {
      assertThat(data.getValues().get(0), nullValue());
    }

    snapshots = sonar.findAll(TimeMachineQuery.createForMetrics(PROJECT, CoreMetrics.LINES_KEY, "notfound"));
    assertThat(snapshots.size(), is(2));
    for (TimeMachineData data : snapshots) {
      assertThat(data.getValues().get(1), nullValue());
    }

    snapshots = sonar.findAll(TimeMachineQuery.createForMetrics(PROJECT));
    assertThat(snapshots.size(), is(0));
  }

  @Test
  public void noDataForInterval() {
    Date date = new Date();
    List<TimeMachineData> snapshots = sonar.findAll(TimeMachineQuery.createForMetrics(PROJECT, CoreMetrics.LINES_KEY)
        .setFrom(date)
        .setTo(date));
    assertThat(snapshots.size(), is(0));
  }

  @Test
  public void unknownResource() {
    List<TimeMachineData> snapshots = sonar.findAll(TimeMachineQuery.createForMetrics("notfound:notfound", CoreMetrics.LINES_KEY));
    assertThat(snapshots.size(), is(0));
  }
}
