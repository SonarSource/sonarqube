/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.gwt.unmarshallers;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import org.sonar.wsclient.services.Source;

public class SourceUnmarshaller extends AbstractUnmarshaller<Source> {

  protected Source parse(JSONObject json) {
    Source source = new Source();

    for (String key : json.keySet()) {
      JSONValue val = json.get(key);
      if (val.isString() != null) {
        source.addLine(Integer.parseInt(key), val.isString().stringValue());
      } else {
        source.addLine(Integer.parseInt(key), "");
      }
    }

    return source;
  }
}
