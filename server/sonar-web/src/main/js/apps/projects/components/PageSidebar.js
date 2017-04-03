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
import React from 'react';
import { Link } from 'react-router';
import FavoriteFilterContainer from './FavoriteFilterContainer';
import CoverageFilter from '../filters/CoverageFilter';
import DuplicationsFilter from '../filters/DuplicationsFilter';
import SizeFilter from '../filters/SizeFilter';
import QualityGateFilter from '../filters/QualityGateFilter';
import ReliabilityFilter from '../filters/ReliabilityFilter';
import SecurityFilter from '../filters/SecurityFilter';
import MaintainabilityFilter from '../filters/MaintainabilityFilter';
import TagsFilterContainer from '../filters/TagsFilterContainer';
import SearchFilterContainer from '../filters/SearchFilterContainer';
import LanguagesFilterContainer from '../filters/LanguagesFilterContainer';
import { translate } from '../../../helpers/l10n';

export default class PageSidebar extends React.PureComponent {
  static propTypes = {
    query: React.PropTypes.object.isRequired,
    isFavorite: React.PropTypes.bool.isRequired,
    organization: React.PropTypes.object
  };

  render() {
    const { query } = this.props;

    const isFiltered = Object.keys(query)
      .filter(key => key !== 'view' && key !== 'visualization')
      .some(key => query[key] != null);

    const basePathName = this.props.organization
      ? `/organizations/${this.props.organization.key}/projects`
      : '/projects';
    const pathname = basePathName + (this.props.isFavorite ? '/favorite' : '');
    const linkQuery = query.view === 'visualizations'
      ? { view: query.view, visualization: query.visualization }
      : undefined;

    return (
      <div>
        <FavoriteFilterContainer organization={this.props.organization} />

        <div className="projects-facets-header clearfix">
          {isFiltered &&
            <div className="projects-facets-reset">
              <Link to={{ pathname, query: linkQuery }} className="button button-red">
                {translate('clear_all_filters')}
              </Link>
            </div>}

          <h3>{translate('filters')}</h3>
          <SearchFilterContainer
            query={query}
            isFavorite={this.props.isFavorite}
            organization={this.props.organization}
          />
        </div>

        <QualityGateFilter
          query={query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}
        />
        <ReliabilityFilter
          query={query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}
        />
        <SecurityFilter
          query={query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}
        />
        <MaintainabilityFilter
          query={query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}
        />
        <CoverageFilter
          query={query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}
        />
        <DuplicationsFilter
          query={query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}
        />
        <SizeFilter
          query={query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}
        />
        <LanguagesFilterContainer
          query={query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}
        />
        <TagsFilterContainer
          query={query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}
        />
      </div>
    );
  }
}
