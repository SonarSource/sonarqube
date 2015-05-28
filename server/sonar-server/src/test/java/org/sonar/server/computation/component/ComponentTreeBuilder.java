/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.component;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import javax.annotation.Nonnull;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.batch.BatchReportReader;

public final class ComponentTreeBuilder {
  public static Component from(final BatchReportReader reportReader) {
    return buildComponentRoot(reportReader);
  }

  private static Component buildComponentRoot(BatchReportReader reportReader) {
    int rootComponentRef = reportReader.readMetadata().getRootComponentRef();
    BatchReport.Component component = reportReader.readComponent(rootComponentRef);
    return newComponent(reportReader, component);
  }

  private static Iterable<Component> buildChildren(final BatchReportReader reportReader, final BatchReport.Component component) {
    return Iterables.transform(
        component.getChildRefList(),
        new Function<Integer, Component>() {
          @Override
          public Component apply(@Nonnull Integer componentRef) {
            BatchReport.Component component = reportReader.readComponent(componentRef);
            return newComponent(reportReader, component);
          }
        }
    );
  }

  private static Component newComponent(BatchReportReader reportReader, BatchReport.Component component) {
    return new ComponentImpl(component, buildChildren(reportReader, component));
  }
}
