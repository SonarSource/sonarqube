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
package org.sonar.ce.task.projectanalysis.component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

public class ReportPostOrderDepthTraversalTypeAwareCrawlerTest {

  private static final Component FILE_5 = component(FILE, 5);
  private static final Component FILE_6 = component(FILE, 6);
  private static final Component DIRECTORY_4 = component(DIRECTORY, 4, FILE_5, FILE_6);
  private static final Component COMPONENT_TREE = component(PROJECT, 1, DIRECTORY_4);

  private final CallRecorderTypeAwareVisitor projectVisitor = new CallRecorderTypeAwareVisitor(CrawlerDepthLimit.PROJECT, POST_ORDER);
  private final CallRecorderTypeAwareVisitor directoryVisitor = new CallRecorderTypeAwareVisitor(CrawlerDepthLimit.DIRECTORY, POST_ORDER);
  private final CallRecorderTypeAwareVisitor fileVisitor = new CallRecorderTypeAwareVisitor(CrawlerDepthLimit.FILE, POST_ORDER);
  private final DepthTraversalTypeAwareCrawler projectCrawler = new DepthTraversalTypeAwareCrawler(projectVisitor);
  private final DepthTraversalTypeAwareCrawler directoryCrawler = new DepthTraversalTypeAwareCrawler(directoryVisitor);
  private final DepthTraversalTypeAwareCrawler fileCrawler = new DepthTraversalTypeAwareCrawler(fileVisitor);

  @Test(expected = NullPointerException.class)
  public void visit_null_Component_throws_NPE() {
    fileCrawler.visit(null);
  }

  @Test
  public void visit_file_with_depth_FILE_calls_visit_file() {
    Component component = component(FILE, 1);
    fileCrawler.visit(component);

    assertThat(fileVisitor.callsRecords).containsExactly(
      reportCallRecord("visitAny", component),
      reportCallRecord("visitFile", component));
  }

  @Test
  public void visit_directory_with_depth_FILE_calls_visit_directory() {
    Component component = component(DIRECTORY, 1);
    fileCrawler.visit(component);

    assertThat(fileVisitor.callsRecords).containsExactly(
      reportCallRecord("visitAny", component),
      reportCallRecord("visitDirectory", component));
  }

  @Test
  public void visit_project_with_depth_FILE_calls_visit_project() {
    Component component = component(PROJECT, 1);
    fileCrawler.visit(component);

    assertThat(fileVisitor.callsRecords).containsExactly(
      reportCallRecord("visitAny", component),
      reportCallRecord("visitProject", component));
  }

  @Test
  public void visit_file_with_depth_DIRECTORY_does_not_call_visit_file_nor_visitAny() {
    Component component = component(FILE, 1);
    directoryCrawler.visit(component);

    assertThat(directoryVisitor.callsRecords).isEmpty();
  }

  @Test
  public void visit_directory_with_depth_DIRECTORY_calls_visit_directory() {
    Component component = component(DIRECTORY, 1);
    directoryCrawler.visit(component);

    assertThat(directoryVisitor.callsRecords).containsExactly(
      reportCallRecord("visitAny", component),
      reportCallRecord("visitDirectory", component));
  }

  @Test
  public void visit_project_with_depth_DIRECTORY_calls_visit_project() {
    Component component = component(PROJECT, 1);
    directoryCrawler.visit(component);

    assertThat(directoryVisitor.callsRecords).containsExactly(
      reportCallRecord("visitAny", component),
      reportCallRecord("visitProject", component));
  }

  @Test
  public void visit_file_with_depth_PROJECT_does_not_call_visit_file_nor_visitAny() {
    Component component = component(FILE, 1);
    projectCrawler.visit(component);

    assertThat(projectVisitor.callsRecords).isEmpty();
  }

  @Test
  public void visit_directory_with_depth_PROJECT_does_not_call_visit_directory_nor_visitAny() {
    Component component = component(DIRECTORY, 1);
    projectCrawler.visit(component);

    assertThat(projectVisitor.callsRecords).isEmpty();
  }

  @Test
  public void visit_project_with_depth_PROJECT_calls_visit_project() {
    Component component = component(PROJECT, 1);
    projectCrawler.visit(component);

    assertThat(projectVisitor.callsRecords).containsExactly(
      reportCallRecord("visitAny", component),
      reportCallRecord("visitProject", component));
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_FILE() {
    fileCrawler.visit(COMPONENT_TREE);

    assertThat(fileVisitor.callsRecords).containsExactly(
      reportCallRecord("visitAny", FILE_5),
      reportCallRecord("visitFile", FILE_5),
      reportCallRecord("visitAny", FILE_6),
      reportCallRecord("visitFile", FILE_6),
      reportCallRecord("visitAny", DIRECTORY_4),
      reportCallRecord("visitDirectory", DIRECTORY_4),
      reportCallRecord("visitAny", COMPONENT_TREE),
      reportCallRecord("visitProject", COMPONENT_TREE));
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_DIRECTORY() {
    directoryCrawler.visit(COMPONENT_TREE);

    assertThat(directoryVisitor.callsRecords).containsExactly(
      reportCallRecord("visitAny", DIRECTORY_4),
      reportCallRecord("visitDirectory", DIRECTORY_4),
      reportCallRecord("visitAny", COMPONENT_TREE),
      reportCallRecord("visitProject", COMPONENT_TREE));
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_PROJECT() {
    projectCrawler.visit(COMPONENT_TREE);

    assertThat(projectVisitor.callsRecords).containsExactly(
      reportCallRecord("visitAny", COMPONENT_TREE),
      reportCallRecord("visitProject", COMPONENT_TREE));
  }

  private static Component component(final Component.Type type, final int ref, final Component... children) {
    return ReportComponent.builder(type, ref).addChildren(children).build();
  }

  private static CallRecord reportCallRecord(String methodName, Component component) {
    return CallRecord.reportCallRecord(methodName, component.getReportAttributes().getRef());
  }

}
