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

import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.batch.protocol.input.ProjectReferentials;
import org.sonar.core.UtcDateUtils;
import org.sonar.core.component.AuthorizedComponentDto;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.plugins.MimeTypes;
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

public class ProjectReferentialsAction implements RequestHandler {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_PROFILE = "profile";

  private final DbClient dbClient;
  private final PropertiesDao propertiesDao;
  private final QProfileFactory qProfileFactory;
  private final QProfileLoader qProfileLoader;
  private final RuleService ruleService;
  private final Languages languages;

  public ProjectReferentialsAction(DbClient dbClient, PropertiesDao propertiesDao, QProfileFactory qProfileFactory, QProfileLoader qProfileLoader,
    RuleService ruleService, Languages languages) {
    this.dbClient = dbClient;
    this.propertiesDao = propertiesDao;
    this.qProfileFactory = qProfileFactory;
    this.qProfileLoader = qProfileLoader;
    this.ruleService = ruleService;
    this.languages = languages;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("project")
      .setDescription("Return project referentials")
      .setSince("4.5")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Project or module key")
      .setExampleValue("org.codehaus.sonar:sonar");

    action
      .createParam(PARAM_PROFILE)
      .setDescription("Profile name")
      .setExampleValue("SonarQube Way");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession userSession = UserSession.get();
    boolean hasScanPerm = userSession.hasGlobalPermission(GlobalPermissions.SCAN_EXECUTION);

    DbSession session = dbClient.openSession(false);
    try {
      String projectOrModuleKey = request.mandatoryParam(PARAM_KEY);
      String profileName = request.param(PARAM_PROFILE);
      ProjectReferentials ref = new ProjectReferentials();

      String projectKey = null;
      AuthorizedComponentDto module = dbClient.componentDao().getNullableAuthorizedComponentByKey(projectOrModuleKey, session);
      // Current project can be null when analysing a new project
      if (module != null) {
        ComponentDto project = dbClient.componentDao().getNullableRootProjectByKey(projectOrModuleKey, session);
        // Can be null if the given project is a provisioned one
        if (project != null) {
          if (!project.key().equals(module.key())) {
            addSettings(ref, module.getKey(), getSettingsFromParents(module.key(), hasScanPerm, session));
          }
          projectKey = project.key();
          addSettingsToChildrenModules(ref, projectOrModuleKey, Maps.<String, String>newHashMap(), hasScanPerm, session);
        } else {
          // Add settings of the provisioned project
          addSettings(ref, projectOrModuleKey, getPropertiesMap(propertiesDao.selectProjectProperties(projectOrModuleKey, session), hasScanPerm));
          projectKey = projectOrModuleKey;
        }
      }

      addProfiles(ref, projectKey, profileName, session);
      addActiveRules(ref);

      response.stream().setMediaType(MimeTypes.JSON);
      IOUtils.write(ref.toJson(), response.stream().output());
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
      parentProperties.putAll(getPropertiesMap(propertiesDao.selectProjectProperties(parent.key(), session), hasScanPerm));
    }
    return parentProperties;
  }

  private void aggregateParentModules(String component, List<ComponentDto> parents, DbSession session){
    ComponentDto parent = dbClient.componentDao().getParentModuleByKey(component, session);
    if (parent != null) {
      parents.add(parent);
      aggregateParentModules(parent.key(), parents, session);
    }
  }

  private void addSettingsToChildrenModules(ProjectReferentials ref, String projectKey, Map<String, String> parentProperties, boolean hasScanPerm, DbSession session) {
    parentProperties.putAll(getPropertiesMap(propertiesDao.selectProjectProperties(projectKey, session), hasScanPerm));
    addSettings(ref, projectKey, parentProperties);

    for (ComponentDto module : dbClient.componentDao().findModulesByProject(projectKey, session)) {
      addSettings(ref, module.key(), parentProperties);
      addSettingsToChildrenModules(ref, module.key(), parentProperties, hasScanPerm, session);
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
   *
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

}
