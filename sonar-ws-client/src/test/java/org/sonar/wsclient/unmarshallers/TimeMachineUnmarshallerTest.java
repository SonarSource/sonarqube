package org.sonar.wsclient.unmarshallers;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.wsclient.services.TimeMachineData;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TimeMachineUnmarshallerTest {

  @Test
  public void toModel() throws IOException {
    TimeMachineData data = new TimeMachineUnmarshaller().toModel(loadFile("/timemachine/timemachine.json"));

    assertThat(data.getData().size(), is(2));
  }

  private static String loadFile(String path) throws IOException {
    return IOUtils.toString(TimeMachineUnmarshallerTest.class.getResourceAsStream(path));
  }

}
