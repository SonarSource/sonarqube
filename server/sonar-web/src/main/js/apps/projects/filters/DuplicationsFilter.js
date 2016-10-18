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
import DuplicationsRating from '../../../components/ui/DuplicationsRating';
import { translate } from '../../../helpers/l10n';

export default class DuplicationsFilter extends React.Component {
  static propTypes = {
    value: React.PropTypes.shape({
      from: React.PropTypes.number,
      to: React.PropTypes.number
    }),
    getFilterUrl: React.PropTypes.func.isRequired,
    toggleFilter: React.PropTypes.func.isRequired
  };

  isOptionAction (from, to) {
    const { value } = this.props;

    if (value == null) {
      return false;
    }

    return value.from === from && value.to === to;
  }

  renderLabel (value) {
    let label;
    if (value.to == null) {
      label = '>' + value.from;
    } else if (value.from == null) {
      label = '<' + value.to;
    } else {
      label = value.from + '–' + value.to;
    }
    return label + '%';
  }

  renderValue () {
    const { value } = this.props;

    let average;
    if (value.to == null) {
      average = value.from;
    } else if (value.from == null) {
      average = value.to / 2;
    } else {
      average = (value.from + value.to) / 2;
    }

    const label = this.renderLabel(value);

    return (
        <div className="projects-filter-value">
          <DuplicationsRating value={average}/>

          <div className="projects-filter-hint note">
            {label}
          </div>
        </div>
    );
  }

  renderOptions () {
    const options = [
      [null, 3, 1.5],
      [3, 5, 4],
      [5, 10, 7.5],
      [10, 20, 15],
      [20, null, 30],
    ];

    return (
        <div>
          {options.map(option => (
              <Link key={option[2]}
                    className={this.isOptionAction(option[0], option[1]) ? 'active' : ''}
                    to={this.props.getFilterUrl({ 'duplications__gte': option[0], 'duplications__lt': option[1] })}
                    onClick={this.props.toggleFilter}>
                <DuplicationsRating value={option[2]}/>
                <span className="spacer-left">{this.renderLabel({ from: option[0], to: option[1] })}</span>
              </Link>
          ))}
          {this.props.value != null && (
              <div>
                <hr/>
                <Link className="text-center"
                      to={this.props.getFilterUrl({ 'duplications__gte': null, 'duplications__lt': null })}
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
            renderName={() => 'Duplications'}
            renderOptions={() => this.renderOptions()}
            renderValue={() => this.renderValue()}
            {...this.props}/>
    );
  }
}
