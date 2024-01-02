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
import { FormattedMessage } from 'react-intl';
import { Button, ButtonLink } from '../../../components/controls/buttons';
import Modal from '../../../components/controls/Modal';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { HotspotStatusOption } from '../../../types/security-hotspots';

export interface StatusUpdateSuccessModalProps {
  hotspotsReviewedMeasure?: string;
  lastStatusChangedTo?: HotspotStatusOption;
  onClose: () => void;
  onSwitchFilterToStatusOfUpdatedHotspot: () => void;
}

export default function StatusUpdateSuccessModal(props: StatusUpdateSuccessModalProps) {
  const { hotspotsReviewedMeasure, lastStatusChangedTo } = props;

  if (!lastStatusChangedTo) {
    return null;
  }

  const closingHotspots = lastStatusChangedTo !== HotspotStatusOption.TO_REVIEW;
  const statusLabel = translate('hotspots.status_option', lastStatusChangedTo);
  const modalTitle = closingHotspots
    ? translate('hotspots.congratulations')
    : translate('hotspots.update.success');

  return (
    <Modal contentLabel={modalTitle}>
      <div className="modal-head big text-center text-bold">
        <p>{translateWithParameters('hotspots.successful_status_change_to_x', statusLabel)}</p>
      </div>

      <div className="modal-body text-center">
        <FormattedMessage
          id="hotspots.successfully_changed_to_x"
          defaultMessage={translate('hotspots.find_in_status_filter_x')}
          values={{
            status_label: <strong>{statusLabel}</strong>,
          }}
        />
        {closingHotspots && (
          <p className="spacer-top">
            <FormattedMessage
              id="hotspots.x_done_keep_going"
              defaultMessage={translate('hotspots.x_done_keep_going')}
              values={{
                percentage: (
                  <strong>
                    {formatMeasure(hotspotsReviewedMeasure, 'PERCENT', {
                      omitExtraDecimalZeros: true,
                    })}
                  </strong>
                ),
              }}
            />
          </p>
        )}
      </div>

      <div className="modal-foot modal-foot-clear display-flex-center display-flex-space-between">
        <ButtonLink onClick={props.onSwitchFilterToStatusOfUpdatedHotspot}>
          {translateWithParameters('hotspots.see_x_hotspots', statusLabel)}
        </ButtonLink>
        <Button className="button padded" onClick={props.onClose}>
          {translate('hotspots.continue_to_next_hotspot')}
        </Button>
      </div>
    </Modal>
  );
}
