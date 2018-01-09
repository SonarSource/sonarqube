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
import Rating from '../../../components/ui/Rating';
import { translate } from '../../../helpers/l10n';
import { Facet } from '../types';
import { RawQuery } from '../../../helpers/query';

interface Props {
  className?: string;
  facet?: Facet;
  headerDetail?: React.ReactNode;
  maxFacetValue?: number;
  name: string;
  onQueryChange: (change: RawQuery) => void;
  organization?: { key: string };
  property: string;
  query: { [x: string]: any };
  value?: any;
}

export default function IssuesFilter(props: Props) {
  return (
    <Filter
      facet={props.facet}
      maxFacetValue={props.maxFacetValue}
      onQueryChange={props.onQueryChange}
      value={props.value}
      property={props.property}
      className={props.className}
      options={[1, 2, 3, 4, 5]}
      query={props.query}
      renderOption={renderOption}
      organization={props.organization}
      getFacetValueForOption={getFacetValueForOption}
      highlightUnder={1}
      header={
        <FilterHeader name={translate('metric_domain', props.name)}>
          {props.headerDetail}
        </FilterHeader>
      }
    />
  );
}

function getFacetValueForOption(facet: Facet, option: number) {
  return facet[option];
}

function renderOption(option: number, selected: boolean) {
  return (
    <span>
      <Rating value={option} small={true} muted={!selected} />
      {option > 1 &&
        option < 5 && <span className="note spacer-left">{translate('and_worse')}</span>}
    </span>
  );
}
