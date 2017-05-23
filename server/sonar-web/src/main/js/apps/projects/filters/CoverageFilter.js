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
import SortingFilter from './SortingFilter';
import CoverageRating from '../../../components/ui/CoverageRating';
import { getCoverageRatingLabel, getCoverageRatingAverageValue } from '../../../helpers/ratings';
import { translate } from '../../../helpers/l10n';

export default class CoverageFilter extends React.PureComponent {
  static propTypes = {
    query: React.PropTypes.object.isRequired,
    isFavorite: React.PropTypes.bool,
    organization: React.PropTypes.object
  };

  property = 'coverage';

  getFacetValueForOption(facet, option) {
    const map = ['80.0-*', '70.0-80.0', '50.0-70.0', '30.0-50.0', '*-30.0'];
    return facet[map[option - 1]];
  }

  renderOption(option, selected) {
    return (
      <span>
        <CoverageRating
          value={getCoverageRatingAverageValue(option)}
          size="small"
          muted={!selected}
        />
        <span className="spacer-left">
          {getCoverageRatingLabel(option)}
        </span>
      </span>
    );
  }

  render() {
    return (
      <FilterContainer
        property={this.property}
        options={[1, 2, 3, 4, 5]}
        query={this.props.query}
        renderOption={this.renderOption}
        isFavorite={this.props.isFavorite}
        organization={this.props.organization}
        getFacetValueForOption={this.getFacetValueForOption}
        highlightUnder={1}
        header={
          <FilterHeader name={translate('metric_domain.Coverage')}>
            <SortingFilter
              property={this.property}
              query={this.props.query}
              isFavorite={this.props.isFavorite}
              organization={this.props.organization}
              sortDesc="right"
            />
          </FilterHeader>
        }
      />
    );
  }
}
