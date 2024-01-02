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
import { isSameDay } from 'date-fns';
import { BarChart, DateRangePicker, FacetBox, FacetItem } from 'design-system';
import { max } from 'lodash';
import * as React from 'react';
import { WrappedComponentProps, injectIntl } from 'react-intl';
import { longFormatterOption } from '../../../components/intl/DateFormatter';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { parseDate } from '../../../helpers/dates';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { MetricType } from '../../../types/metrics';
import { Component, Dict } from '../../../types/types';
import { Query } from '../utils';

interface Props {
  component: Component | undefined;
  createdAfter: Date | undefined;
  createdAfterIncludesTime: boolean;
  createdAt: string;
  createdBefore: Date | undefined;
  createdInLast: string;
  fetching: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  inNewCodePeriod: boolean;
  stats: Dict<number> | undefined;
}

export class CreationDateFacetClass extends React.PureComponent<Props & WrappedComponentProps> {
  property = 'createdAt';

  static defaultProps = {
    open: true,
  };

  hasValue = () =>
    this.props.createdAfter !== undefined ||
    this.props.createdAt.length > 0 ||
    this.props.createdBefore !== undefined ||
    this.props.createdInLast.length > 0 ||
    this.props.inNewCodePeriod;

  resetTo = (changes: Partial<Query>) => {
    this.props.onChange({
      createdAfter: undefined,
      createdAt: undefined,
      createdBefore: undefined,
      createdInLast: undefined,
      inNewCodePeriod: undefined,
      ...changes,
    });
  };

  handlePeriodChange = ({ from, to }: { from?: Date; to?: Date }) => {
    this.resetTo({ createdAfter: from, createdBefore: to });
  };

  handlePeriodClick = (period: string) => this.resetTo({ createdInLast: period });

  getCount() {
    const { createdAfter, createdAt, createdBefore, createdInLast } = this.props;

    let count = 0;

    if (createdInLast || createdAt) {
      count = 1;
    } else {
      if (createdAfter) {
        count++;
      }

      if (createdBefore) {
        count++;
      }
    }

    return count;
  }

  renderBarChart() {
    const { createdBefore, stats } = this.props;

    if (!stats) {
      return null;
    }

    const periods = Object.keys(stats);

    if (periods.length < 2 || periods.every((period) => !stats[period])) {
      return null;
    }

    const { formatDate } = this.props.intl;
    const data = periods.map((start, index) => {
      const startDate = parseDate(start);
      let endDate;
      if (index < periods.length - 1) {
        endDate = parseDate(periods[index + 1]);
        endDate.setDate(endDate.getDate() - 1);
      } else {
        endDate = createdBefore ? parseDate(createdBefore) : undefined;
      }

      const tooltipEndDate = endDate ?? new Date();
      const description = translateWithParameters(
        'issues.facet.createdAt.bar_description',
        formatMeasure(stats[start], MetricType.ShortInteger),
        formatDate(startDate, longFormatterOption),
        formatDate(tooltipEndDate, longFormatterOption),
      );
      let tooltip = `${formatMeasure(stats[start], MetricType.ShortInteger)} ${formatDate(
        startDate,
        longFormatterOption,
      )}`;
      if (!isSameDay(tooltipEndDate, startDate)) {
        tooltip += ` - ${formatDate(tooltipEndDate, longFormatterOption)}`;
      }

      return {
        createdAfter: startDate,
        createdBefore: endDate,
        description,
        tooltip,
        x: index,
        y: stats[start],
      };
    });

    const barsWidth = Math.floor(270 / data.length);
    const width = barsWidth * data.length - 1 + 10;

    const maxValue = max(data.map((d) => d.y));
    const xValues = data.map((d) =>
      d.y === maxValue ? formatMeasure(maxValue, MetricType.ShortInteger) : '',
    );

    return (
      <BarChart
        barsWidth={barsWidth - 1}
        data={data}
        height={75}
        onBarClick={this.resetTo}
        padding={[25, 0, 5, 10]}
        width={width}
        xValues={xValues}
      />
    );
  }

  renderPeriodSelectors() {
    const { createdAfter, createdBefore } = this.props;

    return (
      <DateRangePicker
        clearButtonLabel={translate('clear')}
        fromLabel={translate('start_date')}
        onChange={this.handlePeriodChange}
        separatorText={translate('to_')}
        toLabel={translate('end_date')}
        value={{ from: createdAfter, to: createdBefore }}
      />
    );
  }

  renderPredefinedPeriods() {
    const { createdInLast } = this.props;

    return (
      <div className="sw-flex sw-justify-start">
        <div className="sw-flex sw-gap-1 sw-mt-2">
          <FacetItem
            active={!this.hasValue()}
            className="it__search-navigator-facet"
            name={translate('issues.facet.createdAt.all')}
            onClick={this.handlePeriodClick}
            small
            tooltip={translate('issues.facet.createdAt.all')}
            value=""
          />

          <FacetItem
            active={createdInLast === '1w'}
            className="it__search-navigator-facet"
            name={translate('issues.facet.createdAt.last_week')}
            onClick={this.handlePeriodClick}
            small
            tooltip={translate('issues.facet.createdAt.last_week')}
            value="1w"
          />

          <FacetItem
            active={createdInLast === '1m'}
            className="it__search-navigator-facet"
            name={translate('issues.facet.createdAt.last_month')}
            onClick={this.handlePeriodClick}
            small
            tooltip={translate('issues.facet.createdAt.last_month')}
            value="1m"
          />

          <FacetItem
            active={createdInLast === '1y'}
            className="it__search-navigator-facet"
            name={translate('issues.facet.createdAt.last_year')}
            onClick={this.handlePeriodClick}
            small
            tooltip={translate('issues.facet.createdAt.last_year')}
            value="1y"
          />
        </div>
      </div>
    );
  }

  renderInner() {
    const { createdAfter, createdAfterIncludesTime, createdAt } = this.props;

    if (createdAt) {
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

    if (createdAfter && createdAfterIncludesTime) {
      return (
        <div className="search-navigator-facet-container">
          <strong>{translate('after')} </strong>
          <DateTimeFormatter date={createdAfter} />
        </div>
      );
    }

    return (
      <div>
        <div className="sw-flex sw-justify-center">{this.renderBarChart()}</div>

        {this.renderPeriodSelectors()}

        {this.renderPredefinedPeriods()}
      </div>
    );
  }

  render() {
    const { fetching, open } = this.props;

    const count = this.getCount();
    const headerId = `facet_${this.property}`;

    return (
      <FacetBox
        className="it__search-navigator-facet-box it__search-navigator-facet-header"
        clearIconLabel={translate('clear')}
        count={count}
        countLabel={translateWithParameters('x_selected', count)}
        data-property={this.property}
        id={headerId}
        loading={fetching}
        name={translate('issues.facet', this.property)}
        onClear={() => {
          this.resetTo({});
        }}
        onClick={() => {
          this.props.onToggle(this.property);
        }}
        open={open}
      >
        {this.renderInner()}
      </FacetBox>
    );
  }
}

export const CreationDateFacet = injectIntl(CreationDateFacetClass);
