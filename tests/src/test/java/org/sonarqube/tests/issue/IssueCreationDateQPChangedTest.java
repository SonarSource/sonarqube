/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.tests.issue;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category2Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.ProjectAnalyses;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import org.sonarqube.ws.client.project.CreateRequest;
import org.sonarqube.ws.client.projectanalysis.SearchRequest;
import org.sonarqube.ws.client.qualityprofile.AddProjectRequest;
import util.ItUtils;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.ItUtils.toDatetime;

/**
 * @see <a href="https://jira.sonarsource.com/browse/MMF-567">MMF-567</a>
 */
public class IssueCreationDateQPChangedTest {
  @ClassRule
  public static final Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;
  @Rule
  public Tester tester = new Tester(orchestrator);

  private static final String ISSUE_STATUS_OPEN = "OPEN";
  private static final String LANGUAGE_XOO = "xoo";

  private static final String SAMPLE_PROJECT_KEY = "creation-date-sample";
  private static final String SAMPLE_PROJECT_NAME = "Creation date sample";
  private static final String SAMPLE_QUALITY_PROFILE_NAME = "creation-date-quality-profile";
  private static final String SAMPLE_EXPLICIT_DATE_1 = todayMinusDays(2);
  private static final String SAMPLE_EXPLICIT_DATE_2 = todayMinusDays(1);

  @Before
  public void resetData() {
    tester.wsClient().projects().create(CreateRequest.builder().setKey(SAMPLE_PROJECT_KEY).setName(SAMPLE_PROJECT_NAME).build());
  }

  @Test
  public void should_use_scm_date_for_new_issues_if_scm_is_available() {
    analysis(QProfile.ONE_RULE, SourceCode.INITIAL, ScannerFeature.SCM);

    assertNumberOfIssues(3);
    assertIssueCreationDate(Component.OnlyInInitial, IssueCreationDate.OnlyInInitial_R1);
    assertIssueCreationDate(Component.ForeverAndModified, IssueCreationDate.ForeverAndModified_R1);
    assertIssueCreationDate(Component.ForeverAndUnmodified, IssueCreationDate.ForeverAndUnmodified_R1);
  }

  @Test
  public void should_use_analysis_date_for_new_issues_if_scm_is_not_available() {
    analysis(QProfile.ONE_RULE, SourceCode.INITIAL);

    assertNumberOfIssues(3);
    assertIssueCreationDates(COMPONENTS_OF_SOURCE_INITIAL, IssueCreationDate.FIRST_ANALYSIS);
  }

  @Test
  public void use_explicit_project_date_if_scm_is_not_available() {
    analysis(QProfile.ONE_RULE, SourceCode.INITIAL, ScannerFeature.EXPLICIT_DATE_1);

    assertNumberOfIssues(3);
    assertIssueCreationDates(COMPONENTS_OF_SOURCE_INITIAL, IssueCreationDate.EXPLICIT_DATE_1);
  }

  @Test
  public void use_scm_date_even_if_explicit_project_date_is_set() {
    analysis(QProfile.ONE_RULE, SourceCode.INITIAL, ScannerFeature.SCM, ScannerFeature.EXPLICIT_DATE_1);

    assertNumberOfIssues(3);
    assertIssueCreationDate(Component.OnlyInInitial, IssueCreationDate.OnlyInInitial_R1);
    assertIssueCreationDate(Component.ForeverAndModified, IssueCreationDate.ForeverAndModified_R1);
    assertIssueCreationDate(Component.ForeverAndUnmodified, IssueCreationDate.ForeverAndUnmodified_R1);
  }

  @Test
  public void no_rules_no_issues_if_scm_is_available() {
    analysis(QProfile.NO_RULES, SourceCode.INITIAL, ScannerFeature.SCM);

    assertNoIssue();
  }

  @Test
  public void no_rules_no_issues_if_scm_is_not_available() {
    analysis(QProfile.NO_RULES, SourceCode.INITIAL);

    assertNoIssue();
  }

  @Test
  public void use_scm_date_for_issues_raised_by_new_rules_if_scm_is_newly_available() {
    analysis(QProfile.NO_RULES, SourceCode.INITIAL);
    analysis(QProfile.ONE_RULE, SourceCode.CHANGED, ScannerFeature.SCM);

    assertNumberOfIssues(3);
    assertIssueCreationDate(Component.ForeverAndModified, IssueCreationDate.ForeverAndModified_R2);
    assertIssueCreationDate(Component.ForeverAndUnmodified, IssueCreationDate.ForeverAndUnmodified_R1);
    assertIssueCreationDate(Component.OnlyInChanged, IssueCreationDate.OnlyInChanged_R1);
  }

