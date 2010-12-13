package org.sonar.wsclient.services;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeMachineData extends Model {

  /**
   * We use strings here in order to support measures with string value.
   */
  private Map<Date, List<String>> data = new HashMap<Date, List<String>>();

  public Map<Date, List<String>> getData() {
    return data;
  }

  public TimeMachineData setData(Map<Date, List<String>> data) {
    this.data = data;
    return this;
  }

  public Double getValueAsDouble(Date date, int index) {
    if (data.containsKey(date)) {
      String valueStr = data.get(date).get(index);
      try {
        return valueStr == null ? null : Double.valueOf(valueStr);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }

}
