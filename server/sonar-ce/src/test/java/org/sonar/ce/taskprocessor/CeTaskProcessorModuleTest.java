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
package org.sonar.ce.taskprocessor;

import org.junit.Test;
import org.picocontainer.ComponentAdapter;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.ce.notification.ReportAnalysisFailureNotificationExecutionListener;
import org.sonar.ce.task.CeTaskInterrupter;
import org.sonar.core.platform.ComponentContainer;

import static org.assertj.core.api.Assertions.assertThat;

public class CeTaskProcessorModuleTest {
  private CeTaskProcessorModule underTest = new CeTaskProcessorModule();

  @Test
  public void defines_CeWorker_ExecutionListener_for_CeLogging() {
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    assertThat(container.getPicoContainer().getComponentAdapters(CeWorker.ExecutionListener.class)
      .stream()
      .map(ComponentAdapter::getComponentImplementation))
        .contains(CeLoggingWorkerExecutionListener.class);
  }

  @Test
  public void defines_ExecutionListener_for_report_processing_failure_notifications() {
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    assertThat(container.getPicoContainer().getComponentAdapters(CeWorker.ExecutionListener.class)
      .stream()
      .map(ComponentAdapter::getComponentImplementation))
        .contains(ReportAnalysisFailureNotificationExecutionListener.class);
  }

  @Test
  public void defines_CeTaskInterrupterProvider_object() {
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);


    assertThat(container.getPicoContainer().getComponentAdapter(CeTaskInterrupter.class))
      .isInstanceOf(CeTaskInterrupterProvider.class);
  }
}
