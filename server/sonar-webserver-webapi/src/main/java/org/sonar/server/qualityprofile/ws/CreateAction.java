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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.UrlValidatorUtil;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.builtin.QProfileName;
import org.sonar.server.qualityprofile.QProfileResult;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.language.LanguageParamUtils.getOrderedLanguageKeys;
import static org.sonar.server.qualityprofile.ws.QProfileWsSupport.createOrganizationParam;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;

import org.springframework.beans.factory.annotation.Autowired;

public class CreateAction implements QProfileWsAction {

  private static final String PARAM_BACKUP_FORMAT = "backup_%s";
  static final int NAME_MAXIMUM_LENGTH = 100;

  private final Logger logger = LoggerFactory.getLogger(CreateAction.class);

  private final DbClient dbClient;
  private final QProfileFactory profileFactory;
  private final QProfileExporters exporters;
  private final Languages languages;
  private final ProfileImporter[] importers;
  private final UserSession userSession;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final QProfileWsSupport wsSupport;

  @Autowired(required = false)
  public CreateAction(DbClient dbClient, QProfileFactory profileFactory, QProfileExporters exporters, Languages languages,
    UserSession userSession, ActiveRuleIndexer activeRuleIndexer, QProfileWsSupport wsSupport, ProfileImporter... importers) {
    this.dbClient = dbClient;
    this.profileFactory = profileFactory;
    this.exporters = exporters;
    this.languages = languages;
    this.userSession = userSession;
    this.activeRuleIndexer = activeRuleIndexer;
    this.wsSupport = wsSupport;
    this.importers = importers;
  }

  @Autowired(required = false)
  public CreateAction(DbClient dbClient, QProfileFactory profileFactory, QProfileExporters exporters, Languages languages,
    UserSession userSession, ActiveRuleIndexer activeRuleIndexer, QProfileWsSupport wsSupport) {
    this(dbClient, profileFactory, exporters, languages, userSession, activeRuleIndexer, wsSupport, new ProfileImporter[0]);
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
    List<Change> changelog = new ArrayList<>();

    createOrganizationParam(create);

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

    for (ProfileImporter importer : importers) {
      String backupParamName = getBackupParamName(importer.getKey());
      create.createParam(backupParamName)
        .setDescription(String.format("A configuration file for %s.", importer.getName()))
        .setDeprecatedSince("9.8");
      changelog.add(new Change("9.8", String.format("'%s' parameter is deprecated", backupParamName)));
    }

    create.setChangelog(changelog.toArray(new Change[0]));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganizationByKey(dbSession, request.param(PARAM_ORGANIZATION));
      userSession.checkPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);
      CreateRequest createRequest = toRequest(request, organization);
      checkArgument(UrlValidatorUtil.textContainsValidUrl(createRequest.getName()), "Invalid quality profile name");

      logger.info("Create QProfile Request:: organization: {}, qProfile: {}, language: {}, user: {}",
              organization.getKey(), createRequest.getName(), createRequest.getLanguage(), userSession.getLogin());
      writeProtobuf(doHandle(dbSession, createRequest, request, organization), request, response);
    }
  }

  private CreateWsResponse doHandle(DbSession dbSession, CreateRequest createRequest, Request request, OrganizationDto organization) {
    QProfileResult result = new QProfileResult();
    QProfileDto profile = profileFactory.checkAndCreateCustom(dbSession, organization, QProfileName.createFor(createRequest.getLanguage(), createRequest.getName()));
    result.setProfile(profile);
    for (ProfileImporter importer : importers) {
      String importerKey = importer.getKey();
      InputStream contentToImport = request.paramAsInputStream(getBackupParamName(importerKey));
      if (contentToImport != null) {
        result.add(exporters.importXml(profile, importerKey, contentToImport, dbSession));
      }
    }
    logger.info("Created QProfile:: organization: {}, qProfile: {}, language: {}, user: {}",
            organization.getKey(), profile.getName(), profile.getLanguage(), userSession.getLogin());
    activeRuleIndexer.commitAndIndex(dbSession, result.getChanges());
    return buildResponse(result, organization);
  }

  private static CreateRequest toRequest(Request request, OrganizationDto organization) {
    Builder builder = CreateRequest.builder()
      .setLanguage(request.mandatoryParam(PARAM_LANGUAGE))
      .setOrganizationKey(organization.getKey())
      .setName(request.mandatoryParam(PARAM_NAME));
    return builder.build();
  }

  private CreateWsResponse buildResponse(QProfileResult result, OrganizationDto organization) {
    String language = result.profile().getLanguage();
    CreateWsResponse.QualityProfile.Builder builder = CreateWsResponse.QualityProfile.newBuilder()
      .setOrganization(organization.getKey())
      .setKey(result.profile().getKee())
      .setName(result.profile().getName())
      .setLanguage(language)
      .setLanguageName(languages.get(result.profile().getLanguage()).getName())
      .setIsDefault(false)
      .setIsInherited(false);
    if (!result.infos().isEmpty()) {
      builder.getInfosBuilder().addAllInfos(result.infos());
    }
    if (!result.warnings().isEmpty()) {
      builder.getWarningsBuilder().addAllWarnings(result.warnings());
    }
    return CreateWsResponse.newBuilder().setProfile(builder.build()).build();
  }

  private static String getBackupParamName(String importerKey) {
    return String.format(PARAM_BACKUP_FORMAT, importerKey);
  }

  private static class CreateRequest {
    private final String name;
    private final String language;
    private final String organizationKey;

    private CreateRequest(Builder builder) {
      this.name = builder.name;
      this.language = builder.language;
      this.organizationKey = builder.organizationKey;
    }

    public String getLanguage() {
      return language;
    }

    public String getName() {
      return name;
    }

    public String getOrganizationKey() {
      return organizationKey;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  private static class Builder {
    private String language;
    private String name;
    private String organizationKey;

    private Builder() {
      // enforce factory method use
    }

    public Builder setOrganizationKey(@Nullable String organizationKey) {
      this.organizationKey = organizationKey;
      return this;
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
      checkArgument(organizationKey == null || !organizationKey.isEmpty(), "Organization key may be either null or not empty. Empty organization key is invalid.");
      return new CreateRequest(this);
    }
  }
}
