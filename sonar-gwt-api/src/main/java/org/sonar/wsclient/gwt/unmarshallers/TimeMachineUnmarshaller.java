package org.sonar.wsclient.gwt.unmarshallers;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.sonar.gwt.JsonUtils;
import org.sonar.wsclient.services.TimeMachine;
import org.sonar.wsclient.services.TimeMachineCell;
import org.sonar.wsclient.services.TimeMachineColumn;

public class TimeMachineUnmarshaller extends AbstractUnmarshaller<TimeMachine> {

  protected TimeMachine parse(JSONObject json) {
    JSONArray cols = json.get("cols").isArray();
    JSONArray cells = json.get("cells").isArray();
    return new TimeMachine(toColumns(cols), toCells(cells));
  }

  private TimeMachineColumn[] toColumns(JSONArray cols) {
    int size = cols.size();
    TimeMachineColumn[] result = new TimeMachineColumn[size];
    for (int index = 0; index < JsonUtils.getArraySize(cols); index++) {
      JSONObject elem = JsonUtils.getArray(cols, index);
      result[index] = new TimeMachineColumn(index, JsonUtils.getString(elem, "metric"), null, null);
    }
    return result;
  }

  private TimeMachineCell[] toCells(JSONArray cells) {
    int size = JsonUtils.getArraySize(cells);
    TimeMachineCell[] result = new TimeMachineCell[size];
    for (int i = 0; i < size; i++) {
      JSONObject cellJson = JsonUtils.getArray(cells, i);
      JSONArray valuesJson = cellJson.get("v").isArray();

      Object[] resultValues = new Object[JsonUtils.getArraySize(valuesJson)];
      for (int indexValue = 0; indexValue < JsonUtils.getArraySize(valuesJson); indexValue++) {
        Object value = valuesJson.get(indexValue);
        resultValues[indexValue] = value;
      }
      result[i] = new TimeMachineCell(JsonUtils.getDate(cellJson, "d"), resultValues);
    }
    return result;
  }

}
