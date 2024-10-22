/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { InputSelect, Note } from '~design-system';
import { getOperatorLabel } from '../../../helpers/qualityGates';
import { Metric } from '../../../types/types';
import { getPossibleOperators } from '../utils';

interface Props {
  metric: Metric;
  onOperatorChange: (op: string) => void;
  op?: string;
}

export default class ConditionOperator extends React.PureComponent<Props> {
  handleChange = ({ value }: { label: string; value: string }) => {
    this.props.onOperatorChange(value);
  };

  render() {
    const operators = getPossibleOperators(this.props.metric);

    if (Array.isArray(operators)) {
      const operatorOptions = operators.map((op) => {
        const label = getOperatorLabel(op, this.props.metric);
        return { label, value: op };
      });

      return (
        <InputSelect
          autoFocus
          size="small"
          isClearable={false}
          inputId="condition-operator"
          name="operator"
          onChange={this.handleChange}
          options={operatorOptions}
          isSearchable={false}
          value={operatorOptions.filter((o) => o.value === this.props.op)}
        />
      );
    }

    return <Note className="sw-w-abs-150">{getOperatorLabel(operators, this.props.metric)}</Note>;
  }
}
