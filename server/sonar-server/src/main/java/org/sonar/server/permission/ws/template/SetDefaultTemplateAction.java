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

package org.sonar.server.permission.ws.template;

import java.util.Set;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.server.permission.ws.PermissionDependenciesFinder;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.ws.WsTemplateRef;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Ordering.natural;
import static java.lang.String.format;
import static org.sonar.server.permission.DefaultPermissionTemplates.defaultRootQualifierTemplateProperty;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_QUALIFIER;
import static org.sonar.server.permission.ws.WsPermissionParameters.createTemplateParameters;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateQualifier;
import static org.sonar.server.permission.ws.ResourceTypeToQualifier.RESOURCE_TYPE_TO_QUALIFIER;

public class SetDefaultTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionDependenciesFinder finder;
  private final ResourceTypes resourceTypes;
  private final PersistentSettings settings;
  private final UserSession userSession;
  private final I18n i18n;

  public SetDefaultTemplateAction(DbClient dbClient, PermissionDependenciesFinder finder, ResourceTypes resourceTypes, PersistentSettings settings, UserSession userSession,
    I18n i18n) {
    this.dbClient = dbClient;
    this.finder = finder;
    this.resourceTypes = resourceTypes;
    this.settings = settings;
    this.userSession = userSession;
    this.i18n = i18n;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("set_default_template")
      .setDescription("Set a permission template as default.<br />" +
        "It requires administration permissions to access.")
      .setPost(true)
      .setSince("5.2")
      .setHandler(this);

    createTemplateParameters(action);

    action.createParam(PARAM_QUALIFIER)
      .setDescription("Project qualifier. Possible values are:" + buildRootQualifiersDescription())
      .setDefaultValue(Qualifiers.PROJECT)
      .setPossibleValues(getRootQualifiers());
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    checkGlobalAdminUser(userSession);

    String qualifier = wsRequest.mandatoryParam(PARAM_QUALIFIER);

    PermissionTemplateDto template = getTemplate(wsRequest);
    validateQualifier(qualifier, getRootQualifiers());
    setDefaultTemplateUuid(template.getUuid(), qualifier);
    wsResponse.noContent();
  }

  private Set<String> getRootQualifiers() {
    return from(resourceTypes.getRoots())
      .transform(RESOURCE_TYPE_TO_QUALIFIER)
      .toSortedSet(natural());
  }

  private String buildRootQualifiersDescription() {
    StringBuilder description = new StringBuilder();
    description.append("<ul>");
    String qualifierPattern = "<li>%s - %s</li>";
    for (String qualifier : getRootQualifiers()) {
      description.append(format(qualifierPattern, qualifier, i18n(qualifier)));
    }
    description.append("</ul>");

    return description.toString();
  }

  private String i18n(String qualifier) {
    String qualifiersPropertyPrefix = "qualifiers.";
    return i18n.message(userSession.locale(), qualifiersPropertyPrefix + qualifier, "");
  }

  private PermissionTemplateDto getTemplate(Request wsRequest) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return finder.getTemplate(dbSession, WsTemplateRef.fromRequest(wsRequest));
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void setDefaultTemplateUuid(String templateUuid, String qualifier) {
    settings.saveProperty(defaultRootQualifierTemplateProperty(qualifier), templateUuid);
  }
}
