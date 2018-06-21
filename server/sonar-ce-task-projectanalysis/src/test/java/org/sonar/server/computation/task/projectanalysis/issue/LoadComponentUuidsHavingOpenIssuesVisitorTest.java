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
package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.Arrays;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.VisitorsCrawler;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.MODULE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;

public class LoadComponentUuidsHavingOpenIssuesVisitorTest {

  BaseIssuesLoader baseIssuesLoader = mock(BaseIssuesLoader.class);
  ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues = mock(ComponentsWithUnprocessedIssues.class);
  VisitorsCrawler underTest = new VisitorsCrawler(Arrays.asList(new LoadComponentUuidsHavingOpenIssuesVisitor(baseIssuesLoader, componentsWithUnprocessedIssues)));

  @Test
  public void set_issues_when_visiting_project() {
    when(baseIssuesLoader.loadUuidsOfComponentsWithOpenIssues()).thenReturn(newHashSet("FILE1", "FILE2"));

    underTest.visit(ReportComponent.builder(PROJECT, 1).build());

    verify(componentsWithUnprocessedIssues).setUuids(newHashSet("FILE1", "FILE2"));
  }

  @Test
  public void do_nothing_on_not_project_level() {
    when(baseIssuesLoader.loadUuidsOfComponentsWithOpenIssues()).thenReturn(newHashSet("FILE1", "FILE2"));

    underTest.visit(ReportComponent.builder(MODULE, 1).build());
    underTest.visit(ReportComponent.builder(DIRECTORY, 1).build());
    underTest.visit(ReportComponent.builder(FILE, 1).build());

    verifyZeroInteractions(componentsWithUnprocessedIssues);
  }
}
