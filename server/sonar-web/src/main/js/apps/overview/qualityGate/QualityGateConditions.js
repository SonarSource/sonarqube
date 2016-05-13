/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import shallowCompare from 'react-addons-shallow-compare';
import sortBy from 'lodash/sortBy';

import QualityGateCondition from './QualityGateCondition';
import { ComponentType, ConditionsListType } from '../propTypes';
import { getMeasuresAndMeta } from '../../../api/measures';
import { enhanceMeasuresWithMetrics } from '../../../helpers/measures';

const LEVEL_ORDER = ['ERROR', 'WARN'];

function enhanceConditions (conditions, measures) {
  return conditions.map(c => {
    const measure = measures.find(measure => measure.metric.key === c.metric);
    return { ...c, measure };
  });
}

export default class QualityGateConditions extends React.Component {
  static propTypes = {
    component: ComponentType.isRequired,
    conditions: ConditionsListType.isRequired
  };

  state = {
    loading: true
  };

  componentDidMount () {
    this.mounted = true;
    this.loadFailedMeasures(this.props);
  }

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  componentDidUpdate (nextProps) {
    if (nextProps.conditions !== this.props.conditions ||
        nextProps.component !== this.props.component) {
      this.loadFailedMeasures(nextProps);
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  loadFailedMeasures (props) {
    const { component, conditions } = props;
    const failedConditions = conditions.filter(c => c.level !== 'OK');
    const metrics = failedConditions.map(condition => condition.metric);

    getMeasuresAndMeta(
        component.key,
        metrics,
        { additionalFields: 'metrics' }
    ).then(r => {
      if (this.mounted) {
        const measures = enhanceMeasuresWithMetrics(r.component.measures, r.metrics);
        this.setState({
          conditions: enhanceConditions(failedConditions, measures),
          loading: false
        });
      }
    });
  }

  render () {
    const { component, periods } = this.props;
    const { loading, conditions } = this.state;

    if (loading) {
      return null;
    }

    const sortedConditions = sortBy(
        conditions,
        condition => LEVEL_ORDER.indexOf(condition.level),
        condition => condition.metric.name
    );

    return (
        <ul
            className="overview-quality-gate-conditions-list"
            id="overview-quality-gate-conditions-list">
          {sortedConditions.map(condition => (
              <QualityGateCondition
                  key={condition.measure.metric.key}
                  component={component}
                  periods={periods}
                  condition={condition}/>
          ))}
        </ul>
    );
  }
}
