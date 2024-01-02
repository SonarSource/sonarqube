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
import { withTheme } from '@emotion/react';
import styled from '@emotion/styled';
import {
  LAYOUT_FOOTER_HEIGHT,
  LAYOUT_GLOBAL_NAV_HEIGHT,
  LAYOUT_PROJECT_NAV_HEIGHT,
  LargeCenteredLayout,
  PageContentFontWrapper,
  Spinner,
  themeBorder,
  themeColor,
} from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import A11ySkipTarget from '../../components/a11y/A11ySkipTarget';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import { isBranch } from '../../helpers/branch-like';
import { translate } from '../../helpers/l10n';
import useFollowScroll from '../../hooks/useFollowScroll';
import { BranchLike } from '../../types/branch-like';
import { ComponentQualifier } from '../../types/component';
import { MetricKey } from '../../types/metrics';
import { SecurityStandard, Standards } from '../../types/security';
import { HotspotFilters, HotspotStatusFilter, RawHotspot } from '../../types/security-hotspots';
import { Component, StandardSecurityCategories } from '../../types/types';
import EmptyHotspotsPage from './components/EmptyHotspotsPage';
import HotspotList from './components/HotspotList';
import HotspotSidebarHeader from './components/HotspotSidebarHeader';
import HotspotSimpleList from './components/HotspotSimpleList';
import HotspotFilterByStatus from './components/HotspotStatusFilter';
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
  onLoadMore: () => void;
  onLocationClick: (index?: number) => void;
  onShowAllHotspots: VoidFunction;
  onSwitchStatusFilter: (option: HotspotStatusFilter) => void;
  onUpdateHotspot: (hotspotKey: string) => Promise<void>;
  securityCategories: StandardSecurityCategories;
  selectedHotspot?: RawHotspot;
  selectedHotspotLocation?: number;
  standards: Standards;
}

const STICKY_HEADER_HEIGHT = 73;

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
    onChangeFilters,
    onShowAllHotspots,
    securityCategories,
    selectedHotspot,
    selectedHotspotLocation,
    standards,
  } = props;

  const isProject = component.qualifier === ComponentQualifier.Project;

  const { top: topScroll } = useFollowScroll();

  const distanceFromBottom = topScroll + window.innerHeight - document.body.clientHeight;

  const footerVisibleHeight =
    distanceFromBottom > -LAYOUT_FOOTER_HEIGHT ? LAYOUT_FOOTER_HEIGHT + distanceFromBottom : 0;

  return (
    <>
      <Suggestions suggestions={MetricKey.security_hotspots} />

      <Helmet title={translate('hotspots.page')} />

      <A11ySkipTarget anchor="security_hotspots_main" />

      <LargeCenteredLayout id={MetricKey.security_hotspots}>
        <PageContentFontWrapper>
          <div className="sw-grid sw-grid-cols-12 sw-w-full">
            <StyledSidebar
              aria-label={translate('hotspots.list')}
              className="sw-z-filterbar sw-col-span-4"
            >
              {isProject && (
                <StyledSidebarHeader className="sw-w-full sw-px-4 sw-py-2">
                  <HotspotSidebarHeader
                    branchLike={branchLike}
                    filters={filters}
                    hotspotsReviewedMeasure={hotspotsReviewedMeasure}
                    isStaticListOfHotspots={isStaticListOfHotspots}
                    loadingMeasure={loadingMeasure}
                    onChangeFilters={onChangeFilters}
                  />
                </StyledSidebarHeader>
              )}

              <StyledSidebarContent
                className="sw-p-4 it__hotspot-list"
                style={{
                  height: `calc(
                    100vh - ${
                      LAYOUT_GLOBAL_NAV_HEIGHT +
                      LAYOUT_PROJECT_NAV_HEIGHT +
                      STICKY_HEADER_HEIGHT -
                      footerVisibleHeight
                    }px
                  )`,
                  top: `${
                    LAYOUT_GLOBAL_NAV_HEIGHT + LAYOUT_PROJECT_NAV_HEIGHT + STICKY_HEADER_HEIGHT
                  }px`,
                }}
              >
                <HotspotFilterByStatus
                  filters={filters}
                  isStaticListOfHotspots={isStaticListOfHotspots}
                  onChangeFilters={onChangeFilters}
                  onShowAllHotspots={onShowAllHotspots}
                />
                <Spinner className="sw-mt-3" loading={loading}>
                  {hotspots.length > 0 && selectedHotspot && (
                    <>
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
                          selectedHotspot={selectedHotspot}
                          selectedHotspotLocation={selectedHotspotLocation}
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
                    </>
                  )}
                </Spinner>
              </StyledSidebarContent>
            </StyledSidebar>

            <StyledMain className="sw-col-span-8 sw-relative sw-ml-12">
              {hotspots.length === 0 || !selectedHotspot ? (
                <EmptyHotspotsPage
                  filterByFile={Boolean(filterByFile)}
                  filtered={
                    filters.assignedToMe ||
                    (isBranch(branchLike) && filters.inNewCodePeriod) ||
                    filters.status !== HotspotStatusFilter.TO_REVIEW
                  }
                  isStaticListOfHotspots={isStaticListOfHotspots}
                />
              ) : (
                <HotspotViewer
                  component={component}
                  hotspotKey={selectedHotspot.key}
                  hotspotsReviewedMeasure={hotspotsReviewedMeasure}
                  onLocationClick={props.onLocationClick}
                  onSwitchStatusFilter={props.onSwitchStatusFilter}
                  onUpdateHotspot={props.onUpdateHotspot}
                  selectedHotspotLocation={selectedHotspotLocation}
                  standards={standards}
                />
              )}
            </StyledMain>
          </div>
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    </>
  );
}

const StyledSidebar = withTheme(styled.section`
  box-sizing: border-box;

  background-color: ${themeColor('filterbar')};
  border-right: ${themeBorder('default', 'filterbarBorder')};
`);

const StyledSidebarContent = styled.div`
  position: sticky;
  overflow-x: hidden;
  box-sizing: border-box;
  width: 100%;
`;

const StyledSidebarHeader = withTheme(styled.div`
  position: sticky;
  box-sizing: border-box;
  background-color: inherit;
  border-bottom: ${themeBorder('default')};
  z-index: 1;
  height: ${STICKY_HEADER_HEIGHT}px;
  top: ${LAYOUT_GLOBAL_NAV_HEIGHT + LAYOUT_PROJECT_NAV_HEIGHT}px;
`);

const StyledMain = styled.main`
  flex-grow: 1;
  background-color: ${themeColor('backgroundSecondary')};
  border-left: ${themeBorder('default')};
  border-right: ${themeBorder('default')};
`;
