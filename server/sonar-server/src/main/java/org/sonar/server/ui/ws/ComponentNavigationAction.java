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
package org.sonar.server.ui.ws;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.dashboard.ActiveDashboardDao;
import org.sonar.core.dashboard.DashboardDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.properties.PropertyQuery;
import org.sonar.server.db.DbClient;
import org.sonar.server.ui.ViewProxy;
import org.sonar.server.ui.Views;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ComponentNavigationAction implements NavigationAction {

  private static final String PARAM_COMPONENT_KEY = "componentKey";

  private static final String ANONYMOUS = null;

  private static final String PROPERTY_COMPARABLE = "comparable";
  private static final String PROPERTY_CONFIGURABLE = "configurable";
  private static final String PROPERTY_HAS_ROLE_POLICY = "hasRolePolicy";
  private static final String PROPERTY_MODIFIABLE_HISTORY = "modifiable_history";
  private static final String PROPERTY_UPDATABLE_KEY = "updatable_key";
  private static final String PROPERTY_DELETABLE = "deletable";

  private final DbClient dbClient;
  private final ActiveDashboardDao activeDashboardDao;
  private final Views views;
  private final I18n i18n;
  private final ResourceTypes resourceTypes;

  public ComponentNavigationAction(DbClient dbClient, ActiveDashboardDao activeDashboardDao, Views views, I18n i18n, ResourceTypes resourceTypes) {
    this.dbClient = dbClient;
    this.activeDashboardDao = activeDashboardDao;
    this.views = views;
    this.i18n = i18n;
    this.resourceTypes = resourceTypes;
  }

  @Override
  public void define(NewController context) {
    NewAction projectNavigation = context.createAction("component")
      .setDescription("Get information concerning component navigation for the current user. " +
        "Requires the 'Browse' permission on the component's project.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("example-component.json"))
      .setSince("5.2");

    projectNavigation.createParam(PARAM_COMPONENT_KEY)
      .setDescription("A component key.")
      .setExampleValue("org.codehaus.sonar:sonar")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String componentKey = request.mandatoryParam(PARAM_COMPONENT_KEY);

    UserSession userSession = UserSession.get();
    DbSession session = dbClient.openSession(false);

    try {
      ComponentDto component = dbClient.componentDao().getByKey(session, componentKey);

      userSession.checkProjectUuidPermission(UserRole.USER, component.projectUuid());

      SnapshotDto snapshot = dbClient.snapshotDao().getLastSnapshot(session, new SnapshotDto().setResourceId(component.getId()));

      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      writeComponent(json, session, component, snapshot, userSession);

      if (userSession.hasProjectPermissionByUuid(UserRole.ADMIN, component.projectUuid()) || userSession.hasGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN)) {
        writeConfiguration(json, component, userSession);
      }

      writeBreadCrumbs(json, session, component, snapshot);
      json.endObject().close();

    } finally {
      session.close();
    }
  }

  private void writeComponent(JsonWriter json, DbSession session, ComponentDto component, @Nullable SnapshotDto snapshot, UserSession userSession) {

    json.prop("key", component.key())
      .prop("uuid", component.uuid())
      .prop("name", component.name())
      .prop("isComparable", componentTypeHasProperty(component, PROPERTY_COMPARABLE))
      .prop("canBeFavorite", userSession.isLoggedIn())
      .prop("isFavorite", isFavourite(session, component, userSession));

    List<DashboardDto> dashboards = activeDashboardDao.selectProjectDashboardsForUserLogin(session, userSession.login());
    if (dashboards.isEmpty()) {
      dashboards = activeDashboardDao.selectProjectDashboardsForUserLogin(session, ANONYMOUS);
    }
    writeDashboards(json, component, dashboards, userSession.locale());

    if (snapshot != null) {
      json.prop("version", snapshot.getVersion())
        .prop("date", DateUtils.formatDateTime(new Date(snapshot.getCreatedAt())));
      String[] availableMeasures = dbClient.measureDao().selectMetricKeysForSnapshot(session, snapshot.getId()).toArray(new String[0]);
      List<ViewProxy<Page>> pages = views.getPages(NavigationSection.RESOURCE, component.scope(), component.qualifier(), component.language(), availableMeasures);
      writeExtensions(json, component, pages, userSession.locale());
    }
  }

  private boolean isFavourite(DbSession session, ComponentDto component, UserSession userSession) {
    PropertyQuery propertyQuery = PropertyQuery.builder()
      .setUserId(userSession.userId())
      .setKey("favourite")
      .setComponentId(component.getId())
      .build();
    List<PropertyDto> componentFavourites = dbClient.propertiesDao().selectByQuery(propertyQuery, session);
    return componentFavourites.size() == 1;
  }

  private void writeExtensions(JsonWriter json, ComponentDto component, List<ViewProxy<Page>> pages, Locale locale) {
    json.name("extensions").beginArray();
    for (ViewProxy<Page> page: pages) {
      writePage(json, getPageUrl(page, component), i18n.message(locale, page.getId() + ".page", page.getTitle()));
    }
    json.endArray();
  }

  private String getPageUrl(ViewProxy<Page> page, ComponentDto component) {
    String result = null;
    String componentKey = encodeComponentKey(component);
    if (page.isController()) {
      result = String.format("%s?id=%s", page.getId(), componentKey);
    } else {
      result = String.format("/plugins/resource/%s?page=%s", componentKey, page.getId());
    }
    return result;
  }

  private static String encodeComponentKey(ComponentDto component) {
    String componentKey = component.getKey();
    try {
      componentKey = URLEncoder.encode(componentKey, Charsets.UTF_8.name());
    } catch (UnsupportedEncodingException unknownEncoding) {
      throw new IllegalStateException(unknownEncoding);
    }
    return componentKey;
  }

  private void writeDashboards(JsonWriter json, ComponentDto component, List<DashboardDto> dashboards, Locale locale) {
    json.name("dashboards").beginArray();
    for (DashboardDto dashboard : dashboards) {
      json.beginObject()
        .prop("key", dashboard.getId())
        .prop("name", i18n.message(locale, String.format("dashboard.%s.name", dashboard.getName()), dashboard.getName()))
        .endObject();
    }
    json.endArray();
  }

  private void writeConfiguration(JsonWriter json, ComponentDto component, UserSession userSession) {
    boolean isAdmin = userSession.hasProjectPermissionByUuid(UserRole.ADMIN, component.projectUuid());
    Locale locale = userSession.locale();

    json.name("configuration").beginObject();
    writeConfigPageAccess(json, isAdmin, component, userSession);

    if (isAdmin) {
      json.name("extensions").beginArray();
      List<ViewProxy<Page>> configPages = views.getPages(NavigationSection.RESOURCE_CONFIGURATION, component.scope(), component.qualifier(), component.language(), null);
      for (ViewProxy<Page> page : configPages) {
        writePage(json, getPageUrl(page, component), i18n.message(locale, page.getId() + ".page", page.getTitle()));
      }
      json.endArray();
    }
    json.endObject();
  }

  private void writeConfigPageAccess(JsonWriter json, boolean isAdmin, ComponentDto component, UserSession userSession) {
    boolean isProject = Qualifiers.PROJECT.equals(component.qualifier());

    json.prop("showSettings", isAdmin && componentTypeHasProperty(component, PROPERTY_CONFIGURABLE));
    json.prop("showQualityProfiles", isProject);
    json.prop("showQualityGates", isProject);
    json.prop("showManualMeasures", isAdmin);
    json.prop("showActionPlans", isAdmin && isProject);
    json.prop("showLinks", isAdmin && isProject);
    json.prop("showPermissions", isAdmin && componentTypeHasProperty(component, PROPERTY_HAS_ROLE_POLICY));
    json.prop("showHistory", isAdmin && componentTypeHasProperty(component, PROPERTY_MODIFIABLE_HISTORY));
    json.prop("showUpdateKey", isAdmin && componentTypeHasProperty(component, PROPERTY_UPDATABLE_KEY));
    json.prop("showDeletion", isAdmin && componentTypeHasProperty(component, PROPERTY_DELETABLE));
  }

  private boolean componentTypeHasProperty(ComponentDto component, String resourceTypeProperty) {
    ResourceType resourceType = resourceTypes.get(component.qualifier());
    if (resourceType != null) {
      return resourceType.getBooleanProperty(resourceTypeProperty);
    }
    return false;
  }

  private void writePage(JsonWriter json, String url, String name) {
    json.beginObject()
      .prop("url", url)
      .prop("name", name)
      .endObject();
  }

  private void writeBreadCrumbs(JsonWriter json, DbSession session, ComponentDto component, @Nullable SnapshotDto snapshot) {
    json.name("breadcrumbs").beginArray();

    List<ComponentDto> componentPath = Lists.newArrayList(component);

    if (snapshot != null) {
      SnapshotDto currentSnapshot = snapshot;
      while (currentSnapshot.getParentId() != null) {
        currentSnapshot = dbClient.snapshotDao().getByKey(session, currentSnapshot.getParentId());
        componentPath.add(0, dbClient.componentDao().getById(currentSnapshot.getResourceId(), session));
      }
    }

    for (ComponentDto crumbComponent : componentPath) {
      json.beginObject()
        .prop("key", crumbComponent.key())
        .prop("name", crumbComponent.name())
        .prop("qualifier", crumbComponent.qualifier())
        .endObject();
    }

    json.endArray();
  }
}
