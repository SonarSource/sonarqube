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
// @flow
import React from 'react';
import classNames from 'classnames';
import moment from 'moment';
import { max } from 'lodash';
import FacetBox from './components/FacetBox';
import FacetHeader from './components/FacetHeader';
import { BarChart } from '../../../components/charts/bar-chart';
import DateInput from '../../../components/controls/DateInput';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import type { Component } from '../utils';

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

const DATE_FORMAT = 'YYYY-MM-DDTHH:mm:ssZZ';

export default class CreationDateFacet extends React.PureComponent {
  props: Props;

  static defaultProps = {
    open: true
  };

  property = 'createdAt';

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  resetTo = (changes: {}) => {
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
    { createdAfter, createdBefore }: { createdAfter: Object, createdBefore?: Object }
  ) => {
    this.resetTo({
      createdAfter: createdAfter.format(DATE_FORMAT),
      createdBefore: createdBefore && createdBefore.format(DATE_FORMAT)
    });
  };

  handlePeriodChange = (property: string) =>
    (value: string) => {
      this.props.onChange({
        createdAt: undefined,
        createdInLast: undefined,
        sinceLeakPeriod: undefined,
        [property]: value
      });
    };

  handlePeriodClick = (period?: string) =>
    (e: Event & { target: HTMLElement }) => {
      e.preventDefault();
      e.target.blur;
      this.resetTo({ createdInLast: period });
    };

  handleLeakPeriodClick = () =>
    (e: Event & { target: HTMLElement }) => {
      e.preventDefault();
      e.target.blur;
      this.resetTo({ sinceLeakPeriod: true });
    };

  renderBarChart() {
    const { createdBefore, stats } = this.props;

    if (!stats) {
      return null;
    }

    const periods = Object.keys(stats);

    if (periods.length < 2) {
      return null;
    }

    const data = periods.map((startDate, index) => {
      const startMoment = moment(startDate);
      const nextStartMoment = index < periods.length - 1
        ? moment(periods[index + 1])
        : createdBefore ? moment(createdBefore) : undefined;
      const endMoment = nextStartMoment && nextStartMoment.clone().subtract(1, 'days');

      let tooltip = formatMeasure(stats[startDate], 'SHORT_INT') +
        '<br>' +
        startMoment.format('LL');

      if (endMoment) {
        const isSameDay = endMoment.diff(startMoment, 'days') <= 1;
        if (!isSameDay) {
          tooltip += ' – ' + endMoment.format('LL');
        }
      }

      return {
        createdAfter: startMoment,
        createdBefore: nextStartMoment,
        startMoment,
        tooltip,
        x: index,
        y: stats[startDate]
      };
    });

    const barsWidth = Math.floor(240 / data.length);
    const width = barsWidth * data.length - 1 + 20;

    const maxValue = max(data.map(d => d.y));
    const format = this.props.facetMode === 'count' ? 'SHORT_INT' : 'SHORT_WORK_DUR';
    const xValues = data.map(d => d.y === maxValue ? formatMeasure(maxValue, format) : '');

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
    const m = moment(this.props.createdAt);
    return (
      <div className="search-navigator-facet-container">
        {m.format('LLL')}
        <br />
        <span className="note">({m.fromNow()})</span>
      </div>
    );
  }

  renderPeriodSelectors() {
    const { createdAfter, createdBefore } = this.props;

    return (
      <div className="search-navigator-date-facet-selection">
        <DateInput
          className="search-navigator-date-facet-selection-dropdown-left"
          onChange={this.handlePeriodChange('createdAfter')}
          placeholder={translate('from')}
          value={createdAfter ? moment(createdAfter).format('YYYY-MM-DD') : undefined}
        />
        <DateInput
          className="search-navigator-date-facet-selection-dropdown-right"
          onChange={this.handlePeriodChange('createdBefore')}
          placeholder={translate('to')}
          value={createdBefore ? moment(createdBefore).format('YYYY-MM-DD') : undefined}
        />
      </div>
    );
  }

  renderPrefefinedPeriods() {
    const { component, createdInLast, sinceLeakPeriod } = this.props;
    return (
      <div className="spacer-top">
        <span className="spacer-right">{translate('issues.facet.createdAt.or')}</span>
        <a className="spacer-right" href="#" onClick={this.handlePeriodClick()}>
          {translate('issues.facet.createdAt.all')}
        </a>
        {component == null &&
          <a
            className={classNames('spacer-right', { 'active-link': createdInLast === '1w' })}
            href="#"
            onClick={this.handlePeriodClick('1w')}>
            {translate('issues.facet.createdAt.last_week')}
          </a>}
        {component == null &&
          <a
            className={classNames('spacer-right', { 'active-link': createdInLast === '1m' })}
            href="#"
            onClick={this.handlePeriodClick('1m')}>
            {translate('issues.facet.createdAt.last_month')}
          </a>}
        {component == null &&
          <a
            className={classNames('spacer-right', { 'active-link': createdInLast === '1y' })}
            href="#"
            onClick={this.handlePeriodClick('1y')}>
            {translate('issues.facet.createdAt.last_year')}
          </a>}
        {component != null &&
          <a
            className={classNames('spacer-right', { 'active-link': sinceLeakPeriod })}
            href="#"
            onClick={this.handleLeakPeriodClick()}>
            {translate('issues.leak_period')}
          </a>}
      </div>
    );
  }

  renderInner() {
    const { createdAt } = this.props;
    return createdAt
      ? this.renderExactDate()
      : <div>
          {this.renderBarChart()}
          {this.renderPeriodSelectors()}
          {this.renderPrefefinedPeriods()}
        </div>;
  }

  render() {
    const hasValue = this.props.createdAfter.length > 0 ||
      this.props.createdAt.length > 0 ||
      this.props.createdBefore.length > 0 ||
      this.props.createdInLast.length > 0 ||
      this.props.sinceLeakPeriod;

    const { stats } = this.props;

    if (!stats) {
      return null;
    }

    return (
      <FacetBox property={this.property}>
        <FacetHeader
          hasValue={hasValue}
          name={translate('issues.facet', this.property)}
          onClick={this.handleHeaderClick}
          open={this.props.open}
        />

        {this.props.open && this.renderInner()}
      </FacetBox>
    );
  }
}
