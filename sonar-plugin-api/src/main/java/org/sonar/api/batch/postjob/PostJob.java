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
package org.sonar.api.batch.postjob;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.batch.ScannerSide;

/**
 * PostJobs are executed at the very end of scanner analysis. A PostJob can't do any modification
 * since everything is already computed (issues, measures,...). <br>
 * WARNING: Do not rely on the fact that analysis results are available on server side when using WS since this is an
 * asynchronous process to compute data on server side in 5.x series.
 *
 * @since 5.2
 */
@ScannerSide
@ExtensionPoint
public interface PostJob {

  /**
   * Populate {@link PostJobDescriptor} of this PostJob.
   */
  void describe(PostJobDescriptor descriptor);

  /**
   * Called at the end of the analysis.
   */
  void execute(PostJobContext context);

}
