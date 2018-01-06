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
import { updateIssue } from '../actions';
import BubblePopupHelper from '../../../components/common/BubblePopupHelper';
import CommentPopup from '../popups/CommentPopup';
import { addIssueComment } from '../../../api/issues';
import { translate } from '../../../helpers/l10n';
/*:: import type { Issue } from '../types'; */

/*::
type Props = {|
  commentPlaceholder: string,
  currentPopup: ?string,
  issueKey: string,
  onChange: Issue => void,
  onFail: Error => void,
  toggleComment: (open?: boolean, placeholder?: string) => void
|};
*/

export default class IssueCommentAction extends React.PureComponent {
  /*:: props: Props; */

  addComment = (text /*: string */) => {
    updateIssue(
      this.props.onChange,
      this.props.onFail,
      addIssueComment({ issue: this.props.issueKey, text })
    );
    this.props.toggleComment(false);
  };

  handleCommentClick = () => this.props.toggleComment();

  render() {
    return (
      <li className="issue-meta">
        <BubblePopupHelper
          isOpen={this.props.currentPopup === 'comment'}
          position="bottomleft"
          togglePopup={this.props.toggleComment}
          popup={
            <CommentPopup
              customClass="issue-comment-bubble-popup"
              placeholder={this.props.commentPlaceholder}
              onComment={this.addComment}
              toggleComment={this.props.toggleComment}
            />
          }>
          <button
            className="button-link issue-action js-issue-comment"
            onClick={this.handleCommentClick}>
            <span className="issue-meta-label">{translate('issue.comment.formlink')}</span>
          </button>
        </BubblePopupHelper>
      </li>
    );
  }
}
