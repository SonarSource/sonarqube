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
import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { BasicSeparator, StyledPageTitle } from 'design-system';
import { flatMap } from 'lodash';
import * as React from 'react';
import { RawQuery } from '~sonar-aligned/types/router';
import { translate } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import CoverageFilter from '../filters/CoverageFilter';
import DuplicationsFilter from '../filters/DuplicationsFilter';
import LanguagesFilter from '../filters/LanguagesFilter';
import MaintainabilityFilter from '../filters/MaintainabilityFilter';
import NewCoverageFilter from '../filters/NewCoverageFilter';
import NewDuplicationsFilter from '../filters/NewDuplicationsFilter';
import NewLinesFilter from '../filters/NewLinesFilter';
import NewMaintainabilityFilter from '../filters/NewMaintainabilityFilter';
import NewReliabilityFilter from '../filters/NewReliabilityFilter';
import NewSecurityFilter from '../filters/NewSecurityFilter';
import QualifierFacet from '../filters/QualifierFilter';
import QualityGateFacet from '../filters/QualityGateFilter';
import ReliabilityFilter from '../filters/ReliabilityFilter';
import SecurityFilter from '../filters/SecurityFilter';
import SecurityReviewFilter from '../filters/SecurityReviewFilter';
import SizeFilter from '../filters/SizeFilter';
import TagsFacet from '../filters/TagsFilter';
import { hasFilterParams } from '../query';
import { Facets } from '../types';
import FavoriteFilter from './FavoriteFilter';

export interface PageSidebarProps {
  applicationsEnabled: boolean;
  facets?: Facets;
  loadSearchResultCount: (property: string, values: string[]) => Promise<Dict<number>>;
  onClearAll: () => void;
  onQueryChange: (change: RawQuery) => void;
  query: RawQuery;
  view: string;
}

export default function PageSidebar(props: PageSidebarProps) {
  const {
    applicationsEnabled,
    facets,
    loadSearchResultCount,
    onClearAll,
    onQueryChange,
    query,
    view,
  } = props;
  const isFiltered = hasFilterParams(query);
  const isLeakView = view === 'leak';
  const maxFacetValue = getMaxFacetValue(facets);
  const facetProps = { onQueryChange, maxFacetValue };

  const heading = React.useRef<HTMLHeadingElement>(null);

  const clearAll = React.useCallback(() => {
    onClearAll();
    if (heading.current) {
      heading.current.focus();
    }
  }, [onClearAll, heading]);

  return (
    <div className="sw-body-sm sw-px-4 sw-pt-12 sw-pb-24">
      <FavoriteFilter />

      <div className="sw-flex sw-items-center sw-justify-between">
        <StyledPageTitle className="sw-body-md-highlight" as="h2" tabIndex={-1} ref={heading}>
          {translate('filters')}
        </StyledPageTitle>

        {isFiltered && (
          <Button onClick={clearAll} variety={ButtonVariety.DangerOutline}>
            {translate('clear_all_filters')}
          </Button>
        )}
      </div>

      <BasicSeparator className="sw-my-4" />

      <QualityGateFacet
        {...facetProps}
        facet={getFacet(facets, 'gate')}
        value={query.gate?.split(',')}
      />

      <BasicSeparator className="sw-my-4" />

      {!isLeakView && (
        <>
          <ReliabilityFilter
            {...facetProps}
            facet={getFacet(facets, 'reliability')}
            value={query.reliability}
          />

          <BasicSeparator className="sw-my-4" />

          <SecurityFilter
            {...facetProps}
            facet={getFacet(facets, 'security')}
            value={query.security}
          />

          <BasicSeparator className="sw-my-4" />

          <SecurityReviewFilter
            {...facetProps}
            facet={getFacet(facets, 'security_review')}
            value={query.security_review_rating}
          />

          <BasicSeparator className="sw-my-4" />

          <MaintainabilityFilter
            {...facetProps}
            facet={getFacet(facets, 'maintainability')}
            value={query.maintainability}
          />

          <BasicSeparator className="sw-my-4" />

          <CoverageFilter
            {...facetProps}
            facet={getFacet(facets, 'coverage')}
            value={query.coverage}
          />

          <BasicSeparator className="sw-my-4" />

          <DuplicationsFilter
            {...facetProps}
            facet={getFacet(facets, 'duplications')}
            value={query.duplications}
          />

          <BasicSeparator className="sw-my-4" />

          <SizeFilter {...facetProps} facet={getFacet(facets, 'size')} value={query.size} />
        </>
      )}
      {isLeakView && (
        <>
          <NewReliabilityFilter
            {...facetProps}
            facet={getFacet(facets, 'new_reliability')}
            value={query.new_reliability}
          />

          <BasicSeparator className="sw-my-4" />

          <NewSecurityFilter
            {...facetProps}
            facet={getFacet(facets, 'new_security')}
            value={query.new_security}
          />

          <BasicSeparator className="sw-my-4" />

          <SecurityReviewFilter
            {...facetProps}
            facet={getFacet(facets, 'new_security_review')}
            property="new_security_review"
            value={query.new_security_review_rating}
          />

          <BasicSeparator className="sw-my-4" />

          <NewMaintainabilityFilter
            {...facetProps}
            facet={getFacet(facets, 'new_maintainability')}
            value={query.new_maintainability}
          />

          <BasicSeparator className="sw-my-4" />

          <NewCoverageFilter
            {...facetProps}
            facet={getFacet(facets, 'new_coverage')}
            value={query.new_coverage}
          />

          <BasicSeparator className="sw-my-4" />

          <NewDuplicationsFilter
            {...facetProps}
            facet={getFacet(facets, 'new_duplications')}
            value={query.new_duplications}
          />

          <BasicSeparator className="sw-my-4" />

          <NewLinesFilter
            {...facetProps}
            facet={getFacet(facets, 'new_lines')}
            value={query.new_lines}
          />
        </>
      )}

      <BasicSeparator className="sw-my-4" />

      <LanguagesFilter
        {...facetProps}
        facet={getFacet(facets, 'languages')}
        loadSearchResultCount={loadSearchResultCount}
        query={query}
        value={query.languages}
      />

      <BasicSeparator className="sw-my-4" />

      {applicationsEnabled && (
        <>
          <QualifierFacet
            {...facetProps}
            facet={getFacet(facets, 'qualifier')}
            value={query.qualifier}
          />

          <BasicSeparator className="sw-my-4" />
        </>
      )}
      <TagsFacet
        {...facetProps}
        facet={getFacet(facets, 'tags')}
        loadSearchResultCount={loadSearchResultCount}
        query={query}
        value={query.tags}
      />
    </div>
  );
}

function getFacet(facets: Facets | undefined, name: string) {
  return facets && facets[name];
}

function getMaxFacetValue(facets?: Facets) {
  return facets && Math.max(...flatMap(Object.values(facets), (facet) => Object.values(facet)));
}
