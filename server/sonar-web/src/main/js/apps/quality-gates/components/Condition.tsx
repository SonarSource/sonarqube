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
import { useMetrics } from '../../../app/components/metrics/withMetricsContext';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../../helpers/l10n';
import { useDeleteConditionMutation } from '../../../queries/quality-gates';
import { MetricType } from '../../../types/metrics';
import { CaycStatus, Condition as ConditionType, Metric, QualityGate } from '../../../types/types';
import { getLocalizedMetricNameNoDiffMetric, isConditionWithFixedValue } from '../utils';
import ConditionModal from './ConditionModal';
import ConditionValue from './ConditionValue';

export enum ConditionChange {
  Added = 'added',
  Updated = 'updated',
  Deleted = 'deleted',
}

interface Props {
  condition: ConditionType;
  canEdit: boolean;
  metric: Metric;
  qualityGate: QualityGate;
  showEdit?: boolean;
  isCaycModal?: boolean;
}

export default function ConditionComponent({
  condition,
  canEdit,
  metric,
  qualityGate,
  showEdit,
  isCaycModal,
}: Readonly<Props>) {
  const [deleteFormOpen, setDeleteFormOpen] = React.useState(false);
  const [modal, setModal] = React.useState(false);
  const { mutateAsync: deleteCondition } = useDeleteConditionMutation(qualityGate.name);
  const metrics = useMetrics();

  const handleOpenUpdate = () => {
    setModal(true);
  };

  const handleUpdateClose = () => {
    setModal(false);
  };

  const handleDeleteClick = () => {
    setDeleteFormOpen(true);
  };

  const closeDeleteForm = () => {
    setDeleteFormOpen(false);
  };

  const renderOperator = () => {
    const { op = 'GT' } = condition;
    return metric.type === MetricType.Rating
      ? translate('quality_gates.operator', op, 'rating')
      : translate('quality_gates.operator', op);
  };

  const isCaycCompliantAndOverCompliant = qualityGate.caycStatus !== CaycStatus.NonCompliant;

  return (
    <TableRow>
      <ContentCell>
        {getLocalizedMetricNameNoDiffMetric(metric, metrics)}
        {metric.hidden && <TextError className="sw-ml-1" text={translate('deprecated')} />}
      </ContentCell>

      <ContentCell className="sw-whitespace-nowrap">{renderOperator()}</ContentCell>

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
                  aria-label={translateWithParameters('quality_gates.condition.edit', metric.name)}
                  data-test="quality-gates__condition-update"
                  onClick={handleOpenUpdate}
                  className="sw-mr-4"
                  size="small"
                />
                {modal && (
                  <ConditionModal
                    condition={condition}
                    header={translate('quality_gates.update_condition')}
                    metric={metric}
                    onClose={handleUpdateClose}
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
                  onClick={handleDeleteClick}
                  size="small"
                />
                {deleteFormOpen && (
                  <Modal
                    headerTitle={translate('quality_gates.delete_condition')}
                    onClose={closeDeleteForm}
                    body={translateWithParameters(
                      'quality_gates.delete_condition.confirm.message',
                      getLocalizedMetricName(metric),
                    )}
                    primaryButton={
                      <DangerButtonPrimary
                        autoFocus
                        type="submit"
                        onClick={() => deleteCondition(condition)}
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
