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
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentQuery;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.newindex.DefaultIndexSettings;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.TokenUserSession;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.Components.SearchWsResponse;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentQualifiers.SUBVIEW;
import static org.sonar.db.component.ComponentQualifiers.VIEW;
import static org.sonar.db.user.TokenType.PROJECT_ANALYSIS_TOKEN;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.ws.WsParameterBuilder.createQualifiersParameter;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.*;

public class SearchAction implements ComponentsWsAction {
  private static final ImmutableSet<String> VALID_QUALIFIERS = ImmutableSet.<String>builder()
    .add(APP, PROJECT, VIEW, SUBVIEW)
    .build();
  private final UserSession userSession;
  private final ComponentIndex componentIndex;
  private final DbClient dbClient;
  private final ComponentTypes componentTypes;
  private final I18n i18n;

  public SearchAction(ComponentIndex componentIndex, DbClient dbClient, ComponentTypes componentTypes, I18n i18n, UserSession userSession) {
    this.componentIndex = componentIndex;
    this.dbClient = dbClient;
    this.componentTypes = componentTypes;
    this.i18n = i18n;
    this.userSession = userSession;
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

      action
              .createParam(PARAM_ORGANIZATION)
              .setDescription("Organization key")
              .setRequired(false)
              .setInternal(true)
              .setExampleValue("my-org")
              .setSince("6.3");

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
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setQualifiers(request.mandatoryParamAsStrings(PARAM_QUALIFIERS))
      .setQuery(request.param(Param.TEXT_QUERY))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE));
  }

  private SearchWsResponse doHandle(SearchRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = getOrganization(dbSession, request);
      ComponentQuery esQuery = buildEsQuery(organization, request);
      SearchIdResult<String> results = componentIndex.search(esQuery, new SearchOptions().setPage(request.getPage(), request.getPageSize()));

      List<EntityDto> components = dbClient.entityDao().selectByUuids(dbSession, results.getUuids());

      Optional<UserTokenDto> userToken = getUserToken();
      if (userToken.isPresent() && PROJECT_ANALYSIS_TOKEN.name().equals(userToken.get().getType())) {
        String projectUuid = userToken.get().getProjectUuid();
        components = components.stream()
            .filter(c -> c.getAuthUuid().equals(projectUuid))
            .collect(Collectors.toList());
      }

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

  private Optional<UserTokenDto> getUserToken() {
    if (userSession instanceof ThreadLocalUserSession) {
      UserSession tokenUserSession = ((ThreadLocalUserSession) userSession).get();
      if (tokenUserSession instanceof TokenUserSession) {
        return Optional.ofNullable(((TokenUserSession) tokenUserSession).getUserToken());
      }
    }
    return Optional.empty();
  }

  private OrganizationDto getOrganization(DbSession dbSession, SearchRequest request) {
    String organizationKey = Optional.ofNullable(request.getOrganization())
            .orElseGet(dbClient.organizationDao().getDefaultOrganization(dbSession)::getKey);
    return NotFoundException.checkFoundWithOptional(
            dbClient.organizationDao().selectByKey(dbSession, organizationKey),
            "No organizationDto with key '%s'", organizationKey);
  }

  private static ComponentQuery buildEsQuery(OrganizationDto organization, SearchRequest request) {
    return ComponentQuery.builder()
      .setQuery(request.getQuery())
      .setOrganization(organization.getUuid())
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
      .setOrganization(dto.getKey())
      .setKey(dto.getKey())
      .setProject(projectKey)
      .setName(dto.getName())
      .setQualifier(dto.getQualifier());
    return builder.build();
  }

  static class SearchRequest {
    private String organization;
    private List<String> qualifiers;
    private Integer page;
    private Integer pageSize;
    private String query;

    @CheckForNull
    public String getOrganization() {
      return organization;
    }

    public SearchRequest setOrganization(@Nullable String organization) {
      this.organization = organization;
      return this;
    }

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
