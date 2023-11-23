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
import { FlagErrorIcon, FlagSuccessIcon, InteractiveIcon, MenuIcon } from 'design-system';
import * as React from 'react';
import { useState } from 'react';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { WebhookResponse } from '../../../types/webhook';
import LatestDeliveryForm from './LatestDeliveryForm';

interface Props {
  webhook: WebhookResponse;
}

export default function WebhookItemLatestDelivery({ webhook }: Props) {
  const [modalOpen, setModalOpen] = useState(false);

  if (!webhook.latestDelivery) {
    return <span>{translate('webhooks.last_execution.none')}</span>;
  }

  return (
    <div className="sw-flex sw-items-center">
      {webhook.latestDelivery.success ? <FlagSuccessIcon /> : <FlagErrorIcon />}
      <div className="sw-ml-2 sw-flex sw-items-center">
        <DateTimeFormatter date={webhook.latestDelivery.at} />
        <span title={translateWithParameters('webhooks.last_execution.open_for_x', webhook.name)}>
          <InteractiveIcon
            className="sw-ml-2"
            Icon={MenuIcon}
            aria-label={translateWithParameters('webhooks.last_execution.open_for_x', webhook.name)}
            onClick={() => setModalOpen(true)}
            size="small"
          />
        </span>
      </div>

      {modalOpen && (
        <LatestDeliveryForm
          delivery={webhook.latestDelivery}
          onClose={() => setModalOpen(false)}
          webhook={webhook}
        />
      )}
    </div>
  );
}
