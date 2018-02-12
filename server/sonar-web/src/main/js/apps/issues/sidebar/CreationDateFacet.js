/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import { max } from 'lodash';
import { intlShape } from 'react-intl';
import DateFromNow from '../../../components/intl/DateFromNow';
import { longFormatterOption } from '../../../components/intl/DateFormatter';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import { BarChart } from '../../../components/charts/bar-chart';
import DateInput from '../../../components/controls/DateInput';
import { isSameDay, parseDate, toShortNotSoISOString } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
/*:: import type { Component } from '../utils'; */

/*::
type Props = {|
  component?: Component,
  createdAfter: string,
  createdAt: string,
  createdBefore: string,
  createdInLast: string,
  facetMode: string,
  onChange: (changes: {}) => void,
  onToggle: (property: string) => void,
  open: boolean,
  sinceLeakPeriod: boolean,
  stats?: { [string]: number }
|};
*/

export default class CreationDateFacet extends React.PureComponent {
  /*:: props: Props; */

  property = 'createdAt';

  static defaultProps = {
    open: true
  };

  static contextTypes = {
    intl: intlShape
  };

  hasValue = () =>
    this.props.createdAfter.length > 0 ||
    this.props.createdAt.length > 0 ||
    this.props.createdBefore.length > 0 ||
    this.props.createdInLast.length > 0 ||
    this.props.sinceLeakPeriod;

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleClear = () => {
    this.resetTo({});
  };

  resetTo = (changes /*: {} */) => {
    this.props.onChange({
      createdAfter: undefined,
      createdAt: undefined,
      createdBefore: undefined,
      createdInLast: undefined,
      sinceLeakPeriod: undefined,
      ...changes
    });
  };

  handleBarClick = (
    { createdAfter, createdBefore } /*: {
    createdAfter: Date,
    createdBefore?: Date
  } */
  ) => {
    this.resetTo({
      createdAfter: toShortNotSoISOString(createdAfter),
      createdBefore: createdBefore && toShortNotSoISOString(createdBefore)
    });
  };

  handlePeriodChange = (property /*: string */, value /*: string */) => {
    this.props.onChange({
      createdAt: undefined,
      createdInLast: undefined,
      sinceLeakPeriod: undefined,
      [property]: value ? toShortNotSoISOString(parseDate(value)) : undefined
    });
  };

  handlePeriodChangeBefore = (value /*: string */) =>
    this.handlePeriodChange('createdBefore', value);

  handlePeriodChangeAfter = (value /*: string */) => this.handlePeriodChange('createdAfter', value);

  handlePeriodClick = (period /*: string */) => this.resetTo({ createdInLast: period });

  handleLeakPeriodClick = () => this.resetTo({ sinceLeakPeriod: true });

  getValues() {
    const { createdAfter, createdAt, createdBefore, createdInLast, sinceLeakPeriod } = this.props;
    const { formatDate } = this.context.intl;
    const values = [];
    if (createdAfter) {
      values.push(formatDate(createdAfter, longFormatterOption));
    }
    if (createdAt) {
      values.push(formatDate(createdAt, longFormatterOption));
    }
    if (createdBefore) {
      values.push(formatDate(createdBefore, longFormatterOption));
    }
    if (createdInLast === '1w') {
      values.push(translate('issues.facet.createdAt.last_week'));
    }
    if (createdInLast === '1m') {
      values.push(translate('issues.facet.createdAt.last_month'));
    }
    if (createdInLast === '1y') {
      values.push(translate('issues.facet.createdAt.last_year'));
    }
    if (sinceLeakPeriod) {
      values.push(translate('issues.leak_period'));
    }
    return values;
  }

