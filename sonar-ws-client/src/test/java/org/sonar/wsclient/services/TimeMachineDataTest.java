package org.sonar.wsclient.services;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class TimeMachineDataTest {

  @Test
  public void valueAsDouble() {
    TimeMachineData data = new TimeMachineData().setValues(Arrays.asList(null, "20.3", "hello"));

    assertThat(data.getValueAsDouble(0), nullValue());
    assertThat(data.getValueAsDouble(1), is(20.3));
    assertThat(data.getValueAsDouble(2), nullValue());
  }

}
