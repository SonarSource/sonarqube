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
package org.sonar.server.ui.ws;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.page.Page;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.ProjectAlmBindingDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.BillingValidations;
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.project.Visibility;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Collections.emptySortedSet;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES_KEY;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;

public class ComponentAction implements NavigationWsAction {

  private static final Set<String> MODULE_OR_DIR_QUALIFIERS = ImmutableSet.of(Qualifiers.MODULE, Qualifiers.DIRECTORY);
  static final String PARAM_COMPONENT = "component";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_PULL_REQUEST = "pullRequest";

  private static final String PROPERTY_CONFIGURABLE = "configurable";
  private static final String PROPERTY_HAS_ROLE_POLICY = "hasRolePolicy";
  private static final String PROPERTY_MODIFIABLE_HISTORY = "modifiable_history";
  private static final String PROPERTY_UPDATABLE_KEY = "updatable_key";
  /**
   * The concept of "visibility" will only be configured for these qualifiers.
   */
  private static final Set<String> QUALIFIERS_WITH_VISIBILITY = ImmutableSet.of(Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.APP);

  private final DbClient dbClient;
  private final PageRepository pageRepository;
  private final ResourceTypes resourceTypes;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final QualityGateFinder qualityGateFinder;
  private final BillingValidationsProxy billingValidations;

  public ComponentAction(DbClient dbClient, PageRepository pageRepository, ResourceTypes resourceTypes, UserSession userSession,
    ComponentFinder componentFinder, QualityGateFinder qualityGateFinder, BillingValidationsProxy billingValidations) {
    this.dbClient = dbClient;
    this.pageRepository = pageRepository;
    this.resourceTypes = resourceTypes;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.qualityGateFinder = qualityGateFinder;
    this.billingValidations = billingValidations;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("component")
      .setDescription("Get information concerning component navigation for the current user. " +
        "Requires the 'Browse' permission on the component's project.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("component-example.json"))
      .setSince("5.2")
      .setChangelog(
        new Change("7.6", String.format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)),
        new Change("7.3", "The 'almRepoUrl' and 'almId' fields are added"),
        new Change("6.4", "The 'visibility' field is added"));

    action.createParam(PARAM_COMPONENT)
      .setDescription("A component key.")
      .setDeprecatedKey("componentKey", "6.4")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action
      .createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);

