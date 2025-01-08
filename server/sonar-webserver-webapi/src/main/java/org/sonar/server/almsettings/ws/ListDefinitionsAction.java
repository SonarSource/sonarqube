/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.almsettings.ws;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmSettings.AlmSettingBitbucketCloud;
import org.sonarqube.ws.AlmSettings.AlmSettingGithub;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.AlmSettings.AlmSettingAzure;
import static org.sonarqube.ws.AlmSettings.AlmSettingBitbucket;
import static org.sonarqube.ws.AlmSettings.AlmSettingGitlab;
import static org.sonarqube.ws.AlmSettings.ListDefinitionsWsResponse;

public class ListDefinitionsAction implements AlmSettingsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;

  public ListDefinitionsAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("list_definitions")
      .setDescription("List DevOps Platform Settings, sorted by created date.<br/>" +
        "Requires the 'Administer System' permission")
      .setSince("8.1")
      .setResponseExample(getClass().getResource("list_definitions-example.json"))
      .setChangelog(new Change("8.2", "Field 'URL' added for GitLab definitions"),
        new Change("8.6", "Field 'URL' added for Azure definitions"),
        new Change("8.7", "Fields 'personalAccessToken', 'privateKey', and 'clientSecret' are no longer returned"))
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-list_definitions.json"));
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkIsSystemAdministrator();
    ListDefinitionsWsResponse wsResponse = doHandle();
    writeProtobuf(wsResponse, request, response);
  }

  private ListDefinitionsWsResponse doHandle() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<AlmSettingDto> settings = dbClient.almSettingDao().selectAll(dbSession);
      Map<ALM, List<AlmSettingDto>> settingsByAlm = settings.stream().collect(Collectors.groupingBy(AlmSettingDto::getAlm));
      List<AlmSettingGithub> githubSettings = settingsByAlm.getOrDefault(ALM.GITHUB, emptyList())
        .stream()
        .sorted(Comparator.comparing(AlmSettingDto::getCreatedAt))
        .map(ListDefinitionsAction::toGitHub).toList();
      List<AlmSettingAzure> azureSettings = settingsByAlm.getOrDefault(ALM.AZURE_DEVOPS, emptyList())
        .stream()
        .sorted(Comparator.comparing(AlmSettingDto::getCreatedAt))
        .map(ListDefinitionsAction::toAzure).toList();
      List<AlmSettingBitbucket> bitbucketSettings = settingsByAlm.getOrDefault(ALM.BITBUCKET, emptyList())
        .stream()
        .sorted(Comparator.comparing(AlmSettingDto::getCreatedAt))
        .map(ListDefinitionsAction::toBitbucket).toList();
      List<AlmSettingBitbucketCloud> bitbucketCloudSettings = settingsByAlm.getOrDefault(ALM.BITBUCKET_CLOUD, emptyList())
        .stream()
        .sorted(Comparator.comparing(AlmSettingDto::getCreatedAt))
        .map(ListDefinitionsAction::toBitbucketCloud).toList();
      List<AlmSettingGitlab> gitlabSettings = settingsByAlm.getOrDefault(ALM.GITLAB, emptyList())
        .stream()
        .sorted(Comparator.comparing(AlmSettingDto::getCreatedAt))
        .map(ListDefinitionsAction::toGitlab).toList();
      return ListDefinitionsWsResponse.newBuilder()
        .addAllGithub(githubSettings)
        .addAllAzure(azureSettings)
        .addAllBitbucket(bitbucketSettings)
        .addAllBitbucketcloud(bitbucketCloudSettings)
        .addAllGitlab(gitlabSettings)
        .build();
    }
  }

  private static AlmSettingGithub toGitHub(AlmSettingDto settingDto) {
    AlmSettingGithub.Builder builder = AlmSettingGithub
      .newBuilder()
      .setKey(settingDto.getKey())
      .setUrl(requireNonNull(settingDto.getUrl(), "URL cannot be null for GitHub setting"))
      .setAppId(requireNonNull(settingDto.getAppId(), "App ID cannot be null for GitHub setting"));
    // Don't fail if clientId is not set for migration cases
    Optional.ofNullable(settingDto.getClientId()).ifPresent(builder::setClientId);
    return builder.build();
  }

  private static AlmSettingAzure toAzure(AlmSettingDto settingDto) {
    AlmSettingAzure.Builder builder = AlmSettingAzure
      .newBuilder()
      .setKey(settingDto.getKey());

    if (settingDto.getUrl() != null) {
      builder.setUrl(settingDto.getUrl());
    }

    return builder.build();
  }

  private static AlmSettingGitlab toGitlab(AlmSettingDto settingDto) {
    AlmSettingGitlab.Builder builder = AlmSettingGitlab.newBuilder()
      .setKey(settingDto.getKey());

    if (settingDto.getUrl() != null) {
      builder.setUrl(settingDto.getUrl());
    }
    return builder.build();
  }

  private static AlmSettingBitbucket toBitbucket(AlmSettingDto settingDto) {
    return AlmSettingBitbucket
      .newBuilder()
      .setKey(settingDto.getKey())
      .setUrl(requireNonNull(settingDto.getUrl(), "URL cannot be null for Bitbucket setting"))
      .build();
  }

  private static AlmSettingBitbucketCloud toBitbucketCloud(AlmSettingDto settingDto) {
    AlmSettingBitbucketCloud.Builder builder = AlmSettingBitbucketCloud
      .newBuilder()
      .setKey(settingDto.getKey())
      .setWorkspace(requireNonNull(settingDto.getAppId()))
      .setClientId(requireNonNull(settingDto.getClientId(), "Client ID cannot be null for Bitbucket Cloud setting"));
    return builder.build();
  }
}
