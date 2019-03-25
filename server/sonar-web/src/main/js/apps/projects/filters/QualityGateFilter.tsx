/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Facet } from '../types';
import Level from '../../../components/ui/Level';
import { translate } from '../../../helpers/l10n';
import { RawQuery } from '../../../helpers/query';
import HelpTooltip from '../../../components/controls/HelpTooltip';

export interface Props {
  className?: string;
  facet?: Facet;
  maxFacetValue?: number;
  onQueryChange: (change: RawQuery) => void;
  organization?: { key: string };
  query: T.Dict<any>;
  value?: any;
}

export default function QualityGateFilter(props: Props) {
  return (
    <Filter
      facet={props.facet}
      getFacetValueForOption={getFacetValueForOption}
      header={<FilterHeader name={translate('projects.facets.quality_gate')} />}
      maxFacetValue={props.maxFacetValue}
      onQueryChange={props.onQueryChange}
      options={['OK', 'WARN', 'ERROR']}
      organization={props.organization}
      property="gate"
      query={props.query}
      renderOption={renderOption}
      value={props.value}
    />
  );
}

function getFacetValueForOption(facet: Facet, option: string) {
  return facet[option];
}

function renderOption(option: string, selected: boolean) {
  return (
    <>
      <Level level={option} muted={!selected} small={true} />
      {option === 'WARN' && (
        <HelpTooltip
          className="little-spacer-left"
          overlay={translate('projects.facets.quality_gate.warning_help')}
        />
      )}
    </>
  );
}
