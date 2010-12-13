package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.TimeMachineData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class TimeMachineUnmarshallerTest {

  @Test
  public void toModel() throws Exception {
    TimeMachineData data = new TimeMachineUnmarshaller().toModel(WSTestUtils.loadFile("/timemachine/timemachine.json"));

    Date date = data.getDate();
    final Date expectedDate = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ssZZZZ").parse("2010-12-04T15:59:23+0000");
    assertThat(date, is(expectedDate));
    List<String> values = data.getValues();
    assertThat(values.size(), is(3));
    assertThat(values.get(0), is("20.0"));
    assertThat(values.get(1), nullValue());
    assertThat(values.get(2), is("12.8"));
  }

  @Test
  public void many() throws Exception {
    List<TimeMachineData> data = new TimeMachineUnmarshaller().toModels(WSTestUtils.loadFile("/timemachine/many.json"));

    assertThat(data.size(), is(3));
  }

}
