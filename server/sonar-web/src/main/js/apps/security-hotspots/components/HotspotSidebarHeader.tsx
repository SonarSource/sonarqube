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
import {
  CoverageIndicator,
  DiscreetInteractiveIcon,
  Dropdown,
  FilterIcon,
  HelperHintIcon,
  ItemCheckbox,
  ItemDangerButton,
  ItemDivider,
  ItemHeader,
} from 'design-system';
import * as React from 'react';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Measure from '../../../components/measure/Measure';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { PopupPlacement } from '../../../components/ui/popups';
import { isBranch } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { MetricKey, MetricType } from '../../../types/metrics';
import { HotspotFilters } from '../../../types/security-hotspots';
import { CurrentUser, isLoggedIn } from '../../../types/users';

export interface SecurityHotspotsAppRendererProps {
  branchLike?: BranchLike;
  filters: HotspotFilters;
  hotspotsReviewedMeasure?: string;
  loadingMeasure: boolean;
  onChangeFilters: (filters: Partial<HotspotFilters>) => void;
  currentUser: CurrentUser;
  isStaticListOfHotspots: boolean;
}

function HotspotSidebarHeader(props: SecurityHotspotsAppRendererProps) {
  const {
    branchLike,
    filters,
    hotspotsReviewedMeasure,
    loadingMeasure,
    currentUser,
    isStaticListOfHotspots,
  } = props;

  const userLoggedIn = isLoggedIn(currentUser);
  const filtersCount =
    Number(filters.assignedToMe) + Number(isBranch(branchLike) && filters.inNewCodePeriod);
  const isFiltered = Boolean(filtersCount);

  return (
    <div className="sw-flex sw-py-4 sw-items-center sw-h-6 sw-px-4">
      <DeferredSpinner loading={loadingMeasure}>
        {hotspotsReviewedMeasure !== undefined && (
          <CoverageIndicator value={hotspotsReviewedMeasure} />
        )}
        <Measure
          className="sw-ml-2 sw-body-sm-highlight it__hs-review-percentage"
          metricKey={
            isBranch(branchLike) && !filters.inNewCodePeriod
              ? MetricKey.security_hotspots_reviewed
              : MetricKey.new_security_hotspots_reviewed
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

        {!isStaticListOfHotspots && (isBranch(branchLike) || userLoggedIn || isFiltered) && (
          <div className="sw-flex-grow sw-flex sw-justify-end">
            <Dropdown
              allowResizing
              closeOnClick={false}
              id="filter-hotspots-menu"
              overlay={
                <>
                  <ItemHeader>{translate('hotspot.filters.title')}</ItemHeader>

                  {isBranch(branchLike) && (
                    <ItemCheckbox
                      checked={Boolean(filters.inNewCodePeriod)}
                      onCheck={(inNewCodePeriod: boolean) =>
                        props.onChangeFilters({ inNewCodePeriod })
                      }
                    >
                      <span className="sw-mx-2">
                        {translate('hotspot.filters.period.since_leak_period')}
                      </span>
                    </ItemCheckbox>
                  )}

                  {userLoggedIn && (
                    <ItemCheckbox
                      checked={Boolean(filters.assignedToMe)}
                      onCheck={(assignedToMe: boolean) => props.onChangeFilters({ assignedToMe })}
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
              isPortal
            >
              <DiscreetInteractiveIcon
                Icon={FilterIcon}
                aria-label={translate('hotspot.filters.title')}
              >
                {isFiltered ? filtersCount : null}
              </DiscreetInteractiveIcon>
            </Dropdown>
          </div>
        )}
      </DeferredSpinner>
    </div>
  );
}

export default withCurrentUserContext(HotspotSidebarHeader);
