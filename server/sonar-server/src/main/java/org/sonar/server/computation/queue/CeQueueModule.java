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
package org.sonar.server.computation.queue;

import org.sonar.ce.queue.report.ReportFiles;
import org.sonar.core.platform.Module;
import org.sonar.server.computation.monitoring.CEQueueStatusImpl;
import org.sonar.server.computation.monitoring.CeTasksMBeanImpl;
import org.sonar.server.computation.queue.report.CleanReportQueueListener;

public class CeQueueModule extends Module {
  @Override
  protected void configureModule() {
    add(
      // queue state
      InternalCeQueueImpl.class,

      // queue monitoring
      CEQueueStatusImpl.class,
      CeTasksMBeanImpl.class,

      // queue cleaning
      CeQueueCleaner.class,
      CleanReportQueueListener.class,
      ReportFiles.class,

      // init queue state and queue processing
      CeQueueInitializer.class);
  }
}
