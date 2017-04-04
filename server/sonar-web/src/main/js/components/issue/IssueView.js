/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import classNames from 'classnames';
import Checkbox from '../../components/controls/Checkbox';
import IssueTitleBar from './components/IssueTitleBar';
import IssueActionsBar from './components/IssueActionsBar';
import IssueCommentLine from './components/IssueCommentLine';
import { deleteIssueComment, editIssueComment } from '../../api/issues';
import type { Issue } from './types';

type Props = {
  checked?: boolean,
  currentPopup: string,
  issue: Issue,
  onAssign: (string) => void,
  onCheck?: () => void,
  onClick: (string) => void,
  onFail: (Error) => void,
  onFilterClick?: () => void,
  onIssueChange: (Promise<*>, oldIssue?: Issue, newIssue?: Issue) => void,
  selected: boolean,
  togglePopup: (string) => void
};

export default class IssueView extends React.PureComponent {
  props: Props;

  handleClick = (evt: MouseEvent) => {
    evt.preventDefault();
    if (this.props.onClick) {
      this.props.onClick(this.props.issue.key);
    }
  };

  editComment = (comment: string, text: string) => {
    this.props.onIssueChange(editIssueComment({ comment, text }));
  };

  deleteComment = (comment: string) => {
    this.props.onIssueChange(deleteIssueComment({ comment }));
  };

  render() {
    const { issue } = this.props;

    const hasCheckbox = this.props.onCheck != null;

    const issueClass = classNames('issue', {
      'issue-with-checkbox': hasCheckbox,
      selected: this.props.selected
    });

    return (
      <div
        className={issueClass}
        data-issue={issue.key}
        onClick={this.handleClick}
        tabIndex={0}
        role="listitem">
        <IssueTitleBar
          issue={issue}
          currentPopup={this.props.currentPopup}
          onFail={this.props.onFail}
          onFilterClick={this.props.onFilterClick}
          togglePopup={this.props.togglePopup}
        />
        <IssueActionsBar
          issue={issue}
          currentPopup={this.props.currentPopup}
          onAssign={this.props.onAssign}
          onFail={this.props.onFail}
          togglePopup={this.props.togglePopup}
          onIssueChange={this.props.onIssueChange}
        />
        {issue.comments &&
          issue.comments.length > 0 &&
          <div className="issue-comments">
            {issue.comments.map(comment => (
              <IssueCommentLine
                comment={comment}
                key={comment.key}
                onEdit={this.editComment}
                onDelete={this.deleteComment}
              />
            ))}
          </div>}
        <a className="issue-navigate js-issue-navigate">
          <i className="issue-navigate-to-left icon-chevron-left" />
          <i className="issue-navigate-to-right icon-chevron-right" />
        </a>
        {hasCheckbox &&
          <div className="js-toggle issue-checkbox-container">
            <Checkbox
              className="issue-checkbox"
              onCheck={this.props.onCheck}
              checked={this.props.checked}
            />
          </div>}
      </div>
    );
  }
}
