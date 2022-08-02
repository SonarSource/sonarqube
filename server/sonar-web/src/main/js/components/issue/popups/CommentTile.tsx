/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { sanitizeString } from '../../../helpers/sanitize';
import { IssueComment } from '../../../types/types';
import { Button, DeleteButton, EditButton, ResetButtonLink } from '../../controls/buttons';
import DateTimeFormatter from '../../intl/DateTimeFormatter';
import Avatar from '../../ui/Avatar';

interface CommentTileProps {
  comment: IssueComment;
  handleDelete: (commentKey: string) => void;
  onEdit: (comment: string, text: string) => void;
}

interface CommentTileState {
  showEditArea: boolean;
  editedComment: string;
}

export default class CommentTile extends React.PureComponent<CommentTileProps, CommentTileState> {
  state = {
    showEditArea: false,
    editedComment: ''
  };

  handleEditClick = () => {
    const { comment } = this.props;
    const { showEditArea } = this.state;
    const editedComment = !showEditArea ? comment.markdown : '';
    this.setState({ showEditArea: !showEditArea, editedComment });
  };

  handleSaveClick = () => {
    const { comment } = this.props;
    const { editedComment } = this.state;
    this.props.onEdit(comment.key, editedComment);
    this.setState({ showEditArea: false, editedComment: '' });
  };

  handleCancelClick = () => {
    this.setState({ showEditArea: false });
  };

  handleEditCommentChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ editedComment: event.target.value });
  };

  handleKeyboard = (event: React.KeyboardEvent) => {
    if (event.nativeEvent.key === KeyboardKeys.Enter && (event.metaKey || event.ctrlKey)) {
      this.handleSaveClick();
    }
  };

  render() {
    const { comment } = this.props;
    const { showEditArea, editedComment } = this.state;
    const author = comment.authorName || comment.author;
    const displayName =
      comment.authorActive === false && author
        ? translateWithParameters('user.x_deleted', author)
        : author;
    return (
      <div className="issue-comment-tile spacer-bottom padded">
        <div className="display-flex-center">
          <div className="issue-comment-author display-flex-center" title={displayName}>
            <Avatar
              className="little-spacer-right"
              hash={comment.authorAvatar}
              name={author}
              size={24}
            />
            {displayName}
          </div>
          <span className="little-spacer-left little-spacer-right">-</span>
          <DateTimeFormatter date={comment.createdAt} />
        </div>
        <div className="spacer-top display-flex-space-between">
          {!showEditArea && (
            <div
              className="flex-1 markdown"
              // eslint-disable-next-line react/no-danger
              dangerouslySetInnerHTML={{ __html: sanitizeString(comment.htmlText) }}
            />
          )}
          {showEditArea && (
            <div className="edit-form flex-1">
              <div className="issue-comment-form-text">
                <textarea
                  autoFocus={true}
                  onChange={this.handleEditCommentChange}
                  onKeyDown={this.handleKeyboard}
                  rows={2}
                  value={editedComment}
                />
              </div>
              <div className="issue-comment-form-footer">
                <div className="issue-comment-form-actions little-padded-left">
                  <Button
                    className="js-issue-comment-submit little-spacer-right"
                    disabled={editedComment.trim().length < 1}
                    onClick={this.handleSaveClick}>
                    {translate('save')}
                  </Button>
                  <ResetButtonLink
                    className="js-issue-comment-cancel"
                    onClick={this.handleCancelClick}>
                    {translate('cancel')}
                  </ResetButtonLink>
                </div>
              </div>
            </div>
          )}
          {comment.updatable && (
            <div>
              <EditButton
                aria-label={translate('issue.comment.edit')}
                className="js-issue-comment-edit button-small"
                onClick={this.handleEditClick}
              />
              <DeleteButton
                aria-label={translate('issue.comment.delete')}
                className="js-issue-comment-delete button-small"
                onClick={() => {
                  this.props.handleDelete(comment.key);
                }}
              />
            </div>
          )}
        </div>
      </div>
    );
  }
}
