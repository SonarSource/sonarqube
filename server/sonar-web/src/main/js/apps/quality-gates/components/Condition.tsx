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
import {
  ActionCell,
  ContentCell,
  DangerButtonPrimary,
  DestructiveIcon,
  InteractiveIcon,
  Modal,
  NumericalCell,
  PencilIcon,
  TableRow,
  TextError,
  TrashIcon,
} from 'design-system';
import * as React from 'react';
import { deleteCondition } from '../../../api/quality-gates';
import withMetricsContext from '../../../app/components/metrics/withMetricsContext';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../../helpers/l10n';
import {
  CaycStatus,
  Condition as ConditionType,
  Dict,
  Metric,
  QualityGate,
} from '../../../types/types';
import { getLocalizedMetricNameNoDiffMetric, isConditionWithFixedValue } from '../utils';
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
      () => {},
    );
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
      isCaycModal = false,
    } = this.props;

    const isCaycCompliantAndOverCompliant = qualityGate.caycStatus !== CaycStatus.NonCompliant;

    return (
      <TableRow className={classNames({ highlighted: updated })}>
        <ContentCell>
          {getLocalizedMetricNameNoDiffMetric(metric, metrics)}
          {metric.hidden && <TextError className="sw-ml-1" text={translate('deprecated')} />}
        </ContentCell>

        <ContentCell className="sw-whitespace-nowrap">{this.renderOperator()}</ContentCell>

        <NumericalCell className="sw-whitespace-nowrap">
          <ConditionValue
            metric={metric}
            isCaycModal={isCaycModal}
            condition={condition}
            isCaycCompliantAndOverCompliant={isCaycCompliantAndOverCompliant}
          />
        </NumericalCell>
        <ActionCell>
          {!isCaycModal && canEdit && (
            <>
              {(!isCaycCompliantAndOverCompliant ||
                !isConditionWithFixedValue(condition) ||
                (isCaycCompliantAndOverCompliant && showEdit)) && (
                <>
                  <InteractiveIcon
                    Icon={PencilIcon}
                    aria-label={translateWithParameters(
                      'quality_gates.condition.edit',
                      metric.name,
                    )}
                    data-test="quality-gates__condition-update"
                    onClick={this.handleOpenUpdate}
                    className="sw-mr-4"
                    size="small"
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
                </>
              )}
              {(!isCaycCompliantAndOverCompliant ||
                !condition.isCaycCondition ||
                (isCaycCompliantAndOverCompliant && showEdit)) && (
                <>
                  <DestructiveIcon
                    Icon={TrashIcon}
                    aria-label={translateWithParameters(
                      'quality_gates.condition.delete',
                      metric.name,
                    )}
                    data-test="quality-gates__condition-delete"
                    onClick={this.handleDeleteClick}
                    size="small"
                  />
                  {this.state.deleteFormOpen && (
                    <Modal
                      headerTitle={translate('quality_gates.delete_condition')}
                      onClose={this.closeDeleteForm}
                      body={translateWithParameters(
                        'quality_gates.delete_condition.confirm.message',
                        getLocalizedMetricName(this.props.metric),
                      )}
                      primaryButton={
                        <DangerButtonPrimary
                          autoFocus
                          type="submit"
                          onClick={() => this.removeCondition(condition)}
                        >
                          {translate('delete')}
                        </DangerButtonPrimary>
                      }
                      secondaryButtonLabel={translate('close')}
                    />
                  )}
                </>
              )}
            </>
          )}
        </ActionCell>
      </TableRow>
    );
  }
}

export default withMetricsContext(ConditionComponent);
