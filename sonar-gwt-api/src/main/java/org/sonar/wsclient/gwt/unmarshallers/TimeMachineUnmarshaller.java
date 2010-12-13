package org.sonar.wsclient.gwt.unmarshallers;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import org.sonar.gwt.JsonUtils;
import org.sonar.wsclient.services.TimeMachineData;

import java.util.*;

public class TimeMachineUnmarshaller implements Unmarshaller<TimeMachineData> {

  public TimeMachineData toModel(JavaScriptObject json) {
    JSONObject map = new JSONObject(json);
    Map<Date, List<String>> data = new HashMap<Date, List<String>>();
    for (String dateTimeStr : map.keySet()) {
      JSONArray array = map.get(dateTimeStr).isArray();
      List<String> measures = new ArrayList<String>();
      for (int i = 0; i < JsonUtils.getArraySize(array); i++) {
        // We can't use JsonUtils.getArray here, because it returns JSONObject instead of JSONValue
        JSONValue elem = array.get(i);
        measures.add(JsonUtils.getAsString(elem));
      }
      data.put(JsonUtils.parseDateTime(dateTimeStr), measures);
    }
    return new TimeMachineData().setData(data);
  }

  public List<TimeMachineData> toModels(JavaScriptObject json) {
    return Arrays.asList(toModel(json));
  }

}
