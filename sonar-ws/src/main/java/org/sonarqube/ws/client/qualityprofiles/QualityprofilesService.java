/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.ws.client.qualityprofiles;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.Qualityprofiles.CopyWsResponse;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse;
import org.sonarqube.ws.Qualityprofiles.InheritanceWsResponse;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse;
import org.sonarqube.ws.Qualityprofiles.SearchGroupsResponse;
import org.sonarqube.ws.Qualityprofiles.SearchUsersResponse;
import org.sonarqube.ws.Qualityprofiles.ShowResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class QualityprofilesService extends BaseService {

  public QualityprofilesService(WsConnector wsConnector) {
    super(wsConnector, "api/qualityprofiles");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/activate_rule">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public void activateRule(ActivateRuleRequest request) {
    call(
      new PostRequest(path("activate_rule"))
        .setParam("key", request.getKey())
        .setParam("params", request.getParams() == null ? null : request.getParams().stream().collect(Collectors.joining(",")))
        .setParam("reset", request.getReset())
        .setParam("rule", request.getRule())
        .setParam("severity", request.getSeverity())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/activate_rules">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public void activateRules(ActivateRulesRequest request) {
    call(
      new PostRequest(path("activate_rules"))
        .setParam("activation", request.getActivation())
        .setParam("active_severities", request.getActiveSeverities() == null ? null : request.getActiveSeverities().stream().collect(Collectors.joining(",")))
        .setParam("asc", request.getAsc())
        .setParam("available_since", request.getAvailableSince())
        .setParam("compareToProfile", request.getCompareToProfile())
        .setParam("inheritance", request.getInheritance() == null ? null : request.getInheritance().stream().collect(Collectors.joining(",")))
        .setParam("is_template", request.getIsTemplate())
        .setParam("languages", request.getLanguages() == null ? null : request.getLanguages().stream().collect(Collectors.joining(",")))
        .setParam("organization", request.getOrganization())
        .setParam("q", request.getQ())
        .setParam("qprofile", request.getQprofile())
        .setParam("repositories", request.getRepositories() == null ? null : request.getRepositories().stream().collect(Collectors.joining(",")))
        .setParam("rule_key", request.getRuleKey())
        .setParam("s", request.getS())
        .setParam("severities", request.getSeverities() == null ? null : request.getSeverities().stream().collect(Collectors.joining(",")))
        .setParam("statuses", request.getStatuses() == null ? null : request.getStatuses().stream().collect(Collectors.joining(",")))
        .setParam("tags", request.getTags() == null ? null : request.getTags().stream().collect(Collectors.joining(",")))
        .setParam("targetKey", request.getTargetKey())
        .setParam("targetSeverity", request.getTargetSeverity())
        .setParam("template_key", request.getTemplateKey())
        .setParam("types", request.getTypes() == null ? null : request.getTypes().stream().collect(Collectors.joining(",")))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/add_group">Further information about this action online (including a response example)</a>
   * @since 6.6
   */
  public void addGroup(AddGroupRequest request) {
    call(
      new PostRequest(path("add_group"))
        .setParam("group", request.getGroup())
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("qualityProfile", request.getQualityProfile())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/add_project">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void addProject(AddProjectRequest request) {
    call(
      new PostRequest(path("add_project"))
        .setParam("key", request.getKey())
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("project", request.getProject())
        .setParam("projectUuid", request.getProjectUuid())
        .setParam("qualityProfile", request.getQualityProfile())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/add_user">Further information about this action online (including a response example)</a>
   * @since 6.6
   */
  public void addUser(AddUserRequest request) {
    call(
      new PostRequest(path("add_user"))
        .setParam("language", request.getLanguage())
        .setParam("login", request.getLogin())
        .setParam("organization", request.getOrganization())
        .setParam("qualityProfile", request.getQualityProfile())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/backup">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String backup(BackupRequest request) {
    return call(
      new GetRequest(path("backup"))
        .setParam("key", request.getKey())
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("qualityProfile", request.getQualityProfile())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/change_parent">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void changeParent(ChangeParentRequest request) {
    call(
      new PostRequest(path("change_parent"))
        .setParam("key", request.getKey())
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("parentKey", request.getParentKey())
        .setParam("parentQualityProfile", request.getParentQualityProfile())
        .setParam("qualityProfile", request.getQualityProfile())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/changelog">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String changelog(ChangelogRequest request) {
    return call(
      new GetRequest(path("changelog"))
        .setParam("key", request.getKey())
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("qualityProfile", request.getQualityProfile())
        .setParam("since", request.getSince())
        .setParam("to", request.getTo())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/compare">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String compare(CompareRequest request) {
    return call(
      new GetRequest(path("compare"))
        .setParam("leftKey", request.getLeftKey())
        .setParam("rightKey", request.getRightKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/copy">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public CopyWsResponse copy(CopyRequest request) {
    return call(
      new PostRequest(path("copy"))
        .setParam("fromKey", request.getFromKey())
        .setParam("toName", request.getToName()),
      CopyWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/create">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public CreateWsResponse create(CreateRequest request) {
    return call(
      new PostRequest(path("create"))
        .setParam("language", request.getLanguage())
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization()),
      CreateWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/deactivate_rule">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public void deactivateRule(DeactivateRuleRequest request) {
    call(
      new PostRequest(path("deactivate_rule"))
        .setParam("key", request.getKey())
        .setParam("rule", request.getRule())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/deactivate_rules">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public void deactivateRules(DeactivateRulesRequest request) {
    call(
      new PostRequest(path("deactivate_rules"))
        .setParam("activation", request.getActivation())
        .setParam("active_severities", request.getActiveSeverities() == null ? null : request.getActiveSeverities().stream().collect(Collectors.joining(",")))
        .setParam("asc", request.getAsc())
        .setParam("available_since", request.getAvailableSince())
        .setParam("compareToProfile", request.getCompareToProfile())
        .setParam("inheritance", request.getInheritance() == null ? null : request.getInheritance().stream().collect(Collectors.joining(",")))
        .setParam("is_template", request.getIsTemplate())
        .setParam("languages", request.getLanguages() == null ? null : request.getLanguages().stream().collect(Collectors.joining(",")))
        .setParam("organization", request.getOrganization())
        .setParam("q", request.getQ())
        .setParam("qprofile", request.getQprofile())
        .setParam("repositories", request.getRepositories() == null ? null : request.getRepositories().stream().collect(Collectors.joining(",")))
        .setParam("rule_key", request.getRuleKey())
        .setParam("s", request.getS())
        .setParam("severities", request.getSeverities() == null ? null : request.getSeverities().stream().collect(Collectors.joining(",")))
        .setParam("statuses", request.getStatuses() == null ? null : request.getStatuses().stream().collect(Collectors.joining(",")))
        .setParam("tags", request.getTags() == null ? null : request.getTags().stream().collect(Collectors.joining(",")))
        .setParam("targetKey", request.getTargetKey())
        .setParam("template_key", request.getTemplateKey())
        .setParam("types", request.getTypes() == null ? null : request.getTypes().stream().collect(Collectors.joining(",")))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/delete">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void delete(DeleteRequest request) {
    call(
      new PostRequest(path("delete"))
        .setParam("key", request.getKey())
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("qualityProfile", request.getQualityProfile())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/export">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String export(ExportRequest request) {
    return call(
      new GetRequest(path("export"))
        .setParam("key", request.getKey())
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("qualityProfile", request.getQualityProfile())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/exporters">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String exporters() {
    return call(
      new GetRequest(path("exporters"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/importers">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String importers() {
    return call(
      new GetRequest(path("importers"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/inheritance">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public InheritanceWsResponse inheritance(InheritanceRequest request) {
    return call(
      new GetRequest(path("inheritance"))
        .setParam("key", request.getKey())
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("qualityProfile", request.getQualityProfile()),
      InheritanceWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/projects">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String projects(ProjectsRequest request) {
    return call(
      new GetRequest(path("projects"))
        .setParam("key", request.getKey())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("selected", request.getSelected())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/remove_group">Further information about this action online (including a response example)</a>
   * @since 6.6
   */
  public void removeGroup(RemoveGroupRequest request) {
    call(
      new PostRequest(path("remove_group"))
        .setParam("group", request.getGroup())
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("qualityProfile", request.getQualityProfile())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/remove_project">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void removeProject(RemoveProjectRequest request) {
    call(
      new PostRequest(path("remove_project"))
        .setParam("key", request.getKey())
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("project", request.getProject())
        .setParam("projectUuid", request.getProjectUuid())
        .setParam("qualityProfile", request.getQualityProfile())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/remove_user">Further information about this action online (including a response example)</a>
   * @since 6.6
   */
  public void removeUser(RemoveUserRequest request) {
    call(
      new PostRequest(path("remove_user"))
        .setParam("language", request.getLanguage())
        .setParam("login", request.getLogin())
        .setParam("organization", request.getOrganization())
        .setParam("qualityProfile", request.getQualityProfile())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/rename">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void rename(RenameRequest request) {
    call(
      new PostRequest(path("rename"))
        .setParam("key", request.getKey())
        .setParam("name", request.getName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/restore">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void restore(RestoreRequest request) {
    call(
      new PostRequest(path("restore"))
        .setParam("backup", request.getBackup())
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/restore_built_in">Further information about this action online (including a response example)</a>
   * @since 4.4
   * @deprecated since 6.4
   */
  @Deprecated
  public void restoreBuiltIn() {
    call(
      new PostRequest(path("restore_built_in"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/search">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public SearchWsResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("defaults", request.getDefaults())
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("project", request.getProject())
        .setParam("qualityProfile", request.getQualityProfile()),
      SearchWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/search_groups">Further information about this action online (including a response example)</a>
   * @since 6.6
   */
  public SearchGroupsResponse searchGroups(SearchGroupsRequest request) {
    return call(
      new GetRequest(path("search_groups"))
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("qualityProfile", request.getQualityProfile())
        .setParam("selected", request.getSelected()),
      SearchGroupsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/search_users">Further information about this action online (including a response example)</a>
   * @since 6.6
   */
  public SearchUsersResponse searchUsers(SearchUsersRequest request) {
    return call(
      new GetRequest(path("search_users"))
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("qualityProfile", request.getQualityProfile())
        .setParam("selected", request.getSelected()),
      SearchUsersResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/set_default">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void setDefault(SetDefaultRequest request) {
    call(
      new PostRequest(path("set_default"))
        .setParam("key", request.getKey())
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("qualityProfile", request.getQualityProfile())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/show">Further information about this action online (including a response example)</a>
   * @since 6.5
   */
  public ShowResponse show(ShowRequest request) {
    return call(
      new GetRequest(path("show"))
        .setParam("compareToSonarWay", request.getCompareToSonarWay())
        .setParam("key", request.getKey()),
      ShowResponse.parser());
  }
}
