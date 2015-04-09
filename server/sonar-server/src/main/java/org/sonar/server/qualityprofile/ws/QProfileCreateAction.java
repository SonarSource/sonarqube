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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileResult;
import org.sonar.server.user.UserSession;

import java.io.InputStream;

public class QProfileCreateAction implements BaseQProfileWsAction {

  private static final String PARAM_PROFILE_NAME = "name";
  private static final String PARAM_LANGUAGE = "language";
  private static final String PARAM_BACKUP_FORMAT = "backup_%s";

  private final DbClient dbClient;

  private final QProfileFactory profileFactory;

  private final QProfileExporters exporters;

  private final Languages languages;

  private final ProfileImporter[] importers;

  public QProfileCreateAction(DbClient dbClient, QProfileFactory profileFactory, QProfileExporters exporters, Languages languages, ProfileImporter[] importers) {
    this.dbClient = dbClient;
    this.profileFactory = profileFactory;
    this.exporters = exporters;
    this.languages = languages;
    this.importers = importers;
  }

  public QProfileCreateAction(DbClient dbClient, QProfileFactory profileFactory, QProfileExporters exporters, Languages languages) {
    this(dbClient, profileFactory, exporters, languages, new ProfileImporter[0]);
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction create = controller.createAction("create")
      .setSince("5.2")
      .setDescription("Create a quality profile.")
      .setPost(true)
      .setResponseExample(getClass().getResource("example-create.json"))
      .setHandler(this);

    create.createParam(PARAM_PROFILE_NAME)
      .setDescription("The name for the new quality profile.")
      .setExampleValue("My Sonar way")
      .setRequired(true);

    create.createParam(PARAM_LANGUAGE)
      .setDescription("The language for the quality profile.")
      .setExampleValue("js")
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages))
      .setRequired(true);

    for (ProfileImporter importer : importers) {
      create.createParam(getBackupParamName(importer.getKey()))
        .setDescription(String.format("A configuration file for %s.", importer.getName()));
    }
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession.get().checkLoggedIn().checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    String name = request.mandatoryParam(PARAM_PROFILE_NAME);
    String language = request.mandatoryParam(PARAM_LANGUAGE);

    DbSession dbSession = dbClient.openSession(false);

    try {
      QProfileResult result = new QProfileResult();
      QualityProfileDto profile = profileFactory.create(dbSession, QProfileName.createFor(language, name));
      result.setProfile(profile);
      for (ProfileImporter importer : importers) {
        InputStream contentToImport = request.paramAsInputStream(getBackupParamName(importer.getKey()));
        if (contentToImport != null) {
          result.add(exporters.importXml(profile, importer.getKey(), contentToImport, dbSession));
        }
      }
      dbSession.commit();
      writeResult(response.newJsonWriter(), result);
    } finally {
      dbSession.close();
    }
  }

  private void writeResult(JsonWriter json, QProfileResult result) {
    String language = result.profile().getLanguage();
    json.beginObject().name("profile").beginObject()
      .prop("key", result.profile().getKey())
      .prop("name", result.profile().getName())
      .prop("language", language)
      .prop("languageName", languages.get(result.profile().getLanguage()).getName())
      .prop("isDefault", false)
      .prop("isInherited", false)
      .endObject();

    if (!result.infos().isEmpty()) {
      json.name("infos").beginArray();
      for (String info : result.infos()) {
        json.value(info);
      }
      json.endArray();
    }

    if (!result.warnings().isEmpty()) {
      json.name("warnings").beginArray();
      for (String warning : result.warnings()) {
        json.value(warning);
      }
      json.endArray();
    }

    json.endObject().close();
  }

  private String getBackupParamName(String importerKey) {
    return String.format(PARAM_BACKUP_FORMAT, importerKey);
  }
}
