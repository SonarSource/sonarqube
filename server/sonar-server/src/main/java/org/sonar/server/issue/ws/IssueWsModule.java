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
package org.sonar.server.issue.ws;

import org.sonar.core.platform.Module;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.issue.IssueQueryFactory;
import org.sonar.server.issue.IssueUpdater;
import org.sonar.server.issue.ServerIssueStorage;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.qualitygate.changeevent.QGChangeEventListenersImpl;
import org.sonar.server.settings.ProjectConfigurationLoaderImpl;
import org.sonar.server.webhook.WebhookQGChangeEventListener;
import org.sonar.server.ws.WsResponseCommonFormat;

public class IssueWsModule extends Module {
  @Override
  protected void configureModule() {
    add(
      IssueUpdater.class,
      IssueFinder.class,
      TransitionService.class,
      ServerIssueStorage.class,
      IssueFieldsSetter.class,
      FunctionExecutor.class,
      IssueWorkflow.class,
      IssueQueryFactory.class,
      IssuesWs.class,
      AvatarResolverImpl.class,
      SearchResponseLoader.class,
      SearchResponseFormat.class,
      OperationResponseWriter.class,
      WsResponseCommonFormat.class,
      AddCommentAction.class,
      EditCommentAction.class,
      DeleteCommentAction.class,
      AssignAction.class,
      DoTransitionAction.class,
      SearchAction.class,
      SetSeverityAction.class,
      TagsAction.class,
      SetTagsAction.class,
      SetTypeAction.class,
      ComponentTagsAction.class,
      AuthorsAction.class,
      ChangelogAction.class,
      BulkChangeAction.class,
      ProjectConfigurationLoaderImpl.class,
      WebhookQGChangeEventListener.class,
      QGChangeEventListenersImpl.class);
  }
}
