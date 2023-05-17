/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import styled from '@emotion/styled';
import {
  ButtonPrimary,
  ButtonSecondary,
  DeferredSpinner,
  FormField,
  InputTextArea,
  LightPrimary,
  Note,
  SelectionCard,
} from 'design-system';
import * as React from 'react';
import FormattingTips from '../../../../components/common/FormattingTips';
import Modal from '../../../../components/controls/Modal';
import { translate } from '../../../../helpers/l10n';
import { HotspotStatusOption } from '../../../../types/security-hotspots';

export interface StatusSelectionRendererProps {
  status: HotspotStatusOption;
  onStatusChange: (statusOption: HotspotStatusOption) => void;
  comment?: string;
  onCommentChange: (comment: string) => void;
  onCancel: () => void;
  onSubmit: () => Promise<void>;
  loading: boolean;
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
        vertical={true}
      >
        <Note className="sw-mt-1 sw-mr-12">
          {translate('hotspots.status_option', statusOption, 'description')}
        </Note>
      </SelectionCard>
    );
  };

  return (
    <Modal contentLabel={translate('hotspots.status.review_title')}>
      <header className="sw-p-9">
        <h1 className="sw-heading-lg sw-mb-2">{translate('hotspots.status.review_title')}</h1>
        <LightPrimary as="p" className="sw-body-sm">
          {translate('hotspots.status.select')}
        </LightPrimary>
      </header>
      <MainStyled className="sw-px-9">
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
      </MainStyled>
      <footer className="sw-flex sw-justify-end sw-items-center sw-gap-3 sw-p-9">
        <DeferredSpinner loading={loading} />
        <ButtonPrimary disabled={submitDisabled || loading} onClick={props.onSubmit}>
          {translate('hotspots.status.change_status')}
        </ButtonPrimary>
        <ButtonSecondary onClick={props.onCancel}>{translate('cancel')}</ButtonSecondary>
      </footer>
    </Modal>
  );
}

const MainStyled = styled.main`
  max-height: calc(100vh - 400px);
  overflow: auto;
`;
