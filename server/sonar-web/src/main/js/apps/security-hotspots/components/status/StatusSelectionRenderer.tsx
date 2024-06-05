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
import { ButtonPrimary, FormField, InputTextArea, Modal, Note, SelectionCard } from 'design-system';
import * as React from 'react';
import FormattingTips from '../../../../components/common/FormattingTips';
import { translate } from '../../../../helpers/l10n';
import { HotspotStatusOption } from '../../../../types/security-hotspots';

export interface StatusSelectionRendererProps {
  comment?: string;
  loading: boolean;
  onCancel: () => void;
  onCommentChange: (comment: string) => void;
  onStatusChange: (statusOption: HotspotStatusOption) => void;
  onSubmit: () => Promise<void>;
  status: HotspotStatusOption;
  submitDisabled: boolean;
}

export default function StatusSelectionRenderer(props: StatusSelectionRendererProps) {
  const { comment, loading, status, submitDisabled } = props;

  const renderOption = (statusOption: HotspotStatusOption) => {
    return (
      <SelectionCard
        className="sw-mb-3"
        key={statusOption}
        onClick={() => props.onStatusChange(statusOption)}
        selected={statusOption === status}
        title={translate('hotspots.status_option', statusOption)}
        vertical
      >
        <Note className="sw-mt-1 sw-mr-12">
          {translate('hotspots.status_option', statusOption, 'description')}
        </Note>
      </SelectionCard>
    );
  };

  return (
    <Modal
      headerTitle={translate('hotspots.status.review_title')}
      headerDescription={translate('hotspots.status.select')}
      loading={loading}
      isScrollable
      onClose={props.onCancel}
      secondaryButtonLabel={translate('cancel')}
      body={
        <>
          {renderOption(HotspotStatusOption.TO_REVIEW)}
          {renderOption(HotspotStatusOption.ACKNOWLEDGED)}
          {renderOption(HotspotStatusOption.FIXED)}
          {renderOption(HotspotStatusOption.SAFE)}
          <FormField
            htmlFor="comment-textarea"
            label={translate('hotspots.status.add_comment_optional')}
          >
            <InputTextArea
              className="sw-mb-2 sw-resize-y"
              id="comment-textarea"
              onChange={(event: React.ChangeEvent<HTMLTextAreaElement>) =>
                props.onCommentChange(event.currentTarget.value)
              }
              rows={4}
              size="full"
              value={comment}
            />
            <FormattingTips />
          </FormField>
        </>
      }
      primaryButton={
        <ButtonPrimary disabled={submitDisabled || loading} onClick={props.onSubmit}>
          {translate('hotspots.status.change_status')}
        </ButtonPrimary>
      }
    />
  );
}
