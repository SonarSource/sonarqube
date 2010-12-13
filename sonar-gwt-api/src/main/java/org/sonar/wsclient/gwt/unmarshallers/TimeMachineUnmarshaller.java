package org.sonar.wsclient.gwt.unmarshallers;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.*;
import org.sonar.gwt.JsonUtils;
import org.sonar.wsclient.services.TimeMachineData;

import java.util.*;

public class TimeMachineUnmarshaller implements Unmarshaller<TimeMachineData> {

  public TimeMachineData toModel(JavaScriptObject json) {
    JSONObject map = new JSONObject(json);
    Map<Date, List<String>> data = new HashMap<Date, List<String>>();
    for (String dateTimeStr : map.keySet()) {
      JSONArray array = map.get(dateTimeStr).isArray();
      List<String> values = new ArrayList<String>();
      for (int i = 0; i < JsonUtils.getArraySize(array); i++) {
        JSONValue elem = array.get(i);
        values.add(getAsString(elem));
      }
      data.put(JsonUtils.parseDateTime(dateTimeStr), values);
    }
    return new TimeMachineData().setData(data);
  }

  public String getAsString(JSONValue jsonValue) {
    if (jsonValue == null) {
      return null;
    }
    JSONString jsonString;
    if ((jsonString = jsonValue.isString()) == null) {
      JSONNumber jsonNumber = jsonValue.isNumber();
      return jsonNumber != null ? jsonNumber.toString() : null;
    }
    return jsonString.stringValue();
  }

  public List<TimeMachineData> toModels(JavaScriptObject json) {
    return Arrays.asList(toModel(json));
  }

}
