/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import Filter from './Filter';
import CoverageRating from '../../../components/ui/CoverageRating';
import { translate } from '../../../helpers/l10n';
import { getCoverageRatingLabel, getCoverageRatingAverageValue } from '../../../helpers/ratings';

export default class CoverageFilter extends React.Component {
  static propTypes = {
    value: React.PropTypes.number,
    getFilterUrl: React.PropTypes.func.isRequired,
    toggleFilter: React.PropTypes.func.isRequired
  };

  renderValue () {
    const { value } = this.props;

    const average = getCoverageRatingAverageValue(value);
    const label = getCoverageRatingLabel(value);

    return (
        <div className="projects-filter-value">
          <CoverageRating value={average}/>

          <div className="projects-filter-hint note">
            {label}
          </div>
        </div>
    );
  }

  renderOptions () {
    const options = [1, 2, 3, 4, 5];

    return (
        <div>
          {options.map(option => (
              <Link key={option}
                    className={option === this.props.value ? 'active' : ''}
                    to={this.props.getFilterUrl({ 'coverage': option })}
                    onClick={this.props.toggleFilter}>
                <CoverageRating value={getCoverageRatingAverageValue(option)}/>
                <span className="spacer-left">
                  {getCoverageRatingLabel(option)}
                </span>
              </Link>
          ))}
          {this.props.value != null && (
              <div>
                <hr/>
                <Link className="text-center"
                      to={this.props.getFilterUrl({ 'coverage': null })}
                      onClick={this.props.toggleFilter}>
                  <span className="text-danger">{translate('reset_verb')}</span>
                </Link>
              </div>
          )}
        </div>
    );
  }

  render () {
    return (
        <Filter
            renderName={() => 'Coverage'}
            renderOptions={() => this.renderOptions()}
            renderValue={() => this.renderValue()}
            {...this.props}/>
    );
  }
}
