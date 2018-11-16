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

import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;

class CallRecorderPathAwareVisitor extends PathAwareVisitorAdapter<Integer> {
  final List<PathAwareCallRecord> callsRecords = new ArrayList<>();

  public CallRecorderPathAwareVisitor(CrawlerDepthLimit maxDepth, Order order) {
    super(maxDepth, order, new SimpleStackElementFactory<Integer>() {
      @Override
      public Integer createForAny(Component component) {
        return component.getType().isReportType() ? component.getReportAttributes().getRef() : Integer.valueOf(component.getDbKey());
      }
    });
  }

  @Override
  public void visitProject(Component project, Path<Integer> path) {
    callsRecords.add(reportCallRecord(project, path, "visitProject"));
  }

  @Override
  public void visitDirectory(Component directory, Path<Integer> path) {
    callsRecords.add(reportCallRecord(directory, path, "visitDirectory"));
  }

  @Override
  public void visitFile(Component file, Path<Integer> path) {
    callsRecords.add(reportCallRecord(file, path, "visitFile"));
  }

  @Override
  public void visitView(Component view, Path<Integer> path) {
    callsRecords.add(viewsCallRecord(view, path, "visitView"));
  }

  @Override
  public void visitSubView(Component subView, Path<Integer> path) {
    callsRecords.add(viewsCallRecord(subView, path, "visitSubView"));
  }

  @Override
  public void visitProjectView(Component projectView, Path<Integer> path) {
    callsRecords.add(viewsCallRecord(projectView, path, "visitProjectView"));
  }

  @Override
  public void visitAny(Component component, Path<Integer> path) {
    callsRecords.add(component.getType().isReportType() ? reportCallRecord(component, path, "visitAny") : viewsCallRecord(component, path, "visitAny"));
  }

  private static PathAwareCallRecord reportCallRecord(Component component, Path<Integer> path, String method) {
    return PathAwareCallRecord.reportCallRecord(method, component.getReportAttributes().getRef(), path.current(), getParent(path), path.root(),
      toValueList(path));
  }

  private static PathAwareCallRecord viewsCallRecord(Component component, Path<Integer> path, String method) {
    return PathAwareCallRecord.viewsCallRecord(method, component.getDbKey(), path.current(), getParent(path), path.root(),
      toValueList(path));
  }

  private static List<Integer> toValueList(Path<Integer> path) {
    return from(path.getCurrentPath()).transform(new Function<PathElement<Integer>, Integer>() {
      @Nonnull
      @Override
      public Integer apply(@Nonnull PathElement<Integer> input) {
        return input.getElement();
      }
    }).toList();
  }

  private static Integer getParent(Path<Integer> path) {
    try {
      Integer parent = path.parent();
      checkArgument(parent != null, "Path.parent returned a null value!");
      return parent;
    } catch (NoSuchElementException e) {
      return null;
    }
  }
}
