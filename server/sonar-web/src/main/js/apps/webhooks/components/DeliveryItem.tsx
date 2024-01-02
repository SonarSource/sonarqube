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
import CodeSnippet from '../../../components/common/CodeSnippet';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { WebhookDelivery } from '../../../types/webhook';

interface Props {
  className?: string;
  delivery: WebhookDelivery;
  loading: boolean;
  payload: string | undefined;
}

export default function DeliveryItem({ className, delivery, loading, payload }: Props) {
  return (
    <div className={className}>
      <p className="spacer-bottom">
        {translateWithParameters(
          'webhooks.delivery.response_x',
          delivery.httpStatus || translate('webhooks.delivery.server_unreachable')
        )}
      </p>
      <p className="spacer-bottom">
        {translateWithParameters(
          'webhooks.delivery.duration_x',
          formatMeasure(delivery.durationMs, 'MILLISEC')
        )}
      </p>
      <p className="spacer-bottom">{translate('webhooks.delivery.payload')}</p>
      <DeferredSpinner className="spacer-left spacer-top" loading={loading}>
        {payload && <CodeSnippet noCopy={true} snippet={formatPayload(payload)} />}
      </DeferredSpinner>
    </div>
  );
}

function formatPayload(payload: string) {
  try {
    return JSON.stringify(JSON.parse(payload), undefined, 2);
  } catch (error) {
    return payload;
  }
}
