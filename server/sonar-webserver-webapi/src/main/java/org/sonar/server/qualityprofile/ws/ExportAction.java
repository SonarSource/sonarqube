/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.Response.Stream;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.language.LanguageParamUtils;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonarqube.ws.MediaTypes;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class ExportAction implements QProfileWsAction {

  private static final String PARAM_EXPORTER_KEY = "exporterKey";

  private final DbClient dbClient;
  private final QProfileBackuper backuper;
  private final QProfileExporters exporters;
  private final Languages languages;

  public ExportAction(DbClient dbClient, QProfileBackuper backuper, QProfileExporters exporters, Languages languages) {
    this.dbClient = dbClient;
    this.backuper = backuper;
    this.exporters = exporters;
    this.languages = languages;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("export")
      .setSince("5.2")
      .setDescription("Export a quality profile.")
      .setResponseExample(getClass().getResource("export-example.xml"))
      .setHandler(this);

    action.createParam(PARAM_QUALITY_PROFILE)
      .setDescription("Quality profile name to export. If left empty, the default profile for the language is exported.")
      .setExampleValue("My Sonar way");

    action.createParam(PARAM_LANGUAGE)
      .setDescription("Quality profile language")
      .setRequired(true)
      .setExampleValue(LanguageParamUtils.getExampleValue(languages))
      .setPossibleValues(LanguageParamUtils.getOrderedLanguageKeys(languages));

    Set<String> exporterKeys = Arrays.stream(languages.all())
      .map(language -> exporters.exportersForLanguage(language.getKey()))
      .flatMap(Collection::stream)
      .map(ProfileExporter::getKey)
      .collect(Collectors.toSet());
    if (!exporterKeys.isEmpty()) {
      action.createParam(PARAM_EXPORTER_KEY)
        .setDescription("Output format. If left empty, the same format as api/qualityprofiles/backup is used. " +
          "Possible values are described by api/qualityprofiles/exporters.")
        .setPossibleValues(exporterKeys);
    }
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String name = request.param(PARAM_QUALITY_PROFILE);
    String language = request.mandatoryParam(PARAM_LANGUAGE);

    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto profile = loadProfile(dbSession, language, name);
      String exporterKey = exporters.exportersForLanguage(profile.getLanguage()).isEmpty() ? null : request.param(PARAM_EXPORTER_KEY);
      writeResponse(dbSession, profile, exporterKey, response);
    }
  }

  private void writeResponse(DbSession dbSession, QProfileDto profile, @Nullable String exporterKey, Response response) throws IOException {
    Stream stream = response.stream();
    ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
    try (Writer writer = new OutputStreamWriter(bufferStream, UTF_8)) {
      if (exporterKey == null) {
        stream.setMediaType(MediaTypes.XML);
        backuper.backup(dbSession, profile, writer);
      } else {
        stream.setMediaType(exporters.mimeType(exporterKey));
        exporters.export(dbSession, profile, exporterKey, writer);
      }
    }

    OutputStream output = response.stream().output();
    IOUtils.write(bufferStream.toByteArray(), output);
  }

  private QProfileDto loadProfile(DbSession dbSession, String language, @Nullable String name) {
    QProfileDto profile;

    if (name == null) {
      // return the default profile
      profile = dbClient.qualityProfileDao().selectDefaultProfile(dbSession, language);
    } else {
      profile = dbClient.qualityProfileDao().selectByNameAndLanguage(dbSession, name, language);
    }
    return checkFound(profile, "Could not find profile with name '%s' for language '%s'", name, language);
  }
}
