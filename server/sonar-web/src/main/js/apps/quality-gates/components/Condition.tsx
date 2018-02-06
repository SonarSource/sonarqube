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
import Checkbox from '../../../components/controls/Checkbox';
import Select from '../../../components/controls/Select';
import {
  Condition as ICondition,
  ConditionBase,
  createCondition,
  QualityGate,
  updateCondition
} from '../../../api/quality-gates';
import { Metric } from '../../../app/types';
import { translate, getLocalizedMetricName } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';

interface Props {
  condition: ICondition;
  edit: boolean;
  metric: Metric;
  organization: string;
  onDeleteCondition: (condition: ICondition) => void;
  onError: (error: any) => void;
  onResetError: () => void;
  onSaveCondition: (condition: ICondition, newCondition: ICondition) => void;
  qualityGate: QualityGate;
}

interface State {
  changed: boolean;
  period?: number;
  op?: string;
  openDeleteCondition: boolean;
  warning: string;
  error: string;
}

export default class Condition extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      changed: false,
      period: props.condition.period,
      op: props.condition.op,
      openDeleteCondition: false,
      warning: props.condition.warning || '',
      error: props.condition.error || ''
    };
  }

  handleOperatorChange = ({ value }: any) => this.setState({ changed: true, op: value });

  handlePeriodChange = (checked: boolean) => {
    const period = checked ? 1 : undefined;
    this.setState({ changed: true, period });
  };

  handleWarningChange = (warning: string) => this.setState({ changed: true, warning });

  handleErrorChange = (error: string) => this.setState({ changed: true, error });

  handleSaveClick = () => {
    const { qualityGate, condition, metric, organization } = this.props;
    const { period } = this.state;
    const data: ConditionBase = {
      metric: condition.metric,
      op: metric.type === 'RATING' ? 'GT' : this.state.op,
      warning: this.state.warning,
      error: this.state.error
    };

    if (period && metric.type !== 'RATING') {
      data.period = period;
    }

    if (metric.key.indexOf('new_') === 0) {
      data.period = 1;
    }

    createCondition({ gateId: qualityGate.id, organization, ...data }).then(
      this.handleConditionResponse,
      this.props.onError
    );
  };

  handleUpdateClick = () => {
    const { condition, metric, organization } = this.props;
    const { period } = this.state;
    const data: ICondition = {
      id: condition.id,
      metric: condition.metric,
      op: metric.type === 'RATING' ? 'GT' : this.state.op,
      warning: this.state.warning,
      error: this.state.error
    };

    if (period && metric.type !== 'RATING') {
      data.period = period;
    }

    if (metric.key.indexOf('new_') === 0) {
      data.period = 1;
    }

    updateCondition({ organization, ...data }).then(
      this.handleConditionResponse,
      this.props.onError
    );
  };

  handleConditionResponse = (newCondition: ICondition) => {
    this.setState({ changed: false });
    this.props.onSaveCondition(this.props.condition, newCondition);
    this.props.onResetError();
  };

  handleCancelClick = (e: React.SyntheticEvent<HTMLAnchorElement>) => {
    e.preventDefault();
    e.stopPropagation();
    this.props.onDeleteCondition(this.props.condition);
  };

  openDeleteConditionForm = () => this.setState({ openDeleteCondition: true });
  closeDeleteConditionForm = () => this.setState({ openDeleteCondition: false });

  renderPeriodValue() {
    const { condition, metric } = this.props;
    const isLeakSelected = !!this.state.period;
    const isDiffMetric = condition.metric.indexOf('new_') === 0;
    const isRating = metric.type === 'RATING';

    if (isDiffMetric) {
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
    const { condition, metric, edit } = this.props;

    const isDiffMetric = condition.metric.indexOf('new_') === 0;
    const isRating = metric.type === 'RATING';
    const isLeakSelected = !!this.state.period;

    if (isRating || isDiffMetric || !edit) {
      return this.renderPeriodValue();
    }

    return <Checkbox checked={isLeakSelected} onCheck={this.handlePeriodChange} />;
  }

  renderOperator() {
    const { condition, edit, metric } = this.props;

    if (!edit && condition.op) {
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
        autofocus={true}
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
    const { condition, edit, metric, organization } = this.props;
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
          {edit ? (
            <ThresholdInput
              name="warning"
              value={this.state.warning}
              metric={metric}
              onChange={this.handleWarningChange}
            />
          ) : (
            formatMeasure(condition.warning, metric.type)
          )}
        </td>

        <td className="thin text-middle nowrap">
          {edit ? (
            <ThresholdInput
              name="error"
              value={this.state.error}
              metric={metric}
              onChange={this.handleErrorChange}
            />
          ) : (
            formatMeasure(condition.error, metric.type)
          )}
        </td>

        {edit && (
          <td className="thin text-middle nowrap">
            {condition.id ? (
              <div>
                <button
                  className="update-condition"
                  disabled={!this.state.changed}
                  onClick={this.handleUpdateClick}>
                  {translate('update_verb')}
                </button>
                <button
                  className="button-red delete-condition little-spacer-left"
                  onClick={this.openDeleteConditionForm}>
                  {translate('delete')}
                </button>
                {this.state.openDeleteCondition && (
                  <DeleteConditionForm
                    condition={condition}
                    metric={metric}
                    onClose={this.closeDeleteConditionForm}
                    onDelete={this.props.onDeleteCondition}
                    organization={organization}
                  />
                )}
              </div>
            ) : (
              <div>
                <button className="add-condition" onClick={this.handleSaveClick}>
                  {translate('add_verb')}
                </button>
                <a
                  className="cancel-add-condition spacer-left"
                  href="#"
                  onClick={this.handleCancelClick}>
                  {translate('cancel')}
                </a>
              </div>
            )}
          </td>
        )}
      </tr>
    );
  }
}
