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
import { ButtonPrimary, FormField, Modal, RadioButton } from 'design-system';
import * as React from 'react';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import {
  useCreateConditionMutation,
  useUpdateConditionMutation,
} from '../../../queries/quality-gates';
import { Condition, Metric, QualityGate } from '../../../types/types';
import { getPossibleOperators } from '../utils';
import ConditionOperator from './ConditionOperator';
import MetricSelect from './MetricSelect';
import ThresholdInput from './ThresholdInput';

interface Props {
  condition?: Condition;
  metric?: Metric;
  metrics?: Metric[];
  header: string;
  onClose: () => void;
  qualityGate: QualityGate;
}

const ADD_CONDITION_MODAL_ID = 'add-condition-modal';

export default function ConditionModal({
  condition,
  metric,
  metrics,
  header,
  onClose,
  qualityGate,
}: Readonly<Props>) {
  const [errorThreshold, setErrorThreshold] = React.useState(condition ? condition.error : '');
  const [scope, setScope] = React.useState<'new' | 'overall'>('new');
  const [selectedMetric, setSelectedMetric] = React.useState<Metric | undefined>(metric);
  const [selectedOperator, setSelectedOperator] = React.useState<string | undefined>(
    condition ? condition.op : undefined,
  );
  const { mutateAsync: createCondition } = useCreateConditionMutation(qualityGate.name);
  const { mutateAsync: updateCondition } = useUpdateConditionMutation(qualityGate.name);

  const getSinglePossibleOperator = (metric: Metric) => {
    const operators = getPossibleOperators(metric);
    return Array.isArray(operators) ? undefined : operators;
  };

  const handleFormSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (selectedMetric) {
      const newCondition: Omit<Condition, 'id'> = {
        metric: selectedMetric.key,
        op: getSinglePossibleOperator(selectedMetric) ?? selectedOperator,
        error: errorThreshold,
      };
      const submitPromise = condition
        ? updateCondition({ id: condition.id, ...newCondition })
        : createCondition(newCondition);
      await submitPromise;
      onClose();
    }
  };

  const handleScopeChange = (scope: 'new' | 'overall') => {
    let correspondingMetric;

    if (selectedMetric && metrics) {
      const correspondingMetricKey =
        scope === 'new' ? `new_${selectedMetric.key}` : selectedMetric.key.replace(/^new_/, '');
      correspondingMetric = metrics.find((m) => m.key === correspondingMetricKey);
    }

    setScope(scope);
    setSelectedMetric(correspondingMetric);
  };

  const handleMetricChange = (metric: Metric) => {
    setSelectedMetric(metric);
    setSelectedOperator(undefined);
    setErrorThreshold('');
  };

  const handleOperatorChange = (op: string) => {
    setSelectedOperator(op);
  };

  const handleErrorChange = (error: string) => {
    setErrorThreshold(error);
  };

  const renderBody = () => {
    return (
      <form id={ADD_CONDITION_MODAL_ID} onSubmit={handleFormSubmit}>
        {metric === undefined && (
          <FormField label={translate('quality_gates.conditions.where')}>
            <div className="sw-flex sw-gap-4">
              <RadioButton checked={scope === 'new'} onCheck={handleScopeChange} value="new">
                <span data-test="quality-gates__condition-scope-new">
                  {translate('quality_gates.conditions.new_code')}
                </span>
              </RadioButton>
              <RadioButton
                checked={scope === 'overall'}
                onCheck={handleScopeChange}
                value="overall"
              >
                <span data-test="quality-gates__condition-scope-overall">
                  {translate('quality_gates.conditions.overall_code')}
                </span>
              </RadioButton>
            </div>
          </FormField>
        )}

        <FormField
          description={metric && getLocalizedMetricName(metric)}
          htmlFor="condition-metric"
          label={translate('quality_gates.conditions.fails_when')}
        >
          {metrics && (
            <MetricSelect
              metric={selectedMetric}
              metricsArray={metrics.filter((m) =>
                scope === 'new' ? isDiffMetric(m.key) : !isDiffMetric(m.key),
              )}
              onMetricChange={handleMetricChange}
            />
          )}
        </FormField>

        {selectedMetric && (
          <div className="sw-flex sw-gap-2">
            <FormField
              className="sw-mb-0"
              htmlFor="condition-operator"
              label={translate('quality_gates.conditions.operator')}
            >
              <ConditionOperator
                metric={selectedMetric}
                onOperatorChange={handleOperatorChange}
                op={selectedOperator}
              />
            </FormField>
            <FormField
              htmlFor="condition-threshold"
              label={translate('quality_gates.conditions.value')}
            >
              <ThresholdInput
                metric={selectedMetric}
                name="error"
                onChange={handleErrorChange}
                value={errorThreshold}
              />
            </FormField>
          </div>
        )}
      </form>
    );
  };

  return (
    <Modal
      isScrollable={false}
      isOverflowVisible
      headerTitle={header}
      onClose={onClose}
      body={renderBody()}
      primaryButton={
        <ButtonPrimary
          autoFocus
          disabled={selectedMetric === undefined}
          id="add-condition-button"
          form={ADD_CONDITION_MODAL_ID}
          type="submit"
        >
          {header}
        </ButtonPrimary>
      }
      secondaryButtonLabel={translate('close')}
    />
  );
}
