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

package org.sonar.server.computation.batch;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentImpl;
import org.sonar.server.computation.component.ReportComponent;

public class ComponentTreeRule implements TestRule {

  @CheckForNull
  private final BatchReportReader batchReportReader;
  private final BUILD_OPTIONS buildOptions;

  private Component root;

  private ComponentTreeRule(BatchReportReader batchReportReader, BUILD_OPTIONS buildOptions) {
    this.batchReportReader = batchReportReader;
    this.buildOptions = buildOptions;
  }

  public static ComponentTreeRule from(BatchReportReader batchReportReader, BUILD_OPTIONS buildOptions) {
    return new ComponentTreeRule(Objects.requireNonNull(batchReportReader), buildOptions);
  }

  public static ComponentTreeRule from(BatchReportReader batchReportReader) {
    return new ComponentTreeRule(Objects.requireNonNull(batchReportReader), BUILD_OPTIONS.NONE);
  }

  @Override
  public Statement apply(final Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          statement.evaluate();
        } finally {
          clear();
        }
      }
    };
  }

  private void clear() {
    this.root = null;
  }

  public enum BUILD_OPTIONS {
    NONE(false, false), KEY(false, true), UUID(true, false), KEY_AND_UUID(true, true);
    private final boolean uuid;
    private final boolean key;

    BUILD_OPTIONS(boolean uuid, boolean key) {
      this.uuid = uuid;
      this.key = key;
    }
  }

  public Component getRoot() {
    if (root == null) {
      buildComponentRoot(buildOptions);
    }
    return this.root;
  }

  private Component buildComponentRoot(BUILD_OPTIONS build_options) {
    int rootComponentRef = batchReportReader.readMetadata().getRootComponentRef();
    return newComponent(batchReportReader.readComponent(rootComponentRef), build_options);
  }

  private ReportComponent newComponent(BatchReport.Component component, BUILD_OPTIONS build_options) {
    return ReportComponent.builder(ComponentImpl.convertType(component.getType()), component.getRef())
      .setUuid(build_options.uuid ? uuidOf(component.getRef()) : null)
      .setKey(build_options.key ? keyOf(component.getRef()) : null)
      .addChildren(buildChildren(component, build_options))
      .build();
  }

  private Component[] buildChildren(BatchReport.Component component, final BUILD_OPTIONS build_options) {
    return Iterables.toArray(
      Iterables.transform(
        component.getChildRefList(),
        new Function<Integer, Component>() {
          @Override
          public Component apply(@Nonnull Integer componentRef) {
            return newComponent(batchReportReader.readComponent(componentRef), build_options);
          }
        }
        ), Component.class);
  }

  public String keyOf(int ref) {
    return "key_" + ref;
  }

  public String uuidOf(int ref) {
    return "uuid_" + ref;
  }
}
