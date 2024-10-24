/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.component.ws;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.core.i18n.I18n;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentQuery;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.newindex.DefaultIndexSettings;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.Components.SearchWsResponse;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentQualifiers.SUBVIEW;
import static org.sonar.db.component.ComponentQualifiers.VIEW;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.ws.WsParameterBuilder.createQualifiersParameter;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;

public class SearchAction implements ComponentsWsAction {
  private static final ImmutableSet<String> VALID_QUALIFIERS = ImmutableSet.<String>builder()
    .add(APP, PROJECT, VIEW, SUBVIEW)
    .build();
  private final ComponentIndex componentIndex;
  private final DbClient dbClient;
  private final ComponentTypes componentTypes;
  private final I18n i18n;

  public SearchAction(ComponentIndex componentIndex, DbClient dbClient, ComponentTypes componentTypes, I18n i18n) {
    this.componentIndex = componentIndex;
    this.dbClient = dbClient;
    this.componentTypes = componentTypes;
    this.i18n = i18n;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SEARCH)
      .setSince("6.3")
      .setDescription("Search for components")
      .addPagingParams(100, MAX_PAGE_SIZE)
      .setChangelog(
        new Change("8.4", "Param 'language' has been removed"),
        new Change("8.4", String.format("The use of 'DIR','FIL','UTS' and 'BRC' as values for parameter '%s' is no longer supported", PARAM_QUALIFIERS)),
        new Change("8.0", "Field 'id' from response has been removed"),
        new Change("7.6", String.format("The use of 'BRC' as value for parameter '%s' is deprecated", PARAM_QUALIFIERS)))
      .setResponseExample(getClass().getResource("search-components-example.json"))
      .setHandler(this);

    action.createParam(Param.TEXT_QUERY)
      .setDescription("Limit search to: <ul>" +
        "<li>component names that contain the supplied string</li>" +
        "<li>component keys that are exactly the same as the supplied string</li>" +
        "</ul><br>" +
        "The value length of the param must be between " + DefaultIndexSettings.MINIMUM_NGRAM_LENGTH + " and " +
        DefaultIndexSettings.MAXIMUM_NGRAM_LENGTH + " (inclusive) characters. In case longer value is provided it will be truncated.")
      .setExampleValue("sonar");

    createQualifiersParameter(action, newQualifierParameterContext(i18n, componentTypes), VALID_QUALIFIERS)
      .setRequired(true);
  }

  @Override
  public void handle(org.sonar.api.server.ws.Request wsRequest, Response wsResponse) throws Exception {
    SearchWsResponse searchWsResponse = doHandle(toSearchWsRequest(wsRequest));
    writeProtobuf(searchWsResponse, wsRequest, wsResponse);
  }

  private static SearchRequest toSearchWsRequest(org.sonar.api.server.ws.Request request) {
    return new SearchRequest()
      .setQualifiers(request.mandatoryParamAsStrings(PARAM_QUALIFIERS))
      .setQuery(request.param(Param.TEXT_QUERY))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE));
  }

  private SearchWsResponse doHandle(SearchRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentQuery esQuery = buildEsQuery(request);
      SearchIdResult<String> results = componentIndex.search(esQuery, new SearchOptions().setPage(request.getPage(), request.getPageSize()));

      List<EntityDto> components = dbClient.entityDao().selectByUuids(dbSession, results.getUuids());
      Map<String, String> projectKeysByUuids = searchProjectsKeysByUuids(dbSession, components);

      return buildResponse(components, projectKeysByUuids,
        Paging.forPageIndex(request.getPage()).withPageSize(request.getPageSize()).andTotal((int) results.getTotal()));
    }
  }

  private Map<String, String> searchProjectsKeysByUuids(DbSession dbSession, List<EntityDto> entities) {
    Set<String> projectUuidsToSearch = entities.stream()
      .map(EntityDto::getAuthUuid)
      .collect(Collectors.toSet());
    List<EntityDto> projects = dbClient.entityDao().selectByUuids(dbSession, projectUuidsToSearch);
    return projects.stream().collect(toMap(EntityDto::getUuid, EntityDto::getKey));
  }

  private static ComponentQuery buildEsQuery(SearchRequest request) {
    return ComponentQuery.builder()
      .setQuery(request.getQuery())
      .setQualifiers(request.getQualifiers())
      .build();
  }

  private static SearchWsResponse buildResponse(List<EntityDto> components, Map<String, String> projectKeysByUuids, Paging paging) {
    SearchWsResponse.Builder responseBuilder = SearchWsResponse.newBuilder();
    responseBuilder.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();

    components.stream()
      .map(dto -> dtoToComponent(dto, projectKeysByUuids.get(dto.getAuthUuid())))
      .forEach(responseBuilder::addComponents);

    return responseBuilder.build();
  }

  private static Components.Component dtoToComponent(EntityDto dto, String projectKey) {
    Components.Component.Builder builder = Components.Component.newBuilder()
      .setKey(dto.getKey())
      .setProject(projectKey)
      .setName(dto.getName())
      .setQualifier(dto.getQualifier());
    return builder.build();
  }

  static class SearchRequest {
    private List<String> qualifiers;
    private Integer page;
    private Integer pageSize;
    private String query;

    public List<String> getQualifiers() {
      return qualifiers;
    }

    public SearchRequest setQualifiers(List<String> qualifiers) {
      this.qualifiers = requireNonNull(qualifiers);
      return this;
    }

    @CheckForNull
    public Integer getPage() {
      return page;
    }

    public SearchRequest setPage(int page) {
      this.page = page;
      return this;
    }

    @CheckForNull
    public Integer getPageSize() {
      return pageSize;
    }

    public SearchRequest setPageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    @CheckForNull
    public String getQuery() {
      return query;
    }

    public SearchRequest setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }
  }

}
