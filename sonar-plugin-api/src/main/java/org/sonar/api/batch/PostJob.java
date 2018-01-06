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
package org.sonar.api.batch;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.resources.Project;

/**
 * PostJobs are executed at the very end of batch analysis. A PostJob can't do any modification
 * since everything is already computed (issues, measures,...). <br>
 * WANRING: Do not rely on the fact that analysis results are available on server side. Starting from 5.x
 * it is an asynchronous processing on server side.
 *
 * @since 1.10
 * @deprecated since 5.6 use {@link org.sonar.api.batch.postjob.PostJob}
 */
@Deprecated
@ScannerSide
@ExtensionPoint
public interface PostJob {

  void executeOn(Project project, SensorContext context);

}
