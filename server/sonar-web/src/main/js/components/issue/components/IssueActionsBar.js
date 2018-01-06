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
import IssueAssign from './IssueAssign';
import IssueCommentAction from './IssueCommentAction';
import IssueSeverity from './IssueSeverity';
import IssueTags from './IssueTags';
import IssueTransition from './IssueTransition';
import IssueType from './IssueType';
import { updateIssue } from '../actions';
import { translate, translateWithParameters } from '../../../helpers/l10n';
/*:: import type { Issue } from '../types'; */

/*::
type Props = {
  issue: Issue,
  currentPopup: ?string,
  onAssign: string => void,
  onChange: Issue => void,
  onFail: Error => void,
  togglePopup: (string, boolean | void) => void
};
*/

/*::
type State = {
  commentPlaceholder: string
};
*/

export default class IssueActionsBar extends React.PureComponent {
  /*:: props: Props; */
  state /*: State */ = {
    commentPlaceholder: ''
  };

  setIssueProperty = (
    property /*: string */,
    popup /*: string */,
    apiCall /*: Object => Promise<*> */,
    value /*: string */
  ) => {
    const { issue } = this.props;
    if (issue[property] !== value) {
      const newIssue = { ...issue, [property]: value };
      updateIssue(
        this.props.onChange,
        this.props.onFail,
        apiCall({ issue: issue.key, [property]: value }),
        issue,
        newIssue
      );
    }
    this.props.togglePopup(popup, false);
  };

  toggleComment = (open /*: boolean | void */, placeholder /*: string | void */) => {
    this.setState({ commentPlaceholder: placeholder || '' });
    this.props.togglePopup('comment', open);
  };

  handleTransition = (issue /*: Issue */) => {
    this.props.onChange(issue);
    if (['FALSE-POSITIVE', 'WONTFIX'].includes(issue.resolution)) {
      this.toggleComment(true, translate('issue.comment.tell_why'));
    }
  };

  render() {
    const { issue } = this.props;
    const canAssign = issue.actions.includes('assign');
    const canComment = issue.actions.includes('comment');
    const canSetSeverity = issue.actions.includes('set_severity');
    const canSetTags = issue.actions.includes('set_tags');
    const hasTransitions = issue.transitions && issue.transitions.length > 0;

    return (
      <table className="issue-table">
        <tbody>
          <tr>
            <td>
              <ul className="list-inline issue-meta-list">
                <li className="issue-meta">
                  <IssueType
                    isOpen={this.props.currentPopup === 'set-type' && canSetSeverity}
                    issue={issue}
                    canSetSeverity={canSetSeverity}
                    togglePopup={this.props.togglePopup}
                    setIssueProperty={this.setIssueProperty}
                  />
                </li>
                <li className="issue-meta">
                  <IssueSeverity
                    isOpen={this.props.currentPopup === 'set-severity' && canSetSeverity}
                    issue={issue}
                    canSetSeverity={canSetSeverity}
                    togglePopup={this.props.togglePopup}
                    setIssueProperty={this.setIssueProperty}
                  />
                </li>
                <li className="issue-meta">
                  <IssueTransition
                    isOpen={this.props.currentPopup === 'transition' && hasTransitions}
                    issue={issue}
                    hasTransitions={hasTransitions}
                    onChange={this.handleTransition}
                    onFail={this.props.onFail}
                    togglePopup={this.props.togglePopup}
                  />
                </li>
                <li className="issue-meta">
                  <IssueAssign
                    isOpen={this.props.currentPopup === 'assign' && canAssign}
                    issue={issue}
                    canAssign={canAssign}
                    onAssign={this.props.onAssign}
                    onFail={this.props.onFail}
                    togglePopup={this.props.togglePopup}
                  />
                </li>
                {issue.effort && (
                  <li className="issue-meta">
                    <span className="issue-meta-label">
                      {translateWithParameters('issue.x_effort', issue.effort)}
                    </span>
                  </li>
                )}
                {canComment && (
                  <IssueCommentAction
                    commentPlaceholder={this.state.commentPlaceholder}
                    currentPopup={this.props.currentPopup}
                    issueKey={issue.key}
                    onChange={this.props.onChange}
                    onFail={this.props.onFail}
                    toggleComment={this.toggleComment}
                  />
                )}
              </ul>
            </td>
            <td className="issue-table-meta-cell">
              <ul className="list-inline">
                <li className="issue-meta js-issue-tags">
                  <IssueTags
                    isOpen={this.props.currentPopup === 'edit-tags' && canSetTags}
                    canSetTags={canSetTags}
                    issue={issue}
                    onChange={this.props.onChange}
                    onFail={this.props.onFail}
                    togglePopup={this.props.togglePopup}
                  />
                </li>
              </ul>
            </td>
          </tr>
        </tbody>
      </table>
    );
  }
}
