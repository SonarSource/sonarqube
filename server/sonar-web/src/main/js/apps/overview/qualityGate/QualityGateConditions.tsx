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
import * as React from 'react';
import { sortBy } from 'lodash';
import QualityGateCondition from './QualityGateCondition';
import { QualityGateStatusCondition, QualityGateStatusConditionEnhanced } from '../utils';
import { getMeasuresAndMeta } from '../../../api/measures';
import { enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import { isSameBranchLike, getBranchLikeQuery } from '../../../helpers/branches';

const LEVEL_ORDER = ['ERROR', 'WARN'];

interface Props {
  branchLike?: T.BranchLike;
  component: Pick<T.Component, 'key'>;
  conditions: QualityGateStatusCondition[];
}

interface State {
  conditions?: QualityGateStatusConditionEnhanced[];
  loading: boolean;
}

export default class QualityGateConditions extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    this.loadFailedMeasures();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      !isSameBranchLike(prevProps.branchLike, this.props.branchLike) ||
      prevProps.conditions !== this.props.conditions ||
      prevProps.component !== this.props.component
    ) {
      this.loadFailedMeasures();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadFailedMeasures() {
    const { branchLike, component, conditions } = this.props;
    const failedConditions = conditions.filter(c => c.level !== 'OK');
    if (failedConditions.length > 0) {
      const metrics = failedConditions.map(condition => condition.metric);
      getMeasuresAndMeta(component.key, metrics, {
        additionalFields: 'metrics',
        ...getBranchLikeQuery(branchLike)
      }).then(
        ({ component, metrics }) => {
          if (this.mounted) {
            const measures = enhanceMeasuresWithMetrics(component.measures || [], metrics || []);
            this.setState({
              conditions: enhanceConditions(failedConditions, measures),
              loading: false
            });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ loading: false });
          }
        }
      );
    } else {
      this.setState({ loading: false });
    }
  }

  render() {
    const { branchLike, component } = this.props;
    const { loading, conditions } = this.state;

    if (loading || !conditions) {
      return null;
    }

    const sortedConditions = sortBy(
      conditions,
      condition => LEVEL_ORDER.indexOf(condition.level),
      condition => condition.measure.metric.name
    );

    return (
      <div
        className="overview-quality-gate-conditions-list clearfix"
        id="overview-quality-gate-conditions-list">
        {sortedConditions.map(condition => (
          <QualityGateCondition
            branchLike={branchLike}
            component={component}
            condition={condition}
            key={condition.measure.metric.key}
          />
        ))}
      </div>
    );
  }
}

function enhanceConditions(
  conditions: QualityGateStatusCondition[],
  measures: T.MeasureEnhanced[]
): QualityGateStatusConditionEnhanced[] {
  return conditions.map(condition => {
    const measure = measures.find(measure => measure.metric.key === condition.metric)!;
    return { ...condition, measure };
  });
}
