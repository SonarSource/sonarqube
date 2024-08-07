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

import React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { HotspotStatusFilter } from '../../../types/security-hotspots';

interface HotspotListMetaProps {
  emptyTranslationKey: string;
  hasSelectedHotspot: boolean;
  hotspotsTotal: number;
  isStaticListOfHotspots: boolean;
  loading: boolean;
  statusFilter: HotspotStatusFilter;
}

export default function HotspotListMeta(props: Readonly<HotspotListMetaProps>) {
  const {
    isStaticListOfHotspots,
    statusFilter,
    hotspotsTotal,
    hasSelectedHotspot,
    emptyTranslationKey,
    loading,
  } = props;

  return (
    <output aria-live="polite">
      <span className="sw-sr-only">
        {(hotspotsTotal === 0 || !hasSelectedHotspot) &&
          !loading &&
          translate(`hotspots.${emptyTranslationKey}.title`)}
      </span>
      {(hotspotsTotal > 0 || hasSelectedHotspot) && (
        <span className="sw-body-sm">
          <FormattedMessage
            id="hotspots.list_title"
            defaultMessage={
              isStaticListOfHotspots
                ? translate('hotspots.list_title')
                : translate(`hotspots.list_title.${statusFilter}`)
            }
            values={{
              0: <strong className="sw-body-sm-highlight">{hotspotsTotal}</strong>,
            }}
          />
        </span>
      )}
    </output>
  );
}
