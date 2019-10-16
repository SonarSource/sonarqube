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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmSettings.AlmSettingGithub;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.AlmSettings.AlmSettingAzure;
import static org.sonarqube.ws.AlmSettings.AlmSettingBitbucket;
import static org.sonarqube.ws.AlmSettings.ListDefinitionsWsResponse;

public class ListDefinitionsAction implements AlmSettingsWsAction {

  private final DbClient dbClient;
  private UserSession userSession;

  public ListDefinitionsAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("list_definitions")
      .setDescription("List ALM Settings. <br/>" +
        "Requires the 'Administer System' permission")
      .setSince("8.1")
      .setResponseExample(getClass().getResource("list_definitions-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();
    ListDefinitionsWsResponse wsResponse = doHandle();
    writeProtobuf(wsResponse, request, response);
  }

  private ListDefinitionsWsResponse doHandle() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<AlmSettingDto> settings = dbClient.almSettingDao().selectAll(dbSession);
      Map<ALM, List<AlmSettingDto>> settingsByAlm = settings.stream().collect(Collectors.groupingBy(AlmSettingDto::getAlm));
      List<AlmSettingGithub> githubSettings = settingsByAlm.getOrDefault(ALM.GITHUB, emptyList()).stream()
        .map(ListDefinitionsAction::toGitHub).collect(Collectors.toList());
      List<AlmSettingAzure> azureSettings = settingsByAlm.getOrDefault(ALM.AZURE_DEVOPS, emptyList()).stream()
        .map(ListDefinitionsAction::toAzure).collect(Collectors.toList());
      List<AlmSettingBitbucket> bitbucketSettings = settingsByAlm.getOrDefault(ALM.BITBUCKET, emptyList()).stream()
        .map(ListDefinitionsAction::toBitbucket).collect(Collectors.toList());
      return ListDefinitionsWsResponse.newBuilder()
        .addAllGithub(githubSettings)
        .addAllAzure(azureSettings)
        .addAllBitbucket(bitbucketSettings)
        .build();
    }
  }

  private static AlmSettingGithub toGitHub(AlmSettingDto settingDto) {
    return AlmSettingGithub
      .newBuilder()
      .setKey(settingDto.getKey())
      .setUrl(requireNonNull(settingDto.getUrl(), "URL cannot be null for GitHub ALM setting"))
      .setAppId(requireNonNull(settingDto.getAppId(), "App ID cannot be null for GitHub ALM setting"))
      .setPrivateKey(requireNonNull(settingDto.getPrivateKey(), "Private Key cannot be null for GitHub ALM setting"))
      .build();
  }

  private static AlmSettingAzure toAzure(AlmSettingDto settingDto) {
    return AlmSettingAzure
      .newBuilder()
      .setKey(settingDto.getKey())
      .setPersonalAccessToken(requireNonNull(settingDto.getPersonalAccessToken(), "Personal Access Token cannot be null for Azure ALM setting"))
      .build();
  }

  private static AlmSettingBitbucket toBitbucket(AlmSettingDto settingDto) {
    return AlmSettingBitbucket
      .newBuilder()
      .setKey(settingDto.getKey())
      .setUrl(requireNonNull(settingDto.getUrl(), "URL cannot be null for Bitbucket ALM setting"))
      .setPersonalAccessToken(requireNonNull(settingDto.getPersonalAccessToken(), "Personal Access Token cannot be null for Bitbucket ALM setting"))
      .build();
  }
}