  @Test
  public void use_scm_date_for_issues_raised_by_new_rules_if_scm_is_available_and_ever_has_been_available() {
    analysis(QProfile.NO_RULES, SourceCode.INITIAL, ScannerFeature.SCM);
    analysis(QProfile.ONE_RULE, SourceCode.CHANGED, ScannerFeature.SCM);

    assertNumberOfIssues(3);
    assertIssueCreationDate(Component.ForeverAndModified, IssueCreationDate.ForeverAndModified_R2);
    assertIssueCreationDate(Component.ForeverAndUnmodified, IssueCreationDate.ForeverAndUnmodified_R1);
    assertIssueCreationDate(Component.OnlyInChanged, IssueCreationDate.OnlyInChanged_R1);
  }

  @Test
  public void use_analysis_date_for_issues_raised_by_new_rules_if_scm_is_not_available() {
    analysis(QProfile.NO_RULES, SourceCode.INITIAL);
    analysis(QProfile.ONE_RULE, SourceCode.CHANGED);

    assertNumberOfIssues(3);
    Stream.of(COMPONENTS_OF_SOURCE_CHANGED)
      .forEach(component -> {
        assertIssueCreationDate(component, IssueCreationDate.LATEST_ANALYSIS);
      });
  }

  @Test
  public void keep_the_date_of_an_existing_issue_even_if_the_blame_information_changes() {
    analysis(QProfile.ONE_RULE, SourceCode.INITIAL, ScannerFeature.SCM);
    analysis(QProfile.ONE_RULE, SourceCode.CHANGED, ScannerFeature.SCM);

    assertNumberOfIssues(3);
    assertIssueCreationDate(Component.ForeverAndModified, IssueCreationDate.ForeverAndModified_R1);
    assertIssueCreationDate(Component.ForeverAndUnmodified, IssueCreationDate.ForeverAndUnmodified_R1);

    // this file is new to the second analysis
    assertIssueCreationDate(Component.OnlyInChanged, IssueCreationDate.LATEST_ANALYSIS);
  }

  @Test
  public void ignore_explicit_date_for_issues_related_to_new_rules_if_scm_is_available() {
    analysis(QProfile.NO_RULES, SourceCode.INITIAL, ScannerFeature.SCM, ScannerFeature.EXPLICIT_DATE_1);
    analysis(QProfile.ONE_RULE, SourceCode.CHANGED, ScannerFeature.SCM, ScannerFeature.EXPLICIT_DATE_2);

    assertNumberOfIssues(3);
    assertIssueCreationDate(Component.ForeverAndModified, IssueCreationDate.ForeverAndModified_R2);
    assertIssueCreationDate(Component.ForeverAndUnmodified, IssueCreationDate.ForeverAndUnmodified_R1);
    assertIssueCreationDate(Component.OnlyInChanged, IssueCreationDate.OnlyInChanged_R1);
  }

  @Test
  public void use_explicit_date_for_issues_related_to_new_rules_if_scm_is_not_available() {
    analysis(QProfile.NO_RULES, SourceCode.INITIAL, ScannerFeature.EXPLICIT_DATE_1);
    analysis(QProfile.ONE_RULE, SourceCode.CHANGED, ScannerFeature.EXPLICIT_DATE_2);

    assertNumberOfIssues(3);
    assertIssueCreationDates(COMPONENTS_OF_SOURCE_CHANGED, IssueCreationDate.EXPLICIT_DATE_2);
  }

  private void analysis(QProfile qProfile, SourceCode sourceCode, ScannerFeature... scm) {
    ItUtils.restoreProfile(orchestrator, getClass().getResource(qProfile.path));
    tester.wsClient().qualityProfiles().addProject(
      AddProjectRequest.builder().setProjectKey(SAMPLE_PROJECT_KEY).setQualityProfile(SAMPLE_QUALITY_PROFILE_NAME).setLanguage(LANGUAGE_XOO).build());

    SonarScanner scanner = SonarScanner.create(projectDir(sourceCode.path));
    Arrays.stream(scm).forEach(s -> s.configure(scanner));
    orchestrator.executeBuild(scanner);
  }

  private void assertNoIssue() {
    assertNumberOfIssues(0);
  }

  private void assertNumberOfIssues(int number) {
    assertThat(getIssues(new SearchWsRequest().setStatuses(singletonList(ISSUE_STATUS_OPEN)))).hasSize(number);
  }

  private void assertIssueCreationDate(Component component, IssueCreationDate expectedDate) {
    assertIssueCreationDates(new Component[] {component}, expectedDate);
  }

  private void assertIssueCreationDates(Component[] components, IssueCreationDate expectedDate) {
    List<String> keys = Arrays.stream(components).map(Component::getKey).collect(Collectors.toList());
    List<Issue> issues = getIssues(new SearchWsRequest().setComponentKeys(keys));
    Date[] dates = Arrays.stream(components).map(x -> expectedDate.getDate()).toArray(Date[]::new);

    assertThat(issues)
      .extracting(i -> toDatetime(i.getCreationDate()))
      .containsExactly(dates);
  }

