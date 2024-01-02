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
import {
  commentSecurityHotspot,
  deleteSecurityHotspotComment,
  editSecurityHotspotComment,
} from '../../../api/security-hotspots';
import FormattingTips from '../../../components/common/FormattingTips';
import { Button } from '../../../components/controls/buttons';
import { translate } from '../../../helpers/l10n';
import { Hotspot } from '../../../types/security-hotspots';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import HotspotReviewHistory from './HotspotReviewHistory';

interface Props {
  currentUser: CurrentUser;
  hotspot: Hotspot;
  commentTextRef: React.RefObject<HTMLTextAreaElement>;
  onCommentUpdate: () => void;
}

interface State {
  comment: string;
  showFullHistory: boolean;
}

export default class HotspotReviewHistoryAndComments extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      comment: '',
      showFullHistory: false,
    };
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.hotspot.key !== this.props.hotspot.key) {
      this.setState({
        comment: '',
        showFullHistory: false,
      });
    }
  }

  handleCommentChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ comment: event.target.value });
  };

  handleSubmitComment = () => {
    return commentSecurityHotspot(this.props.hotspot.key, this.state.comment).then(() => {
      this.setState({ comment: '' });
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

  handleShowFullHistory = () => {
    this.setState({ showFullHistory: true });
  };

  render() {
    const { currentUser, hotspot, commentTextRef } = this.props;
    const { comment, showFullHistory } = this.state;
    return (
      <div className="padded it__hs-review-history">
        {isLoggedIn(currentUser) && (
          <>
            <label htmlFor="security-hotspot-comment">{translate('hotspots.comment.field')}</label>
            <textarea
              id="security-hotspot-comment"
              className="form-field fixed-width width-100 spacer-bottom"
              onChange={this.handleCommentChange}
              ref={commentTextRef}
              rows={2}
              value={comment}
            />
            <div className="display-flex-space-between display-flex-center ">
              <FormattingTips className="huge-spacer-bottom" />
              <div>
                <Button
                  className="huge-spacer-bottom"
                  id="hotspot-comment-box-submit"
                  onClick={this.handleSubmitComment}
                >
                  {translate('hotspots.comment.submit')}
                </Button>
              </div>
            </div>
          </>
        )}

        <h2 className="spacer-top big-spacer-bottom">{translate('hotspot.section.activity')}</h2>

        <HotspotReviewHistory
          hotspot={hotspot}
          onDeleteComment={this.handleDeleteComment}
          onEditComment={this.handleEditComment}
          onShowFullHistory={this.handleShowFullHistory}
          showFullHistory={showFullHistory}
        />
      </div>
    );
  }
}
