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
  IconRefresh,
  ModalAlert,
  Text,
} from '@sonarsource/echoes-react';
import { useIntl } from 'react-intl';
import {
  ActionCell,
  ContentCell,
  NumericalCell,
  Pill,
  PillHighlight,
  PillVariant,
  TableRow,
} from '~design-system';
import { useMetrics } from '../../../app/components/metrics/withMetricsContext';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../../helpers/l10n';
import { getOperatorLabel } from '../../../helpers/qualityGates';
import { useStandardExperienceModeQuery } from '../../../queries/mode';
import { useDeleteConditionMutation } from '../../../queries/quality-gates';
import { MetricKey } from '../../../sonar-aligned/types/metrics';
import { CaycStatus, Condition as ConditionType, Metric, QualityGate } from '../../../types/types';
import {
  getLocalizedMetricNameNoDiffMetric,
  isConditionWithFixedValue,
  isNonEditableMetric,
  MQR_CONDITIONS_MAP,
  STANDARD_CONDITIONS_MAP,
} from '../utils';
import ConditionValue from './ConditionValue';
import EditConditionModal from './EditConditionModal';
import UpdateConditionsFromOtherModeModal from './UpdateConditionsFromOtherModeModal';

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
  const intl = useIntl();
  const { data: isStandard } = useStandardExperienceModeQuery();
  const { op = 'GT' } = condition;

  const isCaycCompliantAndOverCompliant = qualityGate.caycStatus !== CaycStatus.NonCompliant;
  const isMetricFromOtherMode = isStandard
    ? MQR_CONDITIONS_MAP[condition.metric as MetricKey] !== undefined
    : STANDARD_CONDITIONS_MAP[condition.metric as MetricKey] !== undefined;

  return (
    <TableRow>
      <ContentCell>
        {getLocalizedMetricNameNoDiffMetric(metric, metrics)}
        {isMetricFromOtherMode && canEdit && (
          <Pill className="sw-ml-2" variant={PillVariant.Neutral} highlight={PillHighlight.Medium}>
            {intl.formatMessage({
              id: `quality_gates.metric.${isStandard ? 'mqr' : 'standard'}_mode_short`,
            })}
          </Pill>
        )}
        {metric.hidden && (
          <Text colorOverride="echoes-color-text-danger" className="sw-ml-1">
            {translate('deprecated')}
          </Text>
        )}
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
            {isMetricFromOtherMode && (
              <UpdateConditionsFromOtherModeModal
                condition={condition}
                qualityGateName={qualityGate.name}
              >
                <ButtonIcon
                  Icon={IconRefresh}
                  variety={ButtonVariety.PrimaryGhost}
                  className="sw-mr-4"
                  ariaLabel={intl.formatMessage(
                    { id: 'quality_gates.mqr_mode_update.single_metric.tooltip.message' },
                    {
                      metric: getLocalizedMetricNameNoDiffMetric(metric, metrics),
                      mode: intl.formatMessage({
                        id: `settings.mode.${isStandard ? 'standard' : 'mqr'}.name`,
                      }),
                    },
                  )}
                />
              </UpdateConditionsFromOtherModeModal>
            )}
            {(!isCaycCompliantAndOverCompliant ||
              !isConditionWithFixedValue(condition) ||
              (isCaycCompliantAndOverCompliant && showEdit)) &&
              !isNonEditableMetric(condition.metric as MetricKey) &&
              !isMetricFromOtherMode && (
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
