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
import * as isSameDay from 'date-fns/is_same_day';
import { max } from 'lodash';
import * as React from 'react';
import { InjectedIntlProps, injectIntl } from 'react-intl';
import BarChart from 'sonar-ui-common/components/charts/BarChart';
import { parseDate } from 'sonar-ui-common/helpers/dates';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import DateRangeInput from '../../../components/controls/DateRangeInput';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import { longFormatterOption } from '../../../components/intl/DateFormatter';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { Query } from '../utils';

interface Props {
  component: T.Component | undefined;
  createdAfter: Date | undefined;
  createdAt: string;
  createdBefore: Date | undefined;
  createdInLast: string;
  fetching: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  sinceLeakPeriod: boolean;
  stats: T.Dict<number> | undefined;
}

class CreationDateFacet extends React.PureComponent<Props & InjectedIntlProps> {
  property = 'createdAt';

  static defaultProps = {
    open: true
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
    const { formatDate } = this.props.intl;
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
      values.push(translate('issues.new_code'));
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
        y: stats[start]
      };
    });

    const barsWidth = Math.floor(250 / data.length);
    const width = barsWidth * data.length - 1 + 10;

    const maxValue = max(data.map(d => d.y));
    const xValues = data.map(d => (d.y === maxValue ? formatMeasure(maxValue, 'SHORT_INT') : ''));

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
          name={translate('issues.facet.createdAt.all')}
          onClick={this.handlePeriodClick}
          tooltip={translate('issues.facet.createdAt.all')}
          value=""
        />
        {component ? (
          <FacetItem
            active={sinceLeakPeriod}
            name={translate('issues.new_code')}
            onClick={this.handleLeakPeriodClick}
            tooltip={translate('issues.new_code_period')}
            value=""
          />
        ) : (
          <>
            <FacetItem
              active={createdInLast === '1w'}
              name={translate('issues.facet.createdAt.last_week')}
              onClick={this.handlePeriodClick}
              tooltip={translate('issues.facet.createdAt.last_week')}
              value="1w"
            />
            <FacetItem
              active={createdInLast === '1m'}
              name={translate('issues.facet.createdAt.last_month')}
              onClick={this.handlePeriodClick}
              tooltip={translate('issues.facet.createdAt.last_month')}
              value="1m"
            />
            <FacetItem
              active={createdInLast === '1y'}
              name={translate('issues.facet.createdAt.last_year')}
              onClick={this.handlePeriodClick}
              tooltip={translate('issues.facet.createdAt.last_year')}
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
          fetching={this.props.fetching}
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

export default injectIntl(CreationDateFacet);
