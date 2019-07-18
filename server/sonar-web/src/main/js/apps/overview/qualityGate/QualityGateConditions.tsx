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
import { sortBy } from 'lodash';
import * as React from 'react';
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import ChevronDownIcon from 'sonar-ui-common/components/icons/ChevronDownIcon';
import { translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { getMeasuresAndMeta } from '../../../api/measures';
import { getBranchLikeQuery, isSameBranchLike } from '../../../helpers/branches';
import { enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import QualityGateCondition from './QualityGateCondition';

const LEVEL_ORDER = ['ERROR', 'WARN'];

interface Props {
  branchLike?: T.BranchLike;
  component: Pick<T.Component, 'key'>;
  collapsible?: boolean;
  conditions: T.QualityGateStatusCondition[];
}

interface State {
  collapsed: boolean;
  conditions?: T.QualityGateStatusConditionEnhanced[];
  loading: boolean;
}

const MAX_CONDITIONS = 5;

export default class QualityGateConditions extends React.PureComponent<Props, State> {
  mounted = false;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      collapsed: Boolean(props.collapsible),
      loading: true
    };
  }

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

  enhanceConditions = (
    conditions: T.QualityGateStatusCondition[],
    measures: T.MeasureEnhanced[]
  ): T.QualityGateStatusConditionEnhanced[] => {
    return conditions.map(condition => {
      const measure = measures.find(measure => measure.metric.key === condition.metric)!;
      return { ...condition, measure };
    });
  };

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
              conditions: this.enhanceConditions(failedConditions, measures),
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

  handleToggleCollapse = () => {
    this.setState(state => ({
      collapsed: !state.collapsed
    }));
  };

  render() {
    const { branchLike, component } = this.props;
    const { loading, collapsed, conditions } = this.state;

    if (loading || !conditions) {
      return null;
    }

    const sortedConditions = sortBy(conditions, condition => LEVEL_ORDER.indexOf(condition.level));

    let renderConditions;
    let renderCollapsed;
    if (collapsed && sortedConditions.length > MAX_CONDITIONS) {
      renderConditions = sortedConditions.slice(0, MAX_CONDITIONS);
      renderCollapsed = true;
    } else {
      renderConditions = sortedConditions;
      renderCollapsed = false;
    }

    return (
      <div
        className="overview-quality-gate-conditions-list clearfix"
        id="overview-quality-gate-conditions-list">
        {renderConditions.map(condition => (
          <QualityGateCondition
            branchLike={branchLike}
            component={component}
            condition={condition}
            key={condition.measure.metric.key}
          />
        ))}
        {renderCollapsed && (
          <ButtonLink
            className="overview-quality-gate-conditions-list-collapse"
            onClick={this.handleToggleCollapse}>
            {translateWithParameters(
              'overview.X_more_failed_conditions',
              sortedConditions.length - MAX_CONDITIONS
            )}
            <ChevronDownIcon className="little-spacer-left" />
          </ButtonLink>
        )}
      </div>
    );
  }
}
