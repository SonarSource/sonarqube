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
// @flow
import React from 'react';
import classNames from 'classnames';
import IssueTitleBar from './components/IssueTitleBar';
import IssueActionsBar from './components/IssueActionsBar';
import IssueCommentLine from './components/IssueCommentLine';
import { updateIssue } from './actions';
import { deleteIssueComment, editIssueComment } from '../../api/issues';
/*:: import type { Issue } from './types'; */

/*::
type Props = {|
  branch?: string,
  checked?: boolean,
  currentPopup: ?string,
  displayLocationsCount?: boolean;
  displayLocationsLink?: boolean;
  issue: Issue,
  onAssign: string => void,
  onChange: Issue => void,
  onCheck?: string => void,
  onClick: string => void,
  onFail: Error => void,
  onFilter?: (property: string, issue: Issue) => void,
  selected: boolean,
  togglePopup: (string, boolean | void) => void
|};
*/

export default class IssueView extends React.PureComponent {
  /*:: props: Props; */

  handleCheck = (event /*: Event */) => {
    event.preventDefault();
    event.stopPropagation();
    if (this.props.onCheck) {
      this.props.onCheck(this.props.issue.key);
    }
  };

  handleClick = (event /*: Event & { target: HTMLElement } */) => {
    event.preventDefault();
    if (this.props.onClick) {
      this.props.onClick(this.props.issue.key);
    }
  };

  editComment = (comment /*: string */, text /*: string */) => {
    updateIssue(this.props.onChange, this.props.onFail, editIssueComment({ comment, text }));
  };

  deleteComment = (comment /*: string */) => {
    updateIssue(this.props.onChange, this.props.onFail, deleteIssueComment({ comment }));
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
          branch={this.props.branch}
          currentPopup={this.props.currentPopup}
          displayLocationsCount={this.props.displayLocationsCount}
          displayLocationsLink={this.props.displayLocationsLink}
          issue={issue}
          onFail={this.props.onFail}
          onFilter={this.props.onFilter}
          togglePopup={this.props.togglePopup}
        />
        <IssueActionsBar
          issue={issue}
          currentPopup={this.props.currentPopup}
          onAssign={this.props.onAssign}
          onFail={this.props.onFail}
          togglePopup={this.props.togglePopup}
          onChange={this.props.onChange}
        />
        {issue.comments &&
          issue.comments.length > 0 && (
            <div className="issue-comments">
              {issue.comments.map(comment => (
                <IssueCommentLine
                  comment={comment}
                  key={comment.key}
                  onEdit={this.editComment}
                  onDelete={this.deleteComment}
                />
              ))}
            </div>
          )}
        <a className="issue-navigate js-issue-navigate">
          <i className="issue-navigate-to-left icon-chevron-left" />
          <i className="issue-navigate-to-right icon-chevron-right" />
        </a>
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
