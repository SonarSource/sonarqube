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
import { Table } from 'design-system';
import * as React from 'react';
import { Condition as ConditionType, Dict, Metric } from '../../../types/types';
import CaycCondition from './CaycCondition';

interface Props {
  metrics: Dict<Metric>;
  conditions: ConditionType[];
}

export default function CaycConditionsTable({ metrics, conditions }: Readonly<Props>) {
  return (
    <Table
      columnCount={2}
      columnWidths={['auto', '1fr']}
      className="sw-my-2"
      data-testid="quality-gates__conditions-cayc"
    >
      {conditions.map((condition) => (
        <CaycCondition
          key={condition.id}
          condition={condition}
          metric={metrics[condition.metric]}
        />
      ))}
    </Table>
  );
}
