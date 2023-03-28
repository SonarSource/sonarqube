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
import classNames from 'classnames';
import * as React from 'react';
import { deleteIssueComment, editIssueComment } from '../../../api/issues';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Issue } from '../../../types/types';
import Checkbox from '../../controls/Checkbox';
import { updateIssue } from '../actions';
import IssueActionsBar from './IssueActionsBar';
import IssueCommentLine from './IssueCommentLine';
import IssueTitleBar from './IssueTitleBar';

interface Props {
  branchLike?: BranchLike;
  checked?: boolean;
  currentPopup?: string;
  displayWhyIsThisAnIssue?: boolean;
  displayLocationsCount?: boolean;
  displayLocationsLink?: boolean;
  issue: Issue;
  onAssign: (login: string) => void;
  onChange: (issue: Issue) => void;
  onCheck?: (issue: string) => void;
  onClick?: (issueKey: string) => void;
  onFilter?: (property: string, issue: Issue) => void;
  selected: boolean;
  togglePopup: (popup: string, show: boolean | void) => void;
}

export default class IssueView extends React.PureComponent<Props> {
  handleCheck = () => {
    if (this.props.onCheck) {
      this.props.onCheck(this.props.issue.key);
    }
  };

  handleBoxClick = (event: React.MouseEvent<HTMLDivElement>) => {
    if (!isClickable(event.target as HTMLElement) && this.props.onClick) {
      event.preventDefault();
      this.handleDetailClick();
    }
  };

  handleDetailClick = () => {
    if (this.props.onClick) {
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
    const {
      issue,
      branchLike,
      checked,
      currentPopup,
      displayWhyIsThisAnIssue,
      displayLocationsLink,
      displayLocationsCount,
    } = this.props;

    const hasCheckbox = this.props.onCheck != null;

    const issueClass = classNames('issue', {
      'no-click': this.props.onClick === undefined,
      'issue-with-checkbox': hasCheckbox,
      selected: this.props.selected,
    });

    return (
      <div
        className={issueClass}
        onClick={this.handleBoxClick}
        role="region"
        aria-label={issue.message}
      >
        {hasCheckbox && (
          <Checkbox
            checked={checked ?? false}
            className="issue-checkbox-container"
            onCheck={this.handleCheck}
            label={translateWithParameters('issues.action_select.label', issue.message)}
            title={translate('issues.action_select')}
          />
        )}
        <IssueTitleBar
          branchLike={branchLike}
          onClick={this.handleDetailClick}
          currentPopup={currentPopup}
          displayLocationsCount={displayLocationsCount}
          displayLocationsLink={displayLocationsLink}
          displayWhyIsThisAnIssue={displayWhyIsThisAnIssue}
          issue={issue}
          onFilter={this.props.onFilter}
          togglePopup={this.props.togglePopup}
        />
        <IssueActionsBar
          className="padded-left"
          currentPopup={currentPopup}
          issue={issue}
          onAssign={this.props.onAssign}
          onChange={this.props.onChange}
          togglePopup={this.props.togglePopup}
        />
        {issue.comments && issue.comments.length > 0 && (
          <ul className="issue-comments" data-testid="issue-comments">
            {issue.comments.map((comment) => (
              <IssueCommentLine
                comment={comment}
                key={comment.key}
                onDelete={this.deleteComment}
                onEdit={this.editComment}
              />
            ))}
          </ul>
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
