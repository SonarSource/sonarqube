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
import { translate } from 'sonar-ui-common/helpers/l10n';
import CoverageRating from '../../../components/ui/CoverageRating';
import { Facet } from '../types';
import Filter from './Filter';
import FilterHeader from './FilterHeader';

export interface Props {
  className?: string;
  facet?: Facet;
  maxFacetValue?: number;
  onQueryChange: (change: T.RawQuery) => void;
  organization?: { key: string };
  property?: string;
  query: T.Dict<any>;
  value?: any;
}

const NO_DATA = 'NO_DATA';
const slices: { [key: string]: { label: string; average: number } } = {
  '80.0-*': { label: '≥ 80%', average: 90 },
  '70.0-80.0': { label: '70% - 80%', average: 75 },
  '50.0-70.0': { label: '50% - 70%', average: 60 },
  '30.0-50.0': { label: '30% - 50%', average: 40 },
  '*-30.0': { label: '< 30%', average: 15 },
  [NO_DATA]: { label: '', average: NaN }
};

export default function CoverageFilter(props: Props) {
  const { property = 'coverage' } = props;

  return (
    <Filter
      className={props.className}
      facet={props.facet}
      header={<FilterHeader name={translate('metric_domain.Coverage')} />}
      highlightUnder={1}
      highlightUnderMax={5}
      maxFacetValue={props.maxFacetValue}
      onQueryChange={props.onQueryChange}
      options={Object.keys(slices)}
      organization={props.organization}
      property={property}
      query={props.query}
      renderOption={renderOption}
      value={props.value}
    />
  );
}

function renderOption(option: string, selected: boolean) {
  return (
    <span>
      {option !== NO_DATA && (
        <CoverageRating muted={!selected} size="small" value={slices[option].average} />
      )}
      <span className="spacer-left">
        {option !== NO_DATA ? (
          slices[option].label
        ) : (
          <span className="big-spacer-left">{translate('no_data')}</span>
        )}
      </span>
    </span>
  );
}
