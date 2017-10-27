/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import CheckIcon from '../../../components/icons-components/CheckIcon';
import { EditionStatus } from '../../../api/marketplace';
import { translate } from '../../../helpers/l10n';

interface Props {
  editionKey: string;
  status: EditionStatus;
}

export default function EditionBoxBadge({ editionKey, status }: Props) {
  const inProgress = ['AUTOMATIC_IN_PROGRESS', 'AUTOMATIC_READY'].includes(
    status.installationStatus
  );
  const isInstalling = inProgress && status.nextEditionKey === editionKey;

  if (isInstalling) {
    const installReady = status.installationStatus === 'AUTOMATIC_READY';
    return (
      <span className="marketplace-edition-badge badge badge-normal-size">
        {installReady ? translate('marketplace.pending') : translate('marketplace.installing')}
      </span>
    );
  }

  const isInstalled = status.currentEditionKey === editionKey;
  if (isInstalled) {
    return (
      <span className="marketplace-edition-badge badge badge-normal-size">
        <CheckIcon size={14} className="little-spacer-right text-bottom" />
        {translate('marketplace.installed')}
      </span>
    );
  }

  return null;
}
