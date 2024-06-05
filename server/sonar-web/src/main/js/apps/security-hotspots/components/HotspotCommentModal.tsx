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
import { ButtonPrimary, FormField, InputTextArea, Modal } from 'design-system';
import * as React from 'react';
import FormattingTips from '../../../components/common/FormattingTips';
import { translate } from '../../../helpers/l10n';

export interface HotspotCommentPopupProps {
  onCancel: () => void;
  onSubmit: (comment: string) => void;
  value?: string;
}

export default function HotspotCommentModal(props: HotspotCommentPopupProps) {
  const [comment, setComment] = React.useState(props.value ?? '');

  return (
    <Modal
      headerTitle={translate(
        props.value !== undefined ? 'issue.comment.edit' : 'hotspots.status.add_comment',
      )}
      onClose={props.onCancel}
      body={
        <FormField htmlFor="security-hotspot-comment" label={translate('hotspots.comment.field')}>
          <InputTextArea
            className="sw-mb-2 sw-resize-y"
            id="security-hotspot-comment"
            size="full"
            onChange={(event) => setComment(event.target.value)}
            rows={3}
            value={comment}
          />
          <FormattingTips />
        </FormField>
      }
      primaryButton={
        <ButtonPrimary onClick={() => props.onSubmit(comment)} disabled={!comment}>
          {translate('hotspots.comment.submit')}
        </ButtonPrimary>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
