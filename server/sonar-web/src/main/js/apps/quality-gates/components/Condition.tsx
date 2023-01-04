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
import classNames from 'classnames';
import * as React from 'react';
import { deleteCondition } from '../../../api/quality-gates';
import withMetricsContext from '../../../app/components/metrics/withMetricsContext';
import { DeleteButton, EditButton } from '../../../components/controls/buttons';
import ConfirmModal from '../../../components/controls/ConfirmModal';
import { Alert } from '../../../components/ui/Alert';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../../helpers/l10n';
import { QGBadgeType } from '../../../types/quality-gates';
import { Condition as ConditionType, Dict, Metric, QualityGate } from '../../../types/types';
import { getLocalizedMetricNameNoDiffMetric, isCaycCondition, isCaycWeakCondition } from '../utils';
import CaycStatusBadge from './CaycStatusBadge';
import ConditionModal from './ConditionModal';
import ConditionValue from './ConditionValue';

interface Props {
  condition: ConditionType;
  canEdit: boolean;
  metric: Metric;
  onRemoveCondition: (Condition: ConditionType) => void;
  onSaveCondition: (newCondition: ConditionType, oldCondition: ConditionType) => void;
  qualityGate: QualityGate;
  updated?: boolean;
  metrics: Dict<Metric>;
  showEdit?: boolean;
  isMissingCondition?: boolean;
  isCaycModal?: boolean;
}

interface State {
  deleteFormOpen: boolean;
  modal: boolean;
}

export class ConditionComponent extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      deleteFormOpen: false,
      modal: false,
    };
  }

  handleUpdateCondition = (newCondition: ConditionType) => {
    this.props.onSaveCondition(newCondition, this.props.condition);
  };

  handleOpenUpdate = () => {
    this.setState({ modal: true });
  };

  handleUpdateClose = () => {
    this.setState({ modal: false });
  };

  handleDeleteClick = () => {
    this.setState({ deleteFormOpen: true });
  };

  closeDeleteForm = () => {
    this.setState({ deleteFormOpen: false });
  };

  removeCondition = (condition: ConditionType) => {
    deleteCondition({ id: condition.id }).then(
      () => this.props.onRemoveCondition(condition),
      () => {}
    );
  };

  getBadgeType = () => {
    const { condition, isCaycModal, isMissingCondition } = this.props;

    if (!isCaycModal) {
      if (isMissingCondition) {
        return QGBadgeType.Missing;
      } else if (isCaycWeakCondition(condition)) {
        return QGBadgeType.Weak;
      }
    }

    return QGBadgeType.Ok;
  };

  renderOperator() {
    // TODO can operator be missing?
    const { op = 'GT' } = this.props.condition;
    return this.props.metric.type === 'RATING'
      ? translate('quality_gates.operator', op, 'rating')
      : translate('quality_gates.operator', op);
  }

  render() {
    const {
      condition,
      canEdit,
      metric,
      qualityGate,
      updated,
      metrics,
      showEdit = true,
      isMissingCondition = false,
      isCaycModal = false,
    } = this.props;

    return (
      <tr className={classNames({ highlighted: updated })}>
        <td
          className={classNames('text-middle', {
            'text-through': isMissingCondition && !isCaycModal,
            'green-text': isMissingCondition && isCaycModal,
          })}
        >
          {getLocalizedMetricNameNoDiffMetric(metric, metrics)}
          {metric.hidden && (
            <span className="text-danger little-spacer-left">{translate('deprecated')}</span>
          )}
        </td>

        <td
          className={classNames('text-middle nowrap', {
            'text-through': isMissingCondition && !isCaycModal,
            'green-text': isMissingCondition && isCaycModal,
          })}
        >
          {this.renderOperator()}
        </td>

        <td
          className={classNames('text-middle nowrap', {
            'text-through': isMissingCondition && !isCaycModal,
            'green-text': isMissingCondition && isCaycModal,
          })}
        >
          <ConditionValue metric={metric} isCaycModal={isCaycModal} condition={condition} />
        </td>

        <td className="text-middle nowrap display-flex-justify-end">
          {isCaycCondition(condition) && <CaycStatusBadge type={this.getBadgeType()} />}
          {canEdit && showEdit && !isMissingCondition && (
            <>
              <EditButton
                aria-label={translateWithParameters('quality_gates.condition.edit', metric.name)}
                data-test="quality-gates__condition-update"
                onClick={this.handleOpenUpdate}
                className="spacer-right"
              />
              <DeleteButton
                aria-label={translateWithParameters('quality_gates.condition.delete', metric.name)}
                data-test="quality-gates__condition-delete"
                onClick={this.handleDeleteClick}
              />
              {this.state.modal && (
                <ConditionModal
                  condition={condition}
                  header={translate('quality_gates.update_condition')}
                  metric={metric}
                  onAddCondition={this.handleUpdateCondition}
                  onClose={this.handleUpdateClose}
                  qualityGate={qualityGate}
                />
              )}
              {this.state.deleteFormOpen && (
                <ConfirmModal
                  confirmButtonText={translate('delete')}
                  confirmData={condition}
                  header={translate('quality_gates.delete_condition')}
                  isDestructive={true}
                  onClose={this.closeDeleteForm}
                  onConfirm={this.removeCondition}
                >
                  {translateWithParameters(
                    'quality_gates.delete_condition.confirm.message',
                    getLocalizedMetricName(this.props.metric)
                  )}
                  {isCaycCondition(condition) && (
                    <Alert className="big-spacer-bottom big-spacer-top" variant="warning">
                      <p className="cayc-warning-description">
                        {translate('quality_gates.cayc_condition.delete_warning')}
                      </p>
                    </Alert>
                  )}
                </ConfirmModal>
              )}
            </>
          )}
        </td>
      </tr>
    );
  }
}

export default withMetricsContext(ConditionComponent);
