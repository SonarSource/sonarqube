/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.issue;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerSide;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

@ServerSide
public class IssueFilterSerializer {

  public static final String SEPARATOR = "|";
  public static final String KEY_VALUE_SEPARATOR = "=";
  public static final String LIST_SEPARATOR = ",";

  public String serialize(Map<String, Object> map) {
    StringBuilder stringBuilder = new StringBuilder();

    for (Map.Entry<String, Object> entries : map.entrySet()) {
      String key = entries.getKey();
      Object value = entries.getValue();
      if (value != null) {
        List valuesList = newArrayList();
        if (value instanceof List) {
          // assume that it contains only strings
          valuesList = (List) value;
        } else if (value instanceof CharSequence) {
          valuesList = Lists.newArrayList(Splitter.on(',').omitEmptyStrings().split((CharSequence) value));
        } else {
          stringBuilder.append(key);
          stringBuilder.append(IssueFilterSerializer.KEY_VALUE_SEPARATOR);
          stringBuilder.append(value);
          stringBuilder.append(IssueFilterSerializer.SEPARATOR);
        }
        if (!valuesList.isEmpty()) {
          stringBuilder.append(key);
          stringBuilder.append(IssueFilterSerializer.KEY_VALUE_SEPARATOR);
          for (Iterator<Object> valueListIter = valuesList.iterator(); valueListIter.hasNext();) {
            Object valueList = valueListIter.next();
            stringBuilder.append(valueList);
            if (valueListIter.hasNext()) {
              stringBuilder.append(IssueFilterSerializer.LIST_SEPARATOR);
            }
          }
          stringBuilder.append(IssueFilterSerializer.SEPARATOR);
        }
      }
    }

    if (stringBuilder.length() > 0) {
      // Delete useless last separator character
      stringBuilder.deleteCharAt(stringBuilder.length() - 1);
    }

    return stringBuilder.toString();
  }

  public Map<String, Object> deserialize(String data) {
    Map<String, Object> map = newHashMap();

    Iterable<String> keyValues = Splitter.on(IssueFilterSerializer.SEPARATOR).split(data);
    for (String keyValue : keyValues) {
      String[] keyValueSplit = StringUtils.split(keyValue, IssueFilterSerializer.KEY_VALUE_SEPARATOR);
      if (keyValueSplit.length == 2) {
        String key = keyValueSplit[0];
        String value = keyValueSplit[1];
        String[] listValues = StringUtils.split(value, IssueFilterSerializer.LIST_SEPARATOR);
        if (listValues.length > 1) {
          map.put(key, newArrayList(listValues));
        } else {
          map.put(key, value);
        }
      }
    }
    return map;
  }

}
