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
import { addIssueComment } from '../../../api/issues';
import { ButtonLink } from '../../../components/controls/buttons';
import Toggler from '../../../components/controls/Toggler';
import { translate } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';
import { updateIssue } from '../actions';
import CommentPopup from '../popups/CommentPopup';

interface Props {
  commentAutoTriggered?: boolean;
  commentPlaceholder: string;
  currentPopup?: string;
  issueKey: string;
  onChange: (issue: Issue) => void;
  toggleComment: (open?: boolean, placeholder?: string, autoTriggered?: boolean) => void;
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
      <div className="issue-meta dropdown">
        <Toggler
          closeOnClickOutside={false}
          onRequestClose={this.handleClose}
          open={this.props.currentPopup === 'comment'}
          overlay={
            <CommentPopup
              autoTriggered={this.props.commentAutoTriggered}
              onComment={this.addComment}
              placeholder={this.props.commentPlaceholder}
              toggleComment={this.props.toggleComment}
            />
          }>
          <ButtonLink
            aria-expanded={this.props.currentPopup === 'comment'}
            aria-label={translate('issue.comment.add_comment')}
            className="issue-action js-issue-comment"
            onClick={this.handleCommentClick}>
            <span className="issue-meta-label">{translate('issue.comment.formlink')}</span>
          </ButtonLink>
        </Toggler>
      </div>
    );
  }
}
