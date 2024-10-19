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
import styled from '@emotion/styled';
import { LightLabel, LightPrimary } from 'design-system';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import { HotspotStatusOption } from '../../../../types/security-hotspots';

export interface StatusDescriptionProps {
  statusOption: HotspotStatusOption;
}

export default function StatusDescription(props: StatusDescriptionProps) {
  const { statusOption } = props;

  return (
    <div>
      <h2>
        <LightPrimary className="sw-typo-semibold">
          {`${translate('status')}: `}
          {translate('hotspots.status_option', statusOption)}
        </LightPrimary>
      </h2>
      <Description className="sw-mt-1">
        <LightLabel className="sw-typo-default">
          {translate('hotspots.status_option', statusOption, 'description')}
        </LightLabel>
      </Description>
    </div>
  );
}

const Description = styled.div`
  max-width: 360px;
`;
