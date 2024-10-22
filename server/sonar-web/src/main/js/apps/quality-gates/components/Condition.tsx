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
  Button,
  ButtonIcon,
  ButtonSize,
  ButtonVariety,
  IconDelete,
  ModalAlert,
} from '@sonarsource/echoes-react';
import { ActionCell, ContentCell, NumericalCell, TableRow, TextError } from '~design-system';
import { useMetrics } from '../../../app/components/metrics/withMetricsContext';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../../helpers/l10n';
import { getOperatorLabel } from '../../../helpers/qualityGates';
import { useDeleteConditionMutation } from '../../../queries/quality-gates';
import { MetricKey } from '../../../sonar-aligned/types/metrics';
import { CaycStatus, Condition as ConditionType, Metric, QualityGate } from '../../../types/types';
import {
  getLocalizedMetricNameNoDiffMetric,
  isConditionWithFixedValue,
  isNonEditableMetric,
} from '../utils';
import ConditionValue from './ConditionValue';
import EditConditionModal from './EditConditionModal';

export enum ConditionChange {
  Added = 'added',
  Updated = 'updated',
  Deleted = 'deleted',
}

interface Props {
  canEdit: boolean;
  condition: ConditionType;
  isCaycModal?: boolean;
  metric: Metric;
  qualityGate: QualityGate;
  showEdit?: boolean;
}

export default function ConditionComponent({
  condition,
  canEdit,
  metric,
  qualityGate,
  showEdit,
  isCaycModal,
}: Readonly<Props>) {
  const { mutateAsync: deleteCondition } = useDeleteConditionMutation(qualityGate.name);
  const metrics = useMetrics();
  const { op = 'GT' } = condition;

  const isCaycCompliantAndOverCompliant = qualityGate.caycStatus !== CaycStatus.NonCompliant;

  return (
    <TableRow>
      <ContentCell>
        {getLocalizedMetricNameNoDiffMetric(metric, metrics)}
        {metric.hidden && <TextError className="sw-ml-1" text={translate('deprecated')} />}
      </ContentCell>

      <ContentCell className="sw-whitespace-nowrap">{getOperatorLabel(op, metric)}</ContentCell>

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
              (isCaycCompliantAndOverCompliant && showEdit)) &&
              !isNonEditableMetric(condition.metric as MetricKey) && (
                <EditConditionModal
                  condition={condition}
                  header={translate('quality_gates.update_condition')}
                  metric={metric}
                  qualityGate={qualityGate}
                />
              )}
            {(!isCaycCompliantAndOverCompliant ||
              !condition.isCaycCondition ||
              (isCaycCompliantAndOverCompliant && showEdit)) && (
              <ModalAlert
                title={translate('quality_gates.delete_condition')}
                description={translateWithParameters(
                  'quality_gates.delete_condition.confirm.message',
                  getLocalizedMetricName(metric),
                )}
                primaryButton={
                  <Button variety={ButtonVariety.Danger} onClick={() => deleteCondition(condition)}>
                    {translate('delete')}
                  </Button>
                }
                secondaryButtonLabel={translate('close')}
              >
                <ButtonIcon
                  Icon={IconDelete}
                  ariaLabel={translateWithParameters('quality_gates.condition.delete', metric.name)}
                  size={ButtonSize.Medium}
                  variety={ButtonVariety.DangerGhost}
                />
              </ModalAlert>
            )}
          </>
        )}
      </ActionCell>
    </TableRow>
  );
}
