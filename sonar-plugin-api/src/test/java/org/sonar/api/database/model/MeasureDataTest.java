package org.sonar.api.database.model;

import com.google.common.base.Charsets;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class MeasureDataTest {
  @Test
  public void text_is_utf8() {
    System.out.println(Charsets.UTF_8.name());
    MeasureData data = new MeasureData();
    String s = "accents éà and special characters ç€";
    data.setData(s.getBytes(Charsets.UTF_8));

    assertThat(data.getText()).isEqualTo(s);
  }
}
