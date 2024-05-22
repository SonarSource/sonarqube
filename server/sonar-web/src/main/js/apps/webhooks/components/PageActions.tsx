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

import { ButtonPrimary } from 'design-system';
import * as React from 'react';
import { useState } from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import CreateWebhookForm from './CreateWebhookForm';

interface Props {
  loading: boolean;
  onCreate: (data: { name: string; secret?: string; url: string }) => Promise<void>;
  webhooksCount: number;
}

export const WEBHOOKS_LIMIT = 10;

export default function PageActions(props: Props) {
  const { loading, onCreate, webhooksCount } = props;

  const [openCreate, setOpenCreate] = useState(false);

  function handleCreateClose() {
    setOpenCreate(false);
  }

  function handleCreateOpen() {
    setOpenCreate(true);
  }

  if (loading) {
    return null;
  }

  if (webhooksCount >= WEBHOOKS_LIMIT) {
    return (
      <Tooltip content={translateWithParameters('webhooks.maximum_reached', WEBHOOKS_LIMIT)}>
        <ButtonPrimary className="it__webhook-create" disabled>
          {translate('create')}
        </ButtonPrimary>
      </Tooltip>
    );
  }

  return (
    <>
      <ButtonPrimary className="it__webhook-create" onClick={handleCreateOpen}>
        {translate('create')}
      </ButtonPrimary>
      {openCreate && <CreateWebhookForm onClose={handleCreateClose} onDone={onCreate} />}
    </>
  );
}
