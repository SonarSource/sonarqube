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
import classNames from 'classnames';
import * as React from 'react';
import { ButtonPlain } from '../../../components/controls/buttons';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { IssueMessageHighlighting } from '../../../components/issue/IssueMessageHighlighting';
import LocationsList from '../../../components/locations/LocationsList';
import { ComponentQualifier } from '../../../types/component';
import { RawHotspot } from '../../../types/security-hotspots';
import { getFilePath, getLocations } from '../utils';

export interface HotspotListItemProps {
  hotspot: RawHotspot;
  onClick: (hotspot: RawHotspot) => void;
  onLocationClick: (index?: number) => void;
  selected: boolean;
  selectedHotspotLocation?: number;
}

export default function HotspotListItem(props: HotspotListItemProps) {
  const { hotspot, selected, selectedHotspotLocation } = props;
  const locations = getLocations(hotspot.flows, undefined);
  const path = getFilePath(hotspot.component, hotspot.project);

  return (
    <ButtonPlain
      aria-current={selected}
      className={classNames('hotspot-item', { highlight: selected })}
      onClick={() => !selected && props.onClick(hotspot)}
    >
      {/* This is not a real interaction it is only for scrolling */
      /* eslint-disable-next-line jsx-a11y/no-static-element-interactions */}
      <div
        className={classNames('little-spacer-left text-bold', { 'cursor-pointer': selected })}
        onClick={selected ? () => props.onLocationClick() : undefined}
      >
        <IssueMessageHighlighting
          message={hotspot.message}
          messageFormattings={hotspot.messageFormattings}
        />
      </div>
      <div className="display-flex-center big-spacer-top">
        <QualifierIcon qualifier={ComponentQualifier.File} />
        <div className="little-spacer-left hotspot-box-filename text-ellipsis" title={path}>
          {/* <bdi> is used to avoid some cases where the path is wrongly displayed */}
          {/* because of the parent's direction=rtl */}
          <bdi>{path}</bdi>
        </div>
      </div>
      {selected && (
        <LocationsList
          locations={locations}
          showCrossFile={false} // To be removed once we support multi file location
          componentKey={hotspot.component}
          onLocationSelect={props.onLocationClick}
          selectedLocationIndex={selectedHotspotLocation}
        />
      )}
    </ButtonPlain>
  );
}