  private List<Issue> getIssues(SearchWsRequest request) {
    return tester.wsClient().issues().search(request).getIssuesList();
  }

  private static String todayMinusDays(int numberOfDays) {
    return ISO_DATE_TIME.format(LocalDate.now().atStartOfDay().minusDays(numberOfDays).atZone(ZoneId.systemDefault()));
  }

  private enum SourceCode {
    INITIAL("issue/creationDateSampleInitial"),
    CHANGED("issue/creationDateSampleChanged"),
    ;

    private final String path;

    SourceCode(String path) {
      this.path = path;
    }
  }

  private enum Component {
    OnlyInInitial("creation-date-sample:src/main/xoo/sample/OnlyInInitial.xoo"),
    ForeverAndModified("creation-date-sample:src/main/xoo/sample/ForeverAndModified.xoo"),
    ForeverAndUnmodified("creation-date-sample:src/main/xoo/sample/ForeverAndUnmodified.xoo"),
    OnlyInChanged("creation-date-sample:src/main/xoo/sample/OnlyInChanged.xoo"),
    ;
    private final String key;

    Component(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }
  }

  private static final Component[] COMPONENTS_OF_SOURCE_INITIAL = {Component.OnlyInInitial, Component.ForeverAndModified, Component.ForeverAndUnmodified};
  private static final Component[] COMPONENTS_OF_SOURCE_CHANGED = {Component.ForeverAndModified, Component.ForeverAndUnmodified, Component.OnlyInChanged};

  private enum QProfile {
    ONE_RULE("/issue/IssueCreationDateQPChangedTest/one-rule.xml"),
    NO_RULES("/issue/IssueCreationDateQPChangedTest/no-rules.xml"),
    ;

    private final String path;

    QProfile(String path) {
      this.path = path;
    }
  }

  private enum ScannerFeature {
    SCM {
      @Override
      void configure(SonarScanner scanner) {
        scanner
          .setProperty("sonar.scm.provider", "xoo")
          .setProperty("sonar.scm.disabled", "false");
      }
    },
    EXPLICIT_DATE_1 {
      @Override
      void configure(SonarScanner scanner) {
        scanner
          .setProperty("sonar.projectDate", SAMPLE_EXPLICIT_DATE_1);
      }
    },
    EXPLICIT_DATE_2 {
      @Override
      void configure(SonarScanner scanner) {
        scanner
          .setProperty("sonar.projectDate", SAMPLE_EXPLICIT_DATE_2);
      }
    },
    ;

    void configure(SonarScanner scanner) {
    }
  }

  private enum IssueCreationDate {
    OnlyInInitial_R1(toDatetime("2001-01-01T00:00:00+00:00")),
    ForeverAndUnmodified_R1(toDatetime("2002-01-01T00:00:00+00:00")),
    ForeverAndModified_R1(toDatetime("2003-01-01T00:00:00+00:00")),
    ForeverAndModified_R2(toDatetime("2004-01-01T00:00:00+00:00")),
    OnlyInChanged_R1(toDatetime("2005-01-01T00:00:00+00:00")),
    EXPLICIT_DATE_1(toDatetime(SAMPLE_EXPLICIT_DATE_1)),
    EXPLICIT_DATE_2(toDatetime(SAMPLE_EXPLICIT_DATE_2)),
    FIRST_ANALYSIS {
      @Override
      Date getDate() {
        return getAnalysisDate(l -> {
          if (l.isEmpty()) {
            return Optional.empty();
          }
          return Optional.of(l.get(l.size() - 1));
        });
      }
    },
    LATEST_ANALYSIS {
      @Override
      Date getDate() {
        return getAnalysisDate(l -> {
          if (l.size() > 0) {
            return Optional.of(l.get(0));
          }
          return Optional.empty();
        });
      }
    },
    ;

    private final Date date;

    IssueCreationDate() {
      this.date = null;
    }

    IssueCreationDate(Date date) {
      this.date = date;
    }

    Date getDate() {
      return date;
    }

    private static Date getAnalysisDate(Function<List<ProjectAnalyses.Analysis>, Optional<ProjectAnalyses.Analysis>> chooseItem) {
      return Optional.of(
        ItUtils.newWsClient(orchestrator)
          .projectAnalysis()
          .search(SearchRequest.builder().setProject(SAMPLE_PROJECT_KEY).build())
          .getAnalysesList())
        .flatMap(chooseItem)
        .map(ProjectAnalyses.Analysis::getDate)
        .map(expectedDate -> toDatetime(expectedDate))
        .orElseThrow(() -> new IllegalStateException("There is no analysis"));
    }
  }
}
