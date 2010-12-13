package org.sonar.wsclient.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TimeMachineData extends Model {
  private Date date;

  /**
   * We use strings here in order to support measures with string value.
   */
  private List<String> values = new ArrayList<String>();

  public Date getDate() {
    return date;
  }

  public TimeMachineData setDate(Date date) {
    this.date = date;
    return this;
  }

  public List<String> getValues() {
    return values;
  }

  public TimeMachineData setValues(List<String> values) {
    this.values = values;
    return this;
  }

  public Double getValueAsDouble(int index) {
    String valueStr = values.get(index);
    try {
      return valueStr == null ? null : Double.valueOf(valueStr);
    } catch (NumberFormatException e) {
      return null;
    }
  }

}
