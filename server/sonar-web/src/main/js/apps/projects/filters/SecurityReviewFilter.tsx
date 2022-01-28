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
import SecurityHotspotIcon from '../../../components/icons/SecurityHotspotIcon';
import Rating from '../../../components/ui/Rating';
import { translate } from '../../../helpers/l10n';
import { Dict, RawQuery } from '../../../types/types';
import { Facet } from '../types';
import Filter from './Filter';
import FilterHeader from './FilterHeader';

export interface Props {
  className?: string;
  facet?: Facet;
  maxFacetValue?: number;
  onQueryChange: (change: RawQuery) => void;
  property?: string;
  value?: any;
}

const labels: Dict<string> = {
  1: '≥ 80%',
  2: '70% - 80%',
  3: '50% - 70%',
  4: '30% - 50%',
  5: '< 30%'
};

export default function SecurityReviewFilter(props: Props) {
  const { property = 'security_review' } = props;

  return (
    <Filter
      className={props.className}
      facet={props.facet}
      header={
        <FilterHeader name={translate('metric_domain.SecurityReview')}>
          <span className="note little-spacer-left">
            {'( '}
            <SecurityHotspotIcon className="little-spacer-right" />
            {translate('metric.security_hotspots.name')}
            {' )'}
          </span>
        </FilterHeader>
      }
      highlightUnder={1}
      maxFacetValue={props.maxFacetValue}
      onQueryChange={props.onQueryChange}
      options={[1, 2, 3, 4, 5]}
      property={property}
      renderOption={renderOption}
      value={props.value}
    />
  );
}

function renderOption(option: number, selected: boolean) {
  return (
    <span>
      <Rating muted={!selected} small={true} value={option} />
      <span className="spacer-left">{labels[option]}</span>
    </span>
  );
}
