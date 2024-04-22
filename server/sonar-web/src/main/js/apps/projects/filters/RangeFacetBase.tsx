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
import { FacetBox, FacetItem, HighlightedFacetItems, LightLabel } from 'design-system';
import * as React from 'react';
import { RawQuery } from '~sonar-aligned/types/router';
import { translate } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import { FacetItemsList } from '../../issues/sidebar/FacetItemsList';
import { formatFacetStat } from '../../issues/utils';
import { Facet } from '../types';

export type Option = string | number;

interface Props {
  property: string;
  className?: string;
  onQueryChange: (change: RawQuery) => void;
  options: Option[];
  renderAccessibleLabel: (option: Option) => string;
  renderOption: (option: Option, isSelected: boolean) => React.ReactNode;

  value?: Option;
  facet?: Facet;
  maxFacetValue?: number;
  optionClassName?: string;

  getFacetValueForOption?: (facet: Facet, option: Option) => number;

  highlightUnder?: number;
  highlightUnderMax?: number;

  header: string;
}

const defaultGetFacetValueForOption = (facet: Facet, option: string | number) => facet[option];

export default class RangeFacetBase extends React.PureComponent<Props> {
  isSelected(option: Option): boolean {
    const { value } = this.props;

    return String(option) === String(value);
  }

  highlightUnder(option?: number): boolean {
    return (
      this.props.highlightUnder !== undefined &&
      option !== undefined &&
      option > this.props.highlightUnder &&
      (this.props.highlightUnderMax == null || option < this.props.highlightUnderMax)
    );
  }

  handleClick = (clicked: string) => {
    const { property, onQueryChange, value } = this.props;

    if (clicked === value?.toString()) {
      onQueryChange({ [property]: undefined });
    } else {
      onQueryChange({
        [property]: clicked,
      });
    }
  };

  renderOption(option: Option) {
    const {
      optionClassName,
      facet,
      getFacetValueForOption = defaultGetFacetValueForOption,
      maxFacetValue,
      value,
    } = this.props;
    const active = this.isSelected(option);

    const facetValue =
      facet && getFacetValueForOption ? getFacetValueForOption(facet, option) : undefined;

    const isUnderSelectedOption =
      typeof value === 'number' &&
      typeof option === 'number' &&
      this.highlightUnder(value) &&
      option > value;

    const statBarPercent =
      isDefined(facetValue) && isDefined(maxFacetValue) && maxFacetValue > 0
        ? facetValue / maxFacetValue
        : undefined;

    return (
      <FacetItem
        active={active}
        disableZero={false}
        aria-label={this.props.renderAccessibleLabel(option)}
        key={option}
        className={classNames(optionClassName)}
        data-key={option}
        onClick={this.handleClick}
        name={this.props.renderOption(option, this.isSelected(option) || isUnderSelectedOption)}
        stat={formatFacetStat(facetValue) ?? 0}
        statBarPercent={statBarPercent}
        value={option.toString()}
      />
    );
  }

  renderOptions = () => {
    const { options, header, highlightUnder } = this.props;

    if (options && options.length > 0) {
      if (highlightUnder != null) {
        const max = this.props.highlightUnderMax ?? options.length;
        const beforeHighlight = options.slice(0, highlightUnder);
        const insideHighlight = options.slice(highlightUnder, max);
        const afterHighlight = options.slice(max);

        return (
          <FacetItemsList label={header}>
            {beforeHighlight.map((option) => this.renderOption(option))}
            <HighlightedFacetItems>
              {insideHighlight.map((option) => this.renderOption(option))}
            </HighlightedFacetItems>
            {afterHighlight.map((option) => this.renderOption(option))}
          </FacetItemsList>
        );
      }

      return <ul>{options.map((option) => this.renderOption(option))}</ul>;
    }

    return (
      <LightLabel>
        <em>{translate('projects.facets.no_available_filters_clear_others')}</em>
      </LightLabel>
    );
  };

  render() {
    const { className, header, property } = this.props;

    return (
      <FacetBox className={className} name={header} data-key={property} open>
        {this.renderOptions()}
      </FacetBox>
    );
  }
}
