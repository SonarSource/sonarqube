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
package org.sonar.api.batch;

import org.sonar.api.BatchExtension;
import org.sonar.api.resources.Project;

/**
 * PostJobs are executed when project is analysed.
 * <p/>
 * <p>
 * Note : executed only on root project, not on modules.
 * </p>
 *
 * @since 1.10
 * @deprecated since 4.5.2. Starting from SQ 5.0 a big part of project analysis will be processed asynchnonously on server side. As
 * a result batch analysis will ends very quickly and probably before analysis results are persisted in SQ server referentials.
 */
@Deprecated
public interface PostJob extends BatchExtension {

  void executeOn(Project project, SensorContext context);

}
