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
import PageSidebarOverall from './PageSidebarOverall';
import QualityGateFilter from '../filters/QualityGateFilter';
import SearchFilterContainer from '../filters/SearchFilterContainer';
import TagsFilterContainer from '../filters/TagsFilterContainer';
import { translate } from '../../../helpers/l10n';

type Props = {
  isFavorite: boolean,
  organization?: { key: string },
  query: { [string]: string },
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
    .filter(key => key !== 'view' && key !== 'visualization')
    .some(key => query[key] != null);
  const isLeakView = view === 'leak';
  const basePathName = organization ? `/organizations/${organization.key}/projects` : '/projects';
  const pathname = basePathName + (isFavorite ? '/favorite' : '');

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
        <SearchFilterContainer query={query} isFavorite={isFavorite} organization={organization} />
      </div>
      <QualityGateFilter query={query} isFavorite={isFavorite} organization={organization} />
      {!isLeakView &&
        <PageSidebarOverall query={query} isFavorite={isFavorite} organization={organization} />}
      <LanguagesFilterContainer query={query} isFavorite={isFavorite} organization={organization} />
      <TagsFilterContainer query={query} isFavorite={isFavorite} organization={organization} />
    </div>
  );
}
