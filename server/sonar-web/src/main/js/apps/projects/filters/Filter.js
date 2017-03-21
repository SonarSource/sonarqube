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
import { getFilterUrl } from './utils';
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';

export default class Filter extends React.PureComponent {
  static propTypes = {
    property: React.PropTypes.string.isRequired,
    options: React.PropTypes.array.isRequired,
    query: React.PropTypes.object.isRequired,
    renderOption: React.PropTypes.func.isRequired,

    value: React.PropTypes.any,
    facet: React.PropTypes.object,
    maxFacetValue: React.PropTypes.number,
    optionClassName: React.PropTypes.string,
    isFavorite: React.PropTypes.bool,
    organization: React.PropTypes.object,

    getFacetValueForOption: React.PropTypes.func,

    halfWidth: React.PropTypes.bool,
    highlightUnder: React.PropTypes.number,

    header: React.PropTypes.object,
    footer: React.PropTypes.object
  };

  static defaultProps = {
    halfWidth: false
  };

  isSelected(option) {
    const { value } = this.props;
    return Array.isArray(value) ? value.includes(option) : option === value;
  }

  highlightUnder(option) {
    return this.props.highlightUnder != null &&
      option !== null &&
      option > this.props.highlightUnder;
  }

  getPath(option) {
    const { property, value } = this.props;
    let urlOption;

    if (Array.isArray(value)) {
      if (this.isSelected(option)) {
        urlOption = value.length > 1 ? value.filter(val => val !== option).join(',') : null;
      } else {
        urlOption = value.concat(option).join(',');
      }
    } else {
      urlOption = this.isSelected(option) ? null : option;
    }
    return getFilterUrl(this.props, { [property]: urlOption });
  }

  renderOptionBar(facetValue) {
    if (facetValue == null || !this.props.maxFacetValue) {
      return null;
    }
    return (
      <div className="projects-facet-bar">
        <div
          className="projects-facet-bar-inner"
          style={{ width: facetValue / this.props.maxFacetValue * 60 }}
        />
      </div>
    );
  }

  renderOption(option) {
    const { facet, getFacetValueForOption, value } = this.props;
    const className = classNames(
      'facet',
      'search-navigator-facet',
      'projects-facet',
      {
        active: this.isSelected(option),
        'search-navigator-facet-half': this.props.halfWidth,
        'search-navigator-facet-highlight-under': this.highlightUnder(option)
      },
      this.props.optionClassName
    );

    const path = this.getPath(option);
    const facetValue = facet && getFacetValueForOption
      ? getFacetValueForOption(facet, option)
      : null;

    const isUnderSelectedOption = this.highlightUnder(value) && option > value;

    return (
      <Link key={option} className={className} to={path} data-key={option}>
        <span className="facet-name">
          {this.props.renderOption(option, this.isSelected(option) || isUnderSelectedOption)}
        </span>
        {facetValue != null &&
          <span className="facet-stat">
            {formatMeasure(facetValue, 'SHORT_INT')}
            {this.renderOptionBar(facetValue)}
          </span>}
      </Link>
    );
  }

  renderOptions() {
    const { options } = this.props;
    if (options && options.length > 0) {
      return (
        <div className="search-navigator-facet-list">
          {options.map(option => this.renderOption(option))}
        </div>
      );
    } else {
      return (
        <div className="search-navigator-facet-empty">
          {translate('no_results')}
        </div>
      );
    }
  }

  render() {
    return (
      <div className="search-navigator-facet-box" data-key={this.props.property}>
        {this.props.header}
        {this.renderOptions()}
        {this.props.footer}
      </div>
    );
  }
}
