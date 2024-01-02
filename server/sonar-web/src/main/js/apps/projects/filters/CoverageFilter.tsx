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
import CoverageRating from '../../../components/ui/CoverageRating';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getCoverageRatingAverageValue, getCoverageRatingLabel } from '../../../helpers/ratings';
import { RawQuery } from '../../../types/types';
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

const NO_DATA_OPTION = 6;

export default function CoverageFilter(props: Props) {
  const { property = 'coverage' } = props;

  return (
    <Filter
      className={props.className}
      facet={props.facet}
      getFacetValueForOption={getFacetValueForOption}
      header={<FilterHeader name={translate('metric_domain.Coverage')} />}
      highlightUnder={1}
      highlightUnderMax={5}
      maxFacetValue={props.maxFacetValue}
      onQueryChange={props.onQueryChange}
      options={[1, 2, 3, 4, 5, 6]}
      property={property}
      renderAccessibleLabel={renderAccessibleLabel}
      renderOption={renderOption}
      value={props.value}
    />
  );
}

function getFacetValueForOption(facet: Facet, option: number): number {
  const map = ['80.0-*', '70.0-80.0', '50.0-70.0', '30.0-50.0', '*-30.0', 'NO_DATA'];
  return facet[map[option - 1]];
}

function renderAccessibleLabel(option: number) {
  return option < NO_DATA_OPTION
    ? translate('projects.facets.coverage.label', option.toString())
    : translateWithParameters(
        'projects.facets.label_no_data_x',
        translate('metric_domain.Coverage')
      );
}

function renderOption(option: number, selected: boolean) {
  return (
    <div className="display-flex-center">
      {option < NO_DATA_OPTION && (
        <CoverageRating muted={!selected} value={getCoverageRatingAverageValue(option)} />
      )}
      <span className="spacer-left">
        {option < NO_DATA_OPTION ? (
          getCoverageRatingLabel(option)
        ) : (
          <span className="big-spacer-left">{translate('no_data')}</span>
        )}
      </span>
    </div>
  );
}
