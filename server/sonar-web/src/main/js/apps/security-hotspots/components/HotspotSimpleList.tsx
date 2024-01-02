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
import * as React from 'react';
import ListFooter from '../../../components/controls/ListFooter';
import Tooltip from '../../../components/controls/Tooltip';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import SecurityHotspotIcon from '../../../components/icons/SecurityHotspotIcon';
import { translateWithParameters } from '../../../helpers/l10n';
import { addSideBarClass, removeSideBarClass } from '../../../helpers/pages';
import { fileFromPath } from '../../../helpers/path';
import { ComponentQualifier } from '../../../types/component';
import { SecurityStandard, Standards } from '../../../types/security';
import { RawHotspot } from '../../../types/security-hotspots';
import { SECURITY_STANDARD_RENDERER } from '../utils';
import HotspotListItem from './HotspotListItem';

export interface HotspotSimpleListProps {
  filterByCategory?: {
    standard: SecurityStandard;
    category: string;
  };
  filterByCWE?: string;
  filterByFile?: string;
  hotspots: RawHotspot[];
  hotspotsTotal: number;
  loadingMore: boolean;
  onHotspotClick: (hotspot: RawHotspot) => void;
  onLocationClick: (index?: number) => void;
  onLoadMore: () => void;
  selectedHotspot: RawHotspot;
  selectedHotspotLocation?: number;
  standards: Standards;
}

export default class HotspotSimpleList extends React.Component<HotspotSimpleListProps> {
  componentDidMount() {
    addSideBarClass();
  }

  componentWillUnmount() {
    removeSideBarClass();
  }

  render() {
    const {
      filterByCategory,
      filterByCWE,
      filterByFile,
      hotspots,
      hotspotsTotal,
      loadingMore,
      selectedHotspot,
      selectedHotspotLocation,
      standards,
    } = this.props;

    const categoryLabel =
      filterByCategory &&
      SECURITY_STANDARD_RENDERER[filterByCategory.standard](standards, filterByCategory.category);

    const cweLabel =
      filterByCWE && SECURITY_STANDARD_RENDERER[SecurityStandard.CWE](standards, filterByCWE);

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
                {filterByFile ? (
                  <Tooltip overlay={filterByFile}>
                    <span>
                      <QualifierIcon
                        className="little-spacer-right"
                        qualifier={ComponentQualifier.File}
                      />
                      {fileFromPath(filterByFile)}
                    </span>
                  </Tooltip>
                ) : (
                  <>
                    {categoryLabel}
                    {categoryLabel && cweLabel && <hr />}
                    {cweLabel}
                  </>
                )}
              </strong>
            </div>
            <ul>
              {hotspots.map((h) => (
                <li data-hotspot-key={h.key} key={h.key}>
                  <HotspotListItem
                    hotspot={h}
                    onClick={this.props.onHotspotClick}
                    onLocationClick={this.props.onLocationClick}
                    selected={h.key === selectedHotspot.key}
                    selectedHotspotLocation={selectedHotspotLocation}
                  />
                </li>
              ))}
            </ul>
          </div>
        </div>
        <ListFooter
          count={hotspots.length}
          loadMore={!loadingMore ? this.props.onLoadMore : undefined}
          loading={loadingMore}
          total={hotspotsTotal}
        />
      </div>
    );
  }
}
