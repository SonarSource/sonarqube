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
package org.sonar.db.createdb;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.metric.SoftwareQualitiesMetrics;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.TokenType;
import org.sonar.db.user.UserDto;
import org.sonar.db.webhook.WebhookDto;

import static org.sonar.db.component.BranchType.BRANCH;

public class PopulateDb {

  private static final Logger LOG = LoggerFactory.getLogger(PopulateDb.class);

  public static final int NB_PROJECT_WISHED = 4;
  public static final int NB_WORKER = 2;
  public static final int BRANCH_PER_PROJECT = 7;
  public static final int FILE_PER_BRANCH = 377;
  public static final int ISSUE_PER_FILE = 3;
  public static final int SNAPSHOT_PER_BRANCH = 13;
  public static final int WEBHOOK_DELIVERIES_PER_COMPONENT = 1;
  public static final int NB_USER = 100;
  public static final int NUMBER_OF_PORTFOLIOS = 100;
  public static final int MAX_PROJECT_PER_PORTFOLIO = 10;

  public static void main(String[] args) throws InterruptedException {
    LOG.info("Population procedure starting");

    System.setProperty("sonar.jdbc.url", "jdbc:postgresql://localhost:5432/sonarqube");
    System.setProperty("sonar.jdbc.username", "sonarqube");
    System.setProperty("sonar.jdbc.password", "sonarqube");
    System.setProperty("sonar.jdbc.dialect", "postgresql");
    System.setProperty("sonar.jdbc.maximumPoolSize", "" + (NB_WORKER + 1));
    final DbTester dbTester = createDbTester();

    LOG.info("Database infrastructure is set up");

    // read base data
    final Map<String, MetricDto> metricDtosByKey;
    final List<ProjectDto> allProjects;
    final List<PortfolioDto> allPortfolios;
    final Set<RuleDto> enabledRules;
    final GroupDto adminGroupDto;

    DbSession initSession = dbTester.getSession();
    metricDtosByKey = dbTester.getDbClient().metricDao().selectAll(initSession).stream().collect(
      Collectors.toMap(MetricDto::getKey, Function.identity())
    );
    allProjects = Collections.synchronizedList(new ArrayList<>(dbTester.getDbClient().projectDao().selectProjects(initSession)));
    enabledRules = new HashSet<>(dbTester.getDbClient().ruleDao().selectEnabled(dbTester.getSession()));
    adminGroupDto = dbTester.getDbClient().groupDao().selectByName(dbTester.getSession(), "sonar-administrators")
      .orElseThrow(() -> new IllegalStateException("group with name \"sonar-administrators\" is expected to exist"));
    SqContext sqContext = new SqContext(allProjects, enabledRules, metricDtosByKey, adminGroupDto, dbTester);
    LOG.info("Existing data has been collected");


    ExecutorService executorService = Executors.newFixedThreadPool(NB_WORKER);
    final AtomicInteger nbProjectsGenerated = new AtomicInteger(0);
    LOG.info("Starting generation of {} projects", NB_PROJECT_WISHED);
    IntStream.rangeClosed(1, NB_PROJECT_WISHED)
      .map(i -> i + allProjects.size())
      .mapToObj(i -> new ProjectStructure("project " + i, BRANCH_PER_PROJECT, FILE_PER_BRANCH, ISSUE_PER_FILE, ISSUE_PER_FILE, SNAPSHOT_PER_BRANCH, WEBHOOK_DELIVERIES_PER_COMPONENT))
      .forEach(projectStructure -> {
        executorService.submit(() -> {
          LOG.info("Worker-{}: Starting generation of project: {}", Thread.currentThread().getName(), projectStructure);
          try {
            sqContext.dbTester.getSession(true);
            allProjects.add(generateProject(
              sqContext, projectStructure
            ));
          } catch (Exception e) {
            LOG.error("Worker-" + Thread.currentThread().getName() + ": Error while generating project", e);
            return;
          }
          nbProjectsGenerated.incrementAndGet();
          LOG.info("Worker-{}: Project generation completed: {}", Thread.currentThread().getName(), projectStructure.projectName);
        });
      });

    executorService.shutdown();
    executorService.awaitTermination(100, TimeUnit.DAYS);
    LOG.info("Ending generation of {}/{} projects", nbProjectsGenerated.get(), NB_PROJECT_WISHED);

    createUsers(sqContext, NB_USER);

    allPortfolios = new ArrayList<>(dbTester.getDbClient().portfolioDao().selectAll(initSession));
    allPortfolios.addAll(createPortfolios(sqContext, new PortfolioGenerationSettings(allPortfolios.size(), NUMBER_OF_PORTFOLIOS, MAX_PROJECT_PER_PORTFOLIO)));

    // close database connection
    dbTester.getDbClient().getDatabase().stop();
  }

