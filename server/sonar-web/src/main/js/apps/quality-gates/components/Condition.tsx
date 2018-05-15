/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import DeleteConditionForm from './DeleteConditionForm';
import ThresholdInput from './ThresholdInput';
import { createCondition, updateCondition } from '../../../api/quality-gates';
import { Condition as ICondition, Metric, QualityGate } from '../../../app/types';
import Checkbox from '../../../components/controls/Checkbox';
import Select from '../../../components/controls/Select';
import { Button, ResetButtonLink } from '../../../components/ui/buttons';
import { translate, getLocalizedMetricName } from '../../../helpers/l10n';
import { formatMeasure, isDiffMetric } from '../../../helpers/measures';

interface Props {
  condition: ICondition;
  canEdit: boolean;
  metric: Metric;
  organization?: string;
  onAddCondition: (metric: string) => void;
  onError: (error: any) => void;
  onRemoveCondition: (Condition: ICondition) => void;
  onResetError: () => void;
  onSaveCondition: (newCondition: ICondition, oldCondition: ICondition) => void;
  qualityGate: QualityGate;
}

interface State {
  changed: boolean;
  error: string;
  op?: string;
  period?: number;
  warning: string;
}

export default class Condition extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      changed: false,
      period: props.condition.period,
      op: props.condition.op,
      warning: props.condition.warning || '',
      error: props.condition.error || ''
    };
  }

  getUpdatedCondition = () => {
    const { metric } = this.props;
    const data: ICondition = {
      metric: metric.key,
      op: metric.type === 'RATING' ? 'GT' : this.state.op,
      warning: this.state.warning,
      error: this.state.error
    };

    const { period } = this.state;
    if (period && metric.type !== 'RATING') {
      data.period = period;
    }

    if (isDiffMetric(metric.key)) {
      data.period = 1;
    }
    return data;
  };

  handleOperatorChange = ({ value }: any) => this.setState({ changed: true, op: value });

  handlePeriodChange = (checked: boolean) => {
    const period = checked ? 1 : undefined;
    this.setState({ changed: true, period });
  };

  handleWarningChange = (warning: string) => this.setState({ changed: true, warning });

  handleErrorChange = (error: string) => this.setState({ changed: true, error });

  handleSaveClick = () => {
    const { qualityGate, organization } = this.props;
    const data = this.getUpdatedCondition();
    createCondition({ gateId: qualityGate.id, organization, ...data }).then(
      this.handleConditionResponse,
      this.props.onError
    );
  };

  handleUpdateClick = () => {
    const { condition, organization } = this.props;
    const data: ICondition = {
      id: condition.id,
      ...this.getUpdatedCondition()
    };

    updateCondition({ organization, ...data }).then(
      this.handleConditionResponse,
      this.props.onError
    );
  };

  handleConditionResponse = (newCondition: ICondition) => {
    this.props.onSaveCondition(newCondition, this.props.condition);
    this.props.onResetError();
    this.setState({ changed: false });
  };

  handleCancelClick = () => {
    this.props.onRemoveCondition(this.props.condition);
  };

  renderPeriodValue() {
    const { condition, metric } = this.props;
    const isLeakSelected = !!this.state.period;
    const isRating = metric.type === 'RATING';

    if (isDiffMetric(condition.metric)) {
      return (
        <span className="note">{translate('quality_gates.condition.leak.unconditional')}</span>
      );
    }

    if (isRating) {
      return <span className="note">{translate('quality_gates.condition.leak.never')}</span>;
    }

    return isLeakSelected
      ? translate('quality_gates.condition.leak.yes')
      : translate('quality_gates.condition.leak.no');
  }

  renderPeriod() {
    const { condition, metric, canEdit } = this.props;
    const isRating = metric.type === 'RATING';
    const isLeakSelected = !!this.state.period;

    if (isRating || isDiffMetric(condition.metric) || !canEdit) {
      return this.renderPeriodValue();
    }

    return <Checkbox checked={isLeakSelected} onCheck={this.handlePeriodChange} />;
  }

  renderOperator() {
    const { condition, canEdit, metric } = this.props;

    if (!canEdit && condition.op) {
      return metric.type === 'RATING'
        ? translate('quality_gates.operator', condition.op, 'rating')
        : translate('quality_gates.operator', condition.op);
    }

    if (metric.type === 'RATING') {
      return <span className="note">{translate('quality_gates.operator.GT.rating')}</span>;
    }

    const operators = ['LT', 'GT', 'EQ', 'NE'];
    const operatorOptions = operators.map(op => {
      const label = translate('quality_gates.operator', op);
      return { label, value: op };
    });

    return (
      <Select
        autoFocus={true}
        className="input-medium"
        clearable={false}
        name="operator"
        onChange={this.handleOperatorChange}
        options={operatorOptions}
        searchable={false}
        value={this.state.op}
      />
    );
  }

  render() {
    const { condition, canEdit, metric, organization } = this.props;
    return (
      <tr>
        <td className="text-middle">
          {getLocalizedMetricName(metric)}
          {metric.hidden && (
            <span className="text-danger little-spacer-left">{translate('deprecated')}</span>
          )}
        </td>

        <td className="thin text-middle nowrap">{this.renderPeriod()}</td>

        <td className="thin text-middle nowrap">{this.renderOperator()}</td>

        <td className="thin text-middle nowrap">
          {canEdit ? (
            <ThresholdInput
              metric={metric}
              name="warning"
              onChange={this.handleWarningChange}
              value={this.state.warning}
            />
          ) : (
            formatMeasure(condition.warning, metric.type)
          )}
        </td>

        <td className="thin text-middle nowrap">
          {canEdit ? (
            <ThresholdInput
              metric={metric}
              name="error"
              onChange={this.handleErrorChange}
              value={this.state.error}
            />
          ) : (
            formatMeasure(condition.error, metric.type)
          )}
        </td>

        {canEdit && (
          <td className="thin text-middle nowrap">
            {condition.id ? (
              <div>
                <Button
                  className="update-condition"
                  disabled={!this.state.changed}
                  onClick={this.handleUpdateClick}>
                  {translate('update_verb')}
                </Button>
                <DeleteConditionForm
                  condition={condition}
                  metric={metric}
                  onDelete={this.props.onRemoveCondition}
                  organization={organization}
                />
              </div>
            ) : (
              <div>
                <Button className="add-condition" onClick={this.handleSaveClick}>
                  {translate('add_verb')}
                </Button>
                <ResetButtonLink
                  className="cancel-add-condition spacer-left"
                  onClick={this.handleCancelClick}>
                  {translate('cancel')}
                </ResetButtonLink>
              </div>
            )}
          </td>
        )}
      </tr>
    );
  }
}
