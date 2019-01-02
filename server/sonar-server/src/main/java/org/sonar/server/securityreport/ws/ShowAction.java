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
package org.sonar.server.securityreport.ws;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.SecurityStandardCategoryStatistics;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.SecurityReports;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySortedSet;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES_KEY;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.issue.index.SecurityStandardHelper.UNKNOWN_STANDARD;
import static org.sonar.server.issue.index.SecurityStandardHelper.getCwe;
import static org.sonar.server.issue.index.SecurityStandardHelper.getOwaspTop10;
import static org.sonar.server.issue.index.SecurityStandardHelper.getSansTop25;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_TOP_10;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SANS_TOP_25;

public class ShowAction implements SecurityReportsWsAction {

  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_INCLUDE_DISTRIBUTION = "includeDistribution";
  private static final String PARAM_STANDARD = "standard";
  private static final String OWASP_CAT_PREFIX = "a";

  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final IssueIndex issueIndex;
  private final DbClient dbClient;

  public ShowAction(UserSession userSession, ComponentFinder componentFinder, IssueIndex issueIndex, DbClient dbClient) {
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.issueIndex = issueIndex;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("show")
      .setResponseExample(getClass().getResource("show-example.json"))
      .setHandler(this)
      .setDescription("Return data used by security reports")
      .setSince("7.3")
      .setInternal(true);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project, view or application key")
      .setRequired(true);
    action.createParam(PARAM_BRANCH)
      .setDescription("Branch name")
      .setExampleValue("branch-2.0");
    action.createParam(PARAM_STANDARD)
      .setDescription("Security standard")
      .setPossibleValues(PARAM_OWASP_TOP_10, PARAM_SANS_TOP_25)
      .setRequired(true);
    action.createParam(PARAM_INCLUDE_DISTRIBUTION)
      .setDescription("To return CWE distribution")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
  }

