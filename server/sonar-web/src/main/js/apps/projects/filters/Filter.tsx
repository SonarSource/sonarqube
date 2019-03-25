/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import * as classNames from 'classnames';
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';
import { Facet } from '../types';
import { RawQuery } from '../../../helpers/query';

export type Option = string | number;

interface Props {
  property: string;
  className?: string;
  onQueryChange: (change: RawQuery) => void;
  options: Option[];
  query: T.Dict<any>;
  renderOption: (option: Option, isSelected: boolean) => React.ReactNode;

  value?: Option | Option[];
  facet?: Facet;
  maxFacetValue?: number;
  optionClassName?: string;
  organization?: { key: string };

  getFacetValueForOption?: (facet: Facet, option: Option) => void;

  halfWidth?: boolean;
  highlightUnder?: number;
  highlightUnderMax?: number;

  header?: React.ReactNode;
  footer?: React.ReactNode;
}

export default class Filter extends React.PureComponent<Props> {
  isSelected(option: Option): boolean {
    const { value } = this.props;
    return Array.isArray(value) ? value.includes(option) : String(option) === String(value);
  }

  highlightUnder(option?: Option): boolean {
    return (
      this.props.highlightUnder != null &&
      option != null &&
      option > this.props.highlightUnder &&
      (this.props.highlightUnderMax == null || option < this.props.highlightUnderMax)
    );
  }

  handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();

    const { property, value } = this.props;
    const { key: option } = event.currentTarget.dataset;
    let urlOption;

    if (option) {
      if (Array.isArray(value) && event.ctrlKey) {
        if (this.isSelected(option)) {
          urlOption = value.length > 1 ? value.filter(val => val !== option).join(',') : null;
        } else {
          urlOption = value.concat(option).join(',');
        }
      } else {
        urlOption =
          this.isSelected(option) && (!Array.isArray(value) || value.length < 2) ? null : option;
      }

      this.props.onQueryChange({ [property]: urlOption });
    }
  };

  renderOptionBar(facetValue: number | undefined) {
    if (facetValue === undefined || !this.props.maxFacetValue) {
      return null;
    }
    return (
      <div className="projects-facet-bar">
        <div
          className="projects-facet-bar-inner"
          style={{ width: (facetValue / this.props.maxFacetValue) * 60 }}
        />
      </div>
    );
  }

  renderOption(option: Option) {
    const { facet, getFacetValueForOption, value } = this.props;
    const className = classNames(
      'facet',
      'search-navigator-facet',
      'projects-facet',
      {
        active: this.isSelected(option),
        'search-navigator-facet-half': this.props.halfWidth
      },
      this.props.optionClassName
    );

    const facetValue =
      facet && getFacetValueForOption ? getFacetValueForOption(facet, option) : undefined;

    const isUnderSelectedOption =
      typeof value === 'number' &&
      typeof option === 'number' &&
      this.highlightUnder(value) &&
      option > value;

    return (
      <a className={className} data-key={option} href="#" key={option} onClick={this.handleClick}>
        <span className="facet-name">
          {this.props.renderOption(option, this.isSelected(option) || isUnderSelectedOption)}
        </span>
        {facetValue != null && (
          <span className="facet-stat">
            {formatMeasure(facetValue, 'SHORT_INT')}
            {this.renderOptionBar(facetValue)}
          </span>
        )}
      </a>
    );
  }

  renderOptions = () => {
    const { options, highlightUnder } = this.props;
    if (options && options.length > 0) {
      if (highlightUnder != null) {
        const max = this.props.highlightUnderMax || options.length;
        const beforeHighlight = options.slice(0, highlightUnder);
        const insideHighlight = options.slice(highlightUnder, max);
        const afterHighlight = options.slice(max);
        return (
          <div className="search-navigator-facet-list projects-facet-list">
            {beforeHighlight.map(option => this.renderOption(option))}
            <div className="search-navigator-facet-highlight-under-container">
              {insideHighlight.map(option => this.renderOption(option))}
            </div>
            {afterHighlight.map(option => this.renderOption(option))}
          </div>
        );
      } else {
        return (
          <div className="search-navigator-facet-list projects-facet-list">
            {options.map(option => this.renderOption(option))}
          </div>
        );
      }
    } else {
      return <div className="search-navigator-facet-empty">{translate('no_results')}</div>;
    }
  };

  render() {
    return (
      <div
        className={classNames('search-navigator-facet-box', this.props.className)}
        data-key={this.props.property}>
        {this.props.header}
        {this.renderOptions()}
        {this.props.footer}
      </div>
    );
  }
}
