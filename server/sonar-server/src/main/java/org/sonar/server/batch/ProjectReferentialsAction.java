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

import org.apache.commons.io.IOUtils;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.batch.protocol.input.ProjectReferentials;
import org.sonar.batch.protocol.input.QProfile;
import org.sonar.core.UtcDateUtils;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class ProjectReferentialsAction implements RequestHandler {

  private static final String PARAM_KEY = "key";

  private final DbClient dbClient;
  private final PropertiesDao propertiesDao;
  private final QProfileFactory qProfileFactory;
  private final Languages languages;

  public ProjectReferentialsAction(DbClient dbClient, PropertiesDao propertiesDao, QProfileFactory qProfileFactory, Languages languages) {
    this.dbClient = dbClient;
    this.propertiesDao = propertiesDao;
    this.qProfileFactory = qProfileFactory;
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
      .setDescription("Project key")
      .setExampleValue("org.codehaus.sonar:sonar");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession userSession = UserSession.get();
    boolean hasScanPerm = userSession.hasGlobalPermission(GlobalPermissions.SCAN_EXECUTION);

    DbSession session = dbClient.openSession(false);
    try {
      ProjectReferentials ref = new ProjectReferentials();
      String projectKey = request.mandatoryParam(PARAM_KEY);
      addSettings(ref, projectKey, hasScanPerm, session);
      addProfiles(ref, projectKey, session);

      response.stream().setMediaType(MimeTypes.JSON);
      IOUtils.write(ref.toJson(), response.stream().output());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void addSettings(ProjectReferentials ref, String projectKey, boolean hasScanPerm, DbSession session) {
    addSettings(ref, projectKey, propertiesDao.selectProjectProperties(projectKey, session), hasScanPerm);
    for (ComponentDto module : dbClient.componentDao().findModulesByProject(projectKey, session)) {
      addSettings(ref, module.getKey(), propertiesDao.selectProjectProperties(module.getKey(), session), hasScanPerm);
    }
  }

  private void addSettings(ProjectReferentials ref, String projectOrModuleKey, List<PropertyDto> propertyDtos, boolean hasScanPerm) {
    Map<String, String> properties = newHashMap();
    for (PropertyDto propertyDto : propertyDtos) {
      String key = propertyDto.getKey();
      String value = propertyDto.getValue();
      if (isPropertyAllowed(key, hasScanPerm)) {
        properties.put(key, value);
      }
    }
    if (!properties.isEmpty()) {
      ref.addSettings(projectOrModuleKey, properties);
    }
  }

  private static boolean isPropertyAllowed(String key, boolean hasScanPerm) {
    return !key.contains(".secured") || hasScanPerm;
  }

  private void addProfiles(ProjectReferentials ref, String projectKey, DbSession session) {
    for (Language language : languages.all()) {
      String languageKey = language.getKey();
      QualityProfileDto qualityProfileDto = qProfileFactory.getByProjectAndLanguage(session, projectKey, languageKey);
      qualityProfileDto = qualityProfileDto != null ? qualityProfileDto : qProfileFactory.getDefault(session, languageKey);
      if (qualityProfileDto != null) {
        QProfile profile = new QProfile(qualityProfileDto.getKey(), qualityProfileDto.getName(), qualityProfileDto.getLanguage(),
          UtcDateUtils.parseDateTime(qualityProfileDto.getRulesUpdatedAt()));
        ref.addQProfile(profile);
      }
    }
  }

}
