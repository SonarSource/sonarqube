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
import FacetBox from '../../../components/facet/FacetBox';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import { translate } from '../../../helpers/l10n';

interface Props {
  onChange: (metric: string) => void;
  selected: string;
  value: string;
}

export default function ProjectOverviewFacet({ value, selected, onChange }: Props) {
  const facetName = translate('component_measures.overview', value, 'facet');
  return (
    <FacetBox property={value}>
      <FacetItemsList>
        <FacetItem
          active={value === selected}
          disabled={false}
          key={value}
          name={<strong id={`measure-overview-${value}-name`}>{facetName}</strong>}
          onClick={onChange}
          tooltip={facetName}
          value={value}
        />
      </FacetItemsList>
    </FacetBox>
  );
}
