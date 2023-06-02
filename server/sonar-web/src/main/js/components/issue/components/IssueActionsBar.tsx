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
import { updateIssue } from '../actions';
import IssueAssign from './IssueAssign';
import IssueCommentAction from './IssueCommentAction';
import IssueMessageTags from './IssueMessageTags';
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

export default class IssueActionsBar extends React.PureComponent<Props, State> {
  state: State = {
    commentAutoTriggered: false,
    commentPlaceholder: '',
  };

  setIssueProperty = (
    property: keyof Issue,
    popup: string,
    apiCall: (query: RawQuery) => Promise<IssueResponse>,
    value: string
  ) => {
    const { issue } = this.props;
    if (issue[property] !== value) {
      const newIssue = { ...issue, [property]: value };
      updateIssue(
        this.props.onChange,
        apiCall({ issue: issue.key, [property]: value }),
        issue,
        newIssue
      );
    }
    this.props.togglePopup(popup, false);
  };

  toggleComment = (open: boolean | undefined, placeholder = '', autoTriggered = false) => {
    this.setState({
      commentPlaceholder: placeholder,
      commentAutoTriggered: autoTriggered,
    });
    this.props.togglePopup('comment', open);
  };

  handleTransition = (issue: Issue) => {
    this.props.onChange(issue);
    if (
      issue.resolution === IssueResolution.FalsePositive ||
      (issue.resolution === IssueResolution.WontFix && issue.type !== IssueTypeEnum.SecurityHotspot)
    ) {
      this.toggleComment(true, translate('issue.comment.explain_why'), true);
    }
  };

  render() {
    const { issue, className, showComments, showCommentsInPopup, showLine } = this.props;
    const canAssign = issue.actions.includes(IssueActions.Assign);
    const canComment = issue.actions.includes(IssueActions.Comment);
    const canSetSeverity = issue.actions.includes(IssueActions.SetSeverity);
    const canSetType = issue.actions.includes(IssueActions.SetType);
    const hasTransitions = issue.transitions.length > 0;
    const hasComments = issue.comments && issue.comments.length > 0;
    const IssueMetaLiClass = classNames(
      className,
      'sw-body-sm sw-overflow-hidden sw-whitespace-nowrap sw-max-w-abs-150'
    );

    return (
      <div className="sw-flex sw-flex-wrap sw-items-center sw-justify-between">
        <ul className="sw-flex sw-items-center sw-gap-3 sw-body-sm">
          <li>
            <IssueType
              canSetType={canSetType}
              issue={issue}
              setIssueProperty={this.setIssueProperty}
            />
          </li>
          <li>
            <IssueSeverity
              isOpen={this.props.currentPopup === 'set-severity'}
              togglePopup={this.props.togglePopup}
              canSetSeverity={canSetSeverity}
              issue={issue}
              setIssueProperty={this.setIssueProperty}
            />
          </li>
          <li>
            <IssueTransition
              isOpen={this.props.currentPopup === 'transition'}
              togglePopup={this.props.togglePopup}
              hasTransitions={hasTransitions}
              issue={issue}
              onChange={this.handleTransition}
            />
          </li>
          <li>
            <IssueAssign
              isOpen={this.props.currentPopup === 'assign'}
              togglePopup={this.props.togglePopup}
              canAssign={canAssign}
              issue={issue}
              onAssign={this.props.onAssign}
            />
          </li>
        </ul>
        {(canComment || showCommentsInPopup) && (
          <IssueCommentAction
            commentAutoTriggered={this.state.commentAutoTriggered}
            commentPlaceholder={this.state.commentPlaceholder}
            currentPopup={this.props.currentPopup}
            issueKey={issue.key}
            onChange={this.props.onChange}
            toggleComment={this.toggleComment}
            comments={issue.comments}
            canComment={canComment}
            showCommentsInPopup={showCommentsInPopup}
          />
        )}

        <ul className="sw-flex sw-items-center sw-gap-2 sw-body-sm">
          <li className={IssueMetaLiClass}>
            <IssueMessageTags
              engine={issue.externalRuleEngine}
              quickFixAvailable={issue.quickFixAvailable}
              ruleStatus={issue.ruleStatus as RuleStatus | undefined}
            />
          </li>

          {issue.externalRuleEngine && (
            <li className={IssueMetaLiClass}>
              <Tooltip
                overlay={translateWithParameters(
                  'issue.from_external_rule_engine',
                  issue.externalRuleEngine
                )}
              >
                <Badge>{issue.externalRuleEngine}</Badge>
              </Tooltip>
            </li>
          )}

          {issue.codeVariants && issue.codeVariants.length > 0 && (
            <IssueMetaLi>
              <Tooltip overlay={issue.codeVariants.join(', ')}>
                <>
                  {issue.codeVariants.length > 1
                    ? translateWithParameters('issue.x_code_variants', issue.codeVariants.length)
                    : translate('issue.1_code_variant')}
                </>
              </Tooltip>
              <SeparatorCircleIcon aria-hidden={true} as="li" />
            </IssueMetaLi>
          )}

          {showComments && hasComments && (
            <>
              <IssueMetaLi className={IssueMetaLiClass}>
                <CommentIcon aria-label={translate('issue.comment.formlink')} />
                {issue.comments?.length}
              </IssueMetaLi>
              <SeparatorCircleIcon aria-hidden={true} as="li" />
            </>
          )}
          {showLine && isDefined(issue.textRange) && (
            <>
              <Tooltip overlay={translate('line_number')}>
                <IssueMetaLi className={IssueMetaLiClass}>
                  {translateWithParameters('issue.ncloc_x.short', issue.textRange.endLine)}
                </IssueMetaLi>
              </Tooltip>
              <SeparatorCircleIcon aria-hidden={true} as="li" />
            </>
          )}
          {issue.effort && (
            <>
              <IssueMetaLi className={IssueMetaLiClass}>
                {translateWithParameters('issue.x_effort', issue.effort)}
              </IssueMetaLi>
              <SeparatorCircleIcon aria-hidden={true} as="li" />
            </>
          )}
          <IssueMetaLi className={IssueMetaLiClass}>
            <DateFromNow date={issue.creationDate} />
          </IssueMetaLi>
        </ul>
      </div>
    );
  }
}

const IssueMetaLi = styled.li`
  color: ${themeColor('pageContentLight')};
`;
