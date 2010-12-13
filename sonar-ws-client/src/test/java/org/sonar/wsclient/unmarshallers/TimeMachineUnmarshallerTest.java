package org.sonar.wsclient.unmarshallers;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.wsclient.services.TimeMachineData;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class TimeMachineUnmarshallerTest {

  @Test
  public void toModel() throws Exception {
    TimeMachineData data = new TimeMachineUnmarshaller().toModel(loadFile("/timemachine/timemachine.json"));

    Map<Date, List<String>> map = data.getData();
    assertThat(map.size(), is(1));
    Date date = map.keySet().iterator().next();
    final Date expectedDate = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ssZZZZ").parse("2010-12-04T15:59:23+0000");
    assertThat(date, is(expectedDate));
    List<String> values = map.values().iterator().next();
    assertThat(values.size(), is(3));
    assertThat(values.get(0), is("20.0"));
    assertThat(values.get(1), nullValue());
    assertThat(values.get(2), is("12.8"));
  }

  @Test
  public void many() throws Exception {
    TimeMachineData data = new TimeMachineUnmarshaller().toModel(loadFile("/timemachine/many.json"));

    Map<Date, List<String>> map = data.getData();
    assertThat(map.size(), is(3));
  }

  private static String loadFile(String path) throws IOException {
    return IOUtils.toString(TimeMachineUnmarshallerTest.class.getResourceAsStream(path));
  }

}
