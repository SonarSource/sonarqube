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
import * as React from 'react';
import { max } from 'lodash';
import { intlShape } from 'react-intl';
import { Query } from '../utils';
import { Component } from '../../../app/types';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import { longFormatterOption } from '../../../components/intl/DateFormatter';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import BarChart from '../../../components/charts/BarChart';
import DateRangeInput from '../../../components/controls/DateRangeInput';
import { isSameDay, parseDate } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';

interface Props {
  component: Component | undefined;
  createdAfter: Date | undefined;
  createdAt: string;
  createdBefore: Date | undefined;
  createdInLast: string;
  facetMode: string;
  loading?: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  sinceLeakPeriod: boolean;
  stats: { [x: string]: number } | undefined;
}

export default class CreationDateFacet extends React.PureComponent<Props> {
  property = 'createdAt';

  static defaultProps = {
    open: true
  };

  static contextTypes = {
    intl: intlShape
  };

  hasValue = () =>
    this.props.createdAfter !== undefined ||
    this.props.createdAt.length > 0 ||
    this.props.createdBefore !== undefined ||
    this.props.createdInLast.length > 0 ||
    this.props.sinceLeakPeriod;

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleClear = () => {
    this.resetTo({});
  };

  resetTo = (changes: Partial<Query>) => {
    this.props.onChange({
      createdAfter: undefined,
      createdAt: undefined,
      createdBefore: undefined,
      createdInLast: undefined,
      sinceLeakPeriod: undefined,
      ...changes
    });
  };

  handleBarClick = ({
    createdAfter,
    createdBefore
  }: {
    createdAfter: Date;
    createdBefore?: Date;
  }) => {
    this.resetTo({ createdAfter, createdBefore });
  };

  handlePeriodChange = ({ from, to }: { from?: Date; to?: Date }) => {
    this.resetTo({ createdAfter: from, createdBefore: to });
  };

  handlePeriodClick = (period: string) => this.resetTo({ createdInLast: period });

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
        endDate = createdBefore ? parseDate(createdBefore) : undefined;
      }

      const tooltipEndDate = endDate || new Date();
      const tooltip = (
        <React.Fragment>
          {formatMeasure(stats[start], 'SHORT_INT')}
          <br />
          {formatDate(startDate, longFormatterOption)}
          {!isSameDay(tooltipEndDate, startDate) &&
            ` - ${formatDate(tooltipEndDate, longFormatterOption)}`}
        </React.Fragment>
      );

      return {
        createdAfter: startDate,
        createdBefore: endDate,
        tooltip,
        x: index,
        y: this.props.loading ? 0 : stats[start]
      };
    });

    const barsWidth = Math.floor(250 / data.length);
    const width = barsWidth * data.length - 1 + 10;

    const maxValue = max(data.map(d => d.y));
    const format = this.props.facetMode === 'count' ? 'SHORT_INT' : 'SHORT_WORK_DUR';
    const xValues = data.map(d => (d.y === maxValue ? formatMeasure(maxValue, format) : ''));

    return (
      <BarChart
        barsWidth={barsWidth - 1}
        data={data}
        height={75}
        onBarClick={this.handleBarClick}
        padding={[25, 0, 5, 10]}
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
        <DateRangeInput
          onChange={this.handlePeriodChange}
          value={{ from: createdAfter, to: createdBefore }}
        />
      </div>
    );
  }

  renderPredefinedPeriods() {
    const { component, createdInLast, sinceLeakPeriod } = this.props;
    return (
      <div className="spacer-top issues-predefined-periods">
        <FacetItem
          active={!this.hasValue()}
          loading={this.props.loading}
          name={translate('issues.facet.createdAt.all')}
          onClick={this.handlePeriodClick}
          value=""
        />
        {component ? (
          <FacetItem
            active={sinceLeakPeriod}
            loading={this.props.loading}
            name={translate('issues.leak_period')}
            onClick={this.handleLeakPeriodClick}
            value=""
          />
        ) : (
          <>
            <FacetItem
              active={createdInLast === '1w'}
              loading={this.props.loading}
              name={translate('issues.facet.createdAt.last_week')}
              onClick={this.handlePeriodClick}
              value="1w"
            />
            <FacetItem
              active={createdInLast === '1m'}
              loading={this.props.loading}
              name={translate('issues.facet.createdAt.last_month')}
              onClick={this.handlePeriodClick}
              value="1m"
            />
            <FacetItem
              active={createdInLast === '1y'}
              loading={this.props.loading}
              name={translate('issues.facet.createdAt.last_year')}
              onClick={this.handlePeriodClick}
              value="1y"
            />
          </>
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
