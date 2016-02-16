/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.projectEvent;

import com.google.common.collect.Lists;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category4Suite;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.wsclient.services.Event;
import org.sonar.wsclient.services.EventQuery;
import util.QaOnly;
import util.selenium.SeleneseTest;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static util.ItUtils.projectDir;

@Category(QaOnly.class)
public class EventTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Before
  public void setUp() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void configuration_of_event() {
    executeAnalysis();

    orchestrator.executeSelenese(
      Selenese.builder().setHtmlTestsInClasspath("events",
        "/projectEvent/EventTest/create_event_with_special_character.html",
        "/projectEvent/EventTest/no_events_widget_on_dir.html"
        ).build()
      );
  }

  @Test
  public void delete_standard_event() {
    executeAnalysis();

    new SeleneseTest(
      Selenese.builder().setHtmlTestsInClasspath("delete-event",
        "/projectEvent/EventTest/create_delete_standard_event.html"
        ).build()).runOn(orchestrator);
  }

  @Test
  public void event_widget() {
    // first build, in the past
    executeAnalysis("sonar.projectDate", "2016-01-01");
    // Second build, today
    executeAnalysis();

    orchestrator.executeSelenese(
      Selenese.builder().setHtmlTestsInClasspath("event-widget",
        "/projectEvent/EventTest/show_events_using_filters.html"
        ).build()
      );
  }

  /**
   * SONAR-3308
   */
  @Test
  public void keep_only_one_event_per_version_in_project_history() throws Exception {
    // first analyse the 1.0-SNAPSHOT version
    executeAnalysis();
    // then analyse the 1.0 version
    executeAnalysis("sonar.projectVersion", "1.0");
    // and do this all over again
    executeAnalysis();
    executeAnalysis("sonar.projectVersion", "1.0");

    // there should be only 1 "0.1-SNAPSHOT" event and only 1 "0.1" event
    List<Event> events = orchestrator.getServer().getWsClient().findAll(new EventQuery().setResourceKey("sample"));
    assertThat(events.size(), is(2));
    List<String> eventNames = Lists.newArrayList(events.get(0).getName(), events.get(1).getName());
    assertThat(eventNames, hasItems("1.0", "1.0-SNAPSHOT"));
  }

  private static void executeAnalysis(String... properties) {
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample")).setProperties(properties);
    orchestrator.executeBuild(sampleProject);
  }
}
