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
import Select from '../../../components/controls/Select';
import { translate } from '../../../helpers/l10n';
import { getPossibleOperators } from '../utils';

interface Props {
  metric: T.Metric;
  onOperatorChange: (op: string) => void;
  op?: string;
}

export default class ConditionOperator extends React.PureComponent<Props> {
  handleChange = ({ value }: { label: string; value: string }) => {
    this.props.onOperatorChange(value);
  };

  getLabel(op: string, metric: T.Metric) {
    return metric.type === 'RATING'
      ? translate('quality_gates.operator', op, 'rating')
      : translate('quality_gates.operator', op);
  }

  render() {
    const operators = getPossibleOperators(this.props.metric);

    if (Array.isArray(operators)) {
      const operatorOptions = operators.map(op => {
        const label = this.getLabel(op, this.props.metric);
        return { label, value: op };
      });

      return (
        <Select
          autoFocus={true}
          className="input-medium"
          clearable={false}
          id="condition-operator"
          name="operator"
          onChange={this.handleChange}
          options={operatorOptions}
          searchable={false}
          value={this.props.op}
        />
      );
    } else {
      return (
        <span className="display-inline-block note abs-width-150">
          {this.getLabel(operators, this.props.metric)}
        </span>
      );
    }
  }
}
