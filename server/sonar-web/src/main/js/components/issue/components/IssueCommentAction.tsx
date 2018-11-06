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
import * as React from 'react';
import { updateIssue } from '../actions';
import Toggler from '../../controls/Toggler';
import { Button } from '../../ui/buttons';
import CommentPopup from '../popups/CommentPopup';
import { addIssueComment } from '../../../api/issues';
import { translate } from '../../../helpers/l10n';
import { Issue } from '../../../app/types';

interface Props {
  commentPlaceholder: string;
  currentPopup?: string;
  issueKey: string;
  onChange: (issue: Issue) => void;
  toggleComment: (open?: boolean, placeholder?: string) => void;
}

export default class IssueCommentAction extends React.PureComponent<Props> {
  addComment = (text: string) => {
    updateIssue(this.props.onChange, addIssueComment({ issue: this.props.issueKey, text }));
    this.props.toggleComment(false);
  };

  handleCommentClick = () => {
    this.props.toggleComment();
  };

  handleClose = () => {
    this.props.toggleComment(false);
  };

  render() {
    return (
      <li className="issue-meta dropdown">
        <Toggler
          onRequestClose={this.handleClose}
          open={this.props.currentPopup === 'comment'}
          overlay={
            <CommentPopup
              onComment={this.addComment}
              placeholder={this.props.commentPlaceholder}
              toggleComment={this.props.toggleComment}
            />
          }>
          <Button
            className="button-link issue-action js-issue-comment"
            onClick={this.handleCommentClick}>
            <span className="issue-meta-label">{translate('issue.comment.formlink')}</span>
          </Button>
        </Toggler>
      </li>
    );
  }
}
