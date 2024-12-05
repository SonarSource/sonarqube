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
package org.sonar.server.hotspot.ws;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Paging;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ProjectAndBranch;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.hotspot.ws.HotspotWsResponseFormatter.SearchResponseData;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueQuery;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Hotspots;
import org.sonarqube.ws.Hotspots.SearchWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.api.issue.Issue.SECURITY_HOTSPOT_RESOLUTIONS;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.utils.DateUtils.longToDate;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_INSECURE_INTERACTION;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_POROUS_DEFENSES;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_RISKY_RESOURCE;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements HotspotsWsAction {
  private static final Set<String> SUPPORTED_QUALIFIERS = Set.of(ComponentQualifiers.PROJECT, ComponentQualifiers.APP);
  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_PROJECT_KEY = "projectKey";
  private static final String PARAM_STATUS = "status";
  private static final String PARAM_RESOLUTION = "resolution";
  private static final String PARAM_HOTSPOTS = "hotspots";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_PULL_REQUEST = "pullRequest";
  private static final String PARAM_IN_NEW_CODE_PERIOD = "inNewCodePeriod";
  private static final String PARAM_ONLY_MINE = "onlyMine";
  private static final String PARAM_OWASP_ASVS_LEVEL = "owaspAsvsLevel";
  private static final String PARAM_PCI_DSS_32 = "pciDss-3.2";
  private static final String PARAM_PCI_DSS_40 = "pciDss-4.0";
  private static final String PARAM_OWASP_ASVS_40 = "owaspAsvs-4.0";
  private static final String PARAM_OWASP_TOP_10_2017 = "owaspTop10";
  private static final String PARAM_OWASP_TOP_10_2021 = "owaspTop10-2021";
  private static final String PARAM_STIG_ASD_V5R3 = "stig-ASD_V5R3";
  private static final String PARAM_CASA = "casa";
  /**
   * @deprecated SansTop25 report is outdated, it has been completely deprecated in version 10.0 and will be removed from version 11.0
   */
  @Deprecated(since = "10.0", forRemoval = true)
  private static final String PARAM_SANS_TOP_25 = "sansTop25";
  private static final String PARAM_SONARSOURCE_SECURITY = "sonarsourceSecurity";
  private static final String PARAM_CWE = "cwe";
  private static final String PARAM_FILES = "files";

  private static final List<String> STATUSES = List.of(STATUS_TO_REVIEW, STATUS_REVIEWED);

  private final DbClient dbClient;
  private final UserSession userSession;
  private final IssueIndex issueIndex;
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker;
  private final HotspotWsResponseFormatter responseFormatter;
  private final System2 system2;
  private final ComponentFinder componentFinder;

  public SearchAction(DbClient dbClient, UserSession userSession, IssueIndex issueIndex, IssueIndexSyncProgressChecker issueIndexSyncProgressChecker,
    HotspotWsResponseFormatter responseFormatter, System2 system2, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.issueIndex = issueIndex;
    this.issueIndexSyncProgressChecker = issueIndexSyncProgressChecker;
    this.responseFormatter = responseFormatter;
    this.system2 = system2;
    this.componentFinder = componentFinder;
  }

  private static Set<String> setFromList(@Nullable List<String> list) {
    return list != null ? Set.copyOf(list) : Set.of();
  }

  private static WsRequest toWsRequest(Request request) {
    Set<String> hotspotKeys = setFromList(request.paramAsStrings(PARAM_HOTSPOTS));
    Set<String> pciDss32 = setFromList(request.paramAsStrings(PARAM_PCI_DSS_32));
    Set<String> pciDss40 = setFromList(request.paramAsStrings(PARAM_PCI_DSS_40));
    Set<String> owaspAsvs40 = setFromList(request.paramAsStrings(PARAM_OWASP_ASVS_40));
    Set<String> owasp2017Top10 = setFromList(request.paramAsStrings(PARAM_OWASP_TOP_10_2017));
    Set<String> owasp2021Top10 = setFromList(request.paramAsStrings(PARAM_OWASP_TOP_10_2021));
    Set<String> stigAsdV5R3 = setFromList(request.paramAsStrings(PARAM_STIG_ASD_V5R3));
    Set<String> casa = setFromList(request.paramAsStrings(PARAM_CASA));
    Set<String> sansTop25 = setFromList(request.paramAsStrings(PARAM_SANS_TOP_25));
    Set<String> sonarsourceSecurity = setFromList(request.paramAsStrings(PARAM_SONARSOURCE_SECURITY));
    Set<String> cwes = setFromList(request.paramAsStrings(PARAM_CWE));
    Set<String> files = setFromList(request.paramAsStrings(PARAM_FILES));

    return new WsRequest(
      request.mandatoryParamAsInt(PAGE), request.mandatoryParamAsInt(PAGE_SIZE), request.param(PARAM_PROJECT), request.param(PARAM_BRANCH),
      request.param(PARAM_PULL_REQUEST), hotspotKeys, request.param(PARAM_STATUS), request.param(PARAM_RESOLUTION),
      request.paramAsBoolean(PARAM_IN_NEW_CODE_PERIOD), request.paramAsBoolean(PARAM_ONLY_MINE), request.paramAsInt(PARAM_OWASP_ASVS_LEVEL),
      pciDss32, pciDss40, owaspAsvs40, owasp2017Top10, owasp2021Top10, stigAsdV5R3, casa, sansTop25, sonarsourceSecurity, cwes, files);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    WsRequest wsRequest = toWsRequest(request);
    validateParameters(wsRequest);
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkIfNeedIssueSync(dbSession, wsRequest);
      Optional<ProjectAndBranch> project = getAndValidateProjectOrApplication(dbSession, wsRequest);
      SearchResponseData searchResponseData = searchHotspots(wsRequest, dbSession, project.orElse(null));
      loadComponents(dbSession, searchResponseData);
      writeProtobuf(formatResponse(searchResponseData), request, response);
    }
  }

  private void checkIfNeedIssueSync(DbSession dbSession, WsRequest wsRequest) {
    Optional<String> projectKey = wsRequest.getProjectKey();
    if (projectKey.isPresent()) {
      issueIndexSyncProgressChecker.checkIfComponentNeedIssueSync(dbSession, projectKey.get());
    } else {
      // component keys not provided - asking for global
      issueIndexSyncProgressChecker.checkIfIssueSyncInProgress(dbSession);
    }
  }

  private static void addSecurityStandardFilters(WsRequest wsRequest, IssueQuery.Builder builder) {
    if (!wsRequest.getPciDss32().isEmpty()) {
      builder.pciDss32(wsRequest.getPciDss32());
    }
    if (!wsRequest.getPciDss40().isEmpty()) {
      builder.pciDss40(wsRequest.getPciDss40());
    }
    if (!wsRequest.getOwaspAsvs40().isEmpty()) {
      builder.owaspAsvs40(wsRequest.getOwaspAsvs40());
      wsRequest.getOwaspAsvsLevel().ifPresent(builder::owaspAsvsLevel);
    }
    if (!wsRequest.getOwaspTop10For2017().isEmpty()) {
      builder.owaspTop10(wsRequest.getOwaspTop10For2017());
    }
    if (!wsRequest.getOwaspTop10For2021().isEmpty()) {
      builder.owaspTop10For2021(wsRequest.getOwaspTop10For2021());
    }
    if (!wsRequest.getStigAsdV5R3().isEmpty()) {
      builder.stigAsdR5V3(wsRequest.getStigAsdV5R3());
    }
    if (!wsRequest.getCasa().isEmpty()) {
      builder.casa(wsRequest.getCasa());
    }
    if (!wsRequest.getSansTop25().isEmpty()) {
      builder.sansTop25(wsRequest.getSansTop25());
    }
    if (!wsRequest.getSonarsourceSecurity().isEmpty()) {
      builder.sonarsourceSecurity(wsRequest.getSonarsourceSecurity());
    }
    if (!wsRequest.getCwe().isEmpty()) {
      builder.cwe(wsRequest.getCwe());
    }
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("search")
      .setHandler(this)
      .setDescription("Search for Security Hotpots. <br>"
        + "Requires the 'Browse' permission on the specified project(s). <br>"
        + "For applications, it also requires 'Browse' permission on its child projects. <br>"
        + "When issue indexing is in progress returns 503 service unavailable HTTP code.")
      .setSince("8.1")
      .setChangelog(
        new Change("10.7", format("Added parameter '%s' and '%s'", PARAM_STIG_ASD_V5R3, PARAM_CASA)),
        new Change("10.2", format("Parameter '%s' renamed to '%s'", PARAM_PROJECT_KEY, PARAM_PROJECT)),
        new Change("10.0", "Parameter 'sansTop25' is deprecated"),
        new Change("9.6", "Added parameters 'pciDss-3.2' and 'pciDss-4.0"),
        new Change("9.7", "Hotspot flows in the response may contain a description and a type"),
        new Change("9.7", "Hotspot in the response contain the corresponding ruleKey"),
        new Change("9.8", "Endpoint visibility change from internal to public"),
        new Change("9.8", "Add message formatting to issue and locations response"));

    action.addPagingParams(100);
    action.createParam(PARAM_PROJECT)
      .setDeprecatedKey(PARAM_PROJECT_KEY, "10.2")
      .setDescription(format(
        "Key of the project or application. This parameter is required unless %s is provided.",
        PARAM_HOTSPOTS))
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key. Not available in the community edition.")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id. Not available in the community edition.")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001);
    action.createParam(PARAM_HOTSPOTS)
      .setDescription(format(
        "Comma-separated list of Security Hotspot keys. This parameter is required unless %s is provided.",
        PARAM_PROJECT))
      .setExampleValue("AWhXpLoInp4On-Y3xc8x");
    action.createParam(PARAM_STATUS)
      .setDescription("If '%s' is provided, only Security Hotspots with the specified status are returned.", PARAM_PROJECT)
      .setPossibleValues(STATUSES)
      .setRequired(false);
    action.createParam(PARAM_RESOLUTION)
      .setDescription(format(
        "If '%s' is provided and if status is '%s', only Security Hotspots with the specified resolution are returned.",
        PARAM_PROJECT, STATUS_REVIEWED))
      .setPossibleValues(SECURITY_HOTSPOT_RESOLUTIONS)
      .setRequired(false);
    action.createParam(PARAM_IN_NEW_CODE_PERIOD)
      .setDescription("If '%s' is provided, only Security Hotspots created in the new code period are returned.", PARAM_IN_NEW_CODE_PERIOD)
      .setBooleanPossibleValues()
      .setDefaultValue("false")
      .setSince("9.5");
    createSecurityStandardParams(action);
    action.createParam(PARAM_FILES)
      .setDescription("Comma-separated list of files. Returns only hotspots found in those files")
      .setExampleValue("src/main/java/org/sonar/server/Test.java")
      .setSince("9.0");

    action.setResponseExample(getClass().getResource("search-example.json"));
  }

  private static void createSecurityStandardParams(WebService.NewAction action) {
    action.createParam(PARAM_OWASP_ASVS_LEVEL)
      .setDescription("Filters hotspots with lower or equal OWASP ASVS level to the parameter value. Should be used in combination with the 'owaspAsvs-4.0' parameter.")
      .setSince("9.7")
      .setPossibleValues(1, 2, 3)
      .setRequired(false)
      .setExampleValue("2");
    action.createParam(PARAM_PCI_DSS_32)
      .setDescription("Comma-separated list of PCI DSS v3.2 categories.")
      .setSince("9.6")
      .setExampleValue("4,6.5.8,10.1");
    action.createParam(PARAM_PCI_DSS_40)
      .setDescription("Comma-separated list of PCI DSS v4.0 categories.")
      .setSince("9.6")
      .setExampleValue("4,6.5.8,10.1");
    action.createParam(PARAM_OWASP_ASVS_40)
      .setDescription("Comma-separated list of OWASP ASVS v4.0 categories or rules.")
      .setSince("9.7")
      .setExampleValue("6,6.1.2");
    action.createParam(PARAM_ONLY_MINE)
      .setDescription("If 'projectKey' is provided, returns only Security Hotspots assigned to the current user")
      .setBooleanPossibleValues()
      .setRequired(false);
    action.createParam(PARAM_OWASP_TOP_10_2017)
      .setDescription("Comma-separated list of OWASP 2017 Top 10 lowercase categories.")
      .setSince("8.6")
      .setPossibleValues("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10");
    action.createParam(PARAM_OWASP_TOP_10_2021)
      .setDescription("Comma-separated list of OWASP 2021 Top 10 lowercase categories.")
      .setSince("9.4")
      .setPossibleValues("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10");
    action.createParam(PARAM_STIG_ASD_V5R3)
      .setDescription("Comma-separated list of STIG V5R3 lowercase categories.")
      .setSince("10.7");
    action.createParam(PARAM_CASA)
      .setDescription("Comma-separated list of CASA categories.")
      .setSince("10.7");
    action.createParam(PARAM_SANS_TOP_25)
      .setDescription("Comma-separated list of SANS Top 25 categories.")
      .setDeprecatedSince("10.0")
      .setSince("8.6")
      .setPossibleValues(SANS_TOP_25_INSECURE_INTERACTION, SANS_TOP_25_RISKY_RESOURCE, SANS_TOP_25_POROUS_DEFENSES);
    action.createParam(PARAM_SONARSOURCE_SECURITY)
      .setDescription("Comma-separated list of SonarSource security categories. Use '" + SecurityStandards.SQCategory.OTHERS.getKey() +
        "' to select issues not associated with any category")
      .setSince("8.6")
      .setPossibleValues(Arrays.stream(SecurityStandards.SQCategory.values()).map(SecurityStandards.SQCategory::getKey).toList());
    action.createParam(PARAM_CWE)
      .setDescription("Comma-separated list of CWE numbers")
      .setExampleValue("89,434,352")
      .setSince("8.8");
  }

  private Optional<ProjectAndBranch> getAndValidateProjectOrApplication(DbSession dbSession, WsRequest wsRequest) {
    return wsRequest.getProjectKey().map(projectKey -> {
      ProjectAndBranch appOrProjectAndBranch = componentFinder.getAppOrProjectAndBranch(dbSession, projectKey, wsRequest.getBranch().orElse(null),
        wsRequest.getPullRequest().orElse(null));

      if (!SUPPORTED_QUALIFIERS.contains(appOrProjectAndBranch.getProject().getQualifier())) {
        throw new NotFoundException(format("Project '%s' not found", projectKey));
      }
      userSession.checkEntityPermission(USER, appOrProjectAndBranch.getProject());
      userSession.checkChildProjectsPermission(USER, appOrProjectAndBranch.getProject());
      return appOrProjectAndBranch;
    });
  }

  private SearchResponseData searchHotspots(WsRequest wsRequest, DbSession dbSession, @Nullable ProjectAndBranch projectorApp) {
    SearchResponse result = doIndexSearch(wsRequest, dbSession, projectorApp);
    result.getHits();
    List<String> issueKeys = Arrays.stream(result.getHits().getHits())
      .map(SearchHit::getId)
      .toList();

    List<IssueDto> hotspots = toIssueDtos(dbSession, issueKeys);

    Paging paging = forPageIndex(wsRequest.getPage()).withPageSize(wsRequest.getIndex()).andTotal((int) getTotalHits(result).value);
    return new SearchResponseData(paging, hotspots);
  }

  private static TotalHits getTotalHits(SearchResponse response) {
    return ofNullable(response.getHits().getTotalHits()).orElseThrow(() -> new IllegalStateException("Could not get total hits of search results"));
  }

  private List<IssueDto> toIssueDtos(DbSession dbSession, List<String> issueKeys) {
    List<IssueDto> unorderedHotspots = dbClient.issueDao().selectByKeys(dbSession, issueKeys);
    Map<String, IssueDto> hotspotsByKey = unorderedHotspots
      .stream()
      .collect(Collectors.toMap(IssueDto::getKey, Function.identity()));

    return issueKeys.stream()
      .map(hotspotsByKey::get)
      .filter(Objects::nonNull)
      .toList();
  }

  private SearchResponse doIndexSearch(WsRequest wsRequest, DbSession dbSession, @Nullable ProjectAndBranch projectOrAppAndBranch) {
    var builder = IssueQuery.builder()
      .types(singleton(RuleType.SECURITY_HOTSPOT.name()))
      .sort(IssueQuery.SORT_HOTSPOTS)
      .asc(true)
      .statuses(wsRequest.getStatus().map(Collections::singletonList).orElse(STATUSES));

    if (projectOrAppAndBranch != null) {
      ProjectDto projectOrApp = projectOrAppAndBranch.getProject();
      BranchDto projectOrAppBranch = projectOrAppAndBranch.getBranch();

      if (ComponentQualifiers.APP.equals(projectOrApp.getQualifier())) {
        builder.viewUuids(singletonList(projectOrAppBranch.getUuid()));
        if (wsRequest.isInNewCodePeriod() && wsRequest.getPullRequest().isEmpty()) {
          addInNewCodePeriodFilterByProjects(builder, dbSession, projectOrAppBranch);
        }
      } else {
        builder.projectUuids(singletonList(projectOrApp.getUuid()));
        if (wsRequest.isInNewCodePeriod() && wsRequest.getPullRequest().isEmpty()) {
          addInNewCodePeriodFilter(dbSession, projectOrAppBranch, builder);
        }
      }

      addMainBranchFilter(projectOrAppAndBranch.getBranch(), builder);
    }

    Set<String> hotspotKeys = wsRequest.getHotspotKeys();
    if (!hotspotKeys.isEmpty()) {
      checkArgument(hotspotKeys.size() <= MAX_PAGE_SIZE, "Number of issue keys must be less than " + MAX_PAGE_SIZE + " (got " + hotspotKeys.size() + ")");
      builder.issueKeys(hotspotKeys);
    }

    if (!wsRequest.getFiles().isEmpty()) {
      builder.files(wsRequest.getFiles());
    }

    if (wsRequest.isOnlyMine()) {
      userSession.checkLoggedIn();
      builder.assigneeUuids(Collections.singletonList(userSession.getUuid()));
    }

    wsRequest.getStatus().ifPresent(status -> builder.resolved(STATUS_REVIEWED.equals(status)));
    wsRequest.getResolution().ifPresent(resolution -> builder.resolutions(singleton(resolution)));
    addSecurityStandardFilters(wsRequest, builder);

    IssueQuery query = builder.build();
    SearchOptions searchOptions = new SearchOptions()
      .setPage(wsRequest.page, wsRequest.index);
    return issueIndex.search(query, searchOptions);
  }

  private void validateParameters(WsRequest wsRequest) {
    Optional<String> projectKey = wsRequest.getProjectKey();
    Optional<String> branch = wsRequest.getBranch();
    Optional<String> pullRequest = wsRequest.getPullRequest();

    Set<String> hotspotKeys = wsRequest.getHotspotKeys();
    checkArgument(
      projectKey.isPresent() || !hotspotKeys.isEmpty(),
      "A value must be provided for either parameter '%s' or parameter '%s'", PARAM_PROJECT, PARAM_HOTSPOTS);

    checkArgument(
      branch.isEmpty() || projectKey.isPresent(),
      "Parameter '%s' must be used with parameter '%s'", PARAM_BRANCH, PARAM_PROJECT);
    checkArgument(
      pullRequest.isEmpty() || projectKey.isPresent(),
      "Parameter '%s' must be used with parameter '%s'", PARAM_PULL_REQUEST, PARAM_PROJECT);
    checkArgument(
      !(branch.isPresent() && pullRequest.isPresent()),
      "Only one of parameters '%s' and '%s' can be provided", PARAM_BRANCH, PARAM_PULL_REQUEST);

    Optional<String> status = wsRequest.getStatus();
    Optional<String> resolution = wsRequest.getResolution();
    checkArgument(status.isEmpty() || hotspotKeys.isEmpty(),
      "Parameter '%s' can't be used with parameter '%s'", PARAM_STATUS, PARAM_HOTSPOTS);
    checkArgument(resolution.isEmpty() || hotspotKeys.isEmpty(),
      "Parameter '%s' can't be used with parameter '%s'", PARAM_RESOLUTION, PARAM_HOTSPOTS);

    resolution.ifPresent(
      r -> checkArgument(status.filter(STATUS_REVIEWED::equals).isPresent(),
        "Value '%s' of parameter '%s' can only be provided if value of parameter '%s' is '%s'",
        r, PARAM_RESOLUTION, PARAM_STATUS, STATUS_REVIEWED));

    if (wsRequest.isOnlyMine()) {
      checkArgument(userSession.isLoggedIn(),
        "Parameter '%s' requires user to be logged in", PARAM_ONLY_MINE);
      checkArgument(wsRequest.getProjectKey().isPresent(),
        "Parameter '%s' can be used with parameter '%s' only", PARAM_ONLY_MINE, PARAM_PROJECT);
    }
  }

  private static void addMainBranchFilter(@NotNull BranchDto branch, IssueQuery.Builder builder) {
    if (branch.isMain()) {
      builder.mainBranch(true);
    } else {
      builder.branchUuid(branch.getUuid());
      builder.mainBranch(false);
    }
  }

  private void addInNewCodePeriodFilter(DbSession dbSession, @NotNull BranchDto projectBranch, IssueQuery.Builder builder) {
    Optional<SnapshotDto> snapshot = dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, projectBranch.getUuid());

    boolean isLastAnalysisUsingReferenceBranch = snapshot.map(SnapshotDto::getPeriodMode)
      .orElse("").equals(REFERENCE_BRANCH.name());

    if (isLastAnalysisUsingReferenceBranch) {
      builder.newCodeOnReference(true);
    } else {
      var sinceDate = snapshot
        .map(s -> longToDate(s.getPeriodDate()))
        .orElseGet(() -> new Date(system2.now()));

      builder.createdAfter(sinceDate, false);
    }
  }

  private void addInNewCodePeriodFilterByProjects(IssueQuery.Builder builder, DbSession dbSession, BranchDto appBranch) {
    Set<String> branchUuids;

    if (appBranch.isMain()) {
      branchUuids = dbClient.applicationProjectsDao().selectProjectsMainBranchesOfApplication(dbSession, appBranch.getProjectUuid()).stream()
        .map(BranchDto::getUuid)
        .collect(Collectors.toSet());
    } else {
      branchUuids = dbClient.applicationProjectsDao().selectProjectBranchesFromAppBranchUuid(dbSession, appBranch.getUuid()).stream()
        .map(BranchDto::getUuid)
        .collect(Collectors.toSet());
    }

    long now = system2.now();

    List<SnapshotDto> snapshots = dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(dbSession, branchUuids);

    Set<String> newCodeReferenceByProjects = snapshots
      .stream()
      .filter(s -> !isNullOrEmpty(s.getPeriodMode()) && s.getPeriodMode().equals(REFERENCE_BRANCH.name()))
      .map(SnapshotDto::getRootComponentUuid)
      .collect(Collectors.toSet());

    Map<String, IssueQuery.PeriodStart> leakByProjects = snapshots
      .stream()
      .filter(s -> isNullOrEmpty(s.getPeriodMode()) || !s.getPeriodMode().equals(REFERENCE_BRANCH.name()))
      .collect(Collectors.toMap(SnapshotDto::getRootComponentUuid, s1 -> new IssueQuery.PeriodStart(longToDate(s1.getPeriodDate() == null ? now : s1.getPeriodDate()), false)));

    builder.createdAfterByProjectUuids(leakByProjects);
    builder.newCodeOnReferenceByProjectUuids(newCodeReferenceByProjects);
  }

  private void loadComponents(DbSession dbSession, SearchResponseData searchResponseData) {
    Set<String> componentUuids = searchResponseData.getHotspots().stream()
      .flatMap(hotspot -> Stream.of(hotspot.getComponentUuid(), hotspot.getProjectUuid()))
      .collect(Collectors.toSet());

    Set<String> locationComponentUuids = searchResponseData.getHotspots()
      .stream()
      .flatMap(hotspot -> getHotspotLocationComponentUuids(hotspot).stream())
      .collect(Collectors.toSet());

    Set<String> aggregatedComponentUuids = Stream.of(componentUuids, locationComponentUuids)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());

    if (!aggregatedComponentUuids.isEmpty()) {
      List<ComponentDto> componentDtos = dbClient.componentDao().selectByUuids(dbSession, aggregatedComponentUuids);
      searchResponseData.addComponents(componentDtos);

      Set<String> branchUuids = componentDtos.stream().map(c -> c.getCopyComponentUuid() != null ? c.getCopyComponentUuid() : c.branchUuid()).collect(Collectors.toSet());
      List<BranchDto> branchDtos = dbClient.branchDao().selectByUuids(dbSession, branchUuids);
      searchResponseData.addBranches(branchDtos);
    }
  }

  private static Set<String> getHotspotLocationComponentUuids(IssueDto hotspot) {
    Set<String> locationComponentUuids = new HashSet<>();
    DbIssues.Locations locations = hotspot.parseLocations();

    if (locations == null) {
      return locationComponentUuids;
    }

    List<DbIssues.Flow> flows = locations.getFlowList();

    for (DbIssues.Flow flow : flows) {
      List<DbIssues.Location> flowLocations = flow.getLocationList();
      for (DbIssues.Location location : flowLocations) {
        if (location.hasComponentId()) {
          locationComponentUuids.add(location.getComponentId());
        }
      }
    }

    return locationComponentUuids;
  }

  private SearchWsResponse formatResponse(SearchResponseData searchResponseData) {
    SearchWsResponse.Builder responseBuilder = SearchWsResponse.newBuilder();
    formatPaging(searchResponseData, responseBuilder);
    if (searchResponseData.isPresent()) {
      responseFormatter.formatHotspots(searchResponseData, responseBuilder);
      formatComponents(searchResponseData, responseBuilder);
    }
    return responseBuilder.build();
  }

  private static void formatPaging(SearchResponseData searchResponseData, SearchWsResponse.Builder responseBuilder) {
    Paging paging = searchResponseData.getPaging();
    Common.Paging.Builder pagingBuilder = Common.Paging.newBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total());

    responseBuilder.setPaging(pagingBuilder.build());
  }

  private void formatComponents(SearchResponseData searchResponseData, SearchWsResponse.Builder responseBuilder) {
    Collection<ComponentDto> components = searchResponseData.getComponents();
    if (components.isEmpty()) {
      return;
    }

    Hotspots.Component.Builder builder = Hotspots.Component.newBuilder();
    for (ComponentDto component : components) {
      String branchUuid = component.getCopyComponentUuid() != null ? component.getCopyComponentUuid() : component.branchUuid();
      BranchDto branchDto = searchResponseData.getBranch(branchUuid);
      if (branchDto == null && component.getCopyComponentUuid() == null) {
        throw new IllegalStateException("Could not find a branch for a component " + component.getKey() + " with uuid " + component.uuid());
      }
      responseBuilder.addComponents(responseFormatter.formatComponent(builder, component, branchDto));
    }
  }

  private static final class WsRequest {
    private final int page;
    private final int index;
    private final String projectKey;
    private final String branch;
    private final String pullRequest;
    private final Set<String> hotspotKeys;
    private final String status;
    private final String resolution;
    private final boolean inNewCodePeriod;
    private final boolean onlyMine;
    private final Integer owaspAsvsLevel;
    private final Set<String> pciDss32;
    private final Set<String> pciDss40;
    private final Set<String> owaspAsvs40;
    private final Set<String> owaspTop10For2017;
    private final Set<String> owaspTop10For2021;
    private final Set<String> stigAsdV5R3;
    private final Set<String> casa;
    private final Set<String> sansTop25;
    private final Set<String> sonarsourceSecurity;
    private final Set<String> cwe;
    private final Set<String> files;

    private WsRequest(int page, int index,
      @Nullable String projectKey, @Nullable String branch, @Nullable String pullRequest, Set<String> hotspotKeys,
      @Nullable String status, @Nullable String resolution, @Nullable Boolean inNewCodePeriod, @Nullable Boolean onlyMine,
      @Nullable Integer owaspAsvsLevel, Set<String> pciDss32, Set<String> pciDss40, Set<String> owaspAsvs40,
      Set<String> owaspTop10For2017, Set<String> owaspTop10For2021, Set<String> stigAsdV5R3, Set<String> casa, Set<String> sansTop25, Set<String> sonarsourceSecurity,
      Set<String> cwe, @Nullable Set<String> files) {
      this.page = page;
      this.index = index;
      this.projectKey = projectKey;
      this.branch = branch;
      this.pullRequest = pullRequest;
      this.hotspotKeys = hotspotKeys;
      this.status = status;
      this.resolution = resolution;
      this.inNewCodePeriod = inNewCodePeriod != null && inNewCodePeriod;
      this.onlyMine = onlyMine != null && onlyMine;
      this.owaspAsvsLevel = owaspAsvsLevel;
      this.pciDss32 = pciDss32;
      this.pciDss40 = pciDss40;
      this.owaspAsvs40 = owaspAsvs40;
      this.owaspTop10For2017 = owaspTop10For2017;
      this.owaspTop10For2021 = owaspTop10For2021;
      this.stigAsdV5R3 = stigAsdV5R3;
      this.casa = casa;
      this.sansTop25 = sansTop25;
      this.sonarsourceSecurity = sonarsourceSecurity;
      this.cwe = cwe;
      this.files = files;
    }

    int getPage() {
      return page;
    }

    int getIndex() {
      return index;
    }

    Optional<String> getProjectKey() {
      return ofNullable(projectKey);
    }

    Optional<String> getBranch() {
      return ofNullable(branch);
    }

    Optional<String> getPullRequest() {
      return ofNullable(pullRequest);
    }

    Set<String> getHotspotKeys() {
      return hotspotKeys;
    }

    Optional<String> getStatus() {
      return ofNullable(status);
    }

    Optional<String> getResolution() {
      return ofNullable(resolution);
    }

    boolean isInNewCodePeriod() {
      return inNewCodePeriod;
    }

    boolean isOnlyMine() {
      return onlyMine;
    }

    public Optional<Integer> getOwaspAsvsLevel() {
      return ofNullable(owaspAsvsLevel);
    }

    public Set<String> getPciDss32() {
      return pciDss32;
    }

    public Set<String> getPciDss40() {
      return pciDss40;
    }

    public Set<String> getOwaspAsvs40() {
      return owaspAsvs40;
    }

    public Set<String> getOwaspTop10For2017() {
      return owaspTop10For2017;
    }

    public Set<String> getOwaspTop10For2021() {
      return owaspTop10For2021;
    }

    public Set<String> getStigAsdV5R3() {
      return stigAsdV5R3;
    }

    public Set<String> getCasa() {
      return casa;
    }

    public Set<String> getSansTop25() {
      return sansTop25;
    }

    public Set<String> getSonarsourceSecurity() {
      return sonarsourceSecurity;
    }

    public Set<String> getCwe() {
      return cwe;
    }

    public Set<String> getFiles() {
      return files;
    }
  }
}
