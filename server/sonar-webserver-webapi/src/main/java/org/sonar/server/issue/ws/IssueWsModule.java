/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.issue.IssueChangeWSSupport;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.issue.NewCodePeriodResolver;
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueQueryComplianceStandardService;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflow;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowActionsFactory;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowDefinition;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflow;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowActionsFactory;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowDefinition;
import org.sonar.server.issue.ws.anticipatedtransition.AnticipatedTransitionHandler;
import org.sonar.server.issue.ws.anticipatedtransition.AnticipatedTransitionParser;
import org.sonar.server.issue.ws.anticipatedtransition.AnticipatedTransitionsAction;
import org.sonar.server.issue.ws.anticipatedtransition.AnticipatedTransitionsActionValidator;
import org.sonar.server.issue.ws.pull.PullActionProtobufObjectGenerator;
import org.sonar.server.issue.ws.pull.PullActionResponseWriter;
import org.sonar.server.issue.ws.pull.PullTaintActionProtobufObjectGenerator;
import org.sonar.server.qualitygate.changeevent.QGChangeEventListenersImpl;

public class IssueWsModule extends Module {
  @Override
  protected void configureModule() {
    add(
      IssueUpdater.class,
      IssueFinder.class,
      TransitionService.class,
      WebIssueStorage.class,
      IssueFieldsSetter.class,
      IssueWorkflow.class,
      CodeQualityIssueWorkflow.class,
      CodeQualityIssueWorkflowDefinition.class,
      CodeQualityIssueWorkflowActionsFactory.class,
      SecurityHotspotWorkflow.class,
      SecurityHotspotWorkflowActionsFactory.class,
      SecurityHotspotWorkflowDefinition.class,
      IssueQueryComplianceStandardService.class,
      IssueQueryFactory.class,
      IssuesWs.class,
      AvatarResolverImpl.class,
      IssueChangeWSSupport.class,
      SearchResponseLoader.class,
      TextRangeResponseFormatter.class,
      UserResponseFormatter.class,
      SearchResponseFormat.class,
      OperationResponseWriter.class,
      NewCodePeriodResolver.class,
      AddCommentAction.class,
      EditCommentAction.class,
      DeleteCommentAction.class,
      AssignAction.class,
      DoTransitionAction.class,
      SearchAction.class,
      ListAction.class,
      SetSeverityAction.class,
      TagsAction.class,
      SetTagsAction.class,
      SetTypeAction.class,
      ComponentTagsAction.class,
      ReindexAction.class,
      AuthorsAction.class,
      ChangelogAction.class,
      BulkChangeAction.class,
      QGChangeEventListenersImpl.class,
      TaintChecker.class,
      PullAction.class,
      PullTaintAction.class,
      PullActionResponseWriter.class,
      PullActionProtobufObjectGenerator.class,
      PullTaintActionProtobufObjectGenerator.class,
      AnticipatedTransitionParser.class,
      AnticipatedTransitionHandler.class,
      AnticipatedTransitionsActionValidator.class,
      AnticipatedTransitionsAction.class);
  }
}
