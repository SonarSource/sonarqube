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
import Avatar from '../../ui/Avatar';
import Toggler from '../../controls/Toggler';
import { EditButton, DeleteButton } from '../../ui/buttons';
import CommentDeletePopup from '../popups/CommentDeletePopup';
import CommentPopup from '../popups/CommentPopup';
import DateFromNow from '../../intl/DateFromNow';
import { PopupPlacement } from '../../ui/popups';

interface Props {
  comment: T.IssueComment;
  onDelete: (comment: string) => void;
  onEdit: (comment: string, text: string) => void;
}

interface State {
  openPopup: string;
}

export default class IssueCommentLine extends React.PureComponent<Props, State> {
  state: State = {
    openPopup: ''
  };

  handleEdit = (text: string) => {
    this.props.onEdit(this.props.comment.key, text);
    this.toggleEditPopup(false);
  };

  handleDelete = () => {
    this.props.onDelete(this.props.comment.key);
    this.toggleDeletePopup(false);
  };

  togglePopup = (popupName: string, force?: boolean) => {
    this.setState(prevState => {
      if (prevState.openPopup !== popupName && force !== false) {
        return { openPopup: popupName };
      } else if (prevState.openPopup === popupName && force !== true) {
        return { openPopup: '' };
      }
      return prevState;
    });
  };

  toggleDeletePopup = (force?: boolean) => {
    this.togglePopup('delete', force);
  };

  toggleEditPopup = (force?: boolean) => {
    this.togglePopup('edit', force);
  };

  closePopups = () => {
    this.setState({ openPopup: '' });
  };

  render() {
    const { comment } = this.props;
    return (
      <div className="issue-comment">
        <div className="issue-comment-author" title={comment.authorName}>
          <Avatar
            className="little-spacer-right"
            hash={comment.authorAvatar}
            name={comment.authorName || comment.author}
            size={16}
          />
          {comment.authorName || comment.author}
        </div>
        <div
          className="issue-comment-text markdown"
          dangerouslySetInnerHTML={{ __html: comment.htmlText }}
        />
        <div className="issue-comment-age">
          <DateFromNow date={comment.createdAt} />
        </div>
        <div className="issue-comment-actions">
          {comment.updatable && (
            <div className="dropdown">
              <Toggler
                closeOnClickOutside={false}
                onRequestClose={this.closePopups}
                open={this.state.openPopup === 'edit'}
                overlay={
                  <CommentPopup
                    comment={comment}
                    onComment={this.handleEdit}
                    placeholder=""
                    placement={PopupPlacement.BottomRight}
                    toggleComment={this.toggleEditPopup}
                  />
                }>
                <EditButton
                  className="js-issue-comment-edit button-small"
                  onClick={this.toggleEditPopup}
                />
              </Toggler>
            </div>
          )}
          {comment.updatable && (
            <div className="dropdown">
              <Toggler
                onRequestClose={this.closePopups}
                open={this.state.openPopup === 'delete'}
                overlay={<CommentDeletePopup onDelete={this.handleDelete} />}>
                <DeleteButton
                  className="js-issue-comment-delete button-small"
                  onClick={this.toggleDeletePopup}
                />
              </Toggler>
            </div>
          )}
        </div>
      </div>
    );
  }
}
