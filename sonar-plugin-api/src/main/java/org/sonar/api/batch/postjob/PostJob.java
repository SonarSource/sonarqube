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
package org.sonar.api.batch.postjob;

import com.google.common.annotations.Beta;
import org.sonar.api.BatchSide;
import org.sonar.api.ExtensionPoint;

/**
 * PostJobs are executed at the very end of batch analysis. A PostJob can't do any modification
 * since everything is already computed (issues, measures,...). <br/>
 * WANRING: Do not rely on the fact that analysis results are available on server side using WS since this is an
 * asynchronous process to compute data on server side in 5.x series.
 *
 * @since 5.2
 */
@Beta
@BatchSide
@ExtensionPoint
public interface PostJob {

  /**
   * Populate {@link PostJobDescriptor} of this PostJob.
   */
  void describe(PostJobDescriptor descriptor);

  /**
   * The actual sensor code.
   */
  void execute(PostJobContext context);

}
