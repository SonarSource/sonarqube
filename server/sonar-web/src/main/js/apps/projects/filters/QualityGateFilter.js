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
import FilterContainer from './FilterContainer';
import Level from '../../../components/ui/Level';

export default class QualityGateFilter extends React.Component {
  renderOption = (option, selected) => {
    return (
        <Level level={option} small={true} muted={!selected}/>
    );
  };

  getFacetValueForOption = (facet, option) => {
    return facet[option];
  };

  render () {
    return (
        <FilterContainer
            property="gate"
            options={['OK', 'WARN', 'ERROR']}
            renderName={() => 'Quality Gate'}
            renderOption={this.renderOption}
            getFacetValueForOption={this.getFacetValueForOption}
            query={this.props.query}
            isFavorite={this.props.isFavorite}
            organization={this.props.organization}/>
    );
  }
}
