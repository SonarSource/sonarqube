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

import * as React from 'react';
import { Button, ResetButtonLink } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { commentSecurityHotspot } from '../../../api/security-hotspots';
import MarkdownTips from '../../../components/common/MarkdownTips';
import { Hotspot } from '../../../types/security-hotspots';

export interface HotspotViewerReviewHistoryTabCommentBoxProps {
  hotspot: Hotspot;
  onUpdateHotspot: () => void;
}

export default function HotspotViewerReviewHistoryTabCommentBox(
  props: HotspotViewerReviewHistoryTabCommentBoxProps
) {
  const { hotspot } = props;
  const [comment, setComment] = React.useState();
  const [isCommentBoxVisible, setCommentBoxVisibility] = React.useState(false);

  const onCancel = () => {
    setComment(null);
    setCommentBoxVisibility(false);
  };

  const onComment = () => {
    return commentSecurityHotspot(hotspot.key, comment).then(() => {
      onCancel();
      props.onUpdateHotspot();
    });
  };

  return (
    <div className="big-spacer-top">
      {!isCommentBoxVisible ? (
        <Button id="hotspot-comment-box-display" onClick={() => setCommentBoxVisibility(true)}>
          {translate('hotspots.tabs.review_history.comment.add')}
        </Button>
      ) : (
        <>
          <div className="little-spacer-bottom">
            {translate('hotspots.tabs.review_history.comment.field')}
          </div>
          <textarea
            autoFocus={true}
            className="form-field fixed-width width-100 spacer-bottom"
            onChange={(event: React.ChangeEvent<HTMLTextAreaElement>) =>
              setComment(event.target.value)
            }
            rows={2}
          />
          <div className="display-flex-space-between display-flex-center">
            <MarkdownTips />
            <div>
              <Button id="hotspot-comment-box-submit" onClick={onComment}>
                {translate('hotspots.tabs.review_history.comment.submit')}
              </Button>
              <ResetButtonLink
                className="spacer-left"
                id="hotspot-comment-box-cancel"
                onClick={onCancel}>
                {translate('cancel')}
              </ResetButtonLink>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
