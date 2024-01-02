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
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Level from '../../../components/ui/Level';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { RawQuery } from '../../../types/types';
import { Facet } from '../types';
import Filter from './Filter';
import FilterHeader from './FilterHeader';

export interface Props {
  facet?: Facet;
  maxFacetValue?: number;
  onQueryChange: (change: RawQuery) => void;
  value?: any;
}

function renderAccessibleLabel(option: string) {
  return translateWithParameters(
    'projects.facets.qualitygate_label_x',
    translate('metric.level', option)
  );
}

export default function QualityGateFilter(props: Props) {
  const hasWarnStatus = props.facet && props.facet['WARN'] !== undefined;
  const options = hasWarnStatus ? ['OK', 'WARN', 'ERROR'] : ['OK', 'ERROR'];

  return (
    <Filter
      facet={props.facet}
      header={<FilterHeader name={translate('projects.facets.quality_gate')} />}
      maxFacetValue={props.maxFacetValue}
      onQueryChange={props.onQueryChange}
      options={options}
      property="gate"
      renderOption={renderOption}
      renderAccessibleLabel={renderAccessibleLabel}
      value={props.value}
    />
  );
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
