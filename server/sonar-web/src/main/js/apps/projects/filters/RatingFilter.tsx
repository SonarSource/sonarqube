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
import { RawQuery } from '~sonar-aligned/types/router';
import { Facets } from '../types';
import RatingFacet from './RatingFacet';

interface Props {
  facets?: Facets;
  headerDetail?: React.ReactNode;
  maxFacetValue?: number;
  onQueryChange: (change: RawQuery) => void;
  property: string;
  value?: any;
}

export default function RatingFilter({ facets, ...props }: Readonly<Props>) {
  return (
    <RatingFacet
      {...props}
      facet={getFacet(facets, props.property)}
      name={getFacetName(props.property)}
    />
  );
}

function getFacetName(property: string) {
  switch (property) {
    case 'new_security':
    case 'security':
      return 'Security';
    case 'new_maintainability':
    case 'maintainability':
      return 'Maintainability';
    case 'new_reliability':
    case 'reliability':
      return 'Reliability';
    case 'new_security_review':
    case 'security_review':
      return 'SecurityReview';
    default:
      return property;
  }
}

function getFacet(facets: Facets | undefined, name: string) {
  return facets && facets[name];
}
