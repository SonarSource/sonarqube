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
import ChevronDownIcon from '../../../components/icons/ChevronDownIcon';
import ChevronUpIcon from '../../../components/icons/ChevronUpIcon';
import { RawHotspot } from '../../../types/security-hotspots';
import HotspotListItem from './HotspotListItem';

export interface HotspotCategoryProps {
  categoryKey: string;
  expanded: boolean;
  hotspots: RawHotspot[];
  onHotspotClick: (hotspot: RawHotspot) => void;
  onToggleExpand?: (categoryKey: string, value: boolean) => void;
  onLocationClick: (index: number) => void;
  selectedHotspot: RawHotspot;
  selectedHotspotLocation?: number;
  title: string;
  isLastAndIncomplete: boolean;
}

export default function HotspotCategory(props: HotspotCategoryProps) {
  const {
    categoryKey,
    expanded,
    hotspots,
    selectedHotspot,
    title,
    isLastAndIncomplete,
    selectedHotspotLocation,
  } = props;

  if (hotspots.length < 1) {
    return null;
  }

  const risk = hotspots[0].vulnerabilityProbability;

  return (
    <div className={classNames('hotspot-category', risk)}>
      {props.onToggleExpand ? (
        <ButtonPlain
          className={classNames(
            'hotspot-category-header display-flex-space-between display-flex-center',
            { 'contains-selected-hotspot': selectedHotspot.securityCategory === categoryKey }
          )}
          onClick={() => props.onToggleExpand && props.onToggleExpand(categoryKey, !expanded)}
          aria-expanded={expanded}
        >
          <strong className="flex-1 spacer-right break-word">{title}</strong>
          <span>
            <span className="counter-badge">
              {hotspots.length}
              {isLastAndIncomplete && '+'}
            </span>
            {expanded ? (
              <ChevronUpIcon className="big-spacer-left" />
            ) : (
              <ChevronDownIcon className="big-spacer-left" />
            )}
          </span>
        </ButtonPlain>
      ) : (
        <div className="hotspot-category-header">
          <strong className="flex-1 spacer-right break-word">{title}</strong>
        </div>
      )}
      {expanded && (
        <ul>
          {hotspots.map((h) => (
            <li data-hotspot-key={h.key} key={h.key}>
              <HotspotListItem
                hotspot={h}
                onClick={props.onHotspotClick}
                onLocationClick={props.onLocationClick}
                selectedHotspotLocation={selectedHotspotLocation}
                selected={h.key === selectedHotspot.key}
              />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
