/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import IssueAssign from './IssueAssign';
import IssueCommentAction from './IssueCommentAction';
import IssueSeverity from './IssueSeverity';
import IssueTags from './IssueTags';
import IssueTransition from './IssueTransition';
import IssueType from './IssueType';
import { updateIssue } from '../actions';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { RawQuery } from '../../../helpers/query';
import { IssueResponse } from '../../../api/issues';

interface Props {
  issue: T.Issue;
  currentPopup?: string;
  onAssign: (login: string) => void;
  onChange: (issue: T.Issue) => void;
  togglePopup: (popup: string, show?: boolean) => void;
}

interface State {
  commentAutoTriggered: boolean;
  commentPlaceholder: string;
}

export default class IssueActionsBar extends React.PureComponent<Props, State> {
  state: State = {
    commentAutoTriggered: false,
    commentPlaceholder: ''
  };

  setIssueProperty = (
    property: keyof T.Issue,
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
      commentAutoTriggered: autoTriggered
    });
    this.props.togglePopup('comment', open);
  };

  handleTransition = (issue: T.Issue) => {
    this.props.onChange(issue);
    if (
      issue.resolution === 'FALSE-POSITIVE' ||
      (issue.resolution === 'WONTFIX' && issue.type !== 'SECURITY_HOTSPOT')
    ) {
      this.toggleComment(true, translate('issue.comment.explain_why'), true);
    }
  };

  render() {
    const { issue } = this.props;
    const canAssign = issue.actions.includes('assign');
    const canComment = issue.actions.includes('comment');
    const canSetSeverity = issue.actions.includes('set_severity');
    const canSetType = issue.actions.includes('set_type');
    const canSetTags = issue.actions.includes('set_tags');
    const hasTransitions = issue.transitions && issue.transitions.length > 0;
    const isSecurityHotspot = issue.type === 'SECURITY_HOTSPOT';

    return (
      <div className="issue-actions">
        <ul className="issue-meta-list">
          <li className="issue-meta">
            <IssueType
              canSetType={canSetType}
              isOpen={this.props.currentPopup === 'set-type' && canSetType}
              issue={issue}
              setIssueProperty={this.setIssueProperty}
              togglePopup={this.props.togglePopup}
            />
          </li>
          {!isSecurityHotspot && (
            <li className="issue-meta">
              <IssueSeverity
                canSetSeverity={canSetSeverity}
                isOpen={this.props.currentPopup === 'set-severity' && canSetSeverity}
                issue={issue}
                setIssueProperty={this.setIssueProperty}
                togglePopup={this.props.togglePopup}
              />
            </li>
          )}
          <li className="issue-meta">
            <IssueTransition
              hasTransitions={hasTransitions}
              isOpen={this.props.currentPopup === 'transition' && hasTransitions}
              issue={issue}
              onChange={this.handleTransition}
              togglePopup={this.props.togglePopup}
            />
          </li>
          {!isSecurityHotspot && (
            <li className="issue-meta">
              <IssueAssign
                canAssign={canAssign}
                isOpen={this.props.currentPopup === 'assign' && canAssign}
                issue={issue}
                onAssign={this.props.onAssign}
                togglePopup={this.props.togglePopup}
              />
            </li>
          )}
          {!isSecurityHotspot && issue.effort && (
            <li className="issue-meta">
              <span className="issue-meta-label">
                {translateWithParameters('issue.x_effort', issue.effort)}
              </span>
            </li>
          )}
          {canComment && (
            <IssueCommentAction
              commentAutoTriggered={this.state.commentAutoTriggered}
              commentPlaceholder={this.state.commentPlaceholder}
              currentPopup={this.props.currentPopup}
              issueKey={issue.key}
              onChange={this.props.onChange}
              toggleComment={this.toggleComment}
            />
          )}
        </ul>
        <ul className="list-inline">
          <li className="issue-meta js-issue-tags">
            <IssueTags
              canSetTags={canSetTags}
              isOpen={this.props.currentPopup === 'edit-tags' && canSetTags}
              issue={issue}
              onChange={this.props.onChange}
              togglePopup={this.props.togglePopup}
            />
          </li>
        </ul>
      </div>
    );
  }
}
