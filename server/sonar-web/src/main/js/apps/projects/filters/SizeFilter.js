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
import SizeRating from '../../../components/ui/SizeRating';
import { getSizeRatingLabel, getSizeRatingAverageValue } from '../../../helpers/ratings';

export default class SizeFilter extends React.Component {
  renderOption = (option, selected) => {
    return (
        <span>
          <SizeRating value={getSizeRatingAverageValue(option)} small={true} muted={!selected}/>
          <span className="spacer-left">
            {getSizeRatingLabel(option)}
          </span>
        </span>
    );
  };

  getFacetValueForOption = (facet, option) => {
    const map = ['*-1000.0', '1000.0-10000.0', '10000.0-100000.0', '100000.0-500000.0', '500000.0-*'];
    return facet[map[option - 1]];
  };

  render () {
    return (
        <FilterContainer
            property="size"
            options={[1, 2, 3, 4, 5]}
            renderName={() => 'Size'}
            renderOption={this.renderOption}
            getFacetValueForOption={this.getFacetValueForOption}
            query={this.props.query}
            isFavorite={this.props.isFavorite}
            organization={this.props.organization}/>
    );
  }
}
