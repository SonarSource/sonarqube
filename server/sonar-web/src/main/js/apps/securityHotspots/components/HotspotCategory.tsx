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
import { RawHotspot } from '../../../types/securityHotspots';
import HotspotListItem from './HotspotListItem';

export interface HotspotCategoryProps {
  category: {
    key: string;
    title: string;
  };
  hotspots: RawHotspot[];
  onHotspotClick: (key: string) => void;
  selectedHotspotKey: string | undefined;
}

export default function HotspotCategory(props: HotspotCategoryProps) {
  const { category, hotspots, selectedHotspotKey } = props;

  const [expanded, setExpanded] = React.useState(true);

  if (hotspots.length < 1) {
    return null;
  }

  const risk = hotspots[0].vulnerabilityProbability;

  return (
    <div className={classNames('hotspot-category', risk)}>
      <a
        className="hotspot-category-header display-flex-space-between display-flex-center"
        href="#"
        onClick={() => setExpanded(!expanded)}>
        <strong className="flex-1">{category.title}</strong>
        <span>
          <span className="hotspot-counter">{hotspots.length}</span>
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
                selected={h.key === selectedHotspotKey}
              />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
