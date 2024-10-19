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
import { Button } from '@sonarsource/echoes-react';
import { PageTitle } from 'design-system';
import * as React from 'react';
import {
  commentSecurityHotspot,
  deleteSecurityHotspotComment,
  editSecurityHotspotComment,
} from '../../../api/security-hotspots';
import { translate } from '../../../helpers/l10n';
import { Hotspot } from '../../../types/security-hotspots';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import HotspotCommentModal from './HotspotCommentModal';
import HotspotReviewHistory from './HotspotReviewHistory';

interface Props {
  currentUser: CurrentUser;
  hotspot: Hotspot;
  onCommentUpdate: () => void;
}

interface State {
  showAddCommentModal: boolean;
}

export default class HotspotReviewHistoryAndComments extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      showAddCommentModal: false,
    };
  }

  handleSubmitComment = (comment: string) => {
    return commentSecurityHotspot(this.props.hotspot.key, comment).then(() => {
      this.setState({ showAddCommentModal: false });
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

  handleShowCommentModal = () => {
    this.setState({ showAddCommentModal: true });
  };

  handleHideCommentModal = () => {
    this.setState({ showAddCommentModal: false });
  };

  render() {
    const { currentUser, hotspot } = this.props;
    const { showAddCommentModal } = this.state;
    return (
      <div className="it__hs-review-history">
        <PageTitle
          as="h2"
          className="sw-typo-lg-semibold"
          text={translate('hotspot.section.activity')}
        />

        {isLoggedIn(currentUser) && (
          <Button className="sw-mt-4 sw-mb-2" onClick={this.handleShowCommentModal}>
            {translate('hotspots.status.add_comment')}
          </Button>
        )}

        <HotspotReviewHistory
          hotspot={hotspot}
          onDeleteComment={this.handleDeleteComment}
          onEditComment={this.handleEditComment}
        />

        {showAddCommentModal && (
          <HotspotCommentModal
            onCancel={this.handleHideCommentModal}
            onSubmit={(comment) => {
              this.handleSubmitComment(comment);
            }}
          />
        )}
      </div>
    );
  }
}
