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
import { DeleteButton, EditButton } from 'sonar-ui-common/components/controls/buttons';
import ConfirmModal from 'sonar-ui-common/components/controls/ConfirmModal';
import {
  getLocalizedMetricName,
  translate,
  translateWithParameters
} from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import { deleteCondition } from '../../../api/quality-gates';
import { getLocalizedMetricNameNoDiffMetric } from '../utils';
import ConditionModal from './ConditionModal';

interface Props {
  condition: T.Condition;
  canEdit: boolean;
  metric: T.Metric;
  organization?: string;
  onRemoveCondition: (Condition: T.Condition) => void;
  onSaveCondition: (newCondition: T.Condition, oldCondition: T.Condition) => void;
  qualityGate: T.QualityGate;
}

interface State {
  deleteFormOpen: boolean;
  modal: boolean;
}

export default class Condition extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      deleteFormOpen: false,
      modal: false
    };
  }

  handleUpdateCondition = (newCondition: T.Condition) => {
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

  removeCondition = (condition: T.Condition) => {
    deleteCondition({ id: condition.id, organization: this.props.organization }).then(
      () => this.props.onRemoveCondition(condition),
      () => {}
    );
  };

  renderOperator() {
    // TODO can operator be missing?
    const { op = 'GT' } = this.props.condition;
    return (
      <span className="note">
        {this.props.metric.type === 'RATING'
          ? translate('quality_gates.operator', op, 'rating')
          : translate('quality_gates.operator', op)}
      </span>
    );
  }

  render() {
    const { condition, canEdit, metric, organization, qualityGate } = this.props;
    return (
      <tr>
        <td className="text-middle">
          {getLocalizedMetricNameNoDiffMetric(metric)}
          {metric.hidden && (
            <span className="text-danger little-spacer-left">{translate('deprecated')}</span>
          )}
        </td>

        <td className="text-middle nowrap">{this.renderOperator()}</td>

        <td className="text-middle nowrap">{formatMeasure(condition.error, metric.type)}</td>

        {canEdit && (
          <>
            <td className="text-center thin">
              <EditButton
                data-test="quality-gates__condition-update"
                onClick={this.handleOpenUpdate}
              />
            </td>
            <td className="text-center thin">
              <DeleteButton
                data-test="quality-gates__condition-delete"
                onClick={this.handleDeleteClick}
              />
            </td>
            {this.state.modal && (
              <ConditionModal
                condition={condition}
                header={translate('quality_gates.update_condition')}
                metric={metric}
                onAddCondition={this.handleUpdateCondition}
                onClose={this.handleUpdateClose}
                organization={organization}
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
                onConfirm={this.removeCondition}>
                {translateWithParameters(
                  'quality_gates.delete_condition.confirm.message',
                  getLocalizedMetricName(this.props.metric)
                )}
              </ConfirmModal>
            )}
          </>
        )}
      </tr>
    );
  }
}
