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
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { ComponentQualifier } from '../../../types/component';
import { RawQuery } from '../../../types/types';
import { Facet } from '../types';
import Filter from './Filter';
import FilterHeader from './FilterHeader';

export interface QualifierFilterProps {
  facet?: Facet;
  maxFacetValue?: number;
  onQueryChange: (change: RawQuery) => void;
  value: ComponentQualifier | undefined;
}

const options = [ComponentQualifier.Project, ComponentQualifier.Application];

export default function QualifierFilter(props: QualifierFilterProps) {
  const { facet, maxFacetValue, value } = props;

  return (
    <Filter
      facet={facet}
      header={<FilterHeader name={translate('projects.facets.qualifier')} />}
      maxFacetValue={maxFacetValue}
      onQueryChange={props.onQueryChange}
      options={options}
      property="qualifier"
      renderAccessibleLabel={renderAccessibleLabel}
      renderOption={renderOption}
      value={value}
    />
  );
}

function renderAccessibleLabel(option: string) {
  return translateWithParameters(
    'projects.facets.label_text_x',
    translate('projects.facets.qualifier'),
    translate('qualifier', option)
  );
}

function renderOption(option: string, selected: boolean) {
  return (
    <span className="display-flex-center">
      <QualifierIcon
        className="spacer-right"
        fill={selected ? undefined : 'currentColor'}
        qualifier={option}
      />
      {translate('qualifier', option)}
    </span>
  );
}
