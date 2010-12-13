package org.sonar.wsclient.services;

import org.junit.Test;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class TimeMachineDataTest {

  @Test
  public void valueAsDouble() {
    Map<Date, List<String>> map = new HashMap<Date, List<String>>();
    Date date = new Date();
    map.put(date, Arrays.asList(null, "20.3", "hello"));
    TimeMachineData data = new TimeMachineData().setData(map);

    assertThat(data.getValueAsDouble(date, 0), nullValue());
    assertThat(data.getValueAsDouble(date, 1), is(20.3));
    assertThat(data.getValueAsDouble(date, 2), nullValue());
  }

}