    action
      .createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String componentKey = request.mandatoryParam(PARAM_COMPONENT);
    try (DbSession session = dbClient.openSession(false)) {
      String branch = request.param(PARAM_BRANCH);
      String pullRequest = request.param(PARAM_PULL_REQUEST);
      ComponentDto component = componentFinder.getByKeyAndOptionalBranchOrPullRequest(session, componentKey, branch, pullRequest);
      checkComponentNotAModuleAndNotADirectory(component);
      ComponentDto rootProjectOrBranch = getRootProjectOrBranch(component, session);
      ComponentDto rootProject = rootProjectOrBranch.getMainBranchProjectUuid() == null ? rootProjectOrBranch
        : componentFinder.getByUuid(session, rootProjectOrBranch.getMainBranchProjectUuid());
      if (!userSession.hasComponentPermission(USER, component) &&
        !userSession.hasComponentPermission(ADMIN, component) &&
        !userSession.isSystemAdministrator()) {
        throw insufficientPrivilegesException();
      }
      OrganizationDto org = componentFinder.getOrganization(session, component);
      Optional<SnapshotDto> analysis = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(session, component.projectUuid());

      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      writeComponent(json, session, component, org, analysis.orElse(null));
      writeAlmDetails(json, session, rootProject);
      writeProfiles(json, session, component);
      writeQualityGate(json, session, org, rootProject);
      if (userSession.hasComponentPermission(ADMIN, component) ||
        userSession.hasPermission(ADMINISTER_QUALITY_PROFILES, org) ||
        userSession.hasPermission(ADMINISTER_QUALITY_GATES, org)) {
        writeConfiguration(json, component, org);
      }
      writeBreadCrumbs(json, session, component);
      json.endObject().close();
    }
  }

  private static void checkComponentNotAModuleAndNotADirectory(ComponentDto component) {
    BadRequestException.checkRequest(!MODULE_OR_DIR_QUALIFIERS.contains(component.qualifier()), "Operation not supported for module or directory components");
  }

  private ComponentDto getRootProjectOrBranch(ComponentDto component, DbSession session) {
    if (!component.isRootProject()) {
      return dbClient.componentDao().selectOrFailByUuid(session, component.projectUuid());
    } else {
      return component;
    }
  }

  private void writeAlmDetails(JsonWriter json, DbSession session, ComponentDto component) {
    Optional<ProjectAlmBindingDto> bindingOpt = dbClient.projectAlmBindingsDao().selectByProjectUuid(session, component.uuid());
    bindingOpt.ifPresent(b -> {
      String almId = String.valueOf(b.getAlm().getId());
      json.name("alm").beginObject()
        .prop("key", almId)
        .prop("url", b.getUrl())
        .endObject();
    });
  }

  private static void writeToJson(JsonWriter json, QualityProfile profile, boolean deleted) {
    json.beginObject()
      .prop("key", profile.getQpKey())
      .prop("name", profile.getQpName())
      .prop("language", profile.getLanguageKey())
      .prop("deleted", deleted)
      .endObject();
  }

  private static void writePage(JsonWriter json, Page page) {
    json.beginObject()
      .prop("key", page.getKey())
      .prop("name", page.getName())
      .endObject();
  }

  private void writeComponent(JsonWriter json, DbSession session, ComponentDto component, OrganizationDto organizationDto, @Nullable SnapshotDto analysis) {
    json.prop("key", component.getKey())
      .prop("organization", organizationDto.getKey())
      .prop("id", component.uuid())
      .prop("name", component.name())
      .prop("description", component.description())
      .prop("isFavorite", isFavourite(session, component));
    String branch = component.getBranch();
    if (branch != null) {
      json.prop("branch", branch);
    }
    if (QUALIFIERS_WITH_VISIBILITY.contains(component.qualifier())) {
      json.prop("visibility", Visibility.getLabel(component.isPrivate()));
    }
    List<Page> pages = pageRepository.getComponentPages(false, component.qualifier());
    writeExtensions(json, component, pages);
    if (analysis != null) {
      json.prop("version", analysis.getProjectVersion())
        .prop("analysisDate", formatDateTime(new Date(analysis.getCreatedAt())));
    }
  }

  private boolean isFavourite(DbSession session, ComponentDto component) {
    PropertyQuery propertyQuery = PropertyQuery.builder()
      .setUserId(userSession.getUserId())
      .setKey("favourite")
      .setComponentId(component.getId())
      .build();
    List<PropertyDto> componentFavourites = dbClient.propertiesDao().selectByQuery(propertyQuery, session);
    return componentFavourites.size() == 1;
  }

  private void writeProfiles(JsonWriter json, DbSession dbSession, ComponentDto component) {
    Set<QualityProfile> qualityProfiles = dbClient.liveMeasureDao().selectMeasure(dbSession, component.projectUuid(), QUALITY_PROFILES_KEY)
      .map(LiveMeasureDto::getDataAsString)
      .map(data -> QPMeasureData.fromJson(data).getProfiles())
      .orElse(emptySortedSet());
    Map<String, QProfileDto> dtoByQPKey = dbClient.qualityProfileDao().selectByUuids(dbSession, qualityProfiles.stream().map(QualityProfile::getQpKey).collect(Collectors.toList()))
      .stream()
      .collect(uniqueIndex(QProfileDto::getKee));
    json.name("qualityProfiles").beginArray();
    qualityProfiles.forEach(qp -> writeToJson(json, qp, !dtoByQPKey.containsKey(qp.getQpKey())));
    json.endArray();
  }

  private void writeQualityGate(JsonWriter json, DbSession session, OrganizationDto organization, ComponentDto component) {
    QualityGateFinder.QualityGateData qualityGateData = qualityGateFinder.getQualityGate(session, organization, component)
      .orElseThrow(() -> new NotFoundException(format("Quality Gate not found for %s", component.getKey())));
    QualityGateDto qualityGateDto = qualityGateData.getQualityGate();
    json.name("qualityGate").beginObject()
      .prop("key", qualityGateDto.getId())
      .prop("name", qualityGateDto.getName())
      .prop("isDefault", qualityGateData.isDefault())
      .endObject();
  }

  private void writeExtensions(JsonWriter json, ComponentDto component, List<Page> pages) {
    json.name("extensions").beginArray();
    Predicate<Page> isAuthorized = page -> {
      String requiredPermission = page.isAdmin() ? UserRole.ADMIN : UserRole.USER;
      return userSession.hasComponentPermission(requiredPermission, component);
    };
    pages.stream()
      .filter(isAuthorized)
      .forEach(page -> writePage(json, page));
    json.endArray();
  }

  private void writeConfiguration(JsonWriter json, ComponentDto component, OrganizationDto organization) {
    boolean isProjectAdmin = userSession.hasComponentPermission(ADMIN, component);

    json.name("configuration").beginObject();
    writeConfigPageAccess(json, isProjectAdmin, component, organization);

    if (isProjectAdmin) {
      json.name("extensions").beginArray();
      List<Page> configPages = pageRepository.getComponentPages(true, component.qualifier());
      configPages.forEach(page -> writePage(json, page));
      json.endArray();
    }
    json.endObject();
  }

  private void writeConfigPageAccess(JsonWriter json, boolean isProjectAdmin, ComponentDto component, OrganizationDto organization) {
    boolean isProject = Qualifiers.PROJECT.equals(component.qualifier());
    boolean showManualMeasures = isProjectAdmin && !Qualifiers.DIRECTORY.equals(component.qualifier());
    boolean showBackgroundTasks = isProjectAdmin && (isProject || Qualifiers.VIEW.equals(component.qualifier()) || Qualifiers.APP.equals(component.qualifier()));
    boolean isQualityProfileAdmin = userSession.hasPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, component.getOrganizationUuid());
    boolean isQualityGateAdmin = userSession.hasPermission(OrganizationPermission.ADMINISTER_QUALITY_GATES, component.getOrganizationUuid());
    boolean isOrganizationAdmin = userSession.hasPermission(OrganizationPermission.ADMINISTER, component.getOrganizationUuid());

    json.prop("showSettings", isProjectAdmin && componentTypeHasProperty(component, PROPERTY_CONFIGURABLE));
    json.prop("showQualityProfiles", isProject && (isProjectAdmin || isQualityProfileAdmin));
    json.prop("showQualityGates", isProject && (isProjectAdmin || isQualityGateAdmin));
    json.prop("showManualMeasures", showManualMeasures);
    json.prop("showLinks", isProjectAdmin && isProject);
    json.prop("showPermissions", isProjectAdmin && componentTypeHasProperty(component, PROPERTY_HAS_ROLE_POLICY));
    json.prop("showHistory", isProjectAdmin && componentTypeHasProperty(component, PROPERTY_MODIFIABLE_HISTORY));
    json.prop("showUpdateKey", isProjectAdmin && componentTypeHasProperty(component, PROPERTY_UPDATABLE_KEY));
    json.prop("showBackgroundTasks", showBackgroundTasks);
    json.prop("canApplyPermissionTemplate", isOrganizationAdmin);
    json.prop("canUpdateProjectVisibilityToPrivate", isProjectAdmin &&
      billingValidations.canUpdateProjectVisibilityToPrivate(new BillingValidations.Organization(organization.getKey(), organization.getUuid(), organization.getName())));
  }

  private boolean componentTypeHasProperty(ComponentDto component, String resourceTypeProperty) {
    ResourceType resourceType = resourceTypes.get(component.qualifier());
    return resourceType != null && resourceType.getBooleanProperty(resourceTypeProperty);
  }

  private void writeBreadCrumbs(JsonWriter json, DbSession session, ComponentDto component) {
    json.name("breadcrumbs").beginArray();

    List<ComponentDto> breadcrumb = Lists.newArrayList();
    breadcrumb.addAll(dbClient.componentDao().selectAncestors(session, component));
    breadcrumb.add(component);

    for (ComponentDto c : breadcrumb) {
      json.beginObject()
        .prop("key", c.getKey())
        .prop("name", c.name())
        .prop("qualifier", c.qualifier())
        .endObject();
    }

    json.endArray();
  }
}
