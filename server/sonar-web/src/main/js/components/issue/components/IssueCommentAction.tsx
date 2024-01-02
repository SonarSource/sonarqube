/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { addIssueComment, deleteIssueComment, editIssueComment } from '../../../api/issues';
import { ButtonLink } from '../../../components/controls/buttons';
import Toggler from '../../../components/controls/Toggler';
import { translate } from '../../../helpers/l10n';
import { Issue, IssueComment } from '../../../types/types';
import { updateIssue } from '../actions';
import CommentListPopup from '../popups/CommentListPopup';
import CommentPopup from '../popups/CommentPopup';

interface Props {
  canComment: boolean;
  commentAutoTriggered?: boolean;
  commentPlaceholder: string;
  currentPopup?: string;
  issueKey: string;
  onChange: (issue: Issue) => void;
  toggleComment: (open?: boolean, placeholder?: string, autoTriggered?: boolean) => void;
  comments?: IssueComment[];
  showCommentsInPopup?: boolean;
}

export default class IssueCommentAction extends React.PureComponent<Props> {
  addComment = (text: string) => {
    const { showCommentsInPopup } = this.props;
    updateIssue(this.props.onChange, addIssueComment({ issue: this.props.issueKey, text }));
    if (!showCommentsInPopup) {
      this.props.toggleComment(false);
    }
  };

  handleEditComment = (comment: string, text: string) => {
    updateIssue(this.props.onChange, editIssueComment({ comment, text }));
  };

  handleDeleteComment = (comment: string) => {
    updateIssue(this.props.onChange, deleteIssueComment({ comment }));
  };

  handleCommentClick = () => {
    this.props.toggleComment();
  };

  handleClose = () => {
    this.props.toggleComment(false);
  };

  render() {
    const { comments, showCommentsInPopup, canComment } = this.props;
    return (
      <div className="issue-meta dropdown">
        <Toggler
          closeOnClickOutside={false}
          onRequestClose={this.handleClose}
          open={this.props.currentPopup === 'comment'}
          overlay={
            showCommentsInPopup ? (
              <CommentListPopup
                comments={comments}
                deleteComment={this.handleDeleteComment}
                onAddComment={this.addComment}
                onEdit={this.handleEditComment}
                placeholder={this.props.commentPlaceholder}
                toggleComment={this.props.toggleComment}
                autoTriggered={this.props.commentAutoTriggered}
                canComment={canComment}
              />
            ) : (
              <CommentPopup
                autoTriggered={this.props.commentAutoTriggered}
                onComment={this.addComment}
                placeholder={this.props.commentPlaceholder}
                toggleComment={this.props.toggleComment}
              />
            )
          }
        >
          <ButtonLink
            aria-expanded={this.props.currentPopup === 'comment'}
            aria-label={translate('issue.comment.add_comment')}
            className="issue-action js-issue-comment"
            onClick={this.handleCommentClick}
          >
            <span className="issue-meta-label">
              {showCommentsInPopup && comments && (
                <span>
                  {comments.length}{' '}
                  {translate(
                    comments.length === 1
                      ? 'issue.comment.formlink.total'
                      : 'issue.comment.formlink.total.plural'
                  )}
                </span>
              )}
              {!showCommentsInPopup && translate('issue.comment.formlink')}
            </span>
          </ButtonLink>
        </Toggler>
      </div>
    );
  }
}
