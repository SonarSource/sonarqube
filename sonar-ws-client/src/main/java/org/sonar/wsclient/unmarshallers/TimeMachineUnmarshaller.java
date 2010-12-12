package org.sonar.wsclient.unmarshallers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.sonar.wsclient.services.TimeMachineData;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeMachineUnmarshaller implements Unmarshaller<TimeMachineData> {

  public TimeMachineData toModel(String json) {
    JSONObject map = (JSONObject) JSONValue.parse(json);
    Map<Date, List<String>> data = new HashMap<Date, List<String>>();
    for (Object key : map.keySet()) {
      JSONArray array = (JSONArray) map.get(key);
      data.put(JsonUtils.parseDateTime((String) key), array);
    }
    return new TimeMachineData().setData(data);
  }

  public List<TimeMachineData> toModels(String json) {
    throw new UnsupportedOperationException();
  }

}
