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
package org.sonar.server.projectanalysis.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.web.UserRole;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.AnalysisPropertyDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventComponentChangeDto;
import org.sonar.db.event.EventComponentChangeDto.ChangeCategory;
import org.sonar.db.event.EventDto;
import org.sonar.db.event.EventPurgeData;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common.Paging;
import org.sonarqube.ws.ProjectAnalyses.Analysis;
import org.sonarqube.ws.ProjectAnalyses.Event;
import org.sonarqube.ws.ProjectAnalyses.Project;
import org.sonarqube.ws.ProjectAnalyses.SearchResponse;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.DateUtils.formatDate;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.event.EventComponentChangeDto.ChangeCategory.ADDED;
import static org.sonar.db.event.EventComponentChangeDto.ChangeCategory.FAILED_QUALITY_GATE;
import static org.sonar.db.event.EventComponentChangeDto.ChangeCategory.REMOVED;
import static org.sonar.db.event.EventDto.CATEGORY_ALERT;
import static org.sonar.db.event.EventTesting.newEvent;
import static org.sonar.server.projectanalysis.ws.EventCategory.DEFINITION_CHANGE;
import static org.sonar.server.projectanalysis.ws.EventCategory.OTHER;
import static org.sonar.server.projectanalysis.ws.EventCategory.QUALITY_GATE;
import static org.sonar.server.projectanalysis.ws.EventCategory.VERSION;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_BRANCH;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_CATEGORY;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_FROM;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_PROJECT;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_TO;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.WsRequest.Method.POST;

