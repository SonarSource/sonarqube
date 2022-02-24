/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import LocationsList from '../../../components/locations/LocationsList';
import { ComponentQualifier } from '../../../types/component';
import { RawHotspot } from '../../../types/security-hotspots';
import { getFilePath, getLocations } from '../utils';

export interface HotspotListItemProps {
  hotspot: RawHotspot;
  onClick: (hotspot: RawHotspot) => void;
  onLocationClick: (index: number) => void;
  onScroll: (element: Element) => void;
  selected: boolean;
  selectedHotspotLocation?: number;
}

export default function HotspotListItem(props: HotspotListItemProps) {
  const { hotspot, selected, selectedHotspotLocation } = props;
  const locations = getLocations(hotspot.flows, undefined);
  const path = getFilePath(hotspot.component, hotspot.project);

  return (
    <a
      className={classNames('hotspot-item', { highlight: selected })}
      href="#"
      onClick={() => !selected && props.onClick(hotspot)}>
      <div className="little-spacer-left text-bold">{hotspot.message}</div>
      <div className="display-flex-center">
        <QualifierIcon qualifier={ComponentQualifier.File} />
        <div
          className="little-spacer-left hotspot-box-filename text-ellipsis big-spacer-top big-spacer-bottom"
          title={path}>
          {path}
        </div>
      </div>
      <div className="spacer-top">
        {selected && (
          <LocationsList
            locations={locations}
            isCrossFile={false} // Currently we are not supporting cross file for security hotspot
            uniqueKey={hotspot.key}
            onLocationSelect={props.onLocationClick}
            selectedLocationIndex={selectedHotspotLocation}
            scroll={props.onScroll}
          />
        )}
      </div>
    </a>
  );
}
