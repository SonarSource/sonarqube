/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { RawQuery } from '../../../types/types';
import { Facet } from '../types';

export type Option = string | number;

interface Props {
  property: string;
  className?: string;
  onQueryChange: (change: RawQuery) => void;
  options: Option[];
  renderAccessibleLabel: (option: Option) => string;
  renderOption: (option: Option, isSelected: boolean) => React.ReactNode;

  value?: Option | Option[];
  facet?: Facet;
  maxFacetValue?: number;
  optionClassName?: string;

  getFacetValueForOption?: (facet: Facet, option: Option) => number;

  halfWidth?: boolean;
  highlightUnder?: number;
  highlightUnderMax?: number;

  header?: React.ReactNode;
  footer?: React.ReactNode;
}

const defaultGetFacetValueForOption = (facet: Facet, option: string | number) => facet[option];

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

  getUrlOptionForSingleValue = (option: string) => {
    return this.isSelected(option) ? null : option;
  };

  getUrlOptionForMultiValue = (
    event: React.MouseEvent<HTMLButtonElement>,
    option: string,
    value: Option[]
  ) => {
    if (event.ctrlKey || event.metaKey) {
      if (this.isSelected(option)) {
        return value.length > 1 ? value.filter((val) => val !== option).join(',') : null;
      }

      return value.concat(option).join(',');
    }

    return this.isSelected(option) && value.length < 2 ? null : option;
  };

  handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();

    const { property, value } = this.props;
    const { key: option } = event.currentTarget.dataset;

    if (option) {
      const urlOption = Array.isArray(value)
        ? this.getUrlOptionForMultiValue(event, option, value)
        : this.getUrlOptionForSingleValue(option);

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

  renderOption(option: Option, highlightable = false, lastHighlightable = false) {
    const { facet, getFacetValueForOption = defaultGetFacetValueForOption, value } = this.props;
    const active = this.isSelected(option);
    const className = classNames(
      'facet',
      'search-navigator-facet',
      'projects-facet',
      'button-link',
      {
        active,
        'search-navigator-facet-half': this.props.halfWidth,
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
      <li
        key={option}
        className={classNames({
          'search-navigator-facet-worse-than-highlight': highlightable,
          last: lastHighlightable,
          active,
        })}
      >
        <button
          aria-label={this.props.renderAccessibleLabel(option)}
          className={className}
          data-key={option}
          type="button"
          tabIndex={0}
          onClick={this.handleClick}
          role="checkbox"
          aria-checked={this.isSelected(option) || isUnderSelectedOption}
        >
          <span className="facet-name">
            {this.props.renderOption(option, this.isSelected(option) || isUnderSelectedOption)}
          </span>
          {facetValue != null && (
            <span className="facet-stat">
              {formatMeasure(facetValue, 'SHORT_INT')}
              {this.renderOptionBar(facetValue)}
            </span>
          )}
        </button>
      </li>
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
          <ul className="search-navigator-facet-list projects-facet-list">
            {beforeHighlight.map((option) => this.renderOption(option))}
            {insideHighlight.map((option, i) =>
              this.renderOption(option, true, i === insideHighlight.length - 1)
            )}
            {afterHighlight.map((option) => this.renderOption(option))}
          </ul>
        );
      }
      return (
        <ul className="search-navigator-facet-list projects-facet-list">
          {options.map((option) => this.renderOption(option))}
        </ul>
      );
    }
    return (
      <div className="search-navigator-facet-empty">
        <em>{translate('projects.facets.no_available_filters_clear_others')}</em>
      </div>
    );
  };

  render() {
    return (
      <div
        className={classNames('search-navigator-facet-box', this.props.className)}
        data-key={this.props.property}
      >
        {this.props.header}
        {this.renderOptions()}
        {this.props.footer}
      </div>
    );
  }
}
