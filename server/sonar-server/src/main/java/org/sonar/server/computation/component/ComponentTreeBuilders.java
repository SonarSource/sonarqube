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
import org.sonar.server.computation.ComputationContext;

import static java.util.Objects.requireNonNull;

public final class ComponentTreeBuilders {
  public static ComponentTreeBuilder from(final BatchReportReader reportReader) {
    // fail fast
    requireNonNull(reportReader);

    return new BatchReportComponentTreeBuilderImpl(reportReader);
  }

  public static ComponentTreeBuilder from(final Component root) {
    // fail-fast
    requireNonNull(root);
    return new ComponentTreeBuilder() {
      @Override
      public Component build(ComputationContext context) {
        return root;
      }
    };
  }

  public interface BatchReportComponentTreeBuilder extends ComponentTreeBuilder {

  }

  private static class BatchReportComponentTreeBuilderImpl implements BatchReportComponentTreeBuilder {
    private final BatchReportReader reportReader;

    public BatchReportComponentTreeBuilderImpl(BatchReportReader reportReader) {
      this.reportReader = reportReader;
    }

    @Override
    public Component build(ComputationContext context) {
      return buildComponentRoot(context, reportReader);
    }

    private Component buildComponentRoot(ComputationContext computationContext, BatchReportReader reportReader) {
      int rootComponentRef = computationContext.getReportMetadata().getRootComponentRef();
      BatchReport.Component component = reportReader.readComponent(rootComponentRef);
      return new ComponentImpl(computationContext, component, buildComponent(computationContext, rootComponentRef));
    }

    private Iterable<Component> buildComponent(final ComputationContext computationContext, int componentRef) {
      BatchReport.Component component = computationContext.getReportReader().readComponent(componentRef);
      return Iterables.transform(
          component.getChildRefList(),
          new Function<Integer, Component>() {
            @Override
            public Component apply(@Nonnull Integer componentRef) {
              BatchReport.Component component = computationContext.getReportReader().readComponent(componentRef);
              return new ComponentImpl(computationContext, component, buildComponent(computationContext, componentRef));
            }
          }
      );
    }
  }
}
