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
import FilterHeader from './FilterHeader';
import { translate } from '../../../helpers/l10n';
import { getSizeRatingLabel } from '../../../helpers/ratings';

export default class NewLinesFilter extends React.PureComponent {
  static propTypes = {
    className: React.PropTypes.string,
    query: React.PropTypes.object.isRequired,
    isFavorite: React.PropTypes.bool,
    organization: React.PropTypes.object,
    property: React.PropTypes.string
  };

  static defaultProps = {
    property: 'new_lines'
  };

  getFacetValueForOption(facet, option) {
    const map = [
      '*-1000.0',
      '1000.0-10000.0',
      '10000.0-100000.0',
      '100000.0-500000.0',
      '500000.0-*'
    ];
    return facet[map[option - 1]];
  }

  renderOption(option) {
    return (
      <span>
        {getSizeRatingLabel(option)}
      </span>
    );
  }

  render() {
    return (
      <FilterContainer
        property={this.props.property}
        className="leak-facet-box"
        options={[1, 2, 3, 4, 5]}
        query={this.props.query}
        renderOption={this.renderOption}
        isFavorite={this.props.isFavorite}
        organization={this.props.organization}
        getFacetValueForOption={this.getFacetValueForOption}
        highlightUnder={1}
        header={<FilterHeader name={translate('projects.facets.new_lines')} />}
      />
    );
  }
}
