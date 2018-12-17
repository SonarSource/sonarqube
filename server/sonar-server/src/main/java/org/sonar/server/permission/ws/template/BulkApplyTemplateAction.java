/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.permission.ws.template;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.parseDateOrDateTime;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.template.WsTemplateRef.newTemplateRef;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_002;
import static org.sonar.server.ws.WsParameterBuilder.createRootQualifiersParameter;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ANALYZED_BEFORE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ON_PROVISIONED_ONLY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class BulkApplyTemplateAction implements PermissionsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionTemplateService permissionTemplateService;
  private final PermissionWsSupport wsSupport;
  private final I18n i18n;
  private final ResourceTypes resourceTypes;

  public BulkApplyTemplateAction(DbClient dbClient, UserSession userSession, PermissionTemplateService permissionTemplateService, PermissionWsSupport wsSupport, I18n i18n,
    ResourceTypes resourceTypes) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.permissionTemplateService = permissionTemplateService;
    this.wsSupport = wsSupport;
    this.i18n = i18n;
    this.resourceTypes = resourceTypes;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("bulk_apply_template")
      .setDescription("Apply a permission template to several projects.<br />" +
        "The template id or name must be provided.<br />" +
        "Requires the following permission: 'Administer System'.")
      .setPost(true)
      .setSince("5.5")
      .setChangelog(new Change("6.7.2", format("Parameter %s accepts maximum %d values", PARAM_PROJECTS, DatabaseUtils.PARTITION_SIZE_FOR_ORACLE)))
      .setHandler(this);

    action.createParam(Param.TEXT_QUERY)
      .setDescription("Limit search to: <ul>" +
        "<li>project names that contain the supplied string</li>" +
        "<li>project keys that are exactly the same as the supplied string</li>" +
        "</ul>")
      .setExampleValue("apac");

    createRootQualifiersParameter(action, newQualifierParameterContext(i18n, resourceTypes))
      .setDefaultValue(Qualifiers.PROJECT)
      .setDeprecatedKey(PARAM_QUALIFIER, "6.6");

    WsParameters.createTemplateParameters(action);

    action
      .createParam(PARAM_PROJECTS)
      .setDescription("Comma-separated list of project keys")
      .setSince("6.6")
      // Limitation of ComponentDao#selectByQuery(), max 1000 values are accepted.
      // Restricting size of HTTP parameter allows to not fail with SQL error
      .setMaxValuesAllowed(DatabaseUtils.PARTITION_SIZE_FOR_ORACLE)
      .setExampleValue(String.join(",", KEY_PROJECT_EXAMPLE_001, KEY_PROJECT_EXAMPLE_002));

    action.createParam(PARAM_VISIBILITY)
      .setDescription("Filter the projects that should be visible to everyone (%s), or only specific user/groups (%s).<br/>" +
        "If no visibility is specified, the default project visibility of the organization will be used.",
        Visibility.PUBLIC.getLabel(), Visibility.PRIVATE.getLabel())
      .setRequired(false)
      .setInternal(true)
      .setSince("6.6")
      .setPossibleValues(Visibility.getLabels());

    action.createParam(PARAM_ANALYZED_BEFORE)
      .setDescription("Filter the projects for which last analysis is older than the given date (exclusive).<br> " +
        "Either a date (server timezone) or datetime can be provided.")
      .setSince("6.6")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");

    action.createParam(PARAM_ON_PROVISIONED_ONLY)
      .setDescription("Filter the projects that are provisioned")
      .setBooleanPossibleValues()
      .setDefaultValue("false")
      .setSince("6.6");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(toBulkApplyTemplateWsRequest(request));
    response.noContent();
  }

  private void doHandle(BulkApplyTemplateRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PermissionTemplateDto template = wsSupport.findTemplate(dbSession, newTemplateRef(
        request.getTemplateId(), request.getOrganization(), request.getTemplateName()));
      checkGlobalAdmin(userSession, template.getOrganizationUuid());

      ComponentQuery componentQuery = buildDbQuery(request);
      List<ComponentDto> projects = dbClient.componentDao().selectByQuery(dbSession, template.getOrganizationUuid(), componentQuery, 0, Integer.MAX_VALUE);

      permissionTemplateService.applyAndCommit(dbSession, template, projects);
    }
  }

  private static BulkApplyTemplateRequest toBulkApplyTemplateWsRequest(Request request) {
    return new BulkApplyTemplateRequest()
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME))
      .setQualifiers(request.mandatoryParamAsStrings(PARAM_QUALIFIERS))
      .setQuery(request.param(Param.TEXT_QUERY))
      .setVisibility(request.param(PARAM_VISIBILITY))
      .setOnProvisionedOnly(request.mandatoryParamAsBoolean(PARAM_ON_PROVISIONED_ONLY))
      .setAnalyzedBefore(request.param(PARAM_ANALYZED_BEFORE))
      .setProjects(request.paramAsStrings(PARAM_PROJECTS));
  }

  private static ComponentQuery buildDbQuery(BulkApplyTemplateRequest request) {
    Collection<String> qualifiers = request.getQualifiers();
    ComponentQuery.Builder query = ComponentQuery.builder()
      .setQualifiers(qualifiers.toArray(new String[qualifiers.size()]));

    ofNullable(request.getQuery()).ifPresent(q -> {
      query.setNameOrKeyQuery(q);
      query.setPartialMatchOnKey(true);
    });
    ofNullable(request.getVisibility()).ifPresent(v -> query.setPrivate(Visibility.isPrivate(v)));
    ofNullable(request.getAnalyzedBefore()).ifPresent(d -> query.setAnalyzedBefore(parseDateOrDateTime(d).getTime()));
    query.setOnProvisionedOnly(request.isOnProvisionedOnly());
    ofNullable(request.getProjects()).ifPresent(keys -> query.setComponentKeys(new HashSet<>(keys)));

    return query.build();
  }

  private static class BulkApplyTemplateRequest {
    private String templateId;
    private String organization;
    private String templateName;
    private String query;
    private Collection<String> qualifiers = singleton(Qualifiers.PROJECT);
    private String visibility;
    private String analyzedBefore;
    private boolean onProvisionedOnly = false;
    private Collection<String> projects;

    @CheckForNull
    public String getTemplateId() {
      return templateId;
    }

    public BulkApplyTemplateRequest setTemplateId(@Nullable String templateId) {
      this.templateId = templateId;
      return this;
    }

    @CheckForNull
    public String getOrganization() {
      return organization;
    }

    public BulkApplyTemplateRequest setOrganization(@Nullable String s) {
      this.organization = s;
      return this;
    }

    @CheckForNull
    public String getTemplateName() {
      return templateName;
    }

    public BulkApplyTemplateRequest setTemplateName(@Nullable String templateName) {
      this.templateName = templateName;
      return this;
    }

    @CheckForNull
    public String getQuery() {
      return query;
    }

    public BulkApplyTemplateRequest setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public Collection<String> getQualifiers() {
      return qualifiers;
    }

    public BulkApplyTemplateRequest setQualifiers(Collection<String> qualifiers) {
      this.qualifiers = requireNonNull(qualifiers);
      return this;
    }

    @CheckForNull
    public String getVisibility() {
      return visibility;
    }

    public BulkApplyTemplateRequest setVisibility(@Nullable String visibility) {
      this.visibility = visibility;
      return this;
    }

    @CheckForNull
    public String getAnalyzedBefore() {
      return analyzedBefore;
    }

    public BulkApplyTemplateRequest setAnalyzedBefore(@Nullable String analyzedBefore) {
      this.analyzedBefore = analyzedBefore;
      return this;
    }

    public boolean isOnProvisionedOnly() {
      return onProvisionedOnly;
    }

    public BulkApplyTemplateRequest setOnProvisionedOnly(boolean onProvisionedOnly) {
      this.onProvisionedOnly = onProvisionedOnly;
      return this;
    }

    @CheckForNull
    public Collection<String> getProjects() {
      return projects;
    }

    public BulkApplyTemplateRequest setProjects(@Nullable Collection<String> projects) {
      this.projects = projects;
      return this;
    }
  }
}
