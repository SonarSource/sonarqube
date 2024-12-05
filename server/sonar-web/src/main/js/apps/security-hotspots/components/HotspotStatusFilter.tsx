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
import { DiscreetLink, ToggleButton, themeBorder } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { HotspotFilters, HotspotStatusFilter } from '../../../types/security-hotspots';

export interface FilterBarProps {
  filters: HotspotFilters;
  isStaticListOfHotspots: boolean;
  onChangeFilters: (filters: Partial<HotspotFilters>) => void;
  onShowAllHotspots: VoidFunction;
}

const statusOptions: Array<{ label: string; value: HotspotStatusFilter }> = [
  { value: HotspotStatusFilter.TO_REVIEW, label: translate('hotspot.filters.status.to_review') },
  {
    value: HotspotStatusFilter.ACKNOWLEDGED,
    label: translate('hotspot.filters.status.acknowledged'),
  },
  { value: HotspotStatusFilter.EXCEPTION, label: translate('hotspot.filters.status.exception') },
  { value: HotspotStatusFilter.FIXED, label: translate('hotspot.filters.status.fixed') },
  { value: HotspotStatusFilter.SAFE, label: translate('hotspot.filters.status.safe') },
];

export enum AssigneeFilterOption {
  ALL = 'all',
  ME = 'me',
}

export default function HotspotFilterByStatus(props: FilterBarProps) {
  const { filters, isStaticListOfHotspots } = props;

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
                  preventDefault
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
        <StyledFilterWrapper className="sw-flex sw-px-2 sw-pb-4 sw-gap-2 sw-justify-between">
          <ToggleButton
            aria-label={translate('hotspot.filters.status')}
            onChange={(status: HotspotStatusFilter) => props.onChangeFilters({ status })}
            options={statusOptions}
            value={statusOptions.find((status) => status.value === filters.status)?.value}
          />
        </StyledFilterWrapper>
      )}
    </div>
  );
}

const StyledFilterWrapper = withTheme(styled.div`
  border-bottom: ${themeBorder('default')};
`);
