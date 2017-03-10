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
import CoverageFilter from '../filters/CoverageFilter';
import DuplicationsFilter from '../filters/DuplicationsFilter';
import SizeFilter from '../filters/SizeFilter';
import QualityGateFilter from '../filters/QualityGateFilter';
import ReliabilityFilter from '../filters/ReliabilityFilter';
import SecurityFilter from '../filters/SecurityFilter';
import MaintainabilityFilter from '../filters/MaintainabilityFilter';
import LanguageFilter from '../filters/LanguageFilter';
import SearchFilterContainer from '../filters/SearchFilterContainer';
import { translate } from '../../../helpers/l10n';

export default class PageSidebar extends React.PureComponent {
  static propTypes = {
    query: React.PropTypes.object.isRequired,
    isFavorite: React.PropTypes.bool.isRequired,
    organization: React.PropTypes.object
  };

  render () {
    const isFiltered = Object.keys(this.props.query).some(key => this.props.query[key] != null);

    const basePathName = this.props.organization
      ? `/organizations/${this.props.organization.key}/projects`
      : '/projects';
    const pathname = basePathName + (this.props.isFavorite ? '/favorite' : '');

    return (
      <div className="search-navigator-facets-list">
        <div className="projects-facets-header clearfix">
          {isFiltered &&
            <div className="projects-facets-reset">
              <Link to={pathname} className="button button-red">
                {translate('projects.clear_all_filters')}
              </Link>
            </div>}

          <h3>{translate('filters')}</h3>
          <SearchFilterContainer
            query={this.props.query}
            isFavorite={this.props.isFavorite}
            organization={this.props.organization}/>
        </div>

        <QualityGateFilter
          query={this.props.query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}/>
        <ReliabilityFilter
          query={this.props.query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}/>
        <SecurityFilter
          query={this.props.query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}/>
        <MaintainabilityFilter
          query={this.props.query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}/>
        <CoverageFilter
          query={this.props.query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}/>
        <DuplicationsFilter
          query={this.props.query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}/>
        <SizeFilter
          query={this.props.query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}/>
        <LanguageFilter
          query={this.props.query}
          isFavorite={this.props.isFavorite}
          organization={this.props.organization}/>
      </div>
    );
  }
}
