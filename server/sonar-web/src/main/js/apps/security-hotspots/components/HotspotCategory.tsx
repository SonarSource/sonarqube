/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import ChevronDownIcon from 'sonar-ui-common/components/icons/ChevronDownIcon';
import ChevronUpIcon from 'sonar-ui-common/components/icons/ChevronUpIcon';
import { RawHotspot } from '../../../types/security-hotspots';
import HotspotListItem from './HotspotListItem';

export interface HotspotCategoryProps {
  categoryKey: string;
  expanded: boolean;
  hotspots: RawHotspot[];
  onHotspotClick: (hotspot: RawHotspot) => void;
  onToggleExpand: (categoryKey: string, value: boolean) => void;
  selectedHotspot: RawHotspot;
  title: string;
}

export default function HotspotCategory(props: HotspotCategoryProps) {
  const { categoryKey, expanded, hotspots, selectedHotspot, title } = props;

  if (hotspots.length < 1) {
    return null;
  }

  const risk = hotspots[0].vulnerabilityProbability;

  return (
    <div className={classNames('hotspot-category', risk)}>
      <a
        className={classNames(
          'hotspot-category-header display-flex-space-between display-flex-center',
          { 'contains-selected-hotspot': selectedHotspot.securityCategory === categoryKey }
        )}
        href="#"
        onClick={() => props.onToggleExpand(categoryKey, !expanded)}>
        <strong className="flex-1">{title}</strong>
        <span>
          <span className="counter-badge">{hotspots.length}</span>
          {expanded ? (
            <ChevronUpIcon className="big-spacer-left" />
          ) : (
            <ChevronDownIcon className="big-spacer-left" />
          )}
        </span>
      </a>
      {expanded && (
        <ul>
          {hotspots.map(h => (
            <li key={h.key}>
              <HotspotListItem
                hotspot={h}
                onClick={props.onHotspotClick}
                selected={h.key === selectedHotspot.key}
              />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
