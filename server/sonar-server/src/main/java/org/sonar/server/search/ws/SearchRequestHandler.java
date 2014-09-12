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
package org.sonar.server.search.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class SearchRequestHandler<QUERY, DOMAIN> implements RequestHandler {

  public static final String PARAM_TEXT_QUERY = "q";
  public static final String PARAM_PAGE = "p";
  public static final String PARAM_PAGE_SIZE = "ps";
  public static final String PARAM_FIELDS = "f";
  public static final String PARAM_SORT = "s";
  public static final String PARAM_ASCENDING = "asc";

  public static final String PARAM_FACETS = "facets";

  private int pageSize;
  private int page;
  private List<String> fields;

  private final String actionName;

  /**
   * The fields to be returned in JSON response. <code>null</code> means that
   * all the fields must be returned.
   */
  @CheckForNull
  public List<String> fields() {
    return fields;
  }

  protected SearchRequestHandler(String actionName) {
    this.actionName = actionName;
  }

  protected abstract Result<DOMAIN> doSearch(QUERY query, QueryContext context);

  protected abstract QUERY doQuery(Request request);

  protected abstract void doResultResponse(Request request, QueryContext context, Result<DOMAIN> result, JsonWriter json);

  protected abstract void doContextResponse(Request request, QueryContext context, Result<DOMAIN> result, JsonWriter json);

  protected abstract void doDefinition(WebService.NewAction action);

  public final void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(this.actionName)
      .setHandler(this);

    action
      .createParam(PARAM_PAGE)
      .setDescription("1-based page number")
      .setExampleValue("42")
      .setDefaultValue("1");

    action
      .createParam(PARAM_PAGE_SIZE)
      .setDescription("Page size. Must be greater than 0.")
      .setExampleValue("20")
      .setDefaultValue(String.valueOf(QueryContext.DEFAULT_LIMIT));

    action.createParam(PARAM_FACETS)
      .setDescription("Compute predefined facets")
      .setBooleanPossibleValues()
      .setDefaultValue("false");

    WebService.NewParam newParam = action
      .createParam(PARAM_FIELDS)
      .setDescription("Comma-separated list of the fields to be returned in response. All the fields are returned by default.");

    // .setPossibleValues(possibleFields);
    // if (possibleFields != null && possibleFields.size() > 1) {
    // Iterator<String> it = possibleFields.iterator();
    // newParam.setExampleValue(String.format("%s,%s", it.next(), it.next()));
    // }

    this.doDefinition(action);
  }

  @Override
  public final void handle(Request request, Response response) throws Exception {
    QueryContext context = getQueryContext(request);
    QUERY query = doQuery(request);
    Result<DOMAIN> result = doSearch(query, context);

    JsonWriter json = response.newJsonWriter().beginObject();
    this.writeStatistics(json, result);
    doResultResponse(request, context, result, json);
    doContextResponse(request, context, result, json);
    if (context.isFacet()) {
      writeFacets(result, json);
    }
    json.endObject().close();
  }

  private final QueryContext getQueryContext(Request request) {
    return new QueryContext().addFieldsToReturn(request.paramAsStrings(PARAM_FIELDS))
      .setFacet(request.mandatoryParamAsBoolean(PARAM_FACETS))
      .setPage(request.mandatoryParamAsInt(PARAM_PAGE),
        request.mandatoryParamAsInt(PARAM_PAGE_SIZE));
  }

  protected void writeStatistics(JsonWriter json, Result searchResult) {
    json.prop("total", searchResult.getTotal());
    json.prop(PARAM_PAGE, page);
    json.prop(PARAM_PAGE_SIZE, pageSize);
  }

  protected void writeFacets(Result<?> results, JsonWriter json) {
    json.name("facets").beginArray();
    for (Map.Entry<String, Collection<FacetValue>> facet : results.getFacets().entrySet()) {
      json.beginObject();
      json.prop("property", facet.getKey());
      json.name("values").beginArray();
      for (FacetValue facetValue : facet.getValue()) {
        json.beginObject();
        json.prop("val", facetValue.getKey());
        json.prop("count", facetValue.getValue());
        json.endObject();
      }
      json.endArray().endObject();
    }
    json.endArray();
  }

}
