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

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.sonar.api.ServerSide;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.web.UserRole;
import org.sonar.batch.protocol.input.FileData;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.core.UtcDateUtils;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.FilePathWithHashDto;
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
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

@ServerSide
public class ProjectRepositoryLoader {

  private static final Logger LOG = Loggers.get(ProjectRepositoryLoader.class);

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

  public ProjectRepositories load(ProjectRepositoryQuery query) {
    boolean hasScanPerm = UserSession.get().hasGlobalPermission(GlobalPermissions.SCAN_EXECUTION);
    checkPermission(query.isPreview());

    DbSession session = dbClient.openSession(false);
    try {
      ProjectRepositories ref = new ProjectRepositories();
      String projectKey = query.getModuleKey();
      ComponentDto module = dbClient.componentDao().getNullableByKey(session, query.getModuleKey());
      // Current project/module can be null when analysing a new project
      if (module != null) {
        // Scan permission is enough to analyze all projects but preview permission is limited to projects user can access
        if (query.isPreview() && !UserSession.get().hasProjectPermissionByUuid(UserRole.USER, module.projectUuid())) {
          throw new ForbiddenException("You're not authorized to access to project '" + module.name() + "', please contact your SonarQube administrator.");
        }

        ComponentDto project = getProject(module, session);
        if (!project.key().equals(module.key())) {
          addSettings(ref, module.getKey(), getSettingsFromParents(module, hasScanPerm, session));
          projectKey = project.key();
        }

        List<ComponentDto> modulesTree = dbClient.componentDao().selectEnabledDescendantModules(session, module.uuid());
        Map<String, String> moduleUuidsByKey = moduleUuidsByKey(module, modulesTree);
        Map<String, Long> moduleIdsByKey = moduleIdsByKey(module, modulesTree);

        List<PropertyDto> modulesTreeSettings = dbClient.propertiesDao().selectEnabledDescendantModuleProperties(module.uuid(), session);
        TreeModuleSettings treeModuleSettings = new TreeModuleSettings(moduleUuidsByKey, moduleIdsByKey, modulesTree, modulesTreeSettings, module);

        addSettingsToChildrenModules(ref, query.getModuleKey(), Maps.<String, String>newHashMap(), treeModuleSettings, hasScanPerm, session);
        List<FilePathWithHashDto> files = module.isRootProject() ?
          dbClient.componentDao().selectEnabledFilesFromProject(session, module.uuid()) :
          dbClient.componentDao().selectEnabledDescendantFiles(session, module.uuid());
        addFileData(session, ref, modulesTree, files);

        // FIXME need real value but actually only used to know if there is a previous analysis in local issue tracking mode so any value is
        // ok
        ref.setLastAnalysisDate(new Date());
      } else {
        ref.setLastAnalysisDate(null);
      }

      addProfiles(ref, projectKey, query.getProfileName(), session);
      addActiveRules(ref);
      addManualRules(ref);
      return ref;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private ComponentDto getProject(ComponentDto module, DbSession session) {
    if (!module.isRootProject()) {
      return dbClient.componentDao().getNullableByUuid(session, module.projectUuid());
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
      parentProperties.putAll(getPropertiesMap(dbClient.propertiesDao().selectProjectProperties(parent.key(), session), hasScanPerm));
    }
    return parentProperties;
  }

  private void aggregateParentModules(ComponentDto component, List<ComponentDto> parents, DbSession session) {
    String moduleUuid = component.moduleUuid();
    if (moduleUuid != null) {
      ComponentDto parent = dbClient.componentDao().getByUuid(session, moduleUuid);
      if (parent != null) {
        parents.add(parent);
        aggregateParentModules(parent, parents, session);
      }
    }
  }

  private void addSettingsToChildrenModules(ProjectRepositories ref, String moduleKey, Map<String, String> parentProperties, TreeModuleSettings treeModuleSettings,
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

  private void addSettings(ProjectRepositories ref, String module, Map<String, String> properties) {
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

  private void addProfiles(ProjectRepositories ref, @Nullable String projectKey, @Nullable String profileName, DbSession session) {
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

  private void addActiveRules(ProjectRepositories ref) {
    for (org.sonar.batch.protocol.input.QProfile qProfile : ref.qProfiles()) {
      // Load all rules of the profile language (only needed fields are loaded)
      Map<RuleKey, Rule> languageRules = ruleByRuleKey(ruleService.search(new RuleQuery().setLanguages(newArrayList(qProfile.language())),
        new QueryContext().setLimit(100).setFieldsToReturn(newArrayList(
          RuleNormalizer.RuleField.KEY.field(), RuleNormalizer.RuleField.NAME.field(), RuleNormalizer.RuleField.INTERNAL_KEY.field(), RuleNormalizer.RuleField.TEMPLATE_KEY.field()
          )).setScroll(true))
        .scroll());
      for (Iterator<ActiveRule> activeRuleIterator = qProfileLoader.findActiveRulesByProfile(qProfile.key()); activeRuleIterator.hasNext();) {
        ActiveRule activeRule = activeRuleIterator.next();
        Rule rule = languageRules.get(activeRule.key().ruleKey());
        if (rule == null) {
          // It should never happen, but we need some log in case it happens
          LOG.warn("Rule could not be found on active rule '{}'", activeRule.key());
        } else {
          RuleKey templateKey = rule.templateKey();
          org.sonar.batch.protocol.input.ActiveRule inputActiveRule = new org.sonar.batch.protocol.input.ActiveRule(
            activeRule.key().ruleKey().repository(),
            activeRule.key().ruleKey().rule(),
            templateKey != null ? templateKey.rule() : null,
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
  }

  private Map<RuleKey, Rule> ruleByRuleKey(Iterator<Rule> rules) {
    return Maps.uniqueIndex(rules, new Function<Rule, RuleKey>() {
      @Override
      public RuleKey apply(@Nullable Rule input) {
        return input != null ? input.key() : null;
      }
    });
  }

  private void addManualRules(ProjectRepositories ref) {
    Result<Rule> ruleSearchResult = ruleService.search(new RuleQuery().setRepositories(newArrayList(RuleKey.MANUAL_REPOSITORY_KEY)), new QueryContext().setScroll(true)
      .setFieldsToReturn(newArrayList(RuleNormalizer.RuleField.KEY.field(), RuleNormalizer.RuleField.NAME.field())));
    Iterator<Rule> rules = ruleSearchResult.scroll();
    while (rules.hasNext()) {
      Rule rule = rules.next();
      ref.addActiveRule(new org.sonar.batch.protocol.input.ActiveRule(
        RuleKey.MANUAL_REPOSITORY_KEY,
        rule.key().rule(),
        null, rule.name(),
        null, null, null));
    }
  }

  private void addFileData(DbSession session, ProjectRepositories ref, List<ComponentDto> moduleChildren, List<FilePathWithHashDto> files) {
    Map<String, String> moduleKeysByUuid = newHashMap();
    for (ComponentDto module : moduleChildren) {
      moduleKeysByUuid.put(module.uuid(), module.key());
    }

    for (FilePathWithHashDto file : files) {
      // TODO should query E/S to know if blame is missing on this file
      FileData fileData = new FileData(file.getSrcHash(), true);
      ref.addFileData(moduleKeysByUuid.get(file.getModuleUuid()), file.getPath(), fileData);
    }
  }

  private void checkPermission(boolean preview) {
    UserSession userSession = UserSession.get();
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

  private Map<String, String> moduleUuidsByKey(ComponentDto module, List<ComponentDto> moduleChildren) {
    Map<String, String> moduleUuidsByKey = newHashMap();
    for (ComponentDto componentDto : moduleChildren) {
      moduleUuidsByKey.put(componentDto.key(), componentDto.uuid());
    }
    return moduleUuidsByKey;
  }

  private Map<String, Long> moduleIdsByKey(ComponentDto module, List<ComponentDto> moduleChildren) {
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
      List<PropertyDto> moduleChildrenSettings, ComponentDto module) {
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
