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
import org.sonar.api.ServerComponent;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.batch.protocol.input.ProjectReferentials;
import org.sonar.core.UtcDateUtils;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public class ProjectRepositoryLoader implements ServerComponent {

  private final DbClient dbClient;
  private final QProfileFactory qProfileFactory;
  private final QProfileLoader qProfileLoader;
  private final RuleService ruleService;
  private final Languages languages;

  public ProjectRepositoryLoader(DbClient dbClient, QProfileFactory qProfileFactory, QProfileLoader qProfileLoader, RuleService ruleService,
    Languages languages) {
    this.dbClient = dbClient;
    this.qProfileFactory = qProfileFactory;
    this.qProfileLoader = qProfileLoader;
    this.ruleService = ruleService;
    this.languages = languages;
  }

  public ProjectReferentials load(ProjectRepositoryQuery query) {
    boolean hasScanPerm = UserSession.get().hasGlobalPermission(GlobalPermissions.SCAN_EXECUTION);
    checkPermission(query.isPreview());

    DbSession session = dbClient.openSession(false);
    try {
      ProjectReferentials ref = new ProjectReferentials();
      String projectKey = query.getModuleKey();
      ComponentDto module = dbClient.componentDao().getNullableByKey(session, query.getModuleKey());
      // Current project/module can be null when analysing a new project
      if (module != null) {
        ComponentDto project = dbClient.componentDao().getNullableRootProjectByKey(query.getModuleKey(), session);

        // Can be null if the given project is a provisioned one
        if (project != null) {
          if (!project.key().equals(module.key())) {
            addSettings(ref, module.getKey(), getSettingsFromParents(module.key(), hasScanPerm, session));
            projectKey = project.key();
          }

          List<PropertyDto> moduleSettings = dbClient.propertiesDao().selectProjectProperties(query.getModuleKey(), session);
          List<ComponentDto> moduleChildren = dbClient.componentDao().findChildrenModulesFromModule(session, query.getModuleKey());
          List<PropertyDto> moduleChildrenSettings = newArrayList();
          if (!moduleChildren.isEmpty()) {
            moduleChildrenSettings = dbClient.propertiesDao().findChildrenModuleProperties(query.getModuleKey(), session);
          }
          TreeModuleSettings treeModuleSettings = new TreeModuleSettings(moduleChildren, moduleChildrenSettings, module, moduleSettings);

          addSettingsToChildrenModules(ref, query.getModuleKey(), Maps.<String, String>newHashMap(), treeModuleSettings, hasScanPerm, session);
        } else {
          // Add settings of the provisioned project
          addSettings(ref, query.getModuleKey(), getPropertiesMap(dbClient.propertiesDao().selectProjectProperties(query.getModuleKey(), session), hasScanPerm));
        }
      }

      addProfiles(ref, projectKey, query.getProfileName(), session);
      addActiveRules(ref);
      return ref;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private Map<String, String> getSettingsFromParents(String moduleKey, boolean hasScanPerm, DbSession session) {
    List<ComponentDto> parents = newArrayList();
    aggregateParentModules(moduleKey, parents, session);
    Collections.reverse(parents);

    Map<String, String> parentProperties = newHashMap();
    for (ComponentDto parent : parents) {
      parentProperties.putAll(getPropertiesMap(dbClient.propertiesDao().selectProjectProperties(parent.key(), session), hasScanPerm));
    }
    return parentProperties;
  }

  private void aggregateParentModules(String component, List<ComponentDto> parents, DbSession session) {
    ComponentDto parent = dbClient.componentDao().getParentModuleByKey(component, session);
    if (parent != null) {
      parents.add(parent);
      aggregateParentModules(parent.key(), parents, session);
    }
  }

  private void addSettingsToChildrenModules(ProjectReferentials ref, String moduleKey, Map<String, String> parentProperties, TreeModuleSettings treeModuleSettings,
                                            boolean hasScanPerm, DbSession session) {
    Map<String, String> currentParentProperties = newHashMap();
    currentParentProperties.putAll(parentProperties);
    currentParentProperties.putAll(getPropertiesMap(treeModuleSettings.findModuleSettings(moduleKey), hasScanPerm));
    addSettings(ref, moduleKey, currentParentProperties);

    for (ComponentDto childModule : treeModuleSettings.findChildrenModule(moduleKey)) {
      addSettings(ref, childModule.getKey(), currentParentProperties);
      addSettingsToChildrenModules(ref, childModule.getKey(), currentParentProperties, treeModuleSettings, hasScanPerm, session);
    }
  }

  private void addSettings(ProjectReferentials ref, String module, Map<String, String> properties) {
    if (!properties.isEmpty()) {
      ref.addSettings(module, properties);
    }
  }

  private Map<String, String> getPropertiesMap(List<PropertyDto> propertyDtos, boolean hasScanPerm) {
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

  private void addProfiles(ProjectReferentials ref, @Nullable String projectKey, @Nullable String profileName, DbSession session) {
    for (Language language : languages.all()) {
      String languageKey = language.getKey();
      QualityProfileDto qualityProfileDto = getProfile(languageKey, projectKey, profileName, session);
      ref.addQProfile(new org.sonar.batch.protocol.input.QProfile(
        qualityProfileDto.getKey(),
        qualityProfileDto.getName(),
        qualityProfileDto.getLanguage(),
        UtcDateUtils.parseDateTime(qualityProfileDto.getRulesUpdatedAt())));
    }
  }

  /**
   * First try to find a quality profile matching the given name (if provided) and current language
   * If no profile found, try to find the quality profile set on the project (if provided)
   * If still no profile found, try to find the default profile of the language
   * <p/>
   * Never return null because a default profile should always be set on ech language
   */
  private QualityProfileDto getProfile(String languageKey, @Nullable String projectKey, @Nullable String profileName, DbSession session) {
    QualityProfileDto qualityProfileDto = profileName != null ? qProfileFactory.getByNameAndLanguage(session, profileName, languageKey) : null;
    if (qualityProfileDto == null && projectKey != null) {
      qualityProfileDto = qProfileFactory.getByProjectAndLanguage(session, projectKey, languageKey);
    }
    qualityProfileDto = qualityProfileDto != null ? qualityProfileDto : qProfileFactory.getDefault(session, languageKey);
    if (qualityProfileDto != null) {
      return qualityProfileDto;
    } else {
      throw new IllegalStateException(String.format("No quality profile can been found on language '%s' for project '%s'", languageKey, projectKey));
    }
  }

  private void addActiveRules(ProjectReferentials ref) {
    for (org.sonar.batch.protocol.input.QProfile qProfile : ref.qProfiles()) {
      for (ActiveRule activeRule : qProfileLoader.findActiveRulesByProfile(qProfile.key())) {
        Rule rule = ruleService.getNonNullByKey(activeRule.key().ruleKey());
        org.sonar.batch.protocol.input.ActiveRule inputActiveRule = new org.sonar.batch.protocol.input.ActiveRule(
          activeRule.key().ruleKey().repository(),
          activeRule.key().ruleKey().rule(),
          rule.name(),
          activeRule.severity(),
          rule.internalKey(),
          qProfile.language());
        for (Map.Entry<String, String> entry : activeRule.params().entrySet()) {
          inputActiveRule.addParam(entry.getKey(), entry.getValue());
        }
        ref.addActiveRule(inputActiveRule);
      }
    }
  }

  private void checkPermission(boolean preview) {
    UserSession userSession = UserSession.get();
    boolean hasScanPerm = userSession.hasGlobalPermission(GlobalPermissions.SCAN_EXECUTION);
    boolean hasPreviewPerm = userSession.hasGlobalPermission(GlobalPermissions.DRY_RUN_EXECUTION);
    if (!hasPreviewPerm && !hasScanPerm) {
      throw new ForbiddenException("You're not authorized to execute any SonarQube analysis. Please contact your SonarQube administrator.");
    }
    if (!preview && !hasScanPerm) {
      throw new ForbiddenException("You're only authorized to execute a local (dry run) SonarQube analysis without pushing the results to the SonarQube server. " +
        "Please contact your SonarQube administrator.");
    }
  }

  private static class TreeModuleSettings {

    private Map<String, Long> moduleIdsByKey;
    private Map<String, String> moduleUuidsByKey;
    private Multimap<Long, PropertyDto> propertiesByModuleId;
    private Multimap<String, ComponentDto> moduleChildrenByModuleUuid;

    private TreeModuleSettings(List<ComponentDto> moduleChildren, List<PropertyDto> moduleChildrenSettings, ComponentDto module, List<PropertyDto> moduleSettings) {
      propertiesByModuleId = ArrayListMultimap.create();
      moduleIdsByKey = newHashMap();
      moduleUuidsByKey = newHashMap();
      moduleChildrenByModuleUuid = ArrayListMultimap.create();

      for (PropertyDto settings : moduleChildrenSettings) {
        propertiesByModuleId.put(settings.getResourceId(), settings);
      }
      propertiesByModuleId.putAll(module.getId(), moduleSettings);

      moduleIdsByKey.put(module.key(), module.getId());
      moduleUuidsByKey.put(module.key(), module.uuid());
      for (ComponentDto componentDto : moduleChildren) {
        moduleIdsByKey.put(componentDto.key(), componentDto.getId());
        moduleUuidsByKey.put(componentDto.key(), componentDto.uuid());
        String moduleUuid = componentDto.moduleUuid();
        if (moduleUuid != null) {
          moduleChildrenByModuleUuid.put(moduleUuid, componentDto);
        } else {
          moduleChildrenByModuleUuid.put(module.uuid(), componentDto);
        }
      }
    }

    private  List<PropertyDto> findModuleSettings(String moduleKey) {
      Long moduleId = moduleIdsByKey.get(moduleKey);
      return newArrayList(propertiesByModuleId.get(moduleId));
    }

    private List<ComponentDto> findChildrenModule(String moduleKey) {
      String moduleUuid = moduleUuidsByKey.get(moduleKey);
      return newArrayList(moduleChildrenByModuleUuid.get(moduleUuid));
    }
  }
}
