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
import * as React from 'react';
import ListFooter from 'sonar-ui-common/components/controls/ListFooter';
import SecurityHotspotIcon from 'sonar-ui-common/components/icons/SecurityHotspotIcon';
import { translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { SecurityStandard, Standards } from '../../../types/security';
import { RawHotspot } from '../../../types/security-hotspots';
import { SECURITY_STANDARD_RENDERER } from '../utils';
import HotspotListItem from './HotspotListItem';

export interface HotspotSimpleListProps {
  filterByCategory: {
    standard: SecurityStandard;
    category: string;
  };
  hotspots: RawHotspot[];
  hotspotsTotal: number;
  loadingMore: boolean;
  onHotspotClick: (hotspot: RawHotspot) => void;
  onLoadMore: () => void;
  selectedHotspot: RawHotspot;
  standards: Standards;
}

export default function HotspotSimpleList(props: HotspotSimpleListProps) {
  const {
    filterByCategory,
    hotspots,
    hotspotsTotal,
    loadingMore,
    selectedHotspot,
    standards
  } = props;

  return (
    <div className="hotspots-list-single-category huge-spacer-bottom">
      <h1 className="hotspot-list-header bordered-bottom">
        <SecurityHotspotIcon className="spacer-right" />
        {translateWithParameters('hotspots.list_title', hotspotsTotal)}
      </h1>
      <div className="big-spacer-bottom">
        <div className="hotspot-category">
          <div className="hotspot-category-header">
            <strong className="flex-1 spacer-right break-word">
              {SECURITY_STANDARD_RENDERER[filterByCategory.standard](
                standards,
                filterByCategory.category
              )}
            </strong>
          </div>
          <ul>
            {hotspots.map(h => (
              <li data-hotspot-key={h.key} key={h.key}>
                <HotspotListItem
                  hotspot={h}
                  onClick={props.onHotspotClick}
                  selected={h.key === selectedHotspot.key}
                />
              </li>
            ))}
          </ul>
        </div>
      </div>
      <ListFooter
        count={hotspots.length}
        loadMore={!loadingMore ? props.onLoadMore : undefined}
        loading={loadingMore}
        total={hotspotsTotal}
      />
    </div>
  );
}
