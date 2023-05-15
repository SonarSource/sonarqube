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
import { SubnavigationItem } from 'design-system';
import React, { useCallback } from 'react';
import LocationsList from '../../../components/locations/LocationsList';
import { RawHotspot } from '../../../types/security-hotspots';
import { getLocations } from '../utils';

interface HotspotListItemProps {
  hotspot: RawHotspot;
  onClick: (hotspot: RawHotspot) => void;
  onLocationClick: (index?: number) => void;
  selected: boolean;
  selectedHotspotLocation?: number;
}

export default function HotspotListItem(props: HotspotListItemProps) {
  const { hotspot, selected, selectedHotspotLocation } = props;
  const locations = getLocations(hotspot.flows, undefined);

  // Use useCallback instead of useEffect/useRef combination to be notified of the ref changes
  const itemRef = useCallback(
    (node) => {
      if (selected && node) {
        node.scrollIntoView({
          block: 'center',
          behavior: 'smooth',
        });
      }
    },
    [selected]
  );

  const handleClick = () => {
    if (!selected) {
      props.onClick(hotspot);
    }
  };

  return (
    <SubnavigationItem
      active={selected}
      innerRef={itemRef}
      onClick={handleClick}
      className="sw-flex-col sw-items-start"
    >
      <div>{hotspot.message}</div>
      {selected && (
        <LocationsList
          locations={locations}
          showCrossFile={false} // To be removed once we support multi file location
          componentKey={hotspot.component}
          onLocationSelect={props.onLocationClick}
          selectedLocationIndex={selectedHotspotLocation}
        />
      )}
    </SubnavigationItem>
  );
}
