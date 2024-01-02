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
import { HighlightedSection } from 'design-system';
import * as React from 'react';
import { Hotspot, HotspotStatusOption } from '../../../../types/security-hotspots';
import { getStatusOptionFromStatusAndResolution } from '../../utils';
import StatusDescription from './StatusDescription';
import StatusReviewButton from './StatusReviewButton';

export interface StatusProps {
  hotspot: Hotspot;
  onStatusChange: (statusOption: HotspotStatusOption) => Promise<void>;
}

export default function Status(props: StatusProps) {
  const { hotspot } = props;

  const statusOption = getStatusOptionFromStatusAndResolution(hotspot.status, hotspot.resolution);

  return (
    <HighlightedSection className="sw-flex sw-rounded-1 sw-p-4 sw-items-center sw-justify-between sw-gap-2 sw-flex-row">
      <StatusDescription statusOption={statusOption} />
      <StatusReviewButton hotspot={hotspot} onStatusChange={props.onStatusChange} />
    </HighlightedSection>
  );
}
