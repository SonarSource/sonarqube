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
package org.sonar.server.almsettings;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.ComponentDto;

public class SetAzureBindingAction implements AlmSettingsWsAction {

  private static final String PARAM_ALM_SETTING = "almSetting";
  private static final String PARAM_PROJECT = "project";

  private final DbClient dbClient;
  private final AlmSettingsSupport almSettingsSupport;

  public SetAzureBindingAction(DbClient dbClient, AlmSettingsSupport almSettingsSupport) {
    this.dbClient = dbClient;
    this.almSettingsSupport = almSettingsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("set_azure_binding")
      .setDescription("Bind a Azure DevOps ALM instance to a project.<br/>" +
        "If the project was already bound to a previous Azure DevOps ALM instance, the binding will be updated to the new one." +
        "Requires the 'Administer' permission on the project")
      .setPost(true)
      .setSince("8.1")
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setDescription("GitHub ALM setting key");
    action.createParam(PARAM_PROJECT)
      .setRequired(true)
      .setDescription("Project key");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(request);
    response.noContent();
  }

  private void doHandle(Request request) {
    String almSetting = request.mandatoryParam(PARAM_ALM_SETTING);
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = almSettingsSupport.getProject(dbSession, projectKey);
      AlmSettingDto almSettingDto = almSettingsSupport.getAlmSetting(dbSession, almSetting);
      dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, new ProjectAlmSettingDto()
        .setProjectUuid(project.uuid())
        .setAlmSettingUuid(almSettingDto.getUuid())
        .setAlmRepo(null)
        .setAlmSlug(null));
      dbSession.commit();
    }
  }

}
