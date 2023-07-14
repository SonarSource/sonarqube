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
import * as React from 'react';
import { useState } from 'react';
import { getDelivery } from '../../../api/webhooks';
import BoxedGroupAccordion from '../../../components/controls/BoxedGroupAccordion';
import AlertErrorIcon from '../../../components/icons/AlertErrorIcon';
import AlertSuccessIcon from '../../../components/icons/AlertSuccessIcon';
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
    <BoxedGroupAccordion
      onClick={handleClick}
      open={open}
      renderHeader={() =>
        delivery.success ? (
          <AlertSuccessIcon aria-label={translate('success')} className="it__success" />
        ) : (
          <AlertErrorIcon aria-label={translate('error')} />
        )
      }
      title={<DateTimeFormatter date={delivery.at} />}
    >
      <DeliveryItem
        className="big-spacer-left"
        delivery={delivery}
        loading={loading}
        payload={payload}
      />
    </BoxedGroupAccordion>
  );
}
