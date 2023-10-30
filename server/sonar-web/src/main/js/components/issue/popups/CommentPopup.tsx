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
import * as React from 'react';
import { DropdownOverlay } from '../../../components/controls/Dropdown';
import { PopupPlacement } from '../../../components/ui/popups';
import { IssueComment } from '../../../types/types';
import CommentForm from './CommentForm';

export interface CommentPopupProps {
  comment?: Pick<IssueComment, 'markdown'>;
  onComment: (text: string) => void;
  toggleComment: (visible: boolean) => void;
  placeholder: string;
  placement?: PopupPlacement;
}

export default class CommentPopup extends React.PureComponent<CommentPopupProps> {
  handleCancelClick = () => {
    this.props.toggleComment(false);
  };

  render() {
    const { comment } = this.props;

    return (
      <DropdownOverlay placement={this.props.placement}>
        <div className="sw-min-w-abs-500 issue-comment-bubble-popup">
          <CommentForm
            placeholder={this.props.placeholder}
            onCancel={this.handleCancelClick}
            onSaveComment={this.props.onComment}
            showFormatHelp
            comment={comment?.markdown}
          />
        </div>
      </DropdownOverlay>
    );
  }
}