  private static List<PortfolioDto> createPortfolios(SqContext sqContext, PortfolioGenerationSettings portfolioGenerationSettings) {
    List<PortfolioDto> generatedPortfolios = new ArrayList<>();
    int startIndex = portfolioGenerationSettings.currentPortfoliosSize + 1;
    int limit = startIndex + portfolioGenerationSettings.numberOfPortolios;

    for (int portfolioIndex = startIndex; portfolioIndex < limit; portfolioIndex++) {
      PortfolioDto portfolioDto = generatePortfolio(sqContext, "portfolio " + portfolioIndex);
      generatedPortfolios.add(portfolioDto);
      for (int projectIndex = 0; projectIndex < Math.min(portfolioGenerationSettings.maxProjectPerPortfolio, sqContext.projects.size()); projectIndex++) {
        sqContext.dbTester.getDbClient().portfolioDao().addProject(sqContext.dbTester.getSession(), portfolioDto.getUuid(), sqContext.projects.get(projectIndex).getUuid());
      }
    }

    return generatedPortfolios;
  }

  private static PortfolioDto generatePortfolio(SqContext sqContext, String portfolioName) {
    PortfolioDto portfolioDto = sqContext.dbTester.components().insertPublicPortfolioDto(
      c -> c.setName(portfolioName),
      // Selection mode is set to MANUAL as we are picking the portfolio projects manually
      p -> p.setSelectionMode(PortfolioDto.SelectionMode.MANUAL));

    insertPortfolioAdminRights(sqContext, portfolioDto);
    return portfolioDto;
  }

  private static void insertPortfolioAdminRights(SqContext sqContext, PortfolioDto portfolioComponentDto) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(sqContext.adminGroup.getUuid())
      .setEntityUuid(portfolioComponentDto.getUuid())
      .setRole("admin");

