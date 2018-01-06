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
import Avatar from '../../../components/ui/Avatar';
import BubblePopupHelper from '../../../components/common/BubblePopupHelper';
import EditIcon from '../../../components/icons-components/EditIcon';
import { EditButton, DeleteButton } from '../../../components/ui/buttons';
import CommentDeletePopup from '../popups/CommentDeletePopup';
import CommentPopup from '../popups/CommentPopup';
import DateFromNow from '../../../components/intl/DateFromNow';
/*:: import type { IssueComment } from '../types'; */

/*::
type Props = {
  comment: IssueComment,
  onDelete: string => void,
  onEdit: (string, string) => void
};
*/

/*::
type State = {
  openPopup: string
};
*/

export default class IssueCommentLine extends React.PureComponent {
  /*:: props: Props; */
  state /*: State */ = {
    openPopup: ''
  };

  handleCommentClick = (event /*: Event & {target: HTMLElement}*/) => {
    if (event.target.tagName === 'A') {
      event.stopPropagation();
    }
  };

  handleEdit = (text /*: string */) => {
    this.props.onEdit(this.props.comment.key, text);
    this.toggleEditPopup(false);
  };

  handleDelete = () => {
    this.props.onDelete(this.props.comment.key);
    this.toggleDeletePopup(false);
  };

  togglePopup = (popupName /*: string */, force /*: ?boolean */) => {
    this.setState((prevState /*: State */) => {
      if (prevState.openPopup !== popupName && force !== false) {
        return { openPopup: popupName };
      } else if (prevState.openPopup === popupName && force !== true) {
        return { openPopup: '' };
      }
      return prevState;
    });
  };

  toggleDeletePopup = (force /*: ?boolean */) => this.togglePopup('delete', force);

  toggleEditPopup = (force /*: ?boolean */) => this.togglePopup('edit', force);

  render() {
    const { comment } = this.props;
    return (
      <div className="issue-comment">
        <div className="issue-comment-author" title={comment.authorName}>
          <Avatar
            className="little-spacer-right"
            hash={comment.authorAvatar}
            name={comment.authorName}
            size={16}
          />
          {comment.authorName}
        </div>
        <div
          className="issue-comment-text markdown"
          dangerouslySetInnerHTML={{ __html: comment.htmlText }}
          onClick={this.handleCommentClick}
          role="Listitem"
          tabIndex={0}
        />
        <div className="issue-comment-age">
          <DateFromNow date={comment.createdAt} />
        </div>
        <div className="issue-comment-actions">
          {comment.updatable && (
            <BubblePopupHelper
              className="bubble-popup-helper-inline"
              isOpen={this.state.openPopup === 'edit'}
              offset={{ vertical: 0, horizontal: -6 }}
              position="bottomright"
              togglePopup={this.toggleDeletePopup}
              popup={
                <CommentPopup
                  comment={comment}
                  customClass="issue-edit-comment-bubble-popup"
                  onComment={this.handleEdit}
                  placeholder=""
                  toggleComment={this.toggleEditPopup}
                />
              }>
              <EditButton
                className="js-issue-comment-edit button-small"
                onClick={this.toggleEditPopup}
              />
            </BubblePopupHelper>
          )}
          {comment.updatable && (
            <BubblePopupHelper
              className="bubble-popup-helper-inline"
              isOpen={this.state.openPopup === 'delete'}
              offset={{ vertical: 0, horizontal: -10 }}
              position="bottomright"
              togglePopup={this.toggleDeletePopup}
              popup={<CommentDeletePopup onDelete={this.handleDelete} />}>
              <DeleteButton
                className="js-issue-comment-delete button-small"
                onClick={this.toggleDeletePopup}
              />
            </BubblePopupHelper>
          )}
        </div>
      </div>
    );
  }
}
