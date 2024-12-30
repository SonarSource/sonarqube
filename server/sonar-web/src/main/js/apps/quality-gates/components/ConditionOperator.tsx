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

import { InputSize, Select } from '@sonarsource/echoes-react';
import { Note } from '~design-system';
import { getOperatorLabel } from '../../../helpers/qualityGates';
import { Metric } from '../../../types/types';
import { getPossibleOperators } from '../utils';

interface Props {
  isDisabled?: boolean;
  metric: Metric;
  onOperatorChange: (op: string) => void;
  op?: string;
}

export default function ConditionOperator(props: Readonly<Props>) {
  const operators = getPossibleOperators(props.metric);

  if (!Array.isArray(operators)) {
    return <Note className="sw-w-abs-150">{getOperatorLabel(operators, props.metric)}</Note>;
  }
  const operatorOptions = operators.map((op) => {
    const label = getOperatorLabel(op, props.metric);
    return { label, value: op };
  });

  return (
    <Select
      isDisabled={props.isDisabled}
      size={InputSize.Small}
      id="condition-operator"
      isNotClearable
      onChange={props.onOperatorChange}
      data={operatorOptions}
      value={operatorOptions.find((o) => o.value === props.op)?.value}
    />
  );
}
