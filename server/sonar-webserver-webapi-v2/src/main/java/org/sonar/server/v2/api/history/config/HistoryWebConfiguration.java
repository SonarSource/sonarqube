/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.history.config;

import org.sonar.server.v2.api.history.controller.DefaultIssueCountHistoryController;
import org.sonar.server.v2.api.history.controller.DefaultIssueDensityHistoryController;
import org.sonar.server.v2.api.history.controller.DefaultIssueResolutionHistoryController;
import org.sonar.server.v2.api.history.controller.DefaultMeasuresHistoryController;
import org.sonar.server.v2.api.history.controller.DefaultProjectIssueCountsController;
import org.sonar.server.v2.api.history.controller.DefaultProjectIssueResolutionController;
import org.sonar.server.v2.api.history.controller.DefaultProjectMeasuresController;
import org.sonar.server.v2.api.history.controller.ProjectCollectionContextLoader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Registers the history controllers in the Web API v2 application context. */
@Configuration
@Import({
  DefaultIssueCountHistoryController.class,
  DefaultIssueDensityHistoryController.class,
  DefaultIssueResolutionHistoryController.class,
  DefaultMeasuresHistoryController.class,
  DefaultProjectIssueCountsController.class,
  DefaultProjectIssueResolutionController.class,
  DefaultProjectMeasuresController.class,
  ProjectCollectionContextLoader.class
})
public class HistoryWebConfiguration {
}
