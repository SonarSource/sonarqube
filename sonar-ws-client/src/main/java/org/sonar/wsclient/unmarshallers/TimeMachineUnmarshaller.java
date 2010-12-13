package org.sonar.wsclient.unmarshallers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sonar.wsclient.services.TimeMachineData;

import java.util.ArrayList;
import java.util.List;

public class TimeMachineUnmarshaller extends AbstractUnmarshaller<TimeMachineData> {

  protected TimeMachineData parse(JSONObject json) {
    String dateTimeStr = (String) json.keySet().iterator().next();
    JSONArray array = (JSONArray) json.get(dateTimeStr);
    List<String> measures = new ArrayList<String>();
    for (int i = 0; i < array.size(); i++) {
      Object elem = array.get(i);
      String value = elem == null ? null : elem.toString();
      measures.add(value);
    }
    return new TimeMachineData()
        .setDate(JsonUtils.parseDateTime(dateTimeStr))
        .setValues(measures);
  }

}
