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
import { max } from 'lodash';
import * as React from 'react';
import { injectIntl, WrappedComponentProps } from 'react-intl';
import BarChart from '../../../components/charts/BarChart';
import DateRangeInput from '../../../components/controls/DateRangeInput';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import { longFormatterOption } from '../../../components/intl/DateFormatter';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter, {
  formatterOption as dateTimeFormatterOption,
} from '../../../components/intl/DateTimeFormatter';
import { parseDate } from '../../../helpers/dates';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
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

export class CreationDateFacet extends React.PureComponent<Props & WrappedComponentProps> {
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
      inNewCodePeriod: undefined,
      ...changes,
    });
  };

  handleBarClick = ({
    createdAfter,
    createdBefore,
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

  getValues() {
    const { createdAfter, createdAfterIncludesTime, createdAt, createdBefore, createdInLast } =
      this.props;
    const { formatDate } = this.props.intl;
    const values = [];
    if (createdAfter) {
      values.push(
        formatDate(
          createdAfter,
          createdAfterIncludesTime ? dateTimeFormatterOption : longFormatterOption
        )
      );
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
    return values;
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

      const tooltipEndDate = endDate || new Date();
      const tooltip = (
        // eslint-disable-next-line react/jsx-fragments
        <React.Fragment>
          {formatMeasure(stats[start], 'SHORT_INT')}
          <br />
          {formatDate(startDate, longFormatterOption)}
          {!isSameDay(tooltipEndDate, startDate) &&
            ` - ${formatDate(tooltipEndDate, longFormatterOption)}`}
        </React.Fragment>
      );
      const description = translateWithParameters(
        'issues.facet.createdAt.bar_description',
        formatMeasure(stats[start], 'SHORT_INT'),
        formatDate(startDate, longFormatterOption),
        formatDate(tooltipEndDate, longFormatterOption)
      );

      return {
        createdAfter: startDate,
        createdBefore: endDate,
        tooltip,
        description,
        x: index,
        y: stats[start],
      };
    });

    const barsWidth = Math.floor(250 / data.length);
    const width = barsWidth * data.length - 1 + 10;

    const maxValue = max(data.map((d) => d.y));
    const xValues = data.map((d) => (d.y === maxValue ? formatMeasure(maxValue, 'SHORT_INT') : ''));

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

  renderPeriodSelectors() {
    const { createdAfter, createdBefore } = this.props;
    return (
      <div className="search-navigator-date-facet-selection">
        <DateRangeInput
          alignEndDateCalandarRight={true}
          onChange={this.handlePeriodChange}
          value={{ from: createdAfter, to: createdBefore }}
        />
      </div>
    );
  }

  renderPredefinedPeriods() {
    const { createdInLast } = this.props;
    return (
      <div className="spacer-top issues-predefined-periods">
        <FacetItem
          active={!this.hasValue()}
          name={translate('issues.facet.createdAt.all')}
          onClick={this.handlePeriodClick}
          tooltip={translate('issues.facet.createdAt.all')}
          value=""
        />

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
