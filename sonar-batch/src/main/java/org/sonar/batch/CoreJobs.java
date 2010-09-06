/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch;

import java.util.ArrayList;
import java.util.List;

/**
 * Core batch extensions, instanciated by picocontainer.
 */
public final class CoreJobs {

  private CoreJobs() {
  }

  public static List<Class<? extends CoreJob>> allJobs() {
    List<Class<? extends CoreJob>> classes = new ArrayList<Class<? extends CoreJob>>();
    classes.add(MavenPluginsConfigurator.class);
    classes.add(MavenPhaseExecutor.class);
    classes.add(SensorsExecutor.class);
    classes.add(DecoratorsExecutor.class);
    classes.add(FinalizeSnapshotsJob.class);
    classes.add(PostJobsExecutor.class);
    return classes;
  }
}
