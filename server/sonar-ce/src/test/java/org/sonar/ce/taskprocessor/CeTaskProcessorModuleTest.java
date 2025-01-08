/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.sonar.ce.notification.ReportAnalysisFailureNotificationExecutionListener;
import org.sonar.core.platform.ExtensionContainer;

public class CeTaskProcessorModuleTest {
  private CeTaskProcessorModule underTest = new CeTaskProcessorModule();

  @Test
  public void defines_module() {
    var container = mock(ExtensionContainer.class);

    underTest.configure(container);

    verify(container).add(CeTaskProcessorRepositoryImpl.class);
    verify(container).add(CeLoggingWorkerExecutionListener.class);
    verify(container).add(ReportAnalysisFailureNotificationExecutionListener.class);
    verify(container).add(any(CeTaskInterrupterProvider.class));
    verify(container).add(CeTaskInterrupterWorkerExecutionListener.class);
    verify(container).add(CeWorkerFactoryImpl.class);
    verify(container).add(CeWorkerControllerImpl.class);
    verify(container).add(CeProcessingSchedulerExecutorServiceImpl.class);
    verify(container).add(CeProcessingSchedulerImpl.class);

  }
}
