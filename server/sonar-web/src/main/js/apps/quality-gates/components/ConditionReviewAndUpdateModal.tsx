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
import * as React from 'react';
import { createCondition, updateCondition } from '../../../api/quality-gates';
import ConfirmModal from '../../../components/controls/ConfirmModal';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Condition, Dict, Metric, QualityGate } from '../../../types/types';
import { getCorrectCaycCondition, getWeakAndMissingConditions } from '../utils';
import ConditionsTable from './ConditionsTable';

interface Props {
  canEdit: boolean;
  metrics: Dict<Metric>;
  updatedConditionId?: string;
  conditions: Condition[];
  scope: 'new' | 'overall' | 'new-cayc';
  onClose: () => void;
  onAddCondition: (condition: Condition) => void;
  onRemoveCondition: (Condition: Condition) => void;
  onSaveCondition: (newCondition: Condition, oldCondition: Condition) => void;
  qualityGate: QualityGate;
}

export default class CaycReviewUpdateConditionsModal extends React.PureComponent<Props> {
  updateCaycQualityGate = () => {
    const { conditions, qualityGate } = this.props;
    const promiseArr: Promise<Condition | undefined>[] = [];
    const { weakConditions, missingConditions } = getWeakAndMissingConditions(conditions);

    weakConditions.forEach((condition) => {
      promiseArr.push(
        updateCondition({
          ...getCorrectCaycCondition(condition),
          id: condition.id,
        }).catch(() => undefined)
      );
    });

    missingConditions.forEach((condition) => {
      promiseArr.push(
        createCondition({
          ...getCorrectCaycCondition(condition),
          gateId: qualityGate.id,
        }).catch(() => undefined)
      );
    });

    return Promise.all(promiseArr).then((data) => {
      data.forEach((condition) => {
        if (condition === undefined) {
          return;
        }
        const currentCondition = conditions.find((con) => con.metric === condition.metric);
        if (currentCondition) {
          this.props.onSaveCondition(condition, currentCondition);
        } else {
          this.props.onAddCondition(condition);
        }
      });
    });
  };

  render() {
    const { conditions, qualityGate } = this.props;
    const { weakConditions, missingConditions } = getWeakAndMissingConditions(conditions);

    return (
      <ConfirmModal
        header={translateWithParameters(
          'quality_gates.cayc.review_update_modal.header',
          qualityGate.name
        )}
        confirmButtonText={translate('quality_gates.cayc.review_update_modal.confirm_text')}
        onClose={this.props.onClose}
        onConfirm={this.updateCaycQualityGate}
        size="medium"
      >
        <div className="quality-gate-section">
          <p className="big-spacer-bottom">
            {translate('quality_gates.cayc.review_update_modal.description')}
          </p>

          {weakConditions.length > 0 && (
            <>
              <h4 className="spacer-top spacer-bottom">
                {translateWithParameters(
                  'quality_gates.cayc.review_update_modal.modify_condition.header',
                  weakConditions.length
                )}
              </h4>
              <ConditionsTable
                {...this.props}
                conditions={weakConditions}
                showEdit={false}
                isCaycModal={true}
              />
            </>
          )}

          {missingConditions.length > 0 && (
            <>
              <h4 className="spacer-top spacer-bottom">
                {translateWithParameters(
                  'quality_gates.cayc.review_update_modal.add_condition.header',
                  missingConditions.length
                )}
              </h4>
              <ConditionsTable
                {...this.props}
                conditions={[]}
                showEdit={false}
                missingConditions={missingConditions}
                isCaycModal={true}
              />
            </>
          )}
        </div>
      </ConfirmModal>
    );
  }
}
