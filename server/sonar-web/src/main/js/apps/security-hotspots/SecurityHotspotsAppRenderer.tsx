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
import { Helmet } from 'react-helmet-async';
import A11ySkipTarget from '../../components/a11y/A11ySkipTarget';
import ScreenPositionHelper from '../../components/common/ScreenPositionHelper';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import DeferredSpinner from '../../components/ui/DeferredSpinner';
import { isBranch } from '../../helpers/branch-like';
import { translate } from '../../helpers/l10n';
import { scrollToElement } from '../../helpers/scrolling';
import { BranchLike } from '../../types/branch-like';
import { SecurityStandard, Standards } from '../../types/security';
import { HotspotFilters, HotspotStatusFilter, RawHotspot } from '../../types/security-hotspots';
import { Component, StandardSecurityCategories } from '../../types/types';
import EmptyHotspotsPage from './components/EmptyHotspotsPage';
import FilterBar from './components/FilterBar';
import HotspotList from './components/HotspotList';
import HotspotSimpleList from './components/HotspotSimpleList';
import HotspotViewer from './components/HotspotViewer';
import './styles.css';

export interface SecurityHotspotsAppRendererProps {
  branchLike?: BranchLike;
  component: Component;
  filterByCategory?: {
    standard: SecurityStandard;
    category: string;
  };
  filterByCWE?: string;
  filterByFile?: string;
  filters: HotspotFilters;
  hotspots: RawHotspot[];
  hotspotsReviewedMeasure?: string;
  hotspotsTotal: number;
  isStaticListOfHotspots: boolean;
  loading: boolean;
  loadingMeasure: boolean;
  loadingMore: boolean;
  onChangeFilters: (filters: Partial<HotspotFilters>) => void;
  onHotspotClick: (hotspot: RawHotspot) => void;
  onLocationClick: (index?: number) => void;
  onLoadMore: () => void;
  onSwitchStatusFilter: (option: HotspotStatusFilter) => void;
  onUpdateHotspot: (hotspotKey: string) => Promise<void>;
  selectedHotspot?: RawHotspot;
  selectedHotspotLocation?: number;
  securityCategories: StandardSecurityCategories;
  standards: Standards;
}

export default function SecurityHotspotsAppRenderer(props: SecurityHotspotsAppRendererProps) {
  const {
    branchLike,
    component,
    filterByCategory,
    filterByCWE,
    filterByFile,
    filters,
    hotspots,
    hotspotsReviewedMeasure,
    hotspotsTotal,
    isStaticListOfHotspots,
    loading,
    loadingMeasure,
    loadingMore,
    securityCategories,
    selectedHotspot,
    selectedHotspotLocation,
    standards,
  } = props;

  const scrollableRef = React.useRef(null);

  React.useEffect(() => {
    const parent = scrollableRef.current;
    const element =
      selectedHotspot && document.querySelector(`[data-hotspot-key="${selectedHotspot.key}"]`);
    if (parent && element) {
      scrollToElement(element, { parent, smooth: true, topOffset: 100, bottomOffset: 100 });
    }
  }, [selectedHotspot]);

  return (
    <div id="security_hotspots">
      <Suggestions suggestions="security_hotspots" />
      <Helmet title={translate('hotspots.page')} />
      <A11ySkipTarget anchor="security_hotspots_main" />

      <FilterBar
        component={component}
        filters={filters}
        hotspotsReviewedMeasure={hotspotsReviewedMeasure}
        isStaticListOfHotspots={isStaticListOfHotspots}
        loadingMeasure={loadingMeasure}
        onBranch={isBranch(branchLike)}
        onChangeFilters={props.onChangeFilters}
      />

      {loading && (
        <div className="layout-page">
          <div className="layout-page-side-inner">
            <DeferredSpinner className="big-spacer-top" />
          </div>
        </div>
      )}

      {!loading &&
        (hotspots.length === 0 || !selectedHotspot ? (
          <EmptyHotspotsPage
            filtered={
              filters.assignedToMe ||
              (isBranch(branchLike) && filters.inNewCodePeriod) ||
              filters.status !== HotspotStatusFilter.TO_REVIEW
            }
            filterByFile={Boolean(filterByFile)}
            isStaticListOfHotspots={isStaticListOfHotspots}
          />
        ) : (
          <div className="layout-page">
            <ScreenPositionHelper className="layout-page-side-outer">
              {({ top }) => (
                <div className="layout-page-side" ref={scrollableRef} style={{ top }}>
                  <div className="layout-page-side-inner">
                    {filterByCategory || filterByCWE || filterByFile ? (
                      <HotspotSimpleList
                        filterByCategory={filterByCategory}
                        filterByCWE={filterByCWE}
                        filterByFile={filterByFile}
                        hotspots={hotspots}
                        hotspotsTotal={hotspotsTotal}
                        loadingMore={loadingMore}
                        onHotspotClick={props.onHotspotClick}
                        onLoadMore={props.onLoadMore}
                        onLocationClick={props.onLocationClick}
                        selectedHotspotLocation={selectedHotspotLocation}
                        selectedHotspot={selectedHotspot}
                        standards={standards}
                      />
                    ) : (
                      <HotspotList
                        hotspots={hotspots}
                        hotspotsTotal={hotspotsTotal}
                        isStaticListOfHotspots={isStaticListOfHotspots}
                        loadingMore={loadingMore}
                        onHotspotClick={props.onHotspotClick}
                        onLoadMore={props.onLoadMore}
                        onLocationClick={props.onLocationClick}
                        securityCategories={securityCategories}
                        selectedHotspot={selectedHotspot}
                        selectedHotspotLocation={selectedHotspotLocation}
                        statusFilter={filters.status}
                      />
                    )}
                  </div>
                </div>
              )}
            </ScreenPositionHelper>

            <div className="layout-page-main">
              <HotspotViewer
                component={component}
                hotspotKey={selectedHotspot.key}
                hotspotsReviewedMeasure={hotspotsReviewedMeasure}
                onSwitchStatusFilter={props.onSwitchStatusFilter}
                onUpdateHotspot={props.onUpdateHotspot}
                onLocationClick={props.onLocationClick}
                selectedHotspotLocation={selectedHotspotLocation}
              />
            </div>
          </div>
        ))}
    </div>
  );
}
