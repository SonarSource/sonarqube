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
import classNames from 'classnames';
import { Link } from 'react-router';
import { formatMeasure } from '../../../helpers/measures';

export default class Filter extends React.Component {
  static propTypes = {
    value: React.PropTypes.any,
    property: React.PropTypes.string.isRequired,
    options: React.PropTypes.array.isRequired,
    maxFacetValue: React.PropTypes.number,
    optionClassName: React.PropTypes.string,

    renderName: React.PropTypes.func.isRequired,
    renderOption: React.PropTypes.func.isRequired,

    getFacetValueForOption: React.PropTypes.func,

    halfWidth: React.PropTypes.bool,

    getFilterUrl: React.PropTypes.func.isRequired
  };

  static defaultProps = {
    halfWidth: false
  };

  renderHeader () {
    return (
        <div className="search-navigator-facet-header projects-facet-header">
          {this.props.renderName()}
        </div>
    );
  }

  renderOptionBar (facetValue) {
    if (facetValue == null || !this.props.maxFacetValue) {
      return null;
    }

    return (
        <div className="projects-facet-bar">
          <div className="projects-facet-bar-inner"
               style={{ width: facetValue / this.props.maxFacetValue * 60 }}/>
        </div>
    );
  }

  renderOption (option) {
    const { property, value, facet, getFacetValueForOption } = this.props;
    const className = classNames('facet', 'search-navigator-facet', 'projects-facet', {
      'active': option === value,
      'search-navigator-facet-half': this.props.halfWidth
    }, this.props.optionClassName);

    const path = option === value ?
        this.props.getFilterUrl({ [property]: null }) :
        this.props.getFilterUrl({ [property]: option });

    const facetValue = (facet && getFacetValueForOption) ? getFacetValueForOption(facet, option) : null;

    return (
        <Link key={option} className={className} to={path} data-key={option}>
          <span className="facet-name">
            {this.props.renderOption(option, option === value)}
          </span>
          {facetValue != null && (
              <span className="facet-stat">
                {formatMeasure(facetValue, 'SHORT_INT')}
                {this.renderOptionBar(facetValue)}
              </span>
          )}
        </Link>
    );
  }

  renderOptions () {
    return (
        <div className="search-navigator-facet-list">
          {this.props.options.map(option => this.renderOption(option))}
        </div>
    );
  }

  render () {
    return (
        <div className="search-navigator-facet-box" data-key={this.props.property}>
          {this.renderHeader()}
          {this.renderOptions()}
        </div>
    );
  }
}
