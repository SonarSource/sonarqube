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
import { Helmet } from 'react-helmet-async';
import { Link } from 'react-router';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import A11ySkipTarget from '../../app/components/a11y/A11ySkipTarget';
import Suggestions from '../../app/components/embed-docs-modal/Suggestions';
import ScreenPositionHelper from '../../components/common/ScreenPositionHelper';
import { BranchLike } from '../../types/branch-like';
import { HotspotFilters, HotspotUpdate, RawHotspot } from '../../types/security-hotspots';
import FilterBar from './components/FilterBar';
import HotspotList from './components/HotspotList';
import HotspotViewer from './components/HotspotViewer';
import './styles.css';

export interface SecurityHotspotsAppRendererProps {
  branchLike?: BranchLike;
  filters: HotspotFilters;
  hotspots: RawHotspot[];
  loading: boolean;
  onChangeFilters: (filters: Partial<HotspotFilters>) => void;
  onHotspotClick: (key: string) => void;
  onUpdateHotspot: (hotspot: HotspotUpdate) => void;
  selectedHotspotKey?: string;
  securityCategories: T.StandardSecurityCategories;
}

export default function SecurityHotspotsAppRenderer(props: SecurityHotspotsAppRendererProps) {
  const { branchLike, hotspots, loading, securityCategories, selectedHotspotKey, filters } = props;

  return (
    <div id="security_hotspots">
      <FilterBar onChangeFilters={props.onChangeFilters} filters={filters} />
      <ScreenPositionHelper>
        {({ top }) => (
          <div className="wrapper" style={{ top }}>
            <Suggestions suggestions="security_hotspots" />
            <Helmet title={translate('hotspots.page')} />

            <A11ySkipTarget anchor="security_hotspots_main" />

            <DeferredSpinner className="huge-spacer-left big-spacer-top" loading={loading}>
              {hotspots.length === 0 ? (
                <div className="display-flex-column display-flex-center">
                  <img
                    alt={translate('hotspots.page')}
                    className="huge-spacer-top"
                    height={166}
                    src={`${getBaseUrl()}/images/hotspot-large.svg`}
                  />
                  <h1 className="huge-spacer-top">{translate('hotspots.no_hotspots.title')}</h1>
                  <div className="abs-width-400 text-center big-spacer-top">
                    {translate('hotspots.no_hotspots.description')}
                  </div>
                  <Link
                    className="big-spacer-top"
                    target="_blank"
                    to={{ pathname: '/documentation/user-guide/security-hotspots/' }}>
                    {translate('hotspots.learn_more')}
                  </Link>
                </div>
              ) : (
                <div className="layout-page">
                  <div className="sidebar">
                    <HotspotList
                      hotspots={hotspots}
                      onHotspotClick={props.onHotspotClick}
                      securityCategories={securityCategories}
                      selectedHotspotKey={selectedHotspotKey}
                      statusFilter={filters.status}
                    />
                  </div>
                  <div className="main">
                    {selectedHotspotKey && (
                      <HotspotViewer
                        branchLike={branchLike}
                        hotspotKey={selectedHotspotKey}
                        onUpdateHotspot={props.onUpdateHotspot}
                        securityCategories={securityCategories}
                      />
                    )}
                  </div>
                </div>
              )}
            </DeferredSpinner>
          </div>
        )}
      </ScreenPositionHelper>
    </div>
  );
}