@RunWith(DataProviderRunner.class)
public class SearchActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public LogTester logTester = new LogTester();

  private final DbClient dbClient = db.getDbClient();
  private final WsActionTester ws = new WsActionTester(new SearchAction(dbClient, TestComponentFinder.from(db), userSession));
  private final UuidFactoryFast uuidFactoryFast = UuidFactoryFast.getInstance();

  @DataProvider
  public static Object[][] changedBranches() {
    return new Object[][]{
      {null, "newbranch"},
      {"newbranch", "anotherbranch"},
      {"newbranch", null},
    };
  }

  @Test
  public void json_example() {
    ProjectData projectData = db.components().insertPrivateProject(c -> c.setKey(KEY_PROJECT_EXAMPLE_001));
    ComponentDto mainBranch = projectData.getMainBranchComponent();

    addProjectPermission(projectData);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(mainBranch)
      .setUuid("A1")
      .setCreatedAt(parseDateTime("2016-12-11T17:12:45+0100").getTime())
      .setProjectVersion("1.2")
      .setBuildString("1.2.0.322")
      .setRevision("bfe36592eb7f9f2708b5d358b5b5f33ed535c8cf"));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(mainBranch)
      .setUuid("A2")
      .setCreatedAt(parseDateTime("2016-12-12T17:12:45+0100").getTime())
      .setProjectVersion("1.2.1")
      .setBuildString("1.2.1.423")
      .setRevision("be6c75b85da526349c44e3978374c95e0b80a96d"));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(mainBranch)
      .setUuid("P1")
      .setCreatedAt(parseDateTime("2015-11-11T10:00:00+0100").getTime())
      .setProjectVersion("1.2")
      .setBuildString("1.2.0.321"));
    db.getDbClient().analysisPropertiesDao().insert(db.getSession(), new AnalysisPropertyDto()
      .setUuid("P1-prop-uuid")
      .setAnalysisUuid(a3.getUuid())
      .setKey(CorePropertyDefinitions.SONAR_ANALYSIS_DETECTEDCI)
      .setValue("Jenkins")
      .setCreatedAt(1L));

    db.newCodePeriods().insert(new NewCodePeriodDto()
      .setProjectUuid(projectData.getProjectDto().getUuid())
      .setBranchUuid(mainBranch.uuid())
      .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
      .setValue(a1.getUuid()));
    db.commit();
    db.events().insertEvent(newEvent(a1).setUuid("AXt91FkXy_c4CIP4ds6A")
      .setName("Failed")
      .setCategory(QUALITY_GATE.getLabel())
      .setDescription(
        "Coverage on New Code < 85, Reliability Rating > 4, Maintainability Rating on New Code > 1, Reliability Rating on New Code > 1, Security Rating on New Code > 1, Duplicated Lines (%) on New Code > 3"));
    db.events().insertEvent(newEvent(a1).setUuid("AXx_QFJ6Wa8wkfuJ6r5P")
      .setName("6.3").setCategory(VERSION.getLabel()));
    db.events().insertEvent(newEvent(a2).setUuid("E21")
      .setName("Quality Profile changed to Sonar Way")
        .setData("key=Profile Key;languageKey=The Key of the Language;name=Profile Name")
      .setCategory(EventCategory.QUALITY_PROFILE.getLabel()));
    db.events().insertEvent(newEvent(a2).setUuid("E22")
      .setName("6.3").setCategory(OTHER.getLabel()));
    db.events().insertEvent(newEvent(a2).setUuid("E23")
      .setName("Use 'Sonar Way update profile' (JavaScript)")
      .setCategory(EventCategory.QUALITY_PROFILE.getLabel()));

    EventDto eventDto = db.events().insertEvent(newEvent(a3)
      .setUuid("E31")
      .setName("Quality Gate is Red")
      .setData("{stillFailing: true, status: \"ERROR\"}")
      .setCategory(CATEGORY_ALERT)
      .setDescription(""));
    EventComponentChangeDto changeDto1 = generateEventComponentChange(eventDto, FAILED_QUALITY_GATE, "My project", "app1", "master", mainBranch.uuid());
    EventComponentChangeDto changeDto2 = generateEventComponentChange(eventDto, FAILED_QUALITY_GATE, "Another project", "app2", "master", uuidFactoryFast.create());
    insertEventComponentChanges(mainBranch, a3, changeDto1, changeDto2);

    String result = ws.newRequest()
      .setParam(PARAM_PROJECT, KEY_PROJECT_EXAMPLE_001)
      .execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("search-example.json"));
  }

  private void addProjectPermission(ProjectData projectData) {
    userSession.addProjectPermission(UserRole.USER, projectData.getProjectDto())
      .addProjectBranchMapping(projectData.getProjectDto().getUuid(), projectData.getMainBranchComponent());
  }

  @Test
  public void return_analyses_ordered_by_analysis_date() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A1").setCreatedAt(1_000_000L));
    db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A2").setCreatedAt(2_000_000L));
    db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A3").setCreatedAt(3_000_000L));

    List<Analysis> result = call(mainBranch.getKey()).getAnalysesList();

    assertThat(result).hasSize(3);
    assertThat(result).extracting(Analysis::getKey, a -> parseDateTime(a.getDate()).getTime()).containsExactly(
      tuple("A3", 3_000_000L),
      tuple("A2", 2_000_000L),
      tuple("A1", 1_000_000L));
  }

  @Test
  public void return_only_processed_analyses() {
    ProjectData projectData = db.components().insertPrivateProject(c -> c.setKey("P1"));
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A1"));
    db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A2").setStatus(SnapshotDto.STATUS_UNPROCESSED));

    List<Analysis> result = call("P1").getAnalysesList();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getKey()).isEqualTo("A1");
  }

  @Test
  public void return_events() {
    ProjectData projectData = db.components().insertPrivateProject(c -> c.setKey("P1"));
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A1"));
    SnapshotDto a42 = db.components().insertSnapshot(newAnalysis(ComponentTesting.newPrivateProjectDto()).setUuid("A42"));
    EventDto e1 = db.events().insertEvent(newEvent(a1).setUuid("E1").setName("N1").setCategory(QUALITY_GATE.getLabel()).setDescription("D1"));
    EventDto e2 = db.events().insertEvent(newEvent(a1).setUuid("E2").setName("N2").setCategory(VERSION.getLabel()).setDescription("D2"));
    db.events().insertEvent(newEvent(a42));

    List<Analysis> result = call("P1").getAnalysesList();

    assertThat(result).hasSize(1);
    List<Event> events = result.get(0).getEventsList();
    assertThat(events).hasSize(2);
    assertThat(events).extracting(Event::getKey, wsToDbCategory(), Event::getName, Event::getDescription).containsOnly(
      tuple(e1.getUuid(), e1.getCategory(), e1.getName(), e1.getDescription()),
      tuple(e2.getUuid(), e2.getCategory(), e2.getName(), e2.getDescription()));
  }

  @Test
  public void return_analyses_of_application() {
    ProjectData appData = db.components().insertPublicApplication();
    ComponentDto appMainBranch = appData.getMainBranchComponent();
    registerApplication(appData);
    SnapshotDto firstAnalysis = db.components().insertSnapshot(newAnalysis(appMainBranch).setCreatedAt(1_000_000L));
    SnapshotDto secondAnalysis = db.components().insertSnapshot(newAnalysis(appMainBranch).setCreatedAt(2_000_000L));
    SnapshotDto thirdAnalysis = db.components().insertSnapshot(newAnalysis(appMainBranch).setCreatedAt(3_000_000L));

    List<Analysis> result = call(appMainBranch.getKey()).getAnalysesList();

    assertThat(result)
      .hasSize(3)
      .extracting(Analysis::getKey).containsExactly(thirdAnalysis.getUuid(), secondAnalysis.getUuid(), firstAnalysis.getUuid());

    assertThat(result.get(0).getEventsList()).isEmpty();
    assertThat(result.get(1).getEventsList()).isEmpty();
    assertThat(result.get(2).getEventsList()).isEmpty();
  }

  @Test
  public void return_definition_change_events_on_application_analyses() {
    ProjectData appData = db.components().insertPublicApplication();
    ComponentDto mainBranch = appData.getMainBranchComponent();
    registerApplication(appData);
    SnapshotDto firstAnalysis = db.components().insertSnapshot(newAnalysis(mainBranch).setCreatedAt(1_000_000L));
    EventDto event = db.events().insertEvent(newEvent(firstAnalysis).setName("").setUuid("E11").setCategory(DEFINITION_CHANGE.getLabel()));
    EventComponentChangeDto changeDto1 = generateEventComponentChange(event, ADDED, "My project", "app1", "master", uuidFactoryFast.create());
    EventComponentChangeDto changeDto2 = generateEventComponentChange(event, REMOVED, "Another project", "app2", "master", uuidFactoryFast.create());
    insertEventComponentChanges(mainBranch, firstAnalysis, changeDto1, changeDto2);

    List<Analysis> result = call(mainBranch.getKey()).getAnalysesList();

    assertThat(result).hasSize(1);
    List<Event> events = result.get(0).getEventsList();
    assertThat(events)
      .extracting(Event::getName, Event::getCategory, Event::getKey)
      .containsExactly(tuple("", DEFINITION_CHANGE.name(), "E11"));
    assertThat(events.get(0).getDefinitionChange().getProjectsList())
      .extracting(Project::getChangeType, Project::getName, Project::getKey, Project::getNewBranch, Project::getOldBranch)
      .containsExactly(
        tuple("ADDED", "My project", "app1", "", ""),
        tuple("REMOVED", "Another project", "app2", "", ""));
  }

  @Test
  @UseDataProvider("changedBranches")
  public void application_definition_change_with_branch(@Nullable String oldBranch, @Nullable String newBranch) {
    ProjectData appData = db.components().insertPublicApplication();
    ComponentDto appMainBranch = appData.getMainBranchComponent();
    registerApplication(appData);
    SnapshotDto firstAnalysis = db.components().insertSnapshot(newAnalysis(appMainBranch).setCreatedAt(1_000_000L));
    EventDto event = db.events().insertEvent(newEvent(firstAnalysis).setName("").setUuid("E11").setCategory(DEFINITION_CHANGE.getLabel()));
    EventComponentChangeDto changeDto1 = generateEventComponentChange(event, REMOVED, "My project", "app1", oldBranch, uuidFactoryFast.create());
    EventComponentChangeDto changeDto2 = generateEventComponentChange(event, ADDED, "My project", "app1", newBranch, changeDto1.getComponentUuid());
    insertEventComponentChanges(appMainBranch, firstAnalysis, changeDto1, changeDto2);

    List<Analysis> result = call(appMainBranch.getKey()).getAnalysesList();

    assertThat(result).hasSize(1);
    List<Event> events = result.get(0).getEventsList();
    assertThat(events)
      .extracting(Event::getName, Event::getCategory, Event::getKey)
      .containsExactly(tuple("", DEFINITION_CHANGE.name(), "E11"));
    assertThat(events.get(0).getDefinitionChange().getProjectsList())
      .extracting(Project::getChangeType, Project::getKey, Project::getName, Project::getNewBranch, Project::getOldBranch)
      .containsExactly(tuple("BRANCH_CHANGED", "app1", "My project", newBranch == null ? "" : newBranch, oldBranch == null ? "" : oldBranch));
  }

  @Test
  public void incorrect_eventcomponentchange_two_identical_changes_added_on_same_project() {
    ProjectData appData = db.components().insertPublicApplication();
    ComponentDto appMainBranch = appData.getMainBranchComponent();
    registerApplication(appData);
    SnapshotDto firstAnalysis = db.components().insertSnapshot(newAnalysis(appMainBranch).setCreatedAt(1_000_000L));
    EventDto event = db.events().insertEvent(newEvent(firstAnalysis).setName("").setUuid("E11").setCategory(DEFINITION_CHANGE.getLabel()));
    EventComponentChangeDto changeDto1 = generateEventComponentChange(event, ADDED, "My project", "app1", "master", uuidFactoryFast.create());
    EventComponentChangeDto changeDto2 = generateEventComponentChange(event, ADDED, "My project", "app1", "master", uuidFactoryFast.create());
    EventPurgeData eventPurgeData = new EventPurgeData(appMainBranch.uuid(), firstAnalysis.getUuid());
    db.getDbClient().eventComponentChangeDao().insert(db.getSession(), changeDto1, eventPurgeData);
    db.getDbClient().eventComponentChangeDao().insert(db.getSession(), changeDto2, eventPurgeData);
    db.getSession().commit();

    List<Analysis> result = call(appMainBranch.getKey()).getAnalysesList();

    assertThat(result).hasSize(1);
    List<Event> events = result.get(0).getEventsList();
    assertThat(events)
      .extracting(Event::getName, Event::getCategory, Event::getKey)
      .containsExactly(tuple("", DEFINITION_CHANGE.name(), "E11"));
    assertThat(events.get(0).getDefinitionChange().getProjectsList())
      .isEmpty();

    assertThat(logTester.getLogs(LoggerLevel.ERROR))
      .extracting(LogAndArguments::getFormattedMsg)
      .contains(
        format("Incorrect changes : [uuid=%s change=ADDED, branch=master] and [uuid=%s, change=ADDED, branch=master]", changeDto1.getUuid(), changeDto2.getUuid()));
  }

  @Test
  public void incorrect_eventcomponentchange_incorrect_category() {
    ProjectData appData = db.components().insertPublicApplication();
    ComponentDto appMainBranch = appData.getMainBranchComponent();
    registerApplication(appData);
    SnapshotDto firstAnalysis = db.components().insertSnapshot(newAnalysis(appMainBranch).setCreatedAt(1_000_000L));
    EventDto event = db.events().insertEvent(newEvent(firstAnalysis).setName("").setUuid("E11").setCategory(DEFINITION_CHANGE.getLabel()));
    EventComponentChangeDto changeDto1 = generateEventComponentChange(event, FAILED_QUALITY_GATE, "My project", "app1", "master", uuidFactoryFast.create());
    EventPurgeData eventPurgeData = new EventPurgeData(appMainBranch.uuid(), firstAnalysis.getUuid());
    db.getDbClient().eventComponentChangeDao().insert(db.getSession(), changeDto1, eventPurgeData);
    db.getSession().commit();

    List<Analysis> result = call(appMainBranch.getKey()).getAnalysesList();

    assertThat(result).hasSize(1);
    List<Event> events = result.get(0).getEventsList();
    assertThat(events)
      .extracting(Event::getName, Event::getCategory, Event::getKey)
      .containsExactly(tuple("", DEFINITION_CHANGE.name(), "E11"));
    assertThat(events.get(0).getDefinitionChange().getProjectsList())
      .isEmpty();

    assertThat(logTester.getLogs(LoggerLevel.ERROR))
      .extracting(LogAndArguments::getFormattedMsg)
      .contains("Unknown change FAILED_QUALITY_GATE for eventComponentChange uuid: " + changeDto1.getUuid());
  }

  @Test
  public void incorrect_eventcomponentchange_three_component_changes_on_same_project() {
    ProjectData appData = db.components().insertPublicApplication();
    ComponentDto appMainBranch = appData.getMainBranchComponent();
    registerApplication(appData);
    SnapshotDto firstAnalysis = db.components().insertSnapshot(newAnalysis(appMainBranch).setCreatedAt(1_000_000L));
    EventDto event = db.events().insertEvent(newEvent(firstAnalysis).setName("").setUuid("E11").setCategory(DEFINITION_CHANGE.getLabel()));
    EventComponentChangeDto changeDto1 = generateEventComponentChange(event, ADDED, "My project", "app1", "master", uuidFactoryFast.create());
    EventComponentChangeDto changeDto2 = generateEventComponentChange(event, REMOVED, "Another project", "app1", "", uuidFactoryFast.create());
    EventComponentChangeDto changeDto3 = generateEventComponentChange(event, REMOVED, "Another project", "app1", "", uuidFactoryFast.create());
    EventPurgeData eventPurgeData = new EventPurgeData(appMainBranch.uuid(), firstAnalysis.getUuid());
    db.getDbClient().eventComponentChangeDao().insert(db.getSession(), changeDto1, eventPurgeData);
    db.getDbClient().eventComponentChangeDao().insert(db.getSession(), changeDto2, eventPurgeData);
    db.getDbClient().eventComponentChangeDao().insert(db.getSession(), changeDto3, eventPurgeData);
    db.getSession().commit();

    List<Analysis> result = call(appMainBranch.getKey()).getAnalysesList();

    assertThat(result).hasSize(1);
    List<Event> events = result.get(0).getEventsList();
    assertThat(events)
      .extracting(Event::getName, Event::getCategory, Event::getKey)
      .containsExactly(tuple("", DEFINITION_CHANGE.name(), "E11"));
    assertThat(events.get(0).getDefinitionChange().getProjectsList())
      .isEmpty();

    assertThat(logTester.getLogs(LoggerLevel.ERROR))
      .extracting(LogAndArguments::getFormattedMsg)
      .contains(
        format("Too many changes on same project (3) for eventComponentChange uuids : %s,%s,%s", changeDto1.getUuid(), changeDto2.getUuid(), changeDto3.getUuid()));
  }

  private void registerApplication(ProjectData appData) {
    userSession.registerApplication(appData.getProjectDto())
      .addProjectBranchMapping(appData.projectUuid(), appData.getMainBranchComponent());
  }

  @Test
  public void incorrect_quality_gate_information() {
    ProjectData appData = db.components().insertPublicApplication();
    ComponentDto appMainBranch = appData.getMainBranchComponent();
    registerApplication(appData);
    SnapshotDto firstAnalysis = db.components().insertSnapshot(newAnalysis(appMainBranch).setCreatedAt(1_000_000L));
    EventDto event = db.events().insertEvent(
      newEvent(firstAnalysis)
        .setName("")
        .setUuid("E11")
        .setCategory(CATEGORY_ALERT)
        .setData("UNPARSEABLE JSON")); // Error in Data
    EventComponentChangeDto changeDto1 = generateEventComponentChange(event, FAILED_QUALITY_GATE, "My project", "app1", "master", uuidFactoryFast.create());
    EventPurgeData eventPurgeData = new EventPurgeData(appMainBranch.uuid(), firstAnalysis.getUuid());
    db.getDbClient().eventComponentChangeDao().insert(db.getSession(), changeDto1, eventPurgeData);
    db.getSession().commit();

    List<Analysis> result = call(appMainBranch.getKey()).getAnalysesList();

    assertThat(result).hasSize(1);
    List<Event> events = result.get(0).getEventsList();
    assertThat(events)
      .extracting(Event::getName, Event::getCategory, Event::getKey)
      .containsExactly(tuple("", QUALITY_GATE.name(), "E11"));

    // Verify that the values are not populated
    assertThat(events.get(0).getQualityGate().hasStatus()).isFalse();
    assertThat(events.get(0).getQualityGate().hasStillFailing()).isFalse();

    assertThat(logTester.getLogs(LoggerLevel.ERROR))
      .extracting(LogAndArguments::getFormattedMsg)
      .contains("Unable to retrieve data from event uuid=E11");
  }

  @Test
  public void return_analyses_of_portfolio() {
    ComponentDto view = db.components().insertPublicPortfolio();
    userSession.registerPortfolios(view);
    SnapshotDto firstAnalysis = db.components().insertSnapshot(newAnalysis(view).setCreatedAt(1_000_000L));
    SnapshotDto secondAnalysis = db.components().insertSnapshot(newAnalysis(view).setCreatedAt(2_000_000L));
    SnapshotDto thirdAnalysis = db.components().insertSnapshot(newAnalysis(view).setCreatedAt(3_000_000L));

    List<Analysis> result = call(view.getKey()).getAnalysesList();

    assertThat(result)
      .hasSize(3)
      .extracting(Analysis::getKey).containsExactly(thirdAnalysis.getUuid(), secondAnalysis.getUuid(), firstAnalysis.getUuid());
  }

  @Test
  public void paginate_analyses() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    IntStream.rangeClosed(1, 9).forEach(i -> db.components().insertSnapshot(newAnalysis(mainBranch).setCreatedAt(1_000_000L * i).setUuid("A" + i)));

    SearchResponse result = call(SearchRequest.builder()
      .setProject(mainBranch.getKey())
      .setPage(2)
      .setPageSize(3)
      .build());

    assertThat(result.getAnalysesList()).extracting(Analysis::getKey)
      .containsExactly("A6", "A5", "A4");
  }

  @Test
  public void filter_by_category() {
    ProjectData projectData = db.components().insertPrivateProject(c -> c.setKey("P1"));
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A1"));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A2"));
    SnapshotDto a42 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A42"));
    db.events().insertEvent(newEvent(a1).setUuid("E11").setCategory(VERSION.getLabel()));
    db.events().insertEvent(newEvent(a1).setUuid("E12").setCategory(QUALITY_GATE.getLabel()));
    db.events().insertEvent(newEvent(a2).setUuid("E21").setCategory(QUALITY_GATE.getLabel()));
    // Analysis A42 doesn't have a quality gate event
    db.events().insertEvent(newEvent(a42).setCategory(OTHER.getLabel()));

    List<Analysis> result = call(SearchRequest.builder()
      .setProject("P1")
      .setCategory(QUALITY_GATE)
      .build()).getAnalysesList();

    assertThat(result).extracting(Analysis::getKey).containsOnly("A1", "A2");
  }

  @Test
  public void paginate_with_filter_on_category() {
    ProjectData projectData = db.components().insertPrivateProject(c -> c.setKey("P1"));
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A1").setCreatedAt(1_000_000L));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A2").setCreatedAt(2_000_000L));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A3").setCreatedAt(3_000_000L));
    SnapshotDto a42 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("A42"));
    db.events().insertEvent(newEvent(a1).setUuid("E11").setCategory(VERSION.getLabel()));
    db.events().insertEvent(newEvent(a1).setUuid("E12").setCategory(QUALITY_GATE.getLabel()));
    db.events().insertEvent(newEvent(a2).setUuid("E21").setCategory(QUALITY_GATE.getLabel()));
    db.events().insertEvent(newEvent(a3).setUuid("E31").setCategory(QUALITY_GATE.getLabel()));
    // Analysis A42 doesn't have a quality gate event
    db.events().insertEvent(newEvent(a42).setCategory(OTHER.getLabel()));

    SearchResponse result = call(SearchRequest.builder()
      .setProject("P1")
      .setCategory(QUALITY_GATE)
      .setPage(2)
      .setPageSize(1)
      .build());

    assertThat(result.getAnalysesList()).extracting(Analysis::getKey).containsOnly("A2");
    assertThat(result.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(2, 1, 3);
  }

  @Test
  public void filter_from_date() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a1").setCreatedAt(1_000_000_000L));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a2").setCreatedAt(2_000_000_000L));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a3").setCreatedAt(3_000_000_000L));
    SnapshotDto a4 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a4").setCreatedAt(4_000_000_000L));

    SearchResponse result = call(SearchRequest.builder()
      .setProject(mainBranch.getKey())
      .setFrom(formatDateTime(2_000_000_000L))
      .build());

    assertThat(result.getAnalysesList())
      .extracting(Analysis::getKey)
      .containsOnly(a2.getUuid(), a3.getUuid(), a4.getUuid())
      .doesNotContain(a1.getUuid());
  }

  @Test
  public void filter_to_date() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a1").setCreatedAt(1_000_000_000L));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a2").setCreatedAt(2_000_000_000L));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a3").setCreatedAt(3_000_000_000L));
    SnapshotDto a4 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a4").setCreatedAt(4_000_000_000L));

    SearchResponse result = call(SearchRequest.builder()
      .setProject(mainBranch.getKey())
      .setTo(formatDateTime(2_000_000_000L))
      .build());

    assertThat(result.getAnalysesList())
      .extracting(Analysis::getKey)
      .containsOnly(a1.getUuid(), a2.getUuid())
      .doesNotContain(a3.getUuid(), a4.getUuid());
  }

  @Test
  public void filter_by_dates_using_datetime_format() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a1").setCreatedAt(1_000_000_000L));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a2").setCreatedAt(2_000_000_000L));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a3").setCreatedAt(3_000_000_000L));
    SnapshotDto a4 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a4").setCreatedAt(4_000_000_000L));

    SearchResponse result = call(SearchRequest.builder()
      .setProject(mainBranch.getKey())
      .setFrom(formatDateTime(2_000_000_000L))
      .setTo(formatDateTime(3_000_000_000L))
      .build());

    assertThat(result.getAnalysesList())
      .extracting(Analysis::getKey)
      .containsOnly(a2.getUuid(), a3.getUuid())
      .doesNotContain(a1.getUuid(), a4.getUuid());
  }

  @Test
  public void filter_by_dates_using_date_format() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a1").setCreatedAt(1_000_000_000L));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a2").setCreatedAt(2_000_000_000L));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a3").setCreatedAt(3_000_000_000L));
    SnapshotDto a4 = db.components().insertSnapshot(newAnalysis(mainBranch).setUuid("a4").setCreatedAt(4_000_000_000L));

    SearchResponse result = call(SearchRequest.builder()
      .setProject(mainBranch.getKey())
      .setFrom(formatDate(new Date(2_000_000_000L)))
      .setTo(formatDate(new Date(3_000_000_000L)))
      .build());

    assertThat(result.getAnalysesList())
      .extracting(Analysis::getKey)
      .containsOnly(a2.getUuid(), a3.getUuid())
      .doesNotContain(a1.getUuid(), a4.getUuid());
  }

  @Test
  public void branch() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setKey("my_branch"));
    userSession.addProjectBranchMapping(projectData.projectUuid(), branch);
    SnapshotDto analysis = db.components().insertSnapshot(newAnalysis(branch));
    EventDto event = db.events().insertEvent(newEvent(analysis).setCategory(QUALITY_GATE.getLabel()));

    List<Analysis> result = call(SearchRequest.builder()
      .setProject(mainBranch.getKey())
      .setBranch("my_branch")
      .build())
      .getAnalysesList();

    assertThat(result).extracting(Analysis::getKey).containsExactlyInAnyOrder(analysis.getUuid());
    assertThat(result.get(0).getEventsList()).extracting(Event::getKey).containsExactlyInAnyOrder(event.getUuid());
  }

  @Test
  public void empty_response() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);

    SearchResponse result = call(mainBranch.getKey());

    assertThat(result.hasPaging()).isTrue();
    assertThat(result.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal).containsExactly(1, 100, 0);
    assertThat(result.getAnalysesCount()).isZero();
  }

  @Test
  public void populates_projectVersion_and_buildString() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto[] analyses = new SnapshotDto[]{
      db.components().insertSnapshot(newAnalysis(mainBranch).setProjectVersion(null).setBuildString(null)),
      db.components().insertSnapshot(newAnalysis(mainBranch).setProjectVersion("a").setBuildString(null)),
      db.components().insertSnapshot(newAnalysis(mainBranch).setProjectVersion(null).setBuildString("b")),
      db.components().insertSnapshot(newAnalysis(mainBranch).setProjectVersion("c").setBuildString("d"))
    };

    SearchResponse result = call(mainBranch.getKey());

    assertThat(result.getAnalysesList())
      .extracting(Analysis::getKey, Analysis::getProjectVersion, Analysis::getBuildString)
      .containsOnly(
        tuple(analyses[0].getUuid(), "", ""),
        tuple(analyses[1].getUuid(), "a", ""),
        tuple(analyses[2].getUuid(), "", "b"),
        tuple(analyses[3].getUuid(), "c", "d"));
  }

  @Test
  public void fail_if_not_enough_permissions() {
    userSession.anonymous();
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();

    var projectDbKey = mainBranch.getKey();
    assertThatThrownBy(() -> call(projectDbKey))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_not_enough_permissions_on_applications_projects() {
    ProjectDto application = db.components().insertPrivateApplication().getProjectDto();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();

    userSession.logIn()
      .registerApplication(application)
      .registerProjects(project1, project2)
      .addProjectPermission(UserRole.USER, application, project1);

    var projectDbKey = application.getKey();
    assertThatThrownBy(() -> call(projectDbKey))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_project_does_not_exist() {
    assertThatThrownBy(() -> call("P1"))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_if_not_a_project_portfolio_or_application() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch));
    db.components().insertSnapshot(newAnalysis(mainBranch));
    userSession.registerProjects(projectData.getProjectDto());

    var fileDbKey = file.getKey();
    assertThatThrownBy(() -> call(fileDbKey))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A project, portfolio or application is required");
  }

  @Test
  public void fail_if_branch_does_not_exist() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    db.components().insertProjectBranch(mainBranch, b -> b.setKey("my_branch"));

    var searchRequest = SearchRequest.builder()
      .setProject(mainBranch.getKey())
      .setBranch("another_branch")
      .build();

    assertThatThrownBy(() -> call(searchRequest))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component '%s' on branch '%s' not found", mainBranch.getKey(), "another_branch"));
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("search");
    assertThat(definition.since()).isEqualTo("6.3");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.param("project").isRequired()).isTrue();
    assertThat(definition.param("category")).isNotNull();
    assertThat(definition.params()).hasSize(8);

    Param from = definition.param("from");
    assertThat(from.since()).isEqualTo("6.5");

    Param to = definition.param("to");
    assertThat(to.since()).isEqualTo("6.5");

    Param branch = definition.param("branch");
    assertThat(branch.since()).isEqualTo("6.6");
    assertThat(branch.isInternal()).isTrue();
    assertThat(branch.isRequired()).isFalse();
  }

  private EventComponentChangeDto generateEventComponentChange(EventDto event, ChangeCategory category, String name, String key, @Nullable String branch,
    String componentUuid) {
    return new EventComponentChangeDto()
      .setCategory(category)
      .setUuid(uuidFactoryFast.create())
      .setComponentName(name)
      .setComponentKey(key)
      .setComponentBranchKey(branch)
      .setComponentUuid(componentUuid)
      .setEventUuid(event.getUuid());
  }

  private void insertEventComponentChanges(ComponentDto component, SnapshotDto analysis, EventComponentChangeDto... changes) {
    EventPurgeData eventPurgeData = new EventPurgeData(component.uuid(), analysis.getUuid());
    for (EventComponentChangeDto change : changes) {
      db.getDbClient().eventComponentChangeDao().insert(db.getSession(), change, eventPurgeData);
    }
    db.getSession().commit();
  }

  private static Function<Event, String> wsToDbCategory() {
    return e -> e == null ? null : EventCategory.valueOf(e.getCategory()).getLabel();
  }

  private SearchResponse call(@Nullable String project) {
    SearchRequest.Builder request = SearchRequest.builder();
    ofNullable(project).ifPresent(request::setProject);
    return call(request.build());
  }

  private SearchResponse call(SearchRequest wsRequest) {
    TestRequest request = ws.newRequest()
      .setMethod(POST.name());
    ofNullable(wsRequest.getProject()).ifPresent(project -> request.setParam(PARAM_PROJECT, project));
    ofNullable(wsRequest.getBranch()).ifPresent(branch1 -> request.setParam(PARAM_BRANCH, branch1));
    ofNullable(wsRequest.getCategory()).ifPresent(category -> request.setParam(PARAM_CATEGORY, category.name()));
    ofNullable(wsRequest.getPage()).ifPresent(page -> request.setParam(Param.PAGE, String.valueOf(page)));
    ofNullable(wsRequest.getPageSize()).ifPresent(pageSize -> request.setParam(Param.PAGE_SIZE, String.valueOf(pageSize)));
    ofNullable(wsRequest.getFrom()).ifPresent(from -> request.setParam(PARAM_FROM, from));
    ofNullable(wsRequest.getTo()).ifPresent(to -> request.setParam(PARAM_TO, to));

    return request.executeProtobuf(SearchResponse.class);
  }
}
