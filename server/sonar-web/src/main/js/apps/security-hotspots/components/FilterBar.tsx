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
import * as React from 'react';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import RadioToggle from '../../../components/controls/RadioToggle';
import SelectLegacy from '../../../components/controls/SelectLegacy';
import { withCurrentUser } from '../../../components/hoc/withCurrentUser';
import Measure from '../../../components/measure/Measure';
import CoverageRating from '../../../components/ui/CoverageRating';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { isLoggedIn } from '../../../helpers/users';
import { ComponentQualifier } from '../../../types/component';
import { HotspotFilters, HotspotStatusFilter } from '../../../types/security-hotspots';
import { Component, CurrentUser } from '../../../types/types';

export interface FilterBarProps {
  currentUser: CurrentUser;
  component: Component;
  filters: HotspotFilters;
  hotspotsReviewedMeasure?: string;
  isStaticListOfHotspots: boolean;
  loadingMeasure: boolean;
  onBranch: boolean;
  onChangeFilters: (filters: Partial<HotspotFilters>) => void;
  onShowAllHotspots: () => void;
}

const statusOptions: Array<{ label: string; value: string }> = [
  { value: HotspotStatusFilter.TO_REVIEW, label: translate('hotspot.filters.status.to_review') },
  { value: HotspotStatusFilter.FIXED, label: translate('hotspot.filters.status.fixed') },
  { value: HotspotStatusFilter.SAFE, label: translate('hotspot.filters.status.safe') }
];

const periodOptions = [
  { value: true, label: translate('hotspot.filters.period.since_leak_period') },
  { value: false, label: translate('hotspot.filters.period.overall') }
];

export enum AssigneeFilterOption {
  ALL = 'all',
  ME = 'me'
}

const assigneeFilterOptions = [
  { value: AssigneeFilterOption.ME, label: translate('hotspot.filters.assignee.assigned_to_me') },
  { value: AssigneeFilterOption.ALL, label: translate('hotspot.filters.assignee.all') }
];

export function FilterBar(props: FilterBarProps) {
  const {
    currentUser,
    component,
    filters,
    hotspotsReviewedMeasure,
    isStaticListOfHotspots,
    loadingMeasure,
    onBranch
  } = props;
  const isProject = component.qualifier === ComponentQualifier.Project;

  return (
    <div className="filter-bar-outer">
      <div className="filter-bar">
        <div className="filter-bar-inner display-flex-center">
          {isStaticListOfHotspots ? (
            <a
              id="show_all_hotspot"
              onClick={() => props.onShowAllHotspots()}
              role="link"
              tabIndex={0}>
              {translate('hotspot.filters.show_all')}
            </a>
          ) : (
            <div className="display-flex-space-between width-100">
              <div className="display-flex-center">
                <h3 className="huge-spacer-right">{translate('hotspot.filters.title')}</h3>

                {isLoggedIn(currentUser) && (
                  <RadioToggle
                    className="huge-spacer-right"
                    name="assignee-filter"
                    onCheck={(value: AssigneeFilterOption) =>
                      props.onChangeFilters({ assignedToMe: value === AssigneeFilterOption.ME })
                    }
                    options={assigneeFilterOptions}
                    value={
                      filters.assignedToMe ? AssigneeFilterOption.ME : AssigneeFilterOption.ALL
                    }
                  />
                )}

                <span className="spacer-right">{translate('status')}</span>
                <SelectLegacy
                  className="input-medium big-spacer-right"
                  clearable={false}
                  onChange={(option: { value: HotspotStatusFilter }) =>
                    props.onChangeFilters({ status: option.value })
                  }
                  options={statusOptions}
                  searchable={false}
                  value={filters.status}
                />

                {onBranch && (
                  <SelectLegacy
                    className="input-medium big-spacer-right"
                    clearable={false}
                    onChange={(option: { value: boolean }) =>
                      props.onChangeFilters({ sinceLeakPeriod: option.value })
                    }
                    options={periodOptions}
                    searchable={false}
                    value={filters.sinceLeakPeriod}
                  />
                )}
              </div>

              {isProject && (
                <div className="display-flex-center">
                  <span className="little-spacer-right">
                    {translate('metric.security_hotspots_reviewed.name')}
                  </span>
                  <HelpTooltip
                    className="big-spacer-right"
                    overlay={translate('hotspots.reviewed.tooltip')}
                  />
                  <DeferredSpinner loading={loadingMeasure}>
                    {hotspotsReviewedMeasure && <CoverageRating value={hotspotsReviewedMeasure} />}
                    <Measure
                      className="spacer-left huge it__hs-review-percentage"
                      metricKey={
                        onBranch && !filters.sinceLeakPeriod
                          ? 'security_hotspots_reviewed'
                          : 'new_security_hotspots_reviewed'
                      }
                      metricType="PERCENT"
                      value={hotspotsReviewedMeasure}
                    />
                  </DeferredSpinner>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default withCurrentUser(FilterBar);
