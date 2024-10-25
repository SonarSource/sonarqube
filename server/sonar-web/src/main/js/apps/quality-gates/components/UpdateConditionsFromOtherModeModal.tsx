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
  ButtonVariety,
  Heading,
  IconArrowRight,
  Modal,
  ModalSize,
  Text,
  TextSize,
} from '@sonarsource/echoes-react';
import * as React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { useMetrics } from '../../../app/components/metrics/withMetricsContext';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { ContentCell, FlagMessageV2, Table, TableRow } from '../../../design-system';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import { getOperatorLabel } from '../../../helpers/qualityGates';
import { useUpdateOrDeleteConditionsMutation } from '../../../queries/quality-gates';
import { useStandardExperienceMode } from '../../../queries/settings';
import { MetricKey } from '../../../sonar-aligned/types/metrics';
import { Condition } from '../../../types/types';
import {
  getLocalizedMetricNameNoDiffMetric,
  MQR_CONDITIONS_MAP,
  STANDARD_CONDITIONS_MAP,
} from '../utils';
import ConditionValue from './ConditionValue';

type Props = React.PropsWithChildren & { qualityGateName: string } & (
    | {
        condition?: never;
        isSingleMetric?: false;
        newCodeConditions: Condition[];
        overallCodeConditions: Condition[];
      }
    | {
        condition: Condition;
        isSingleMetric: true;
        newCodeConditions?: never;
        overallCodeConditions?: never;
      }
  );

export default function UpdateConditionsFromOtherModeModal({
  newCodeConditions,
  overallCodeConditions,
  qualityGateName,
  isSingleMetric,
  condition,
  children,
}: Readonly<Props>) {
  const { data: isStandard } = useStandardExperienceMode();
  const [isOpen, setOpen] = React.useState(false);
  const [error, setError] = React.useState(false);
  const intl = useIntl();
  const mapper = isStandard ? MQR_CONDITIONS_MAP : STANDARD_CONDITIONS_MAP;
  const { mutate: updateConditions, isPending } = useUpdateOrDeleteConditionsMutation(
    qualityGateName,
    isSingleMetric,
  );

  const onSubmit = () => {
    const conditions = isSingleMetric
      ? [condition]
      : [...newCodeConditions, ...overallCodeConditions];

    updateConditions(
      conditions.map((c) => ({ ...c, metric: mapper[c.metric as MetricKey] })),
      {
        onSuccess: () => setOpen(false),
        onError: () => setError(true),
      },
    );
  };

  return (
    <Modal
      isOpen={isOpen}
      onOpenChange={setOpen}
      title={intl.formatMessage(
        {
          id: `quality_gates.update_conditions.header${isSingleMetric ? '.single_metric' : ''}`,
        },
        { qualityGate: qualityGateName },
      )}
      size={isSingleMetric ? ModalSize.Default : ModalSize.Wide}
      primaryButton={
        <Button
          isDisabled={isPending}
          isLoading={isPending}
          onClick={onSubmit}
          id="update-metrics-button"
          variety={ButtonVariety.Primary}
        >
          {intl.formatMessage({
            id: isSingleMetric ? 'update_verb' : 'quality_gates.update_conditions.update_metrics',
          })}
        </Button>
      }
      secondaryButton={<Button onClick={() => setOpen(false)}>{translate('cancel')}</Button>}
      content={
        <>
          {error && (
            <div>
              <FlagMessageV2 variant="error">
                {intl.formatMessage({ id: 'quality_gates.update_conditions.error' })}
              </FlagMessageV2>
            </div>
          )}
          <Text>
            <FormattedMessage
              id="quality_gates.update_conditions.description.line1"
              values={{
                b: (chunks) => <Text isHighlighted>{chunks}</Text>,
                mode: intl.formatMessage({
                  id: `settings.mode.${isStandard ? 'standard' : 'mqr'}.name`,
                }),
              }}
            />
          </Text>

          {isSingleMetric && <SingleMetric condition={condition} />}
          <Text>
            <FormattedMessage
              id="quality_gates.update_conditions.description.line2"
              values={{
                b: (chunks) => <Text isHighlighted>{chunks}</Text>,
              }}
            />
            <br />
            <br />
            <FormattedMessage
              id="quality_gates.update_conditions.description.line3"
              values={{
                mode: intl.formatMessage({
                  id: `settings.mode.${isStandard ? 'standard' : 'mqr'}.name`,
                }),
                link: (
                  <DocumentationLink to={isStandard ? DocLink.ModeStandard : DocLink.ModeMQR}>
                    {intl.formatMessage({
                      id: 'quality_gates.update_conditions.description.link',
                    })}
                  </DocumentationLink>
                ),
              }}
            />
          </Text>
          {!isSingleMetric && (
            <>
              {newCodeConditions.length > 0 && (
                <>
                  <Heading as="h3" className="sw-mt-8">
                    {intl.formatMessage({ id: 'overview.new_code' })}
                  </Heading>
                  <Table
                    columnCount={3}
                    columnWidths={['35%', '35%', 'auto']}
                    className="sw-my-2"
                    header={<Header />}
                  >
                    {newCodeConditions.map((condition) => (
                      <ConditionRow condition={condition} key={condition.id} />
                    ))}
                  </Table>
                </>
              )}
              {overallCodeConditions.length > 0 && (
                <>
                  <Heading as="h3" className="sw-mt-8">
                    {intl.formatMessage({ id: 'overview.overall_code' })}
                  </Heading>
                  <Table
                    columnCount={3}
                    columnWidths={['35%', '35%', 'auto']}
                    className="sw-my-2"
                    header={<Header />}
                  >
                    {overallCodeConditions.map((condition) => (
                      <ConditionRow condition={condition} key={condition.id} />
                    ))}
                  </Table>
                </>
              )}
            </>
          )}
        </>
      }
    >
      {React.cloneElement(children as React.ReactElement, { onClick: () => setOpen(true) })}
    </Modal>
  );
}

