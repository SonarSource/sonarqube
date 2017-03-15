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
import sortBy from 'lodash/sortBy';
import {
  FilterContainer,
  LanguageFilterFooterContainer,
  LanguageFilterOptionContainer
} from './containers';

export default class LanguageFilter extends React.PureComponent {
  static propTypes = {
    query: React.PropTypes.object.isRequired,
    isFavorite: React.PropTypes.bool,
    organization: React.PropTypes.object
  };

  property = 'languages';

  renderOption = option => {
    return <LanguageFilterOptionContainer languageKey={option}/>;
  };

  getSortedOptions (facet) {
    return sortBy(Object.keys(facet), [option => -facet[option]]);
  }

  renderFooter = () => (
    <LanguageFilterFooterContainer
      property={this.property}
      query={this.props.query}
      isFavorite={this.props.isFavorite}
      organization={this.props.organization}/>
  );

  getFacetValueForOption = (facet, option) => facet[option];

  getOptions = facet => facet ? this.getSortedOptions(facet) : [];

  renderName = () => 'Languages';

  render () {
    return (
      <FilterContainer
        property={this.property}
        getOptions={this.getOptions}
        renderName={this.renderName}
        renderOption={this.renderOption}
        renderFooter={this.renderFooter}
        getFacetValueForOption={this.getFacetValueForOption}
        query={this.props.query}
        isFavorite={this.props.isFavorite}
        organization={this.props.organization}/>
    );
  }
}
