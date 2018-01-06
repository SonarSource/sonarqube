/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.batch;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.FilePathWithHashDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.property.PropertyDto;
import org.sonar.scanner.protocol.input.FileData;
import org.sonar.scanner.protocol.input.ProjectRepositories;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.ws.WsUtils.checkRequest;

@ServerSide
public class ProjectDataLoader {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public ProjectDataLoader(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  public ProjectRepositories load(ProjectDataQuery query) {
    try (DbSession session = dbClient.openSession(false)) {
      ProjectRepositories data = new ProjectRepositories();
      String moduleKey = query.getModuleKey();
      String branch = query.getBranch();
      ComponentDto mainModule = componentFinder.getByKey(session, moduleKey);
      checkRequest(isProjectOrModule(mainModule), "Key '%s' belongs to a component which is not a Project", moduleKey);
      boolean hasScanPerm = userSession.hasComponentPermission(SCAN_EXECUTION, mainModule) ||
        userSession.hasPermission(OrganizationPermission.SCAN, mainModule.getOrganizationUuid());
      boolean hasBrowsePerm = userSession.hasComponentPermission(USER, mainModule);
      checkPermission(query.isIssuesMode(), hasScanPerm, hasBrowsePerm);
      ComponentDto branchOrMainModule = branch == null ? mainModule : componentFinder.getByKeyAndBranch(session, moduleKey, branch);

      ComponentDto project = getProject(branchOrMainModule, session);
      if (!project.getKey().equals(branchOrMainModule.getKey())) {
        addSettings(data, branchOrMainModule.getKey(), getSettingsFromParents(branchOrMainModule, hasScanPerm, session));
      }

      List<ComponentDto> modulesTree = dbClient.componentDao().selectEnabledDescendantModules(session, branchOrMainModule.uuid());
      List<PropertyDto> modulesTreeSettings = dbClient.propertiesDao().selectEnabledDescendantModuleProperties(mainModule.uuid(), session);
      TreeModuleSettings treeModuleSettings = new TreeModuleSettings(session, modulesTree, modulesTreeSettings);

      addSettingsToChildrenModules(data, moduleKey, Maps.newHashMap(), treeModuleSettings, hasScanPerm);
      List<FilePathWithHashDto> files = searchFilesWithHashAndRevision(session, branchOrMainModule);
      addFileData(data, modulesTree, files);

      // FIXME need real value but actually only used to know if there is a previous analysis in local issue tracking mode so any value is
      // ok
      data.setLastAnalysisDate(new Date());

      return data;
    }
  }

  private static boolean isProjectOrModule(ComponentDto module) {
    if (!Scopes.PROJECT.equals(module.scope())) {
      return false;
    }
    return Qualifiers.PROJECT.equals(module.qualifier()) || Qualifiers.MODULE.equals(module.qualifier());
  }

  private List<FilePathWithHashDto> searchFilesWithHashAndRevision(DbSession session, @Nullable ComponentDto module) {
    if (module == null) {
      return Collections.emptyList();
    }
    return module.isRootProject() ? dbClient.componentDao().selectEnabledFilesFromProject(session, module.uuid())
      : dbClient.componentDao().selectEnabledDescendantFiles(session, module.uuid());
  }

  private ComponentDto getProject(ComponentDto module, DbSession session) {
    if (!module.isRootProject()) {
      return dbClient.componentDao().selectOrFailByUuid(session, module.projectUuid());
    } else {
      return module;
    }
  }

  private Map<String, String> getSettingsFromParents(ComponentDto module, boolean hasScanPerm, DbSession session) {
    List<ComponentDto> parents = newArrayList();
    aggregateParentModules(module, parents, session);
    Collections.reverse(parents);

    Map<String, String> parentProperties = newHashMap();
    for (ComponentDto parent : parents) {
      parentProperties.putAll(getPropertiesMap(dbClient.propertiesDao().selectProjectProperties(session, parent.getKey()), hasScanPerm));
    }
    return parentProperties;
  }

  private void aggregateParentModules(ComponentDto component, List<ComponentDto> parents, DbSession session) {
    String moduleUuid = component.moduleUuid();
    if (moduleUuid != null) {
      ComponentDto parent = dbClient.componentDao().selectOrFailByUuid(session, moduleUuid);
      if (parent != null) {
        parents.add(parent);
        aggregateParentModules(parent, parents, session);
      }
    }
  }

  private static void addSettingsToChildrenModules(ProjectRepositories ref, String moduleKey, Map<String, String> parentProperties, TreeModuleSettings treeModuleSettings,
    boolean hasScanPerm) {
    Map<String, String> currentParentProperties = newHashMap();
    currentParentProperties.putAll(parentProperties);
    currentParentProperties.putAll(getPropertiesMap(treeModuleSettings.findModuleSettings(moduleKey), hasScanPerm));
    addSettings(ref, moduleKey, currentParentProperties);

    for (ComponentDto childModule : treeModuleSettings.findChildrenModule(moduleKey)) {
      addSettings(ref, childModule.getKey(), currentParentProperties);
      addSettingsToChildrenModules(ref, childModule.getKey(), currentParentProperties, treeModuleSettings, hasScanPerm);
    }
  }

  private static void addSettings(ProjectRepositories ref, String module, Map<String, String> properties) {
    if (!properties.isEmpty()) {
      ref.addSettings(module, properties);
    }
  }

  private static Map<String, String> getPropertiesMap(List<PropertyDto> propertyDtos, boolean hasScanPerm) {
    Map<String, String> properties = newHashMap();
    for (PropertyDto propertyDto : propertyDtos) {
      String key = propertyDto.getKey();
      String value = propertyDto.getValue();
      if (isPropertyAllowed(key, hasScanPerm)) {
        properties.put(key, value);
      }
    }
    return properties;
  }

  private static boolean isPropertyAllowed(String key, boolean hasScanPerm) {
    return !key.contains(".secured") || hasScanPerm;
  }

  private static void addFileData(ProjectRepositories data, List<ComponentDto> moduleChildren, List<FilePathWithHashDto> files) {
    Map<String, String> moduleKeysByUuid = newHashMap();
    for (ComponentDto module : moduleChildren) {
      moduleKeysByUuid.put(module.uuid(), module.getKey());
    }

    for (FilePathWithHashDto file : files) {
      FileData fileData = new FileData(file.getSrcHash(), file.getRevision());
      data.addFileData(moduleKeysByUuid.get(file.getModuleUuid()), file.getPath(), fileData);
    }
  }

  private static void checkPermission(boolean preview, boolean hasScanPerm, boolean hasBrowsePerm) {
    if (!hasBrowsePerm && !hasScanPerm) {
      throw new ForbiddenException(Messages.NO_PERMISSION);
    }
    if (!preview && !hasScanPerm) {
      throw new ForbiddenException("You're only authorized to execute a local (preview) SonarQube analysis without pushing the results to the SonarQube server. " +
        "Please contact your SonarQube administrator.");
    }
    if (preview && !hasBrowsePerm) {
      throw new ForbiddenException("You don't have the required permissions to access this project. Please contact your SonarQube administrator.");
    }
  }

  private class TreeModuleSettings {

    private Map<String, ComponentDto> modulesByKey;
    private Multimap<String, PropertyDto> propertiesByModuleKey;
    private Multimap<String, ComponentDto> moduleChildrenByModuleUuid;

    private TreeModuleSettings(DbSession session, List<ComponentDto> moduleChildren, List<PropertyDto> moduleChildrenSettings) {
      modulesByKey = moduleChildren.stream().collect(uniqueIndex(ComponentDto::getKey));
      moduleChildrenByModuleUuid = ArrayListMultimap.create();

      Set<Long> propertiesByComponentId = moduleChildrenSettings.stream().map(PropertyDto::getResourceId).collect(MoreCollectors.toSet());
      Map<Long, ComponentDto> componentsById = dbClient.componentDao().selectByIds(session, propertiesByComponentId).stream().collect(uniqueIndex(ComponentDto::getId));
      propertiesByModuleKey = moduleChildrenSettings.stream().collect(index(s -> componentsById.get(s.getResourceId()).getKey()));
      moduleChildrenByModuleUuid = moduleChildren.stream().filter(c -> c.moduleUuid() != null).collect(index(ComponentDto::moduleUuid));
    }

    List<PropertyDto> findModuleSettings(String moduleKey) {
      return ImmutableList.copyOf(propertiesByModuleKey.get(moduleKey));
    }

    List<ComponentDto> findChildrenModule(String moduleKey) {
      String moduleUuid = modulesByKey.get(moduleKey).uuid();
      return ImmutableList.copyOf(moduleChildrenByModuleUuid.get(moduleUuid));
    }
  }
}
