/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { Button, ResetButtonLink } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  commentSecurityHotspot,
  deleteSecurityHotspotComment,
  editSecurityHotspotComment
} from '../../../api/security-hotspots';
import MarkdownTips from '../../../components/common/MarkdownTips';
import { isLoggedIn } from '../../../helpers/users';
import { Hotspot } from '../../../types/security-hotspots';
import HotspotReviewHistory from './HotspotReviewHistory';

interface Props {
  currentUser: T.CurrentUser;
  hotspot: Hotspot;
  commentTextRef: React.RefObject<HTMLTextAreaElement>;
  commentVisible: boolean;
  onCommentUpdate: () => void;
  onOpenComment: () => void;
  onCloseComment: () => void;
}

interface State {
  comment: string;
}

export default class HotspotReviewHistoryAndComments extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      comment: ''
    };
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.hotspot !== this.props.hotspot) {
      this.setState({
        comment: ''
      });
    }
  }

  handleCommentChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ comment: event.target.value });
  };

  handleCloseComment = () => {
    this.setState({ comment: '' });
    this.props.onCloseComment();
  };

  handleSubmitComment = () => {
    return commentSecurityHotspot(this.props.hotspot.key, this.state.comment).then(() => {
      this.setState({ comment: '' });
      this.props.onCloseComment();
      this.props.onCommentUpdate();
    });
  };

  handleDeleteComment = (key: string) => {
    return deleteSecurityHotspotComment(key).then(() => {
      this.props.onCommentUpdate();
    });
  };

  handleEditComment = (key: string, comment: string) => {
    return editSecurityHotspotComment(key, comment).then(() => {
      this.props.onCommentUpdate();
    });
  };

  render() {
    const { currentUser, hotspot, commentTextRef, commentVisible } = this.props;
    const { comment } = this.state;
    return (
      <>
        <h1>{translate('hotspot.section.activity')}</h1>
        <div className="padded">
          <HotspotReviewHistory
            hotspot={hotspot}
            onDeleteComment={this.handleDeleteComment}
            onEditComment={this.handleEditComment}
          />

          {isLoggedIn(currentUser) && (
            <>
              <hr />
              <div className="big-spacer-top">
                <Button
                  className={classNames({ invisible: commentVisible })}
                  id="hotspot-comment-box-display"
                  onClick={this.props.onOpenComment}>
                  {translate('hotspots.comment.open')}
                </Button>

                <div className={classNames({ invisible: !commentVisible })}>
                  <div className="little-spacer-bottom">{translate('hotspots.comment.field')}</div>
                  <textarea
                    className="form-field fixed-width width-100 spacer-bottom"
                    onChange={this.handleCommentChange}
                    ref={commentTextRef}
                    rows={2}
                    value={comment}
                  />
                  <div className="display-flex-space-between display-flex-center ">
                    <MarkdownTips className="huge-spacer-bottom" />
                    <div>
                      <Button
                        className="huge-spacer-bottom"
                        id="hotspot-comment-box-submit"
                        onClick={this.handleSubmitComment}>
                        {translate('hotspots.comment.submit')}
                      </Button>
                      <ResetButtonLink
                        className="spacer-left huge-spacer-bottom"
                        id="hotspot-comment-box-cancel"
                        onClick={this.handleCloseComment}>
                        {translate('cancel')}
                      </ResetButtonLink>
                    </div>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      </>
    );
  }
}
