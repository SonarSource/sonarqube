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
import DuplicationsRating from '../../../components/ui/DuplicationsRating';
import {
  getDuplicationsRatingLabel,
  getDuplicationsRatingAverageValue
} from '../../../helpers/ratings';
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

export default function DuplicationsFilter(props: Props) {
  const { property = 'duplications' } = props;
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
      header={<FilterHeader name={translate('metric_domain.Duplications')} />}
    />
  );
}

function getFacetValueForOption(facet: Facet, option: number) {
  const map = ['*-3.0', '3.0-5.0', '5.0-10.0', '10.0-20.0', '20.0-*', 'NO_DATA'];
  return facet[map[option - 1]];
}

function renderOption(option: number, selected: boolean) {
  return (
    <span>
      {option < 6 && (
        <DuplicationsRating
          value={getDuplicationsRatingAverageValue(option)}
          size="small"
          muted={!selected}
        />
      )}
      <span className="spacer-left">
        {option < 6 ? (
          getDuplicationsRatingLabel(option)
        ) : (
          <span className="big-spacer-left">{translate('no_data')}</span>
        )}
      </span>
    </span>
  );
}
