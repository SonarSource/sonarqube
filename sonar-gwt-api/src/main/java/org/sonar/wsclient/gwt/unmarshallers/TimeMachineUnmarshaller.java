package org.sonar.wsclient.gwt.unmarshallers;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import org.sonar.gwt.JsonUtils;
import org.sonar.wsclient.services.TimeMachineData;

import java.util.ArrayList;
import java.util.List;

public class TimeMachineUnmarshaller extends AbstractUnmarshaller<TimeMachineData> {

  protected TimeMachineData parse(JSONObject json) {
    String dateTimeStr = (String) json.keySet().iterator().next();
    JSONArray array = json.get(dateTimeStr).isArray();
    List<String> measures = new ArrayList<String>();
    for (int i = 0; i < JsonUtils.getArraySize(array); i++) {
      // We can't use JsonUtils.getArray here, because it returns JSONObject instead of JSONValue
      JSONValue elem = array.get(i);
      measures.add(JsonUtils.getAsString(elem));
    }
    return new TimeMachineData()
        .setDate(JsonUtils.parseDateTime(dateTimeStr))
        .setValues(measures);
  }

}
