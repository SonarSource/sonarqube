package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.Metric;
import org.sonar.wsclient.services.Model;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UnmarshallersTest {

  @Test
  public void forModel() {
    assertThat(Unmarshallers.forModel(Metric.class), is(MetricUnmarshaller.class));
    assertThat(Unmarshallers.forModel(Model.class), nullValue());
  }

}
