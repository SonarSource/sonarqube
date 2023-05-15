/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
  CoverageIndicator,
  DiscreetInteractiveIcon,
  DiscreetLink,
  Dropdown,
  FilterIcon,
  HelperHintIcon,
  ItemCheckbox,
  ItemDangerButton,
  ItemDivider,
  ItemHeader,
  PopupPlacement,
  ToggleButton,
  themeBorder,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Measure from '../../../components/measure/Measure';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { ComponentQualifier } from '../../../types/component';
import { MetricType } from '../../../types/metrics';
import { HotspotFilters, HotspotStatusFilter } from '../../../types/security-hotspots';
import { Component } from '../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../types/users';

export interface FilterBarProps {
  currentUser: CurrentUser;
  component: Component;
  filters: HotspotFilters;
  hotspotsReviewedMeasure?: string;
  isStaticListOfHotspots: boolean;
  loadingMeasure: boolean;
  onBranch: boolean;
  onChangeFilters: (filters: Partial<HotspotFilters>) => void;
  onShowAllHotspots: VoidFunction;
}

const statusOptions: Array<{ label: string; value: HotspotStatusFilter }> = [
  { value: HotspotStatusFilter.TO_REVIEW, label: translate('hotspot.filters.status.to_review') },
  {
    value: HotspotStatusFilter.ACKNOWLEDGED,
    label: translate('hotspot.filters.status.acknowledged'),
  },
  { value: HotspotStatusFilter.FIXED, label: translate('hotspot.filters.status.fixed') },
  { value: HotspotStatusFilter.SAFE, label: translate('hotspot.filters.status.safe') },
];

export enum AssigneeFilterOption {
  ALL = 'all',
  ME = 'me',
}

export function FilterBar(props: FilterBarProps) {
  const {
    currentUser,
    component,
    filters,
    hotspotsReviewedMeasure,
    loadingMeasure,
    onBranch,
    isStaticListOfHotspots,
  } = props;
  const isProject = component.qualifier === ComponentQualifier.Project;
  const userLoggedIn = isLoggedIn(currentUser);
  const filtersCount = Number(filters.assignedToMe) + Number(filters.inNewCodePeriod);
  const isFiltered = Boolean(filtersCount);

  return (
    <div className="sw-flex sw-flex-col sw-justify-between sw-pb-4 sw-mb-3">
      {isStaticListOfHotspots ? (
        <StyledFilterWrapper className="sw-flex sw-px-2 sw-py-4">
          <FormattedMessage
            id="hotspot.filters.by_file_or_list_x"
            values={{
              show_all_link: (
                <DiscreetLink
                  className="sw-ml-1"
                  onClick={props.onShowAllHotspots}
                  preventDefault={true}
                  to={{}}
                >
                  {translate('hotspot.filters.show_all')}
                </DiscreetLink>
              ),
            }}
            defaultMessage={translate('hotspot.filters.by_file_or_list_x')}
          />
        </StyledFilterWrapper>
      ) : (
        <>
          {isProject && (
            <StyledFilterWrapper className="sw-flex sw-px-2 sw-py-4 sw-items-center sw-h-6">
              <DeferredSpinner loading={loadingMeasure}>
                {hotspotsReviewedMeasure !== undefined && (
                  <CoverageIndicator value={hotspotsReviewedMeasure} />
                )}
                <Measure
                  className="sw-ml-2 sw-body-sm-highlight"
                  metricKey={
                    onBranch && !filters.inNewCodePeriod
                      ? 'security_hotspots_reviewed'
                      : 'new_security_hotspots_reviewed'
                  }
                  metricType={MetricType.Percent}
                  value={hotspotsReviewedMeasure}
                />
                <span className="sw-ml-1 sw-body-sm">
                  {translate('metric.security_hotspots_reviewed.name')}
                </span>
                <HelpTooltip className="sw-ml-1" overlay={translate('hotspots.reviewed.tooltip')}>
                  <HelperHintIcon aria-label="help-tooltip" />
                </HelpTooltip>
              </DeferredSpinner>
            </StyledFilterWrapper>
          )}

          <StyledFilterWrapper className="sw-flex sw-px-2 sw-py-4 sw-gap-2 sw-justify-between">
            <ToggleButton
              aria-label={translate('hotspot.filters.status')}
              onChange={(status: HotspotStatusFilter) => props.onChangeFilters({ status })}
              options={statusOptions}
              value={statusOptions.find((status) => status.value === filters.status)?.value}
            />
            {(onBranch || userLoggedIn || isFiltered) && (
              <Dropdown
                allowResizing={true}
                closeOnClick={false}
                id="filter-hotspots-menu"
                overlay={
                  <>
                    <ItemHeader>{translate('hotspot.filters.title')}</ItemHeader>

                    {onBranch && (
                      <ItemCheckbox
                        checked={Boolean(filters.inNewCodePeriod)}
                        onCheck={(inNewCodePeriod) => props.onChangeFilters({ inNewCodePeriod })}
                      >
                        <span className="sw-mx-2">
                          {translate('hotspot.filters.period.since_leak_period')}
                        </span>
                      </ItemCheckbox>
                    )}

                    {userLoggedIn && (
                      <ItemCheckbox
                        checked={Boolean(filters.assignedToMe)}
                        onCheck={(assignedToMe) => props.onChangeFilters({ assignedToMe })}
                      >
                        <span className="sw-mx-2">
                          {translate('hotspot.filters.assignee.assigned_to_me')}
                        </span>
                      </ItemCheckbox>
                    )}

                    {isFiltered && <ItemDivider />}

                    {isFiltered && (
                      <ItemDangerButton
                        onClick={() =>
                          props.onChangeFilters({
                            assignedToMe: false,
                            inNewCodePeriod: false,
                          })
                        }
                      >
                        {translate('hotspot.filters.clear')}
                      </ItemDangerButton>
                    )}
                  </>
                }
                placement={PopupPlacement.BottomRight}
              >
                <DiscreetInteractiveIcon
                  Icon={FilterIcon}
                  aria-label={translate('hotspot.filters.title')}
                >
                  {isFiltered ? filtersCount : null}
                </DiscreetInteractiveIcon>
              </Dropdown>
            )}
          </StyledFilterWrapper>
        </>
      )}
    </div>
  );
}

const StyledFilterWrapper = withTheme(styled.div`
  border-bottom: ${themeBorder('default')};
`);

export default withCurrentUserContext(FilterBar);
