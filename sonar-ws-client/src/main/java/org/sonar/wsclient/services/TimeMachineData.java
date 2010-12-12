package org.sonar.wsclient.services;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeMachineData extends Model {

  private Map<Date, List<String>> data = new HashMap<Date, List<String>>();

  public Map<Date, List<String>> getData() {
    return data;
  }

  public TimeMachineData setData(Map<Date, List<String>> data) {
    this.data = data;
    return this;
  }

}
