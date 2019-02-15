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
import MetricSelect from './MetricSelect';
import ConditionOperator from './ConditionOperator';
import ThresholdInput from './ThresholdInput';
import { translate, getLocalizedMetricName } from '../../../helpers/l10n';
import { createCondition, updateCondition } from '../../../api/quality-gates';
import ConfirmModal from '../../../components/controls/ConfirmModal';
import { Alert } from '../../../components/ui/Alert';
import { getPossibleOperators } from '../utils';

interface Props {
  condition?: T.Condition;
  metric?: T.Metric;
  metrics?: T.Metric[];
  header: string;
  onAddCondition: (condition: T.Condition) => void;
  onClose: () => void;
  organization?: string;
  qualityGate: T.QualityGate;
}

interface State {
  error: string;
  errorMessage?: string;
  metric?: T.Metric;
  op?: string;
}

export default class ConditionModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      error: props.condition ? props.condition.error : '',
      metric: props.metric ? props.metric : undefined,
      op: props.condition ? props.condition.op : undefined
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getSinglePossibleOperator(metric: T.Metric) {
    const operators = getPossibleOperators(metric);
    return Array.isArray(operators) ? undefined : operators;
  }

  handleFormSubmit = () => {
    if (this.state.metric) {
      const { condition, qualityGate, organization } = this.props;
      const newCondition: T.Omit<T.Condition, 'id'> = {
        metric: this.state.metric.key,
        op: this.getSinglePossibleOperator(this.state.metric) || this.state.op,
        error: this.state.error
      };
      const submitPromise = condition
        ? updateCondition({ organization, id: condition.id, ...newCondition })
        : createCondition({ gateId: qualityGate.id, organization, ...newCondition });
      return submitPromise.then(this.props.onAddCondition);
    }
    return Promise.reject();
  };

  handleMetricChange = (metric: T.Metric) => {
    this.setState({ metric, op: undefined, error: '' });
  };

  handleOperatorChange = (op: string) => {
    this.setState({ op });
  };

  handleErrorChange = (error: string) => {
    this.setState({ error });
  };

  render() {
    const { header, metrics, onClose } = this.props;
    const { op, error, metric } = this.state;
    return (
      <ConfirmModal
        confirmButtonText={header}
        confirmDisable={metric === undefined}
        header={header}
        onClose={onClose}
        onConfirm={this.handleFormSubmit}
        size="small">
        {this.state.errorMessage && <Alert variant="error">{this.state.errorMessage}</Alert>}
        <div className="modal-field">
          <label htmlFor="condition-metric">{translate('quality_gates.conditions.metric')}</label>
          {metrics && <MetricSelect metrics={metrics} onMetricChange={this.handleMetricChange} />}
          {this.props.metric && (
            <span className="note">{getLocalizedMetricName(this.props.metric)}</span>
          )}
        </div>
        {metric && (
          <>
            <div className="modal-field display-inline-block">
              <label htmlFor="condition-operator">
                {translate('quality_gates.conditions.operator')}
              </label>
              <ConditionOperator
                metric={metric}
                onOperatorChange={this.handleOperatorChange}
                op={op}
              />
            </div>
            <div className="modal-field display-inline-block spacer-left">
              <label htmlFor="condition-threshold">
                {translate('quality_gates.conditions.error')}
              </label>
              <ThresholdInput
                metric={metric}
                name="error"
                onChange={this.handleErrorChange}
                value={error}
              />
            </div>
          </>
        )}
      </ConfirmModal>
    );
  }
}
