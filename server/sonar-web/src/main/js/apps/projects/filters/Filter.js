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
import classNames from 'classnames';
import { Link } from 'react-router';
import { formatMeasure } from '../../../helpers/measures';

export default class Filter extends React.Component {
  static propTypes = {
    isOpen: React.PropTypes.bool.isRequired,
    value: React.PropTypes.any,
    property: React.PropTypes.string.isRequired,
    options: React.PropTypes.array.isRequired,
    maxFacetValue: React.PropTypes.number,
    optionClassName: React.PropTypes.string,

    renderName: React.PropTypes.func.isRequired,
    renderOption: React.PropTypes.func.isRequired,

    getFacetValueForOption: React.PropTypes.func,

    halfWidth: React.PropTypes.bool,

    getFilterUrl: React.PropTypes.func.isRequired,
    openFilter: React.PropTypes.func.isRequired,
    closeFilter: React.PropTypes.func.isRequired,

    router: React.PropTypes.object
  };

  static defaultProps = {
    halfWidth: false
  };

  handleHeaderClick = e => {
    e.preventDefault();
    e.target.blur();

    const { value, isOpen, property } = this.props;
    const hasValue = value != null;
    const isDisplayedOpen = isOpen || hasValue;

    if (isDisplayedOpen) {
      this.props.closeFilter();
    } else {
      this.props.openFilter();
    }

    if (hasValue) {
      this.props.router.push(this.props.getFilterUrl({ [property]: null }));
    }
  };

  renderHeader () {
    const { value, isOpen, renderName } = this.props;
    const hasValue = value != null;
    const checkboxClassName = classNames('icon-checkbox', {
      'icon-checkbox-checked': hasValue || isOpen
    });

    return (
        <a className="search-navigator-facet-header projects-facet-header" href="#" onClick={this.handleHeaderClick}>
          <i className={checkboxClassName}/> {renderName()}
        </a>
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
      active: option === value,
      'search-navigator-facet-half': this.props.halfWidth
    }, this.props.optionClassName);

    const path = option === value ?
        this.props.getFilterUrl({ [property]: null }) :
        this.props.getFilterUrl({ [property]: option });

    const facetValue = (facet && getFacetValueForOption) ? getFacetValueForOption(facet, option) : null;

    return (
        <Link key={option} className={className} to={path}>
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
    const { value, isOpen, options } = this.props;
    const hasValue = value != null;

    if (!hasValue && !isOpen) {
      return null;
    }

    return (
        <div className="search-navigator-facet-list">
          {this.props.children}

          {options.map(option => this.renderOption(option))}
        </div>
    );
  }

  render () {
    const { value, isOpen } = this.props;
    const hasValue = value != null;
    const className = classNames('search-navigator-facet-box', {
      'search-navigator-facet-box-collapsed': !hasValue && !isOpen
    });

    return (
        <div className={className}>
          {this.renderHeader()}
          {this.renderOptions()}
        </div>
    );
  }
}
