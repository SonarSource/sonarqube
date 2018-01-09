/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import Filter from './Filter';
import FilterHeader from './FilterHeader';
import CoverageRating from '../../../components/ui/CoverageRating';
import { getCoverageRatingLabel, getCoverageRatingAverageValue } from '../../../helpers/ratings';
import { translate } from '../../../helpers/l10n';
import { Facet } from '../types';
import { RawQuery } from '../../../helpers/query';

export interface Props {
  className?: string;
  facet?: Facet;
  maxFacetValue?: number;
  onQueryChange: (change: RawQuery) => void;
  organization?: { key: string };
  property?: string;
  query: { [x: string]: any };
  value?: any;
}

export default function CoverageFilter(props: Props) {
  const { property = 'coverage' } = props;

  return (
    <Filter
      facet={props.facet}
      maxFacetValue={props.maxFacetValue}
      onQueryChange={props.onQueryChange}
      value={props.value}
      property={property}
      className={props.className}
      options={[1, 2, 3, 4, 5, 6]}
      query={props.query}
      renderOption={renderOption}
      organization={props.organization}
      getFacetValueForOption={getFacetValueForOption}
      highlightUnder={1}
      highlightUnderMax={5}
      header={<FilterHeader name={translate('metric_domain.Coverage')} />}
    />
  );
}

function getFacetValueForOption(facet: Facet, option: number): number {
  const map = ['80.0-*', '70.0-80.0', '50.0-70.0', '30.0-50.0', '*-30.0', 'NO_DATA'];
  return facet[map[option - 1]];
}

function renderOption(option: number, selected: boolean) {
  return (
    <span>
      {option < 6 && (
        <CoverageRating
          value={getCoverageRatingAverageValue(option)}
          size="small"
          muted={!selected}
        />
      )}
      <span className="spacer-left">
        {option < 6 ? (
          getCoverageRatingLabel(option)
        ) : (
          <span className="big-spacer-left">{translate('no_data')}</span>
        )}
      </span>
    </span>
  );
}
