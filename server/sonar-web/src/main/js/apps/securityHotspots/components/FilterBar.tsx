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
import Select from 'sonar-ui-common/components/controls/Select';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { HotspotStatusFilters } from '../../../types/security-hotspots';

export interface FilterBarProps {
  onChangeStatus: (status: HotspotStatusFilters) => void;
  statusFilter: HotspotStatusFilters;
}

const statusOptions: Array<{ label: string; value: string }> = [
  { label: translate('hotspot.filters.status.to_review'), value: HotspotStatusFilters.TO_REVIEW },
  { label: translate('hotspot.filters.status.fixed'), value: HotspotStatusFilters.FIXED },
  { label: translate('hotspot.filters.status.safe'), value: HotspotStatusFilters.SAFE }
];

export default function FilterBar(props: FilterBarProps) {
  const { statusFilter } = props;
  return (
    <div className="filter-bar display-flex-center">
      <h3 className="big-spacer-right">{translate('hotspot.filters.title')}</h3>

      <span className="spacer-right">{translate('status')}</span>
      <Select
        className="input-medium big-spacer-right"
        clearable={false}
        onChange={(option: { value: HotspotStatusFilters }) => props.onChangeStatus(option.value)}
        options={statusOptions}
        searchable={false}
        value={statusFilter}
      />
    </div>
  );
}
