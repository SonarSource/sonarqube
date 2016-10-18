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
import SizeRating from '../../../components/ui/SizeRating';
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';

export default class SizeFilter extends React.Component {
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
      label = '>' + formatMeasure(value.from, 'SHORT_INT');
    } else if (value.from == null) {
      label = '<' + formatMeasure(value.to, 'SHORT_INT');
    } else {
      label = formatMeasure(value.from, 'SHORT_INT') + '–' + formatMeasure(value.to, 'SHORT_INT');
    }
    return label;
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
          <SizeRating value={average}/>

          <div className="projects-filter-hint note">
            {label}
          </div>
        </div>
    );
  }

  renderOptions () {
    const options = [
      [null, 1000, 0],
      [1000, 10000, 1000],
      [10000, 100000, 10000],
      [100000, 500000, 100000],
      [500000, null, 500000],
    ];

    return (
        <div>
          {options.map(option => (
              <Link key={option[2]}
                    className={this.isOptionAction(option[0], option[1]) ? 'active' : ''}
                    to={this.props.getFilterUrl({ 'size__gte': option[0], 'size__lt': option[1] })}
                    onClick={this.props.toggleFilter}>
                <SizeRating value={option[2]}/>
                <span className="spacer-left">{this.renderLabel({ from: option[0], to: option[1] })}</span>
              </Link>
          ))}
          {this.props.value != null && (
              <div>
                <hr/>
                <Link className="text-center"
                      to={this.props.getFilterUrl({ 'size__gte': null, 'size__lt': null })}
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
            renderName={() => 'Size'}
            renderOptions={() => this.renderOptions()}
            renderValue={() => this.renderValue()}
            {...this.props}/>
    );
  }
}
