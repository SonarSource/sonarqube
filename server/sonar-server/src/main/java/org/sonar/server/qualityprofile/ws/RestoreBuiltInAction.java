/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.qualityprofile.QProfileReset;

import static org.sonar.server.util.LanguageParamUtils.getExampleValue;
import static org.sonar.server.util.LanguageParamUtils.getLanguageKeys;

public class RestoreBuiltInAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final QProfileReset reset;
  private final Languages languages;
  private final QProfileWsSupport qProfileWsSupport;

  public RestoreBuiltInAction(DbClient dbClient, QProfileReset reset, Languages languages, QProfileWsSupport qProfileWsSupport) {
    this.dbClient = dbClient;
    this.reset = reset;
    this.languages = languages;
    this.qProfileWsSupport = qProfileWsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction restoreDefault = controller.createAction("restore_built_in")
      .setDescription("Restore built-in profiles from the definitions declared by plugins. " +
        "Missing profiles are created, existing ones are updated.")
      .setSince("4.4")
      .setPost(true)
      .setHandler(this);
    restoreDefault.createParam("language")
      .setDescription("Restore the built-in profiles defined for this language")
      .setExampleValue(getExampleValue(languages))
      .setPossibleValues(getLanguageKeys(languages))
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) {
    qProfileWsSupport.checkQProfileAdminPermission();

    String language = request.mandatoryParam("language");
    try (DbSession dbSession = dbClient.openSession(false)) {
      reset.resetLanguage(dbSession, language);
    }
    response.noContent();
  }
}
