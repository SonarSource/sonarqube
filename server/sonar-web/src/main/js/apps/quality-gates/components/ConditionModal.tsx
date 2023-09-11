/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { ButtonPrimary, FlagMessage, FormField, Modal, RadioButton } from 'design-system';
import * as React from 'react';
import { createCondition, updateCondition } from '../../../api/quality-gates';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { Condition, Metric, QualityGate } from '../../../types/types';
import { getPossibleOperators } from '../utils';
import ConditionOperator from './ConditionOperator';
import MetricSelect from './MetricSelect';
import ThresholdInput from './ThresholdInput';

interface Props {
  condition?: Condition;
  metric?: Metric;
  metrics?: Metric[];
  header: string;
  onAddCondition: (condition: Condition) => void;
  onClose: () => void;
  qualityGate: QualityGate;
}

interface State {
  error: string;
  errorMessage?: string;
  metric?: Metric;
  op?: string;
  scope: 'new' | 'overall';
}

const ADD_CONDITION_MODAL_ID = 'add-condition-modal';

export default class ConditionModal extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      error: props.condition ? props.condition.error : '',
      scope: 'new',
      metric: props.metric ? props.metric : undefined,
      op: props.condition ? props.condition.op : undefined,
    };
  }

  getSinglePossibleOperator(metric: Metric) {
    const operators = getPossibleOperators(metric);
    return Array.isArray(operators) ? undefined : operators;
  }

  handleFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const { condition, qualityGate } = this.props;
    const newCondition: Omit<Condition, 'id'> = {
      metric: this.state.metric!.key,
      op: this.getSinglePossibleOperator(this.state.metric!) || this.state.op,
      error: this.state.error,
    };
    const submitPromise = condition
      ? updateCondition({ id: condition.id, ...newCondition })
      : createCondition({ gateName: qualityGate.name, ...newCondition });
    return submitPromise.then(this.props.onAddCondition).then(this.props.onClose);
  };

  handleScopeChange = (scope: 'new' | 'overall') => {
    this.setState(({ metric }) => {
      const { metrics } = this.props;
      let correspondingMetric;

      if (metric && metrics) {
        const correspondingMetricKey =
          scope === 'new' ? `new_${metric.key}` : metric.key.replace(/^new_/, '');
        correspondingMetric = metrics.find((m) => m.key === correspondingMetricKey);
      }

      return { scope, metric: correspondingMetric };
    });
  };

  handleMetricChange = (metric: Metric) => {
    this.setState({ metric, op: undefined, error: '' });
  };

  handleOperatorChange = (op: string) => {
    this.setState({ op });
  };

  handleErrorChange = (error: string) => {
    this.setState({ error });
  };

  renderBody = () => {
    const { metrics } = this.props;
    const { op, error, scope, metric, errorMessage } = this.state;

    return (
      <form id={ADD_CONDITION_MODAL_ID} onSubmit={this.handleFormSubmit}>
        {errorMessage && (
          <FlagMessage className="sw-mb-2" variant="error">
            {errorMessage}
          </FlagMessage>
        )}
        {this.props.metric === undefined && (
          <FormField label={translate('quality_gates.conditions.where')}>
            <div className="sw-flex sw-gap-4">
              <RadioButton checked={scope === 'new'} onCheck={this.handleScopeChange} value="new">
                <span data-test="quality-gates__condition-scope-new">
                  {translate('quality_gates.conditions.new_code')}
                </span>
              </RadioButton>
              <RadioButton
                checked={scope === 'overall'}
                onCheck={this.handleScopeChange}
                value="overall"
              >
                <span data-test="quality-gates__condition-scope-overall">
                  {translate('quality_gates.conditions.overall_code')}
                </span>
              </RadioButton>
            </div>
          </FormField>
        )}

        <FormField
          description={this.props.metric && getLocalizedMetricName(this.props.metric)}
          htmlFor="condition-metric"
          label={translate('quality_gates.conditions.fails_when')}
        >
          {metrics && (
            <MetricSelect
              metric={metric}
              metricsArray={metrics.filter((m) =>
                scope === 'new' ? isDiffMetric(m.key) : !isDiffMetric(m.key),
              )}
              onMetricChange={this.handleMetricChange}
            />
          )}
        </FormField>

        {metric && (
          <div className="sw-flex sw-gap-2">
            <FormField
              className="sw-mb-0"
              htmlFor="condition-operator"
              label={translate('quality_gates.conditions.operator')}
            >
              <ConditionOperator
                metric={metric}
                onOperatorChange={this.handleOperatorChange}
                op={op}
              />
            </FormField>
            <FormField
              htmlFor="condition-threshold"
              label={translate('quality_gates.conditions.value')}
            >
              <ThresholdInput
                metric={metric}
                name="error"
                onChange={this.handleErrorChange}
                value={error}
              />
            </FormField>
          </div>
        )}
      </form>
    );
  };

  render() {
    const { header } = this.props;
    const { metric } = this.state;
    return (
      <Modal
        isScrollable={false}
        isOverflowVisible
        headerTitle={header}
        onClose={this.props.onClose}
        body={this.renderBody()}
        primaryButton={
          <ButtonPrimary
            autoFocus
            disabled={metric === undefined}
            id="add-condition-button"
            form={ADD_CONDITION_MODAL_ID}
            type="submit"
          >
            {header}
          </ButtonPrimary>
        }
        secondaryButtonLabel={translate('close')}
      />
    );
  }
}
