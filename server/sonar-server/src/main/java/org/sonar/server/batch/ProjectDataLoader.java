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

package org.sonar.server.batch;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.web.UserRole;
import org.sonar.batch.protocol.input.FileData;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.FilePathWithHashDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static org.sonar.server.ws.WsUtils.checkFound;

@ServerSide
public class ProjectDataLoader {

  private static final Logger LOG = Loggers.get(ProjectDataLoader.class);

  private final DbClient dbClient;
  private final UserSession userSession;

  public ProjectDataLoader(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  public ProjectRepositories load(ProjectDataQuery query) {
    boolean hasScanPerm = userSession.hasGlobalPermission(GlobalPermissions.SCAN_EXECUTION);
    checkPermission(query.isIssuesMode());

    DbSession session = dbClient.openSession(false);
    try {
      ProjectRepositories data = new ProjectRepositories();
      ComponentDto module = checkFound(dbClient.componentDao().selectByKey(session, query.getModuleKey()),
        format("Project or module with key '%s' is not found", query.getModuleKey()));

      // Scan permission is enough to analyze all projects but preview permission is limited to projects user can access
      if (query.isIssuesMode() && !userSession.hasProjectPermissionByUuid(UserRole.USER, module.projectUuid())) {
        throw new ForbiddenException("You're not authorized to access to project '" + module.name() + "', please contact your SonarQube administrator.");
      }

      ComponentDto project = getProject(module, session);
      if (!project.key().equals(module.key())) {
        addSettings(data, module.getKey(), getSettingsFromParents(module, hasScanPerm, session));
      }

      List<ComponentDto> modulesTree = dbClient.componentDao().selectEnabledDescendantModules(session, module.uuid());
      Map<String, String> moduleUuidsByKey = moduleUuidsByKey(modulesTree);
      Map<String, Long> moduleIdsByKey = moduleIdsByKey(modulesTree);

      List<PropertyDto> modulesTreeSettings = dbClient.propertiesDao().selectEnabledDescendantModuleProperties(module.uuid(), session);
      TreeModuleSettings treeModuleSettings = new TreeModuleSettings(moduleUuidsByKey, moduleIdsByKey, modulesTree, modulesTreeSettings);

      addSettingsToChildrenModules(data, query.getModuleKey(), Maps.<String, String>newHashMap(), treeModuleSettings, hasScanPerm);
      List<FilePathWithHashDto> files = searchFilesWithHashAndRevision(session, module);
      addFileData(data, modulesTree, files);

      // FIXME need real value but actually only used to know if there is a previous analysis in local issue tracking mode so any value is
      // ok
      data.setLastAnalysisDate(new Date());

      return data;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private List<FilePathWithHashDto> searchFilesWithHashAndRevision(DbSession session, ComponentDto module) {
    return module.isRootProject() ?
      dbClient.componentDao().selectEnabledFilesFromProject(session, module.uuid())
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
      parentProperties.putAll(getPropertiesMap(dbClient.propertiesDao().selectProjectProperties(session, parent.key()), hasScanPerm));
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

  private void addSettingsToChildrenModules(ProjectRepositories ref, String moduleKey, Map<String, String> parentProperties, TreeModuleSettings treeModuleSettings,
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
      moduleKeysByUuid.put(module.uuid(), module.key());
    }

    for (FilePathWithHashDto file : files) {
      FileData fileData = new FileData(file.getSrcHash(), file.getRevision());
      data.addFileData(moduleKeysByUuid.get(file.getModuleUuid()), file.getPath(), fileData);
    }
  }

  private void checkPermission(boolean preview) {
    boolean hasScanPerm = userSession.hasGlobalPermission(GlobalPermissions.SCAN_EXECUTION);
    boolean hasPreviewPerm = userSession.hasGlobalPermission(GlobalPermissions.PREVIEW_EXECUTION);
    if (!hasPreviewPerm && !hasScanPerm) {
      throw new ForbiddenException(Messages.NO_PERMISSION);
    }
    if (!preview && !hasScanPerm) {
      throw new ForbiddenException("You're only authorized to execute a local (preview) SonarQube analysis without pushing the results to the SonarQube server. " +
        "Please contact your SonarQube administrator.");
    }
    if (preview && !hasPreviewPerm) {
      throw new ForbiddenException("You're not authorized to execute a preview analysis. Please contact your SonarQube administrator.");
    }
  }

  private static Map<String, String> moduleUuidsByKey(List<ComponentDto> moduleChildren) {
    Map<String, String> moduleUuidsByKey = newHashMap();
    for (ComponentDto componentDto : moduleChildren) {
      moduleUuidsByKey.put(componentDto.key(), componentDto.uuid());
    }
    return moduleUuidsByKey;
  }

  private static Map<String, Long> moduleIdsByKey(List<ComponentDto> moduleChildren) {
    Map<String, Long> moduleIdsByKey = newHashMap();
    for (ComponentDto componentDto : moduleChildren) {
      moduleIdsByKey.put(componentDto.key(), componentDto.getId());
    }
    return moduleIdsByKey;
  }

  private static class TreeModuleSettings {

    private Map<String, Long> moduleIdsByKey;
    private Map<String, String> moduleUuidsByKey;
    private Multimap<Long, PropertyDto> propertiesByModuleId;
    private Multimap<String, ComponentDto> moduleChildrenByModuleUuid;

    private TreeModuleSettings(Map<String, String> moduleUuidsByKey, Map<String, Long> moduleIdsByKey, List<ComponentDto> moduleChildren,
      List<PropertyDto> moduleChildrenSettings) {
      this.moduleIdsByKey = moduleIdsByKey;
      this.moduleUuidsByKey = moduleUuidsByKey;
      propertiesByModuleId = ArrayListMultimap.create();
      moduleChildrenByModuleUuid = ArrayListMultimap.create();

      for (PropertyDto settings : moduleChildrenSettings) {
        propertiesByModuleId.put(settings.getResourceId(), settings);
      }

      for (ComponentDto componentDto : moduleChildren) {
        String moduleUuid = componentDto.moduleUuid();
        if (moduleUuid != null) {
          moduleChildrenByModuleUuid.put(moduleUuid, componentDto);
        }
      }
    }

    List<PropertyDto> findModuleSettings(String moduleKey) {
      Long moduleId = moduleIdsByKey.get(moduleKey);
      return newArrayList(propertiesByModuleId.get(moduleId));
    }

    List<ComponentDto> findChildrenModule(String moduleKey) {
      String moduleUuid = moduleUuidsByKey.get(moduleKey);
      return newArrayList(moduleChildrenByModuleUuid.get(moduleUuid));
    }
  }
}
