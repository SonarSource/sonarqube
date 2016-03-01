/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.search.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Generic options for search web services
 *
 */
public class SearchOptions {

  private int pageSize;
  private int page;
  private List<String> fields;

  public static SearchOptions create(Request request) {
    SearchOptions options = new SearchOptions();

    // both parameters have default values
    options.setPage(request.mandatoryParamAsInt(WebService.Param.PAGE));
    options.setPageSize(request.mandatoryParamAsInt(WebService.Param.PAGE_SIZE));

    // optional field
    options.setFields(request.paramAsStrings(WebService.Param.FIELDS));

    return options;
  }

  public int pageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  /**
   * 1-based page id
   */
  public int page() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  /**
   * The fields to be returned in JSON response. <code>null</code> means that
   * all the fields must be returned.
   */
  @CheckForNull
  public List<String> fields() {
    return fields;
  }

  public SearchOptions setFields(@Nullable List<String> fields) {
    this.fields = fields;
    return this;
  }

  public boolean hasField(String key) {
    return fields == null || fields.contains(key);
  }

  public SearchOptions writeStatistics(JsonWriter json, Result searchResult) {
    json.prop("total", searchResult.getTotal());
    json.prop(WebService.Param.PAGE, page);
    json.prop(WebService.Param.PAGE_SIZE, pageSize);
    return this;
  }

  public static void defineFieldsParam(WebService.NewAction action, @Nullable Collection<String> possibleFields) {
    WebService.NewParam newParam = action
      .createParam(WebService.Param.FIELDS)
      .setDescription("Comma-separated list of the fields to be returned in response. All the fields are returned by default.")
      .setPossibleValues(possibleFields);
    if (possibleFields != null && possibleFields.size() > 1) {
      Iterator<String> it = possibleFields.iterator();
      newParam.setExampleValue(String.format("%s,%s", it.next(), it.next()));
    }
  }

  public static void definePageParams(WebService.NewAction action) {
    action
      .createParam(WebService.Param.PAGE)
      .setDescription("1-based page number")
      .setExampleValue("42")
      .setDefaultValue("1");

    action
      .createParam(WebService.Param.PAGE_SIZE)
      .setDescription("Page size. Must be greater than 0.")
      .setExampleValue("20")
      .setDefaultValue(String.valueOf(QueryContext.DEFAULT_LIMIT));
  }
}
