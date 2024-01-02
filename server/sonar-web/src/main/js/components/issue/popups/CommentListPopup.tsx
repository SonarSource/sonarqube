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
import { IssueComment } from '../../../types/types';
import { DropdownOverlay } from '../../controls/Dropdown';
import { PopupPlacement } from '../../ui/popups';
import CommentForm from './CommentForm';
import CommentList from './CommentList';

export interface Props {
  onAddComment: (text: string) => void;
  toggleComment: (visible: boolean) => void;
  deleteComment: (comment: string) => void;
  onEdit: (comment: string, text: string) => void;
  placeholder: string;
  placement?: PopupPlacement;
  autoTriggered?: boolean;
  comments?: IssueComment[];
  canComment: boolean;
}

export default class CommentListPopup extends React.PureComponent<Props, {}> {
  handleCommentClick = (comment: string) => {
    const { autoTriggered } = this.props;

    this.props.onAddComment(comment);

    if (autoTriggered) {
      this.props.toggleComment(false);
    }
  };

  handleCancelClick = () => {
    this.props.toggleComment(false);
  };

  render() {
    const { comments, placeholder, autoTriggered, canComment } = this.props;

    return (
      <DropdownOverlay placement={this.props.placement}>
        <div className="issue-comment-bubble-popup">
          <CommentList
            comments={comments}
            deleteComment={this.props.deleteComment}
            onEdit={this.props.onEdit}
          />
          {canComment && (
            <CommentForm
              autoTriggered={autoTriggered}
              placeholder={placeholder}
              onCancel={this.handleCancelClick}
              onSaveComment={this.handleCommentClick}
              showFormatHelp={true}
            />
          )}
        </div>
      </DropdownOverlay>
    );
  }
}