function SingleMetric({ condition }: Readonly<{ condition: Condition }>) {
  const { data: isStandard } = useStandardExperienceMode();
  const intl = useIntl();
  const metrics = useMetrics();
  const metric = metrics[condition.metric];
  const mapper = isStandard ? MQR_CONDITIONS_MAP : STANDARD_CONDITIONS_MAP;
  const metricFromOtherMode = mapper[metric.key as MetricKey];

  return (
    <div className="sw-flex sw-justify-between sw-my-8 sw-items-center sw-w-[80%]">
      <div>
        <Text as="div" size={TextSize.Small}>
          {intl.formatMessage({
            id: `quality_gates.update_conditions.${isStandard ? 'mqr' : 'standard'}_mode_header`,
          })}
        </Text>
        <Text as="div" size={TextSize.Small} isHighlighted>
          {getLocalizedMetricNameNoDiffMetric(metrics[condition.metric], metrics)}
        </Text>
      </div>
      <IconArrowRight />
      <div>
        {metricFromOtherMode ? (
          <>
            <Text as="div" size={TextSize.Small}>
              {intl.formatMessage({
                id: `quality_gates.update_conditions.${isStandard ? 'standard' : 'mqr'}_mode_header`,
              })}
            </Text>
            <Text as="div" size={TextSize.Small} isHighlighted>
              {getLocalizedMetricNameNoDiffMetric(metrics[metricFromOtherMode], metrics)}
            </Text>
          </>
        ) : (
          <Text size={TextSize.Small}>
            {intl.formatMessage({ id: 'quality_gates.update_conditions.removed' })}
          </Text>
        )}
      </div>
    </div>
  );
}

function Header() {
  const intl = useIntl();
  const { data: isStandard } = useStandardExperienceMode();

  return (
    <TableRow>
      <ContentCell className="sw-justify-between sw-pr-4">
        <Heading as="h4" className="sw-typo-semibold sw-m-0 sw-whitespace-nowrap">
          {intl.formatMessage({
            id: `quality_gates.update_conditions.${isStandard ? 'mqr' : 'standard'}_mode_header`,
          })}
        </Heading>
        <IconArrowRight />
      </ContentCell>
      <ContentCell>
        <Heading as="h4" className="sw-typo-semibold sw-m-0 sw-whitespace-nowrap">
          {intl.formatMessage({
            id: `quality_gates.update_conditions.${isStandard ? 'standard' : 'mqr'}_mode_header`,
          })}
        </Heading>
      </ContentCell>
      <ContentCell>
        <Heading as="h4" className="sw-typo-semibold sw-m-0 sw-whitespace-nowrap">
          {intl.formatMessage({ id: 'quality_gates.update_conditions.operator_and_value_header' })}
        </Heading>
      </ContentCell>
      <ContentCell />
    </TableRow>
  );
}

function ConditionRow({ condition }: Readonly<{ condition: Condition }>) {
  const { data: isStandard } = useStandardExperienceMode();
  const intl = useIntl();
  const metrics = useMetrics();
  const { op = 'GT' } = condition;
  const metric = metrics[condition.metric];
  const mapper = isStandard ? MQR_CONDITIONS_MAP : STANDARD_CONDITIONS_MAP;
  const metricFromOtherMode = mapper[metric?.key as MetricKey];

  return (
    <TableRow>
      <ContentCell>{getLocalizedMetricNameNoDiffMetric(metric, metrics)}</ContentCell>
      <ContentCell>
        {metricFromOtherMode ? (
          getLocalizedMetricNameNoDiffMetric(metrics[metricFromOtherMode], metrics)
        ) : (
          <Text colorOverride="echoes-color-text-danger">
            {intl.formatMessage({ id: 'quality_gates.update_conditions.removed' })}
          </Text>
        )}
      </ContentCell>

      <ContentCell className="sw-whitespace-nowrap">
        {metricFromOtherMode && (
          <Text isSubdued>
            {getOperatorLabel(op, metric)}&nbsp;
            <ConditionValue condition={condition} metric={metric} />
          </Text>
        )}
      </ContentCell>
    </TableRow>
  );
}
