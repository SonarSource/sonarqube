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
import { translate } from '../../../helpers/l10n';
import { Condition as ConditionType, Dict, Metric, QualityGate } from '../../../types/types';
import Condition from './Condition';

interface Props {
  canEdit: boolean;
  metrics: Dict<Metric>;
  onRemoveCondition: (Condition: ConditionType) => void;
  onSaveCondition: (newCondition: ConditionType, oldCondition: ConditionType) => void;
  qualityGate: QualityGate;
  updatedConditionId?: string;
  conditions: ConditionType[];
  scope: 'new' | 'overall' | 'new-cayc';
  isCaycModal?: boolean;
  showEdit?: boolean;
}

export default class ConditionsTable extends React.PureComponent<Props> {
  render() {
    const {
      qualityGate,
      metrics,
      canEdit,
      onRemoveCondition,
      onSaveCondition,
      updatedConditionId,
      scope,
      conditions,
      isCaycModal,
      showEdit,
    } = this.props;

    return (
      <table
        className="data zebra"
        data-test={`quality-gates__conditions-${scope}`}
        data-testid={`quality-gates__conditions-${scope}`}
      >
        <thead>
          <tr>
            <th className="nowrap abs-width-300">{translate('quality_gates.conditions.metric')}</th>
            <th className="nowrap">{translate('quality_gates.conditions.operator')}</th>
            <th className="nowrap">{translate('quality_gates.conditions.value')}</th>
            <th className="thin">&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          {conditions.map((condition) => (
            <Condition
              canEdit={canEdit}
              condition={condition}
              key={condition.id}
              metric={metrics[condition.metric]}
              onRemoveCondition={onRemoveCondition}
              onSaveCondition={onSaveCondition}
              qualityGate={qualityGate}
              updated={condition.id === updatedConditionId}
              isCaycModal={isCaycModal}
              showEdit={showEdit}
            />
          ))}
        </tbody>
      </table>
    );
  }
}