  @Override
  public final void handle(Request request, Response response) {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    ComponentDto projectDto;
    try (DbSession dbSession = dbClient.openSession(false)) {
      projectDto = componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, projectKey, request.param(PARAM_BRANCH), null);
    }
    userSession.checkComponentPermission(USER, projectDto);
    String qualifier = projectDto.qualifier();
    boolean isViewOrApp;
    switch (qualifier) {
      case Qualifiers.VIEW:
      case Qualifiers.SUBVIEW:
      case Qualifiers.APP:
        isViewOrApp = true;
        break;
      case Qualifiers.PROJECT:
        isViewOrApp = false;
        break;
      default:
        throw new IllegalArgumentException("Unsupported component type " + qualifier);
    }
    String standard = request.mandatoryParam(PARAM_STANDARD);
    boolean includeCwe = request.mandatoryParamAsBoolean(PARAM_INCLUDE_DISTRIBUTION);
    switch (standard) {
      case PARAM_OWASP_TOP_10:
        List<SecurityStandardCategoryStatistics> owaspCategories = issueIndex.getOwaspTop10Report(projectDto.uuid(), isViewOrApp, includeCwe)
          .stream()
          .sorted(comparing(ShowAction::index))
          .collect(toList());
        completeStatistics(owaspCategories, projectDto, standard, includeCwe);
        writeResponse(request, response, owaspCategories);
        break;
      case PARAM_SANS_TOP_25:
        List<SecurityStandardCategoryStatistics> sansTop25Report = issueIndex.getSansTop25Report(projectDto.uuid(), isViewOrApp, includeCwe);
        completeStatistics(sansTop25Report, projectDto, standard, includeCwe);
        writeResponse(request, response, sansTop25Report);
        break;
      default:
        throw new IllegalArgumentException("Unsupported standard: '" + standard + "'");
    }
  }

  private void completeStatistics(List<SecurityStandardCategoryStatistics> input, ComponentDto project, String standard, boolean includeCwe) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<QualityProfile> qualityProfiles = dbClient.liveMeasureDao().selectMeasure(dbSession, project.projectUuid(), QUALITY_PROFILES_KEY)
        .map(LiveMeasureDto::getDataAsString)
        .map(data -> QPMeasureData.fromJson(data).getProfiles())
        .orElse(emptySortedSet());

      List<OrgActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().selectByTypeAndProfileUuids(dbSession,
        asList(RuleType.SECURITY_HOTSPOT.getDbConstant(), RuleType.VULNERABILITY.getDbConstant()),
        qualityProfiles.stream()
        .map(QualityProfile::getQpKey)
        .collect(toList()));

      Multimap<String, OrgActiveRuleDto> activeRulesByCategory = ArrayListMultimap.create();
      activeRuleDtos
        .forEach(r -> {
            List<String> cwe = getCwe(r.getSecurityStandards());
            if (includeCwe) {
              cwe.forEach(s -> activeRulesByCategory.put(s, r));
            }
            switch (standard) {
              case PARAM_OWASP_TOP_10:
                getOwaspTop10(r.getSecurityStandards()).forEach(s -> activeRulesByCategory.put(s, r));
                break;
              case PARAM_SANS_TOP_25:
                getSansTop25(cwe).forEach(s -> activeRulesByCategory.put(s, r));
                break;
              default:
                throw new IllegalArgumentException("Unsupported standard: '" + standard + "'");
            }
          });

      List<RuleDto> ruleDtos = dbClient.ruleDao().selectByTypeAndLanguages(dbSession,
        project.getOrganizationUuid(),
        asList(RuleType.SECURITY_HOTSPOT.getDbConstant(), RuleType.VULNERABILITY.getDbConstant()),
        qualityProfiles.stream()
          .map(QualityProfile::getLanguageKey)
          .collect(toList()));

      Multimap<String, RuleDto> rulesByCategory = ArrayListMultimap.create();
      ruleDtos
        .forEach(r -> {
          List<String> cwe = getCwe(r.getSecurityStandards());
          if (includeCwe) {
            cwe.forEach(s -> rulesByCategory.put(s, r));
          }
          switch (standard) {
            case PARAM_OWASP_TOP_10:
              getOwaspTop10(r.getSecurityStandards()).forEach(s -> rulesByCategory.put(s, r));
              break;
            case PARAM_SANS_TOP_25:
              getSansTop25(cwe).forEach(s -> rulesByCategory.put(s, r));
              break;
            default:
              throw new IllegalArgumentException("Unsupported standard: '" + standard + "'");
          }
        });

      input.forEach(c -> {
          c.setTotalRules(rulesByCategory.get(c.getCategory()).size());
          c.setActiveRules(activeRulesByCategory.get(c.getCategory()).size());
          c.getChildren().forEach(child -> {
            child.setTotalRules(rulesByCategory.get(child.getCategory()).size());
            child.setActiveRules(activeRulesByCategory.get(child.getCategory()).size());
          });
        });
    }
  }

  private static Integer index(SecurityStandardCategoryStatistics owaspCat) {
    if (owaspCat.getCategory().startsWith(OWASP_CAT_PREFIX)) {
      return parseInt(owaspCat.getCategory().substring(OWASP_CAT_PREFIX.length()));
    }
    // unknown
    return 11;
  }

  private static void writeResponse(Request request, Response response, List<SecurityStandardCategoryStatistics> categories) {
    SecurityReports.ShowWsResponse.Builder builder = SecurityReports.ShowWsResponse.newBuilder();
    categories.forEach(cat -> {
      SecurityReports.SecurityStandardCategoryStatistics.Builder catBuilder = SecurityReports.SecurityStandardCategoryStatistics.newBuilder();
      catBuilder
        .setCategory(cat.getCategory())
        .setVulnerabilities(cat.getVulnerabilities());
      cat.getVulnerabiliyRating().ifPresent(catBuilder::setVulnerabilityRating);
      catBuilder
        .setOpenSecurityHotspots(cat.getOpenSecurityHotspots())
        .setToReviewSecurityHotspots(cat.getToReviewSecurityHotspots())
        .setWontFixSecurityHotspots(cat.getWontFixSecurityHotspots())
        .setTotalRules(cat.getTotalRules())
        .setActiveRules(cat.getActiveRules());
      if (cat.getChildren() != null) {
        cat.getChildren().stream()
          .sorted(comparing(cweIndex()))
          .forEach(cwe -> {
            SecurityReports.CweStatistics.Builder cweBuilder = SecurityReports.CweStatistics.newBuilder();
            cweBuilder
              .setCwe(cwe.getCategory())
              .setVulnerabilities(cwe.getVulnerabilities());
            cwe.getVulnerabiliyRating().ifPresent(cweBuilder::setVulnerabilityRating);
            cweBuilder
              .setOpenSecurityHotspots(cwe.getOpenSecurityHotspots())
              .setToReviewSecurityHotspots(cwe.getToReviewSecurityHotspots())
              .setWontFixSecurityHotspots(cwe.getWontFixSecurityHotspots())
              .setActiveRules(cwe.getActiveRules())
              .setTotalRules(cwe.getTotalRules());
            catBuilder.addDistribution(cweBuilder);
          });
      }
      builder.addCategories(catBuilder);
    });

    writeProtobuf(builder.build(), request, response);
  }

  private static Function<SecurityStandardCategoryStatistics, Integer> cweIndex() {
    return securityStandardCategoryStatistics -> {
      String category = securityStandardCategoryStatistics.getCategory();
      return category.equals(UNKNOWN_STANDARD) ? Integer.MAX_VALUE : parseInt(category);
    };
  }

}
