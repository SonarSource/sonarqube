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
import classNames from 'classnames';
import BubblePopup from '../../../components/common/BubblePopup';
import MarkdownTips from '../../../components/common/MarkdownTips';
import { translate } from '../../../helpers/l10n';
/*:: import type { IssueComment } from '../types'; */

/*::
type Props = {
  comment?: IssueComment,
  customClass?: string,
  onComment: string => void,
  toggleComment: boolean => void,
  placeholder: string,
  popupPosition?: {}
};
*/

/*::
type State = {
  textComment: string
};
*/

export default class CommentPopup extends React.PureComponent {
  /*:: props: Props; */
  /*:: state: State; */

  constructor(props /*: Props */) {
    super(props);
    this.state = {
      textComment: props.comment ? props.comment.markdown : ''
    };
  }

  handleCommentChange = (evt /*: SyntheticInputEvent */) => {
    this.setState({ textComment: evt.target.value });
  };

  handleCommentClick = () => {
    if (this.state.textComment.trim().length > 0) {
      this.props.onComment(this.state.textComment);
    }
  };

  handleCancelClick = (evt /*: MouseEvent */) => {
    evt.preventDefault();
    this.props.toggleComment(false);
  };

  handleKeyboard = (evt /*: KeyboardEvent */) => {
    if (evt.keyCode === 13 && (evt.metaKey || evt.ctrlKey)) {
      // Ctrl + Enter
      this.handleCommentClick();
    } else if ([37, 38, 39, 40].includes(evt.keyCode)) {
      // Arrow keys
      evt.stopPropagation();
    }
  };

  render() {
    const { comment } = this.props;
    return (
      <BubblePopup
        position={this.props.popupPosition}
        customClass={classNames(this.props.customClass, 'bubble-popup-bottom-right')}>
        <div className="issue-comment-form-text">
          <textarea
            autoFocus={true}
            placeholder={this.props.placeholder}
            onChange={this.handleCommentChange}
            onKeyDown={this.handleKeyboard}
            value={this.state.textComment}
            rows="2"
          />
        </div>
        <div className="issue-comment-form-footer">
          <div className="issue-comment-form-actions">
            <button
              className="js-issue-comment-submit little-spacer-right"
              disabled={this.state.textComment.trim().length < 1}
              onClick={this.handleCommentClick}>
              {comment && translate('save')}
              {!comment && translate('issue.comment.submit')}
            </button>
            <a href="#" className="js-issue-comment-cancel" onClick={this.handleCancelClick}>
              {translate('cancel')}
            </a>
          </div>
          <div className="issue-comment-form-tips">
            <MarkdownTips />
          </div>
        </div>
      </BubblePopup>
    );
  }
}
