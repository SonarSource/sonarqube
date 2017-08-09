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
import { Link } from 'react-router';
import ComplexityDistribution from '../../../components/shared/ComplexityDistribution';
import HistoryIcon from '../../../components/icons-components/HistoryIcon';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';
import LanguageDistribution from '../../../components/charts/LanguageDistribution';
import LeakPeriodLegend from './LeakPeriodLegend';
import Measure from '../../../components/measure/Measure';
import Tooltip from '../../../components/controls/Tooltip';
import { isFileType } from '../utils';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../../helpers/l10n';
import { getComponentMeasureHistory } from '../../../helpers/urls';
import { isDiffMetric } from '../../../helpers/measures';
import type { Component, Period } from '../types';
import type { MeasureEnhanced } from '../../../components/measure/types';

type Props = {|
  component: Component,
  components: Array<Component>,
  leakPeriod?: Period,
  handleSelect: string => void,
  measure: MeasureEnhanced,
  secondaryMeasure: ?MeasureEnhanced,
  selectedIdx: ?number
|};

export default class MeasureHeader extends React.PureComponent {
  props: Props;

  handleSelectPrevious = (e: Event & { target: HTMLElement }) => {
    e.target.blur();
    if (this.props.selectedIdx != null) {
      const prevComponent = this.props.components[this.props.selectedIdx - 1];
      if (prevComponent) {
        this.props.handleSelect(prevComponent.key);
      }
    }
  };

  handleSelectNext = (e: Event & { target: HTMLElement }) => {
    e.target.blur();
    if (this.props.selectedIdx != null) {
      const prevComponent = this.props.components[this.props.selectedIdx + 1];
      if (prevComponent) {
        this.props.handleSelect(prevComponent.key);
      }
    }
  };

  renderFileNav() {
    const { components, selectedIdx } = this.props;
    if (selectedIdx == null) {
      return null;
    }
    const hasPrevious = selectedIdx > 0;
    const hasNext = selectedIdx < components.length - 1;
    return (
      <div className="display-inline-block">
        {components.length > 0 &&
          <span className="note spacer-right">
            {translateWithParameters(
              'component_measures.x_of_y',
              selectedIdx + 1,
              components.length
            )}
          </span>}
        <div className="button-group">
          {hasPrevious && <button onClick={this.handleSelectPrevious}>&lt;</button>}
          {hasNext && <button onClick={this.handleSelectNext}>&gt;</button>}
        </div>
      </div>
    );
  }

  render() {
    const { component, components, leakPeriod, measure, secondaryMeasure } = this.props;
    const metric = measure.metric;
    const isDiff = isDiffMetric(metric.key);
    const hasHistory = ['TRK', 'VW', 'SVW', 'APP'].includes(component.qualifier);
    const hasComponents = components && components.length > 1;
    return (
      <div className="measure-details-header big-spacer-bottom">
        <div className="measure-details-primary">
          <div className="measure-details-metric">
            <IssueTypeIcon query={metric.key} className="little-spacer-right text-text-bottom" />
            {getLocalizedMetricName(metric)}
            <span className="measure-details-value spacer-left">
              <strong>
                {isDiff
                  ? <Measure className="domain-measures-leak" measure={measure} metric={metric} />
                  : <Measure measure={measure} metric={metric} />}
              </strong>
            </span>
            {!isDiff &&
              hasHistory &&
              <Tooltip
                placement="right"
                overlay={translate('component_measures.show_metric_history')}>
                <Link
                  className="js-show-history spacer-left button button-small button-compact"
                  to={getComponentMeasureHistory(component.key, metric.key)}>
                  <HistoryIcon />
                </Link>
              </Tooltip>}
          </div>
          <div className="measure-details-primary-actions">
            {hasComponents && isFileType(component) && this.renderFileNav()}
            {leakPeriod != null &&
              <LeakPeriodLegend
                className="spacer-left"
                component={component}
                period={leakPeriod}
              />}
          </div>
        </div>
        {secondaryMeasure &&
          secondaryMeasure.metric.key === 'ncloc_language_distribution' &&
          <div className="measure-details-secondary">
            <LanguageDistribution alignTicks={true} distribution={secondaryMeasure.value} />
          </div>}
        {secondaryMeasure &&
          secondaryMeasure.metric.key === 'function_complexity_distribution' &&
          <div className="measure-details-secondary">
            <ComplexityDistribution distribution={secondaryMeasure.value} of="function" />
          </div>}
        {secondaryMeasure &&
          secondaryMeasure.metric.key === 'file_complexity_distribution' &&
          <div className="measure-details-secondary">
            <ComplexityDistribution distribution={secondaryMeasure.value} of="file" />
          </div>}
      </div>
    );
  }
}
