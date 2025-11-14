/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import javax.annotation.Nullable;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.builtin.QProfileName;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.language.LanguageParamUtils.getOrderedLanguageKeys;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_NAME;

public class CreateAction implements QProfileWsAction {

  static final int NAME_MAXIMUM_LENGTH = 100;

  private final DbClient dbClient;
  private final QProfileFactory profileFactory;
  private final Languages languages;
  private final UserSession userSession;

  public CreateAction(DbClient dbClient, QProfileFactory profileFactory, Languages languages,
    UserSession userSession) {
    this.dbClient = dbClient;
    this.profileFactory = profileFactory;
    this.languages = languages;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction create = controller.createAction(ACTION_CREATE)
      .setPost(true)
      .setDescription("Create a quality profile.<br>" +
        "Requires to be logged in and the 'Administer Quality Profiles' permission.")
      .setResponseExample(getClass().getResource("create-example.json"))
      .setSince("5.2")
      .setHandler(this);

    create.createParam(PARAM_NAME)
      .setRequired(true)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setDescription("Quality profile name")
      .setExampleValue("My Sonar way");

    create.createParam(PARAM_LANGUAGE)
      .setRequired(true)
      .setDescription("Quality profile language")
      .setExampleValue("js")
      .setPossibleValues(getOrderedLanguageKeys(languages));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    userSession.checkPermission(ADMINISTER_QUALITY_PROFILES);
    try (DbSession dbSession = dbClient.openSession(false)) {
      CreateRequest createRequest = toRequest(request);
      writeProtobuf(doHandle(dbSession, createRequest), request, response);
    }
  }

  private CreateWsResponse doHandle(DbSession dbSession, CreateRequest createRequest) {
    QProfileDto profile = profileFactory.checkAndCreateCustom(dbSession, QProfileName.createFor(createRequest.getLanguage(), createRequest.getName()));
    dbSession.commit();
    return buildResponse(profile);
  }

  private static CreateRequest toRequest(Request request) {
    Builder builder = CreateRequest.builder()
      .setLanguage(request.mandatoryParam(PARAM_LANGUAGE))
      .setName(request.mandatoryParam(PARAM_NAME));
    return builder.build();
  }

  private CreateWsResponse buildResponse(QProfileDto profile) {
    String language = profile.getLanguage();
    CreateWsResponse.QualityProfile.Builder builder = CreateWsResponse.QualityProfile.newBuilder()
      .setKey(profile.getKee())
      .setName(profile.getName())
      .setLanguage(language)
      .setLanguageName(languages.get(profile.getLanguage()).getName())
      .setIsDefault(false)
      .setIsInherited(false);
    return CreateWsResponse.newBuilder().setProfile(builder.build()).build();
  }

  private static class CreateRequest {
    private final String name;
    private final String language;

    private CreateRequest(Builder builder) {
      this.name = builder.name;
      this.language = builder.language;
    }

    public String getLanguage() {
      return language;
    }

    public String getName() {
      return name;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  private static class Builder {
    private String language;
    private String name;

    private Builder() {
      // enforce factory method use
    }

    public Builder setLanguage(@Nullable String language) {
      this.language = language;
      return this;
    }

    public Builder setName(@Nullable String profileName) {
      this.name = profileName;
      return this;
    }

    public CreateRequest build() {
      checkArgument(language != null && !language.isEmpty(), "Language is mandatory and must not be empty.");
      checkArgument(name != null && !name.isEmpty(), "Profile name is mandatory and must not be empty.");
      return new CreateRequest(this);
    }
  }
}
