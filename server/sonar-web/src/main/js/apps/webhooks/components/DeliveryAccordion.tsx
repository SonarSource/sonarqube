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

import { useState } from 'react';
import { useIntl } from 'react-intl';
import { FlagErrorIcon, FlagSuccessIcon, TextAccordion } from '~design-system';
import { getDelivery } from '../../../api/webhooks';
import { longFormatterOption } from '../../../components/intl/DateFormatter';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { translate } from '../../../helpers/l10n';
import { WebhookDelivery } from '../../../types/webhook';
import DeliveryItem from './DeliveryItem';

interface Props {
  delivery: WebhookDelivery;
}

export default function DeliveryAccordion({ delivery }: Props) {
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [payload, setPayload] = useState<string | undefined>(undefined);

  const intl = useIntl();

  async function fetchPayload() {
    setLoading(true);
    try {
      const response = await getDelivery({ deliveryId: delivery.id });
      setPayload(response.delivery.payload);
    } finally {
      setLoading(false);
    }
  }

  function handleClick() {
    if (!payload) {
      fetchPayload();
    }
    setOpen(!open);
  }

  return (
    <TextAccordion
      ariaLabel={intl.formatDate(delivery.at, longFormatterOption)}
      onClick={handleClick}
      open={open}
      renderHeader={() =>
        delivery.success ? (
          <FlagSuccessIcon
            aria-label={translate('success')}
            className="sw-pt-4 sw-pb-2 sw-pr-4 sw-float-right it__success"
          />
        ) : (
          <FlagErrorIcon
            aria-label={translate('error')}
            className="sw-pt-4 sw-pb-2 sw-pr-4 sw-float-right js-error"
          />
        )
      }
      title={<DateTimeFormatter date={delivery.at} />}
    >
      <DeliveryItem
        className="it__accordion-content sw-ml-4"
        delivery={delivery}
        loading={loading}
        payload={payload}
      />
    </TextAccordion>
  );
}
