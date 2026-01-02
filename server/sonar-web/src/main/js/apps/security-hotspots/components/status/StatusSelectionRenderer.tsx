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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { DatePicker, FormField, InputTextArea, Modal, Note, SelectionCard } from '~design-system';
import FormattingTips from '../../../../components/common/FormattingTips';
import { translate } from '../../../../helpers/l10n';
import { Hotspot, HotspotStatusOption } from '../../../../types/security-hotspots';

export interface StatusSelectionRendererProps {
  comment?: string;
  expiryDate?: string;
  loading: boolean;
  onCancel: () => void;
  onCommentChange: (comment: string) => void;
  onExpiryDateChange: (date?: string) => void;
  onStatusChange: (statusOption: HotspotStatusOption) => void;
  onSubmit: () => Promise<void>;
  status: HotspotStatusOption;
  submitDisabled: boolean;
  issueResolutionExpiryDate?: string;
  hotspot: Hotspot;
}

export default function StatusSelectionRenderer(props: StatusSelectionRendererProps) {
  const { comment, expiryDate, loading, status, submitDisabled } = props;
  const [date, setDate] = useState<Date | undefined>(expiryDate ? new Date(expiryDate) : undefined);

  // Load existing expiry date from server when component mounts using the show api
  useEffect(() => {
    const expiryTimestamp = props.hotspot.issueResolutionExpiresAt;

    if (typeof expiryTimestamp === 'number' && expiryTimestamp > 0) {
      const loadedDate = new Date(expiryTimestamp);
      setDate(loadedDate);

      const yyyy = loadedDate.getFullYear();
      const mm = String(loadedDate.getMonth() + 1).padStart(2, '0');
      const dd = String(loadedDate.getDate()).padStart(2, '0');

      props.onExpiryDateChange(`${yyyy}-${mm}-${dd}`);
    } else {
      setDate(undefined);
      props.onExpiryDateChange(undefined);
    }
  }, [props.hotspot.key, props.hotspot.issueResolutionExpiresAt]);

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
        {statusOption === HotspotStatusOption.EXCEPTION && (
          <div style={{ marginTop: '40px', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <DatePicker
              clearButtonLabel="Clear"
              minDate={(() => {
                const d = new Date();
                d.setDate(d.getDate() + 1); // Today + 1
                return d;
              })()}
              maxDate={new Date(2050, 11, 31)} // Maximum date
              placeholder="No Expiry"
              name="myDate"
              value={date} // Pass Date or undefined, not a string
              onChange={(d?: Date) => {
                const newDate = d ?? undefined;
                setDate(newDate);

                if (newDate) {
                  const yyyy = newDate.getFullYear();
                  const mm = String(newDate.getMonth() + 1).padStart(2, '0');
                  const dd = String(newDate.getDate()).padStart(2, '0');
                  props.onExpiryDateChange(`${yyyy}-${mm}-${dd}`);
                } else {
                  props.onExpiryDateChange(undefined);
                }
              }}
            />

            {date && (
              <div style={{ marginLeft: '16px' }}>
                {`Expires in ${Math.ceil(
                  (date.getTime() - Date.now()) / (1000 * 60 * 60 * 24),
                )} days`}
              </div>
            )}
          </div>
        )}
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
          {renderOption(HotspotStatusOption.EXCEPTION)}
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
        <Button
          isDisabled={submitDisabled || loading}
          onClick={props.onSubmit}
          variety={ButtonVariety.Primary}
        >
          {translate('hotspots.status.change_status')}
        </Button>
      }
    />
  );
}
