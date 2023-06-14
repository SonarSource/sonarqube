/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import styled from '@emotion/styled';
import classNames from 'classnames';
import { Badge, CommentIcon, SeparatorCircleIcon, themeColor } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import {
  IssueActions,
  IssueResolution,
  IssueResponse,
  IssueType as IssueTypeEnum,
} from '../../../types/issues';
import { RuleStatus } from '../../../types/rules';
import { Issue, RawQuery } from '../../../types/types';
import Tooltip from '../../controls/Tooltip';
import DateFromNow from '../../intl/DateFromNow';
import { WorkspaceContext } from '../../workspace/context';
import { updateIssue } from '../actions';
import IssueAssign from './IssueAssign';
import IssueBadges from './IssueBadges';
import IssueCommentAction from './IssueCommentAction';
import IssueSeverity from './IssueSeverity';
import IssueTransition from './IssueTransition';
import IssueType from './IssueType';

interface Props {
  issue: Issue;
  currentPopup?: string;
  onAssign: (login: string) => void;
  onChange: (issue: Issue) => void;
  togglePopup: (popup: string, show?: boolean) => void;
  className?: string;
  showComments?: boolean;
  showCommentsInPopup?: boolean;
  showLine?: boolean;
}

interface State {
  commentAutoTriggered: boolean;
  commentPlaceholder: string;
}

export default function IssueActionsBar(props: Props) {
  const {
    issue,
    currentPopup,
    onAssign,
    onChange,
    togglePopup,
    className,
    showComments,
    showCommentsInPopup,
    showLine,
  } = props;

  const [commentState, setCommentState] = React.useState<State>({
    commentAutoTriggered: false,
    commentPlaceholder: '',
  });

  const setIssueProperty = (
    property: keyof Issue,
    popup: string,
    apiCall: (query: RawQuery) => Promise<IssueResponse>,
    value: string
  ) => {
    if (issue[property] !== value) {
      const newIssue = { ...issue, [property]: value };
      updateIssue(onChange, apiCall({ issue: issue.key, [property]: value }), issue, newIssue);
    }

    togglePopup(popup, false);
  };

  const toggleComment = (open: boolean, placeholder = '', autoTriggered = false) => {
    setCommentState({
      commentPlaceholder: placeholder,
      commentAutoTriggered: autoTriggered,
    });

    togglePopup('comment', open);
  };

  const handleTransition = (issue: Issue) => {
    onChange(issue);

    if (
      issue.resolution === IssueResolution.FalsePositive ||
      (issue.resolution === IssueResolution.WontFix && issue.type !== IssueTypeEnum.SecurityHotspot)
    ) {
      toggleComment(true, translate('issue.comment.explain_why'), true);
    }
  };

  const { externalRulesRepoNames } = React.useContext(WorkspaceContext);

  const ruleEngine =
    (issue.externalRuleEngine && externalRulesRepoNames[issue.externalRuleEngine]) ||
    issue.externalRuleEngine;

  const canAssign = issue.actions.includes(IssueActions.Assign);
  const canComment = issue.actions.includes(IssueActions.Comment);
  const canSetSeverity = issue.actions.includes(IssueActions.SetSeverity);
  const canSetType = issue.actions.includes(IssueActions.SetType);
  const hasTransitions = issue.transitions.length > 0;
  const hasComments = !!issue.comments?.length;

  const issueMetaListItemClassNames = classNames(
    className,
    'sw-body-sm sw-overflow-hidden sw-whitespace-nowrap sw-max-w-abs-150'
  );

  return (
    <div
      className={classNames(className, 'sw-flex sw-flex-wrap sw-items-center sw-justify-between')}
    >
      <ul className="it__issue-header-actions sw-flex sw-items-center sw-gap-3 sw-body-sm">
        <li>
          <IssueType canSetType={canSetType} issue={issue} setIssueProperty={setIssueProperty} />
        </li>

        <li>
          <IssueSeverity
            isOpen={currentPopup === 'set-severity'}
            togglePopup={togglePopup}
            canSetSeverity={canSetSeverity}
            issue={issue}
            setIssueProperty={setIssueProperty}
          />
        </li>

        <li>
          <IssueTransition
            isOpen={currentPopup === 'transition'}
            togglePopup={togglePopup}
            hasTransitions={hasTransitions}
            issue={issue}
            onChange={handleTransition}
          />
        </li>

        <li>
          <IssueAssign
            isOpen={currentPopup === 'assign'}
            togglePopup={togglePopup}
            canAssign={canAssign}
            issue={issue}
            onAssign={onAssign}
          />
        </li>
      </ul>

      {(canComment || showCommentsInPopup) && (
        <IssueCommentAction
          commentAutoTriggered={commentState.commentAutoTriggered}
          commentPlaceholder={commentState.commentPlaceholder}
          currentPopup={currentPopup === 'comment'}
          issueKey={issue.key}
          onChange={onChange}
          toggleComment={toggleComment}
          comments={issue.comments}
          canComment={canComment}
          showCommentsInPopup={showCommentsInPopup}
        />
      )}

      <ul className="sw-flex sw-items-center sw-gap-2 sw-body-sm">
        <li className={issueMetaListItemClassNames}>
          <IssueBadges
            quickFixAvailable={issue.quickFixAvailable}
            ruleStatus={issue.ruleStatus as RuleStatus | undefined}
          />
        </li>

        {ruleEngine && (
          <li className={issueMetaListItemClassNames}>
            <Tooltip
              overlay={translateWithParameters('issue.from_external_rule_engine', ruleEngine)}
            >
              <Badge>{ruleEngine}</Badge>
            </Tooltip>
          </li>
        )}

        {!!issue.codeVariants?.length && (
          <>
            <IssueMetaListItem>
              <Tooltip overlay={issue.codeVariants.join(', ')}>
                <>
                  {issue.codeVariants.length > 1
                    ? translateWithParameters('issue.x_code_variants', issue.codeVariants.length)
                    : translate('issue.1_code_variant')}
                </>
              </Tooltip>
            </IssueMetaListItem>
            <SeparatorCircleIcon aria-hidden as="li" />
          </>
        )}

        {showComments && hasComments && (
          <>
            <IssueMetaListItem className={issueMetaListItemClassNames}>
              <CommentIcon aria-label={translate('issue.comment.formlink')} />
              {issue.comments?.length}
            </IssueMetaListItem>

            <SeparatorCircleIcon aria-hidden as="li" />
          </>
        )}

        {showLine && isDefined(issue.textRange) && (
          <>
            <Tooltip overlay={translate('line_number')}>
              <IssueMetaListItem className={issueMetaListItemClassNames}>
                {translateWithParameters('issue.ncloc_x.short', issue.textRange.endLine)}
              </IssueMetaListItem>
            </Tooltip>

            <SeparatorCircleIcon aria-hidden as="li" />
          </>
        )}

        {issue.effort && (
          <>
            <IssueMetaListItem className={issueMetaListItemClassNames}>
              {translateWithParameters('issue.x_effort', issue.effort)}
            </IssueMetaListItem>

            <SeparatorCircleIcon aria-hidden as="li" />
          </>
        )}

        <IssueMetaListItem className={issueMetaListItemClassNames}>
          <DateFromNow date={issue.creationDate} />
        </IssueMetaListItem>
      </ul>
    </div>
  );
}

const IssueMetaListItem = styled.li`
  color: ${themeColor('pageContentLight')};
`;
