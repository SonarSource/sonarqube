/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
//@flow
import React from 'react';
import { Link } from 'react-router';
import FavoriteFilterContainer from './FavoriteFilterContainer';
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
import TagsFilterContainer from '../filters/TagsFilterContainer';
import { translate } from '../../../helpers/l10n';
import type { RawQuery } from '../../../helpers/query';

type Props = {
  isFavorite: boolean,
  organization?: { key: string },
  query: RawQuery,
  view: string,
  visualization: string
};

export default function PageSidebar({
  query,
  isFavorite,
  organization,
  view,
  visualization
}: Props) {
  const isFiltered = Object.keys(query)
    .filter(key => !['view', 'visualization', 'sort'].includes(key))
    .some(key => query[key] != null);
  const isLeakView = view === 'leak';
  const basePathName = organization ? `/organizations/${organization.key}/projects` : '/projects';
  const pathname = basePathName + (isFavorite ? '/favorite' : '');
  const facetProps = { query, isFavorite, organization };

  let linkQuery: ?{ view: string, visualization?: string };
  if (view !== 'overall') {
    linkQuery = { view };

    if (view === 'visualizations') {
      linkQuery.visualization = visualization;
    }
  }

  return (
    <div>
      <FavoriteFilterContainer query={linkQuery} organization={organization} />

      <div className="projects-facets-header clearfix">
        {isFiltered &&
          <div className="projects-facets-reset">
            <Link to={{ pathname, query: linkQuery }} className="button button-red">
              {translate('clear_all_filters')}
            </Link>
          </div>}

        <h3>{translate('filters')}</h3>
      </div>
      <QualityGateFilter {...facetProps} />
      {!isLeakView && [
        <ReliabilityFilter key="reliability" {...facetProps} />,
        <SecurityFilter key="security" {...facetProps} />,
        <MaintainabilityFilter key="maintainability" {...facetProps} />,
        <CoverageFilter key="coverage" {...facetProps} />,
        <DuplicationsFilter key="duplications" {...facetProps} />,
        <SizeFilter key="size" {...facetProps} />
      ]}
      {isLeakView && [
        <NewReliabilityFilter key="new_reliability" {...facetProps} />,
        <NewSecurityFilter key="new_security" {...facetProps} />,
        <NewMaintainabilityFilter key="new_maintainability" {...facetProps} />,
        <NewCoverageFilter key="new_coverage" {...facetProps} />,
        <NewDuplicationsFilter key="new_duplications" {...facetProps} />,
        <NewLinesFilter key="new_size" {...facetProps} />
      ]}
      <LanguagesFilterContainer {...facetProps} />
      <TagsFilterContainer {...facetProps} />
    </div>
  );
}
