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
import Select from '../../../components/controls/Select';
import { Metric } from '../../../app/types';
import { translate } from '../../../helpers/l10n';

interface Props {
  op?: string;
  canEdit: boolean;
  metric: Metric;
  onOperatorChange?: (op: string) => void;
}

export default class ConditionOperator extends React.PureComponent<Props> {
  handleChange = ({ value }: { label: string; value: string }) => {
    if (this.props.onOperatorChange) {
      this.props.onOperatorChange(value);
    }
  };

  render() {
    const { canEdit, metric, op } = this.props;
    if (!canEdit && op) {
      return metric.type === 'RATING' ? (
        <span className="note">{translate('quality_gates.operator', op, 'rating')}</span>
      ) : (
        <span className="note">{translate('quality_gates.operator', op)}</span>
      );
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
        onChange={this.handleChange}
        options={operatorOptions}
        searchable={false}
        value={op}
      />
    );
  }
}
