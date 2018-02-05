/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarqube.tests.source;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import org.sonar.wsclient.jsonsimple.JSONArray;
import org.sonar.wsclient.jsonsimple.JSONObject;
import org.sonar.wsclient.jsonsimple.JSONValue;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.GetRequest;

public class SourceScmWS {
  public final Tester tester;

  public SourceScmWS(Tester tester) {
    this.tester = tester;
  }

  public Map<Integer, LineData> getScmData(String fileKey) throws ParseException {
    Map<Integer, LineData> result = new HashMap<>();
    String json = tester.asAnonymous().wsClient().wsConnector().call(new GetRequest("api/sources/scm").setParam("key", fileKey)).content();
    JSONObject obj = (JSONObject) JSONValue.parse(json);
    JSONArray array = (JSONArray) obj.get("scm");
    for (Object anArray : array) {
      JSONArray item = (JSONArray) anArray;
      String datetime = (String) item.get(2);
      result.put(((Long) item.get(0)).intValue(), new LineData((String) item.get(3), datetime, (String) item.get(1)));
    }
    return result;
  }

}
