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

import java.util.Date;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES_KEY;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newIssue;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonar.test.JsonAssert.assertJson;

public class ShowActionTest {

  @Rule
  public UserSessionRule userSessionRule = standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public ExpectedException expectedException = none();

  private DbClient dbClient = db.getDbClient();
  private DbSession session = db.getSession();
  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSessionRule, new WebAuthorizationTypeSupport(userSessionRule));
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient));
  private WsActionTester ws = new WsActionTester(new ShowAction(userSessionRule, TestComponentFinder.from(db), issueIndex, dbClient));
  private StartupIndexer permissionIndexer = new PermissionIndexer(dbClient, es.client(), issueIndexer);

  private UserDto user;
  private ComponentDto project;
  private QProfileDto qualityProfile;
  private RuleDefinitionDto rule1;
  private RuleDefinitionDto rule2;
  private RuleDefinitionDto rule3;
  private RuleDefinitionDto rule4;
  private MetricDto qpMetric;

  @Before
  public void setUp() {
    user = db.users().insertUser("john");
    userSessionRule.logIn(user);
    project = insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization(), "PROJECT_ID").setDbKey("PROJECT_KEY"));
    qualityProfile = db.qualityProfiles().insert(db.getDefaultOrganization(), qp -> qp.setLanguage("java"));
    // owasp : a2 and TopSans25 insecure
    rule1 = newRule(newHashSet("owaspTop10:a2", "cwe:123", "cwe:89"));
    // owasp : a1 and TopSans25 risky
    rule2 = newRule(newHashSet("owaspTop10:a1", "cwe:22", "cwe:190"));
    // owasp : a4 and TopSans25 porous, not activated
    rule3 = newRule(newHashSet("owaspTop10:a3", "cwe:759", "cwe:000"));
    // cwe with unknown
    rule4 = newRule(newHashSet("cwe:999"));
    db.qualityProfiles().activateRule(qualityProfile, rule1);
    db.qualityProfiles().activateRule(qualityProfile, rule2);
    qpMetric = db.measures().insertMetric(m -> m.setValueType(STRING.name()).setKey(QUALITY_PROFILES_KEY));

    insertQPLiveMeasure(project);
  }

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("show");
    assertThat(def.isInternal()).isTrue();
    assertThat(def.isPost()).isFalse();
    assertThat(def.since()).isEqualTo("7.3");

    assertThat(def.params()).extracting("key").containsExactlyInAnyOrder("standard", "project", "branch", "includeDistribution");
  }

  @Test
  public void owasp_empty() {
    userSessionRule.addProjectPermission(UserRole.USER, project);
    indexPermissions();
    ComponentDto file = insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY"));
    IssueDto issue1 = newIssue(rule1, project, file)
      .setStatus("OPEN")
      .setSeverity("MAJOR")
      .setType(RuleType.CODE_SMELL);
    IssueDto issue2 = newIssue(rule1, project, file)
      .setStatus("OPEN")
      .setSeverity("MAJOR")
      .setType(RuleType.BUG);
    IssueDto issue3 = newIssue(rule1, project, file)
      .setStatus("OPEN")
      .setSeverity("MAJOR")
      .setType(RuleType.CODE_SMELL);
    dbClient.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();
    indexIssues();

    assertJson(ws.newRequest()
      .setParam("standard", "owaspTop10")
      .setParam("project", project.getKey())
      .execute().getInput())
      .withStrictArrayOrder()
      .isSimilarTo(this.getClass().getResource("ShowActionTest/empty.json"));
  }

  @Test
  public void owasp_without_cwe() {
    userSessionRule.addProjectPermission(UserRole.USER, project);
    indexPermissions();
    ComponentDto file = insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY"));
    IssueDto issue1 = newIssue(rule1, project, file)
      .setStatus("OPEN")
      .setSeverity("MAJOR")
      .setType(RuleType.VULNERABILITY);
    IssueDto issue2 = newIssue(rule1, project, file)
      .setStatus("OPEN")
      .setSeverity("MAJOR")
      .setType(RuleType.SECURITY_HOTSPOT);
    IssueDto issue3 = newIssue(rule1, project, file)
      .setStatus(Issue.STATUS_RESOLVED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setSeverity("MAJOR")
      .setType(RuleType.SECURITY_HOTSPOT);
    IssueDto issue4 = newIssue(rule1, project, file)
      .setStatus(Issue.STATUS_RESOLVED)
      .setResolution(Issue.RESOLUTION_WONT_FIX)
      .setSeverity("MAJOR")
      .setType(RuleType.SECURITY_HOTSPOT);
    dbClient.issueDao().insert(session, issue1, issue2, issue3, issue4);
    session.commit();
    indexIssues();

    assertJson(ws.newRequest()
      .setParam("standard", "owaspTop10")
      .setParam("project", project.getKey())
      .execute().getInput())
      .withStrictArrayOrder()
      .isSimilarTo(this.getClass().getResource("ShowActionTest/owaspNoCwe.json"));
  }

  @Test
  public void owasp_with_cwe__matching_json_example() {
    userSessionRule.addProjectPermission(UserRole.USER, project);
    indexPermissions();
    ComponentDto file = insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY"));
    IssueDto issue1 = newIssue(rule1, project, file)
      .setStatus("OPEN")
      .setSeverity("MAJOR")
      .setType(RuleType.VULNERABILITY);
    IssueDto issue2 = newIssue(rule1, project, file)
      .setStatus("OPEN")
      .setSeverity("MAJOR")
      .setType(RuleType.SECURITY_HOTSPOT);
    IssueDto issue3 = newIssue(rule1, project, file)
      .setStatus(Issue.STATUS_RESOLVED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setSeverity("MAJOR")
      .setType(RuleType.SECURITY_HOTSPOT);
    IssueDto issue4 = newIssue(rule1, project, file)
      .setStatus(Issue.STATUS_RESOLVED)
      .setResolution(Issue.RESOLUTION_WONT_FIX)
      .setSeverity("MAJOR")
      .setType(RuleType.SECURITY_HOTSPOT);
    dbClient.issueDao().insert(session, issue1, issue2, issue3, issue4);
    session.commit();
    indexIssues();

    assertJson(ws.newRequest()
      .setParam("standard", "owaspTop10")
      .setParam("project", project.getKey())
      .setParam("includeDistribution", "true")
      .execute().getInput())
      .withStrictArrayOrder()
      .isSimilarTo(getClass().getResource("show-example.json"));
  }

  @Test
  public void sans_with_cwe() {
    userSessionRule.addProjectPermission(UserRole.USER, project);
    indexPermissions();
    ComponentDto file = insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY"));
    IssueDto issue1 = newIssue(rule1, project, file)
      .setStatus("OPEN")
      .setSeverity("MAJOR")
      .setType(RuleType.VULNERABILITY);
    IssueDto issue2 = newIssue(rule1, project, file)
      .setStatus("OPEN")
      .setSeverity("MAJOR")
      .setType(RuleType.SECURITY_HOTSPOT);
    IssueDto issue3 = newIssue(rule1, project, file)
      .setStatus(Issue.STATUS_RESOLVED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setSeverity("MAJOR")
      .setType(RuleType.SECURITY_HOTSPOT);
    IssueDto issue4 = newIssue(rule1, project, file)
      .setStatus(Issue.STATUS_RESOLVED)
      .setResolution(Issue.RESOLUTION_WONT_FIX)
      .setSeverity("MAJOR")
      .setType(RuleType.SECURITY_HOTSPOT);
    dbClient.issueDao().insert(session, issue1, issue2, issue3, issue4);
    session.commit();
    indexIssues();

    assertJson(ws.newRequest()
      .setParam("standard", "sansTop25")
      .setParam("project", project.getKey())
      .setParam("includeDistribution", "true")
      .execute().getInput())
      .withStrictArrayOrder()
      .isSimilarTo(this.getClass().getResource("ShowActionTest/sansWithCwe.json"));
  }

  @Test
  public void sans_with_cwe_for_branches() {
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setDbKey("prj1"));
    ComponentDto project1Branch1 = db.components().insertProjectBranch(project1);
    ComponentDto fileOnProject1Branch1 = db.components().insertComponent(newFileDto(project1Branch1));
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setDbKey("prj2"));

    insertQPLiveMeasure(project1);
    insertQPLiveMeasure(project1Branch1);
    insertQPLiveMeasure(project2);

    userSessionRule.addProjectPermission(UserRole.USER, project1);
    userSessionRule.addProjectPermission(UserRole.USER, project2);
    indexPermissions();
    IssueDto issue1 = newIssue(rule1, project1Branch1, fileOnProject1Branch1)
      .setStatus("OPEN")
      .setSeverity("MAJOR")
      .setType(RuleType.VULNERABILITY);
    IssueDto issue2 = newIssue(rule1, project1Branch1, fileOnProject1Branch1)
      .setStatus("OPEN")
      .setSeverity("MAJOR")
      .setType(RuleType.SECURITY_HOTSPOT);
    IssueDto issue3 = newIssue(rule1, project1Branch1, fileOnProject1Branch1)
      .setStatus(Issue.STATUS_RESOLVED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setSeverity("MAJOR")
      .setType(RuleType.SECURITY_HOTSPOT);
    IssueDto issue4 = newIssue(rule1, project1Branch1, fileOnProject1Branch1)
      .setStatus(Issue.STATUS_RESOLVED)
      .setResolution(Issue.RESOLUTION_WONT_FIX)
      .setSeverity("MAJOR")
      .setType(RuleType.SECURITY_HOTSPOT);
    dbClient.issueDao().insert(session, issue1, issue2, issue3, issue4);
    session.commit();
    indexIssues();

    assertJson(ws.newRequest()
      .setParam("standard", "sansTop25")
      .setParam("project", project1Branch1.getKey())
      .setParam("branch", project1Branch1.getBranch())
      .setParam("includeDistribution", "true")
      .execute().getInput())
      .withStrictArrayOrder()
      .isSimilarTo(this.getClass().getResource("ShowActionTest/sansWithCwe.json"));
  }

  private RuleDefinitionDto newRule(Set tags) {
    RuleDefinitionDto rule = RuleTesting.newRule()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setLanguage("java")
      .setSecurityStandards(tags);
    db.rules().insert(rule);
    return rule;
  }

  private void indexPermissions() {
    permissionIndexer.indexOnStartup(permissionIndexer.getIndexTypes());
  }

  private void indexIssues() {
    issueIndexer.indexOnStartup(issueIndexer.getIndexTypes());
  }

  private void insertQPLiveMeasure(ComponentDto project) {
    QualityProfile qp = new QualityProfile(qualityProfile.getKee(), qualityProfile.getName(), qualityProfile.getLanguage(), new Date());
    db.measures().insertLiveMeasure(project, qpMetric, lm -> lm.setData(QPMeasureData.toJson(new QPMeasureData(singletonList(qp)))));
  }

  private ComponentDto insertComponent(ComponentDto component) {
    dbClient.componentDao().insert(session, component);
    session.commit();
    return component;
  }
}
