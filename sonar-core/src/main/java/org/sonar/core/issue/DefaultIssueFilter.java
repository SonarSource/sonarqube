/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public class DefaultIssueFilter {

  public static final String SEPARATOR = "|";
  public static final String KEY_VALUE_SEPARATOR = "=";
  public static final String LIST_SEPARATOR = ",";

  private Long id;
  private String name;
  private String user;
  private Boolean shared = false;
  private String description;
  private String data;
  private Date createdAt;
  private Date updatedAt;

  public DefaultIssueFilter() {

  }

  public static DefaultIssueFilter create(String name) {
    DefaultIssueFilter issueFilter = new DefaultIssueFilter();
    Date now = new Date();
    issueFilter.setName(name);
    issueFilter.setCreatedAt(now).setUpdatedAt(now);
    return issueFilter;
  }

  public DefaultIssueFilter(Map<String, Object> mapData) {
    setData(mapData);
  }

  public Long id() {
    return id;
  }

  public DefaultIssueFilter setId(Long id) {
    this.id = id;
    return this;
  }

  public String name() {
    return name;
  }

  public DefaultIssueFilter setName(String name) {
    this.name = name;
    return this;
  }

  public String user() {
    return user;
  }

  public DefaultIssueFilter setUser(String user) {
    this.user = user;
    return this;
  }

  public Boolean shared() {
    return shared;
  }

  public DefaultIssueFilter setShared(Boolean shared) {
    this.shared = shared;
    return this;
  }

  public String description() {
    return description;
  }

  public DefaultIssueFilter setDescription(String description) {
    this.description = description;
    return this;
  }

  public String data() {
    return data;
  }

  public DefaultIssueFilter setData(String data) {
    this.data = data;
    return this;
  }

  public Date createdAt() {
    return createdAt;
  }

  public DefaultIssueFilter setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public DefaultIssueFilter setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public final DefaultIssueFilter setData(Map<String, Object> mapData) {
    this.data = mapAsdata(mapData);
    return this;
  }

  /**
   * Used by ui
   */
  public Map<String, Object> dataAsMap(){
    return dataAsMap(data);
  }

  @VisibleForTesting
  final Map<String, Object> dataAsMap(String data) {
    Map<String, Object> map = newHashMap();

    Iterable<String> keyValues = Splitter.on(DefaultIssueFilter.SEPARATOR).split(data);
    for (String keyValue : keyValues) {
      String[] keyValueSplit = StringUtils.split(keyValue, DefaultIssueFilter.KEY_VALUE_SEPARATOR);
      if (keyValueSplit.length == 2) {
        String key = keyValueSplit[0];
        String value = keyValueSplit[1];
        String[] listValues = StringUtils.split(value, DefaultIssueFilter.LIST_SEPARATOR);
        if (listValues.length > 1) {
          map.put(key, newArrayList(listValues));
        } else {
          map.put(key, value);
        }
      }
    }
    return map;
  }

  @VisibleForTesting
  final String mapAsdata(Map<String, Object> map) {
    StringBuilder stringBuilder = new StringBuilder();

    for (Map.Entry<String, Object> entries : map.entrySet()){
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
          stringBuilder.append(DefaultIssueFilter.KEY_VALUE_SEPARATOR);
          stringBuilder.append(value);
          stringBuilder.append(DefaultIssueFilter.SEPARATOR);
        }
        if (!valuesList.isEmpty()) {
          stringBuilder.append(key);
          stringBuilder.append(DefaultIssueFilter.KEY_VALUE_SEPARATOR);
          for (Iterator<Object> valueListIter = valuesList.iterator(); valueListIter.hasNext();) {
            Object valueList = valueListIter.next();
            stringBuilder.append(valueList);
            if (valueListIter.hasNext()) {
              stringBuilder.append(DefaultIssueFilter.LIST_SEPARATOR);
            }
          }
          stringBuilder.append(DefaultIssueFilter.SEPARATOR);
        }
      }
    }

    if (stringBuilder.length() > 0) {
      // Delete useless last separator character
      stringBuilder.deleteCharAt(stringBuilder.length() - 1);
    }

    return stringBuilder.toString();
  }

}