  renderBarChart() {
    const { createdBefore, stats } = this.props;

    if (!stats) {
      return null;
    }

    const periods = Object.keys(stats);

    if (periods.length < 2 || periods.every(period => !stats[period])) {
      return null;
    }

    const { formatDate } = this.context.intl;
    const data = periods.map((start, index) => {
      const startDate = parseDate(start);
      let endDate;
      if (index < periods.length - 1) {
        endDate = parseDate(periods[index + 1]);
        endDate.setDate(endDate.getDate() - 1);
      } else {
        endDate = createdBefore && parseDate(createdBefore);
      }

      let tooltip =
        formatMeasure(stats[start], 'SHORT_INT') +
        '<br/>' +
        formatDate(startDate, longFormatterOption);
      const tooltipEndDate = endDate || Date.now();
      if (!isSameDay(tooltipEndDate, startDate)) {
        tooltip += ' – ' + formatDate(tooltipEndDate, longFormatterOption);
      }

      return {
        createdAfter: startDate,
        createdBefore: endDate,
        tooltip,
        x: index,
        y: stats[start]
      };
    });

    const barsWidth = Math.floor(240 / data.length);
    const width = barsWidth * data.length - 1 + 20;

    const maxValue = max(data.map(d => d.y));
    const format = this.props.facetMode === 'count' ? 'SHORT_INT' : 'SHORT_WORK_DUR';
    const xValues = data.map(d => (d.y === maxValue ? formatMeasure(maxValue, format) : ''));

    return (
      <BarChart
        barsWidth={barsWidth - 1}
        data={data}
        height={75}
        onBarClick={this.handleBarClick}
        padding={[25, 10, 5, 10]}
        width={width}
        xValues={xValues}
      />
    );
  }

  renderExactDate() {
    return (
      <div className="search-navigator-facet-container">
        <DateTimeFormatter date={this.props.createdAt} />
        <br />
        <span className="note">
          <DateFromNow date={this.props.createdAt} />
        </span>
      </div>
    );
  }

  renderPeriodSelectors() {
    const { createdAfter, createdBefore } = this.props;
    return (
      <div className="search-navigator-date-facet-selection">
        <DateInput
          className="search-navigator-date-facet-selection-dropdown-left"
          inputClassName="search-navigator-date-facet-selection-input"
          maxDate={createdBefore ? toShortNotSoISOString(createdBefore) : '+0'}
          onChange={this.handlePeriodChangeAfter}
          placeholder={translate('from')}
          value={createdAfter ? toShortNotSoISOString(createdAfter) : undefined}
        />
        <DateInput
          className="search-navigator-date-facet-selection-dropdown-right"
          inputClassName="search-navigator-date-facet-selection-input"
          minDate={createdAfter ? toShortNotSoISOString(createdAfter) : undefined}
          onChange={this.handlePeriodChangeBefore}
          placeholder={translate('to')}
          value={createdBefore ? toShortNotSoISOString(createdBefore) : undefined}
        />
      </div>
    );
  }

  renderPredefinedPeriods() {
    const { component, createdInLast, sinceLeakPeriod } = this.props;
    if (component != null && component.branch != null) {
      // FIXME handle long-living branches
      return null;
    }
    return (
      <div className="spacer-top issues-predefined-periods">
        <FacetItem
          active={!this.hasValue()}
          name={translate('issues.facet.createdAt.all')}
          onClick={this.handlePeriodClick}
          value=""
        />
        {component == null && (
          <FacetItem
            active={createdInLast === '1w'}
            name={translate('issues.facet.createdAt.last_week')}
            onClick={this.handlePeriodClick}
            value="1w"
          />
        )}
        {component == null && (
          <FacetItem
            active={createdInLast === '1m'}
            name={translate('issues.facet.createdAt.last_month')}
            onClick={this.handlePeriodClick}
            value="1m"
          />
        )}
        {component == null && (
          <FacetItem
            active={createdInLast === '1y'}
            name={translate('issues.facet.createdAt.last_year')}
            onClick={this.handlePeriodClick}
            value="1y"
          />
        )}
        {component != null && (
          <FacetItem
            active={sinceLeakPeriod}
            name={translate('issues.leak_period')}
            onClick={this.handleLeakPeriodClick}
            value=""
          />
        )}
      </div>
    );
  }

  renderInner() {
    const { createdAt } = this.props;
    return createdAt ? (
      this.renderExactDate()
    ) : (
      <div>
        {this.renderBarChart()}
        {this.renderPeriodSelectors()}
        {this.renderPredefinedPeriods()}
      </div>
    );
  }

  render() {
    return (
      <FacetBox property={this.property}>
        <FacetHeader
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={this.getValues()}
        />

        {this.props.open && this.renderInner()}
      </FacetBox>
    );
  }
}