    sqContext.dbTester.getDbClient().groupPermissionDao().insert(sqContext.dbTester.getSession(), dto, portfolioComponentDto, null);
  }

  private static void createUsers(SqContext sqContext, int nbUser) {
    for (int i = 0; i < nbUser; i++) {
      UserDto userDto = sqContext.dbTester.users().insertUserRealistic();
      ProjectDto projectDto = ThreadLocalRandom.current().nextBoolean() ? null : sqContext.projects.get(ThreadLocalRandom.current().nextInt(sqContext.projects.size()));
      if (i % 60 == 0 && projectDto != null) {
        createUserTokensDto(sqContext, userDto, projectDto);
      }
      if (i % 50 == 5 && projectDto != null) {
        createUserDismissedMessages(sqContext, userDto, projectDto);
      }
    }
  }

  private static void createUserDismissedMessages(SqContext sqContext, UserDto userDto, ProjectDto projectDto) {
    MessageType type = ThreadLocalRandom.current().nextBoolean() ? MessageType.GENERIC : MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE;
    sqContext.dbTester.users().insertUserDismissedMessageOnProject(userDto, projectDto, type);
  }

  private static void createUserTokensDto(SqContext sqContext, UserDto userDto, ProjectDto randomProject) {
    long now = System.currentTimeMillis();
    Long expirationDate = ThreadLocalRandom.current().nextBoolean() ? now + 123_123 : null;
    sqContext.dbTester.users().insertToken(userDto, a -> a.setCreatedAt(now).setExpirationDate(expirationDate).setProjectKey(randomProject.getKey())
      .setLastConnectionDate(now).setType(randomProject.getKey() != null ? TokenType.PROJECT_ANALYSIS_TOKEN.name() : TokenType.USER_TOKEN.name()));
  }

  private record SqContext(List<ProjectDto> projects, Set<RuleDto> rules, Map<String, MetricDto> metricDtosByKey, GroupDto adminGroup,
                           DbTester dbTester) {
    public RuleDto findNotSecurityHotspotRule() {
      return rules.stream().filter(r -> r.getType() != RuleType.SECURITY_HOTSPOT.getDbConstant()).findAny().orElseThrow();
    }
  }

  private static @NotNull DbTester createDbTester() {
    return DbTester.createWithDifferentUuidFactory(UuidFactoryImpl.INSTANCE);
  }

  private record ProjectStructure(String projectName, int branchPerProject, int filePerBranch, int issuePerFile, int issueChangePerIssue,
                                  int snapshotPerBranch, int webhookDeliveriesPerBranch) {
  }

  private record PortfolioGenerationSettings(int currentPortfoliosSize, int numberOfPortolios, int maxProjectPerPortfolio) {
  }

  private record BranchAndComponentDto(BranchDto branch, ComponentDto compo) {
  }

  private static ProjectDto generateProject(SqContext sqContext, ProjectStructure pj) {
    final ProjectData projectCompoDto = sqContext.dbTester.components().insertPublicProject(p -> p.setName(pj.projectName));

    sqContext.dbTester.forceCommit();
    final WebhookDto projectWebHook = sqContext.dbTester.webhooks().insertWebhook(projectCompoDto.getProjectDto());
    Streams.concat(
        // main branch
        Stream.of(new BranchAndComponentDto(
          projectCompoDto.getMainBranchDto(),
          projectCompoDto.getMainBranchComponent())),
        // other branches
        Stream.generate(() -> {
          BranchDto branchDto = ComponentTesting.newBranchDto(projectCompoDto.getProjectDto().getUuid(), BRANCH);
          return new BranchAndComponentDto(
            branchDto,
            sqContext.dbTester.components().insertProjectBranch(projectCompoDto.getProjectDto(), branchDto));
        }))
      // until there are enough branches generated
      .limit(pj.branchPerProject)
      // for every branch (main included)
      .forEach(branchAndComponentDto -> {

        // create live measure for the branch
        projectLiveMeasureMetrics.stream()
          .map(sqContext.metricDtosByKey::get)
          .forEach(metricDto -> sqContext.dbTester().measures().insertLiveMeasureWithSensibleValues(branchAndComponentDto.compo, metricDto));

        // create snapshots for the current branch
        long time = System2.INSTANCE.now();
        List<SnapshotDto> snapshots = new ArrayList<>();
        // for every snapshot on the current branch
        for (int snapshotNum = 0; snapshotNum < pj.snapshotPerBranch; snapshotNum++) {
          SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(branchAndComponentDto.branch);
          snapshotDto.setLast(false);
          snapshotDto.setCreatedAt(time);
          time -= 10_000_000;
          snapshots.add(snapshotDto);
          // insert project measure for the snapshot
          projectProjectMeasureMetrics.stream()
            .map(sqContext.metricDtosByKey::get)
            .forEach(metricDto -> sqContext.dbTester().measures().insertMeasureWithSensibleValues(branchAndComponentDto.compo, snapshotDto, metricDto));
        }
        SnapshotDto lastSnapshotDto = snapshots.get(0);
        lastSnapshotDto.setLast(true);
        sqContext.dbTester.components().insertSnapshots(snapshots.toArray(new SnapshotDto[0]));

        // create webhook deliveries for every branch and only the last snapshot
        for (int whdNum = 0; whdNum < pj.webhookDeliveriesPerBranch; whdNum++) {
          sqContext.dbTester.webhookDelivery().insert(whd -> whd
            .setAnalysisUuid(lastSnapshotDto.getUuid())
            .setProjectUuid(projectCompoDto.getProjectDto().getUuid())
            .setWebhookUuid(projectWebHook.getUuid()));
        }

        // for every file in branch
        for (int fileNum = 0; fileNum < pj.filePerBranch; fileNum++) {
          ComponentDto fileComponentDto = sqContext.dbTester.components().insertFile(branchAndComponentDto.compo);
          sqContext.dbTester.fileSources().insertFileSource(fileComponentDto, pj.issuePerFile,
            fs -> fs.setSourceData(fs.getSourceData()));
          // for every issue in file
          for (int issueNum = 0; issueNum < pj.issuePerFile; issueNum++) {
            IssueDto issueDto = sqContext.dbTester.issues().insertIssue(sqContext.findNotSecurityHotspotRule(), branchAndComponentDto.compo, fileComponentDto);
            // for every issue change in issue
            for (int issueChangeNum = 0; issueChangeNum < pj.issueChangePerIssue; issueChangeNum++) {
              sqContext.dbTester.issues().insertChange(issueDto);
            }
          }
          // create live measure for this file
          fileLiveMeasureMetrics.stream()
            .map(sqContext.metricDtosByKey::get)
            .forEach(metricDto -> sqContext.dbTester().measures().insertLiveMeasureWithSensibleValues(fileComponentDto, metricDto));
        }

        sqContext.dbTester.forceCommit();
      });

    return projectCompoDto.getProjectDto();
  }

  private static final List<String> projectLiveMeasureMetrics = List.of(
    CoreMetrics.ACCEPTED_ISSUES_KEY,
    CoreMetrics.ALERT_STATUS_KEY,
    CoreMetrics.ANALYSIS_FROM_SONARQUBE_9_4_KEY,
    CoreMetrics.BLOCKER_VIOLATIONS_KEY,
    CoreMetrics.BUGS_KEY,
    CoreMetrics.CODE_SMELLS_KEY,
    CoreMetrics.COMMENT_LINES_KEY,
    CoreMetrics.COMMENT_LINES_DENSITY_KEY,
    CoreMetrics.CONFIRMED_ISSUES_KEY,
    CoreMetrics.CRITICAL_VIOLATIONS_KEY,
    CoreMetrics.DEVELOPMENT_COST_KEY,
    CoreMetrics.DUPLICATED_BLOCKS_KEY,
    CoreMetrics.DUPLICATED_FILES_KEY,
    CoreMetrics.DUPLICATED_LINES_KEY,
    CoreMetrics.DUPLICATED_LINES_DENSITY_KEY,
    CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY,
    SoftwareQualitiesMetrics.EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A_KEY,
    CoreMetrics.FALSE_POSITIVE_ISSUES_KEY,
    CoreMetrics.FILES_KEY,
    CoreMetrics.HIGH_IMPACT_ACCEPTED_ISSUES_KEY,
    CoreMetrics.INFO_VIOLATIONS_KEY,
    CoreMetrics.LAST_COMMIT_DATE_KEY,
    CoreMetrics.LINES_KEY,
    CoreMetrics.MAINTAINABILITY_ISSUES_KEY,
    CoreMetrics.MAJOR_VIOLATIONS_KEY,
    CoreMetrics.MINOR_VIOLATIONS_KEY,
    CoreMetrics.NCLOC_KEY,
    CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY,
    CoreMetrics.OPEN_ISSUES_KEY,
    CoreMetrics.QUALITY_GATE_DETAILS_KEY,
    CoreMetrics.QUALITY_PROFILES_KEY,
    CoreMetrics.RELIABILITY_ISSUES_KEY,
    CoreMetrics.RELIABILITY_RATING_KEY,
    CoreMetrics.RELIABILITY_REMEDIATION_EFFORT_KEY,
    CoreMetrics.REOPENED_ISSUES_KEY,
    CoreMetrics.SECURITY_HOTSPOTS_KEY,
    CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY,
    CoreMetrics.SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY,
    CoreMetrics.SECURITY_ISSUES_KEY,
    CoreMetrics.SECURITY_RATING_KEY,
    CoreMetrics.SECURITY_REMEDIATION_EFFORT_KEY,
    CoreMetrics.SECURITY_REVIEW_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY,
    CoreMetrics.SQALE_DEBT_RATIO_KEY,
    CoreMetrics.TECHNICAL_DEBT_KEY,
    CoreMetrics.SQALE_RATING_KEY,
    CoreMetrics.VIOLATIONS_KEY,
    CoreMetrics.VULNERABILITIES_KEY
  );

  private static final List<String> fileLiveMeasureMetrics = List.of(
    CoreMetrics.CODE_SMELLS_KEY,
    CoreMetrics.COMMENT_LINES_KEY,
    CoreMetrics.COMMENT_LINES_DENSITY_KEY,
    CoreMetrics.DEVELOPMENT_COST_KEY,
    CoreMetrics.FILES_KEY,
    CoreMetrics.LAST_COMMIT_DATE_KEY,
    CoreMetrics.LINES_KEY,
    CoreMetrics.MAINTAINABILITY_ISSUES_KEY,
    CoreMetrics.MAJOR_VIOLATIONS_KEY,
    CoreMetrics.NCLOC_KEY,
    CoreMetrics.NCLOC_DATA_KEY,
    CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY,
    CoreMetrics.OPEN_ISSUES_KEY,
    CoreMetrics.RELIABILITY_ISSUES_KEY,
    CoreMetrics.RELIABILITY_RATING_KEY,
    CoreMetrics.SECURITY_ISSUES_KEY,
    CoreMetrics.SECURITY_RATING_KEY,
    CoreMetrics.SECURITY_REVIEW_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    CoreMetrics.SQALE_DEBT_RATIO_KEY,
    CoreMetrics.TECHNICAL_DEBT_KEY,
    CoreMetrics.SQALE_RATING_KEY,
    CoreMetrics.VIOLATIONS_KEY);

  private static final List<String> projectProjectMeasureMetrics = List.of(
    CoreMetrics.ACCEPTED_ISSUES_KEY,
    CoreMetrics.ALERT_STATUS_KEY,
    CoreMetrics.ANALYSIS_FROM_SONARQUBE_9_4_KEY,
    CoreMetrics.BLOCKER_VIOLATIONS_KEY,
    CoreMetrics.BUGS_KEY,
    CoreMetrics.CODE_SMELLS_KEY,
    CoreMetrics.COMMENT_LINES_KEY,
    CoreMetrics.COMMENT_LINES_DENSITY_KEY,
    CoreMetrics.CONFIRMED_ISSUES_KEY,
    CoreMetrics.CRITICAL_VIOLATIONS_KEY,
    CoreMetrics.DEVELOPMENT_COST_KEY,
    CoreMetrics.DUPLICATED_BLOCKS_KEY,
    CoreMetrics.DUPLICATED_FILES_KEY,
    CoreMetrics.DUPLICATED_LINES_KEY,
    CoreMetrics.DUPLICATED_LINES_DENSITY_KEY,
    CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY,
    SoftwareQualitiesMetrics.EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A_KEY,
    CoreMetrics.FALSE_POSITIVE_ISSUES_KEY,
    CoreMetrics.FILES_KEY,
    CoreMetrics.HIGH_IMPACT_ACCEPTED_ISSUES_KEY,
    CoreMetrics.INFO_VIOLATIONS_KEY,
    CoreMetrics.LAST_COMMIT_DATE_KEY,
    CoreMetrics.LINES_KEY,
    CoreMetrics.MAINTAINABILITY_ISSUES_KEY,
    CoreMetrics.MAJOR_VIOLATIONS_KEY,
    CoreMetrics.MINOR_VIOLATIONS_KEY,
    CoreMetrics.NCLOC_KEY,
    CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY,
    CoreMetrics.OPEN_ISSUES_KEY,
    CoreMetrics.QUALITY_GATE_DETAILS_KEY,
    CoreMetrics.QUALITY_PROFILES_KEY,
    CoreMetrics.RELIABILITY_ISSUES_KEY,
    CoreMetrics.RELIABILITY_RATING_KEY,
    CoreMetrics.RELIABILITY_REMEDIATION_EFFORT_KEY,
    CoreMetrics.REOPENED_ISSUES_KEY,
    CoreMetrics.SECURITY_HOTSPOTS_KEY,
    CoreMetrics.SECURITY_ISSUES_KEY,
    CoreMetrics.SECURITY_RATING_KEY,
    CoreMetrics.SECURITY_REMEDIATION_EFFORT_KEY,
    CoreMetrics.SECURITY_REVIEW_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY,
    CoreMetrics.SQALE_DEBT_RATIO_KEY,
    CoreMetrics.TECHNICAL_DEBT_KEY,
    CoreMetrics.SQALE_RATING_KEY,
    CoreMetrics.VIOLATIONS_KEY,
    CoreMetrics.VULNERABILITIES_KEY
  );
}
