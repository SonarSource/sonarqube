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
import { flatMap } from 'lodash';
import FavoriteFilterContainer from './FavoriteFilterContainer';
import ClearAll from './ClearAll';
import LanguagesFilterContainer from '../filters/LanguagesFilterContainer';
import CoverageFilter from '../filters/CoverageFilter';
import DuplicationsFilter from '../filters/DuplicationsFilter';
import MaintainabilityFilter from '../filters/MaintainabilityFilter';
import NewCoverageFilter from '../filters/NewCoverageFilter';
import NewDuplicationsFilter from '../filters/NewDuplicationsFilter';
import NewMaintainabilityFilter from '../filters/NewMaintainabilityFilter';
import NewReliabilityFilter from '../filters/NewReliabilityFilter';
import NewSecurityFilter from '../filters/NewSecurityFilter';
import NewLinesFilter from '../filters/NewLinesFilter';
import QualityGateFilter from '../filters/QualityGateFilter';
import ReliabilityFilter from '../filters/ReliabilityFilter';
import SecurityFilter from '../filters/SecurityFilter';
import SizeFilter from '../filters/SizeFilter';
import TagsFilter from '../filters/TagsFilter';
import { translate } from '../../../helpers/l10n';
import { RawQuery } from '../../../helpers/query';
import { Facets } from '../types';

interface Props {
  facets?: Facets;
  onClearAll: () => void;
  onQueryChange: (change: RawQuery) => void;
  organization?: { key: string };
  query: RawQuery;
  showFavoriteFilter: boolean;
  view: string;
  visualization: string;
}

export default function PageSidebar(props: Props) {
  const { facets, onQueryChange, query, organization, view, visualization } = props;
  const isFiltered = Object.keys(query)
    .filter(key => !['view', 'visualization', 'sort'].includes(key))
    .some(key => query[key] != null);
  const isLeakView = view === 'leak';
  const maxFacetValue = getMaxFacetValue(facets);
  const facetProps = { onQueryChange, maxFacetValue, organization, query };

  let linkQuery: RawQuery | undefined = undefined;
  if (view !== 'overall') {
    linkQuery = { view };

    if (view === 'visualizations') {
      linkQuery.visualization = visualization;
    }
  }

  return (
    <div>
      {props.showFavoriteFilter && (
        <FavoriteFilterContainer query={linkQuery} organization={organization} />
      )}

      <div className="projects-facets-header clearfix">
        {isFiltered && <ClearAll onClearAll={props.onClearAll} />}

        <h3>{translate('filters')}</h3>
      </div>
      <QualityGateFilter {...facetProps} facet={facets && facets.gate} value={query.gate} />
      {!isLeakView && (
        <>
          <ReliabilityFilter
            {...facetProps}
            facet={facets && facets.reliability}
            value={query.reliability}
          />
          <SecurityFilter
            {...facetProps}
            facet={facets && facets.security}
            value={query.security}
          />
          <MaintainabilityFilter
            {...facetProps}
            facet={facets && facets.maintainability}
            value={query.maintainability}
          />
          <CoverageFilter
            {...facetProps}
            facet={facets && facets.coverage}
            value={query.coverage}
          />
          <DuplicationsFilter
            {...facetProps}
            facet={facets && facets.duplications}
            value={query.duplications}
          />
          <SizeFilter {...facetProps} facet={facets && facets.size} value={query.size} />
        </>
      )}
      {isLeakView && (
        <>
          <NewReliabilityFilter
            {...facetProps}
            facet={facets && facets.new_reliability}
            value={query.new_reliability}
          />
          <NewSecurityFilter
            {...facetProps}
            facet={facets && facets.new_security}
            value={query.new_security}
          />
          <NewMaintainabilityFilter
            {...facetProps}
            facet={facets && facets.new_maintainability}
            value={query.new_maintainability}
          />
          <NewCoverageFilter
            {...facetProps}
            facet={facets && facets.new_coverage}
            value={query.new_coverage}
          />
          <NewDuplicationsFilter
            {...facetProps}
            facet={facets && facets.new_duplications}
            value={query.new_duplications}
          />
          <NewLinesFilter
            {...facetProps}
            facet={facets && facets.new_lines}
            value={query.new_lines}
          />
        </>
      )}
      <LanguagesFilterContainer
        {...facetProps}
        facet={facets && facets.languages}
        value={query.languages}
      />
      <TagsFilter {...facetProps} facet={facets && facets.tags} value={query.tags} />
    </div>
  );
}

function getMaxFacetValue(facets?: Facets) {
  return facets && Math.max(...flatMap(Object.values(facets), facet => Object.values(facet)));
}
