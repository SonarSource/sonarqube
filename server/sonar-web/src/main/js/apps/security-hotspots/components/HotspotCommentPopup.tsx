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
import FormattingTips from '../../../components/common/FormattingTips';
import { Button, ResetButtonLink } from '../../../components/controls/buttons';
import { translate } from '../../../helpers/l10n';

export interface HotspotCommentPopupProps {
  markdownComment: string;
  onCommentEditSubmit: (comment: string) => void;
  onCancelEdit: () => void;
}

export default function HotspotCommentPopup(props: HotspotCommentPopupProps) {
  const [comment, setComment] = React.useState(props.markdownComment);

  return (
    <div className="issue-comment-bubble-popup">
      <div className="issue-comment-form-text">
        <textarea
          autoFocus={true}
          onChange={(event) => setComment(event.target.value)}
          rows={2}
          value={comment}
        />
      </div>
      <div className="spacer-top display-flex-space-between">
        <div className="issue-comment-form-tips">
          <FormattingTips />
        </div>
        <div className="">
          <Button
            className="little-spacer-right"
            onClick={() => props.onCommentEditSubmit(comment)}
          >
            {translate('save')}
          </Button>
          <ResetButtonLink
            onClick={() => {
              setComment('');
              props.onCancelEdit();
            }}
          >
            {translate('cancel')}
          </ResetButtonLink>
        </div>
      </div>
    </div>
  );
}
