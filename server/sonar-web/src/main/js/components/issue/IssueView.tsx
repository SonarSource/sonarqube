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
import classNames from 'classnames';
import IssueTitleBar from './components/IssueTitleBar';
import IssueActionsBar from './components/IssueActionsBar';
import IssueCommentLine from './components/IssueCommentLine';
import { updateIssue } from './actions';
import { deleteIssueComment, editIssueComment } from '../../api/issues';

interface Props {
  branchLike?: T.BranchLike;
  checked?: boolean;
  currentPopup?: string;
  displayLocationsCount?: boolean;
  displayLocationsLink?: boolean;
  issue: T.Issue;
  onAssign: (login: string) => void;
  onChange: (issue: T.Issue) => void;
  onCheck?: (issue: string, event: { shiftKey?: boolean }) => void;
  onClick: (issueKey: string) => void;
  onFilter?: (property: string, issue: T.Issue) => void;
  selected: boolean;
  togglePopup: (popup: string, show: boolean | void) => void;
}

export default class IssueView extends React.PureComponent<Props> {
  handleCheck = (event: React.MouseEvent) => {
    event.preventDefault();
    if (this.props.onCheck) {
      this.props.onCheck(this.props.issue.key, event);
    }
  };

  handleClick = (event: React.MouseEvent) => {
    if (!isClickable(event.target as HTMLElement) && this.props.onClick) {
      event.preventDefault();
      this.props.onClick(this.props.issue.key);
    }
  };

  editComment = (comment: string, text: string) => {
    updateIssue(this.props.onChange, editIssueComment({ comment, text }));
  };

  deleteComment = (comment: string) => {
    updateIssue(this.props.onChange, deleteIssueComment({ comment }));
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
        role="listitem"
        tabIndex={0}>
        <IssueTitleBar
          branchLike={this.props.branchLike}
          currentPopup={this.props.currentPopup}
          displayLocationsCount={this.props.displayLocationsCount}
          displayLocationsLink={this.props.displayLocationsLink}
          issue={issue}
          onFilter={this.props.onFilter}
          togglePopup={this.props.togglePopup}
        />
        <IssueActionsBar
          currentPopup={this.props.currentPopup}
          issue={issue}
          onAssign={this.props.onAssign}
          onChange={this.props.onChange}
          togglePopup={this.props.togglePopup}
        />
        {issue.comments &&
          issue.comments.length > 0 && (
            <div className="issue-comments">
              {issue.comments.map(comment => (
                <IssueCommentLine
                  comment={comment}
                  key={comment.key}
                  onDelete={this.deleteComment}
                  onEdit={this.editComment}
                />
              ))}
            </div>
          )}
        {hasCheckbox && (
          <a className="js-toggle issue-checkbox-container" href="#" onClick={this.handleCheck}>
            <i
              className={classNames('issue-checkbox', 'icon-checkbox', {
                'icon-checkbox-checked': this.props.checked
              })}
            />
          </a>
        )}
      </div>
    );
  }
}

function isClickable(node: HTMLElement | undefined | null): boolean {
  if (!node) {
    return false;
  }
  const clickableTags = ['A', 'BUTTON', 'INPUT', 'TEXTAREA'];
  const tagName = (node.tagName || '').toUpperCase();
  return clickableTags.includes(tagName) || isClickable(node.parentElement);
}
