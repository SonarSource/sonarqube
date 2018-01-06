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
package org.sonar.server.computation.task.projectanalysis.component;

import java.util.ArrayList;
import java.util.List;

class CallRecorderTypeAwareVisitor extends TypeAwareVisitorAdapter {
  final List<CallRecord> callsRecords = new ArrayList<>();

  public CallRecorderTypeAwareVisitor(CrawlerDepthLimit maxDepth, Order order) {
    super(maxDepth, order);
  }

  @Override
  public void visitProject(Component project) {
    callsRecords.add(reportCallRecord(project, "visitProject"));
  }

  @Override
  public void visitModule(Component module) {
    callsRecords.add(reportCallRecord(module, "visitModule"));
  }

  @Override
  public void visitDirectory(Component directory) {
    callsRecords.add(reportCallRecord(directory, "visitDirectory"));
  }

  @Override
  public void visitFile(Component file) {
    callsRecords.add(reportCallRecord(file, "visitFile"));
  }

  @Override
  public void visitView(Component view) {
    callsRecords.add(viewsCallRecord(view, "visitView"));
  }

  @Override
  public void visitSubView(Component subView) {
    callsRecords.add(viewsCallRecord(subView, "visitSubView"));
  }

  @Override
  public void visitProjectView(Component projectView) {
    callsRecords.add(viewsCallRecord(projectView, "visitProjectView"));
  }

  @Override
  public void visitAny(Component component) {
    callsRecords.add(component.getType().isReportType() ? reportCallRecord(component, "visitAny") : viewsCallRecord(component, "visitAny"));
  }

  private static CallRecord reportCallRecord(Component component, String method) {
    return CallRecord.reportCallRecord(method, component.getReportAttributes().getRef());
  }

  private static CallRecord viewsCallRecord(Component component, String method) {
    return CallRecord.viewsCallRecord(method, component.getKey());
  }

}
