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

import { Button, Heading, IconQuestionMark, Link, Spinner, Text } from '@sonarsource/echoes-react';
import { uniqBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { FlagMessage, HighlightedSection, Note } from '~design-system';
import DocHelpTooltip from '~sonar-aligned/components/controls/DocHelpTooltip';
import { useAvailableFeatures } from '../../../app/components/available-features/withAvailableFeatures';
import { useMetrics } from '../../../app/components/metrics/withMetricsContext';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { ModalProps } from '../../../components/controls/ModalButton';
import AIAssuredIcon from '../../../components/icon-mappers/AIAssuredIcon';
import { DocLink } from '../../../helpers/doc-links';
import { useDocUrl } from '../../../helpers/docs';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { useStandardExperienceModeQuery } from '../../../queries/mode';
import { MetricKey } from '../../../sonar-aligned/types/metrics';
import { Feature } from '../../../types/features';
import { CaycStatus, Condition as ConditionType, QualityGate } from '../../../types/types';
import {
  groupAndSortByPriorityConditions,
  isQualityGateOptimized,
  MQR_CONDITIONS_MAP,
  STANDARD_CONDITIONS_MAP,
} from '../utils';
import AddConditionModal from './AddConditionModal';
import CaycCompliantBanner from './CaycCompliantBanner';
import CaYCConditionsSimplificationGuide from './CaYCConditionsSimplificationGuide';
import CaycFixOptimizeBanner from './CaycFixOptimizeBanner';
import CaycReviewUpdateConditionsModal from './ConditionReviewAndUpdateModal';
import ConditionsTable from './ConditionsTable';
import CaycCondition from './NewCodeBuiltInCondition';
import AiCondition from './OverallBuiltInCondition';
import QGRecommendedIcon from './QGRecommendedIcon';
import UpdateConditionsFromOtherModeBanner from './UpdateConditionsFromOtherModeBanner';

interface Props {
  isFetching?: boolean;
  qualityGate: QualityGate;
}

export default function Conditions({ qualityGate, isFetching }: Readonly<Props>) {
  const { name, isBuiltIn, actions, conditions = [], caycStatus, isAiCodeSupported } = qualityGate;

  const [editing, setEditing] = React.useState<boolean>(caycStatus === CaycStatus.NonCompliant);
  const metrics = useMetrics();
  const { hasFeature } = useAvailableFeatures();
  const { data: isStandardMode, isLoading } = useStandardExperienceModeQuery();

  const canEdit = Boolean(actions?.manageConditions);
  const existingConditions = conditions.filter((condition) => metrics[condition.metric]);
  const {
    overallCodeConditions,
    newCodeConditions,
    builtInNewCodeConditions,
    builtInOverallConditions,
  } = groupAndSortByPriorityConditions(existingConditions, metrics, isBuiltIn, isAiCodeSupported);
  const isAICodeAssuranceQualityGate = hasFeature(Feature.AiCodeAssurance) && isAiCodeSupported;

  const duplicates: ConditionType[] = [];
  const savedConditions = existingConditions.filter((condition) => condition.id != null);
  savedConditions.forEach((condition) => {
    const sameCount = savedConditions.filter((sample) => sample.metric === condition.metric).length;
    if (sameCount > 1) {
      duplicates.push(condition);
    }
  });

  const uniqDuplicates = uniqBy(duplicates, (d) => d.metric).map((condition) => ({
    ...condition,
    metric: metrics[condition.metric],
  }));

  // set edit only when the name is change
  // i.e when user changes the quality gate
  React.useEffect(() => {
    setEditing(caycStatus === CaycStatus.NonCompliant);
  }, [name]); // eslint-disable-line react-hooks/exhaustive-deps

  const docUrl = useDocUrl(DocLink.CaYC);
  const isCompliantCustomQualityGate = caycStatus !== CaycStatus.NonCompliant && !isBuiltIn;
  const isOptimizing = isCompliantCustomQualityGate && !isQualityGateOptimized(qualityGate);
  const isBuiltInAiCodeSupported = isBuiltIn && isAiCodeSupported;
  const isBuiltInCaYC = isBuiltIn && !isAiCodeSupported;

  const renderCaycModal = React.useCallback(
    ({ onClose }: ModalProps) => {
      return (
        <CaycReviewUpdateConditionsModal
          qualityGate={qualityGate}
          metrics={metrics}
          canEdit={canEdit}
          lockEditing={() => setEditing(false)}
          conditions={conditions}
          scope="new-cayc"
          onClose={onClose}
          isOptimizing={isOptimizing}
        />
      );
    },
    [qualityGate, conditions, metrics, isOptimizing, canEdit],
  );

  const conditionsToOtherModeMap = isStandardMode ? MQR_CONDITIONS_MAP : STANDARD_CONDITIONS_MAP;
  const hasConditionsFromOtherMode =
    qualityGate[isStandardMode ? 'hasMQRConditions' : 'hasStandardConditions'];

  return (
    <Spinner isLoading={isLoading}>
      {isBuiltInCaYC && <CaYCConditionsSimplificationGuide qualityGate={qualityGate} />}
      {isAICodeAssuranceQualityGate && (
        <div className="sw-flex sw-items-center">
          <AIAssuredIcon className="sw-mr-1" />
          <Text isSubdued>
            <FormattedMessage
              defaultMessage="quality_gates.ai_generated.description"
              id="quality_gates.ai_generated.description"
              values={{
                link: (
                  <DocumentationLink shouldOpenInNewTab to={DocLink.AiCodeAssurance}>
                    {translate('quality_gates.ai_generated.description.clean_ai_generated_code')}
                  </DocumentationLink>
                ),
              }}
            />
          </Text>
        </div>
      )}
      {isBuiltInAiCodeSupported && (
        <div className="sw-flex sw-items-center sw-mt-2">
          <QGRecommendedIcon className="sw-mr-1" />
          <Text isSubdued>
            <FormattedMessage
              defaultMessage="quality_gates.is_built_in.ai.description"
              id="quality_gates.is_built_in.ai.description"
              values={{
                link: (
                  <DocumentationLink shouldOpenInNewTab to={DocLink.CaYC}>
                    {translate('clean_as_you_code')}
                  </DocumentationLink>
                ),
              }}
            />
          </Text>
        </div>
      )}
      {isBuiltInCaYC && (
        <div className="sw-flex sw-items-center sw-mt-2">
          <QGRecommendedIcon className="sw-mr-1" />
          <Text isSubdued>
            <FormattedMessage
              defaultMessage="quality_gates.is_built_in.cayc.description"
              id="quality_gates.is_built_in.cayc.description"
              values={{
                link: (
                  <DocumentationLink to={DocLink.CaYC}>
                    {translate('clean_as_you_code')}
                  </DocumentationLink>
                ),
              }}
            />
          </Text>
        </div>
      )}
      {(!hasConditionsFromOtherMode || !canEdit) &&
        isCompliantCustomQualityGate &&
        !isOptimizing && <CaycCompliantBanner />}
      {!hasConditionsFromOtherMode && isCompliantCustomQualityGate && isOptimizing && canEdit && (
        <CaycFixOptimizeBanner renderCaycModal={renderCaycModal} isOptimizing />
      )}
      {!hasConditionsFromOtherMode && caycStatus === CaycStatus.NonCompliant && canEdit && (
        <CaycFixOptimizeBanner renderCaycModal={renderCaycModal} />
      )}
      {hasConditionsFromOtherMode && canEdit && (
        <UpdateConditionsFromOtherModeBanner
          qualityGateName={qualityGate.name}
          newCodeConditions={newCodeConditions.filter(
            (c) => conditionsToOtherModeMap[c.metric as MetricKey] !== undefined,
          )}
          overallCodeConditions={overallCodeConditions.filter(
            (c) => conditionsToOtherModeMap[c.metric as MetricKey] !== undefined,
          )}
        />
      )}

      <header className="sw-flex sw-items-center sw-mt-9 sw-mb-4 sw-justify-between">
        <div className="sw-flex">
          <Heading as="h2" className="sw-typo-lg-semibold sw-m-0">
            {translate('quality_gates.conditions')}
          </Heading>
          {!isBuiltIn && (
            <DocHelpTooltip
              className="sw-ml-2"
              content={translate('quality_gates.conditions.help')}
              links={[
                {
                  href: DocLink.CaYC,
                  label: translate('quality_gates.conditions.help.link'),
                },
              ]}
            >
              <IconQuestionMark />
            </DocHelpTooltip>
          )}
          {isBuiltIn && (
            <DocHelpTooltip
              className="sw-ml-2"
              content={translate('quality_gates.conditions.cayc.hint')}
              placement="right"
            >
              <IconQuestionMark />
            </DocHelpTooltip>
          )}
          <Spinner isLoading={isFetching} className="sw-ml-4 sw-mt-1" />
        </div>
        <div>
          {(caycStatus === CaycStatus.NonCompliant || editing) && canEdit && (
            <AddConditionModal qualityGate={qualityGate} />
          )}
        </div>
      </header>
      {uniqDuplicates.length > 0 && (
        <FlagMessage variant="warning" className="sw-flex sw-mb-4">
          <div>
            <p>{translate('quality_gates.duplicated_conditions')}</p>
            <ul className="sw-my-2 sw-list-disc sw-pl-10">
              {uniqDuplicates.map((d) => (
                <li key={d.metric.key}>{getLocalizedMetricName(d.metric)}</li>
              ))}
            </ul>
          </div>
        </FlagMessage>
      )}
      <div className="sw-flex sw-flex-col sw-gap-8">
        {builtInNewCodeConditions.length > 0 && (
          <div>
            <div className="sw-flex sw-items-center sw-gap-2 sw-mb-2">
              <Heading as="h3">
                {isBuiltInAiCodeSupported
                  ? translate('quality_gates.conditions.new_code', 'long')
                  : translate('quality_gates.conditions.cayc')}
              </Heading>
            </div>

            <HighlightedSection className="sw-p-0 sw-my-2 sw-w-3/4" id="cayc-highlight">
              <ul
                className="sw-my-2"
                aria-label={translate('quality_gates.cayc.condition_simplification_list')}
              >
                {builtInNewCodeConditions.map((condition) => (
                  <CaycCondition
                    key={condition.id}
                    condition={condition}
                    metric={metrics[condition.metric]}
                  />
                ))}
              </ul>
            </HighlightedSection>

            {hasFeature(Feature.BranchSupport) && (
              <Note className="sw-mb-2 sw-typo-default">
                {translate('quality_gates.conditions.cayc', 'description')}
              </Note>
            )}
          </div>
        )}

        {newCodeConditions.length > 0 && (
          <div>
            <div className="sw-flex sw-justify-between">
              <Heading as="h3" className="sw-mb-2">
                {translate('quality_gates.conditions.new_code', 'long')}
              </Heading>
              {hasFeature(Feature.BranchSupport) && (
                <Note className="sw-mb-2 sw-typo-default">
                  {translate('quality_gates.conditions.new_code', 'description')}
                </Note>
              )}
            </div>

            <ConditionsTable
              qualityGate={qualityGate}
              metrics={metrics}
              canEdit={canEdit}
              conditions={newCodeConditions}
              showEdit={editing}
              scope="new"
            />
          </div>
        )}

        {overallCodeConditions.length > 0 && (
          <div className="sw-mt-5">
            <div className="sw-flex sw-justify-between">
              <Heading as="h3" className="sw-mb-2">
                {translate('quality_gates.conditions.overall_code', 'long')}
              </Heading>
              {hasFeature(Feature.BranchSupport) && (
                <Note className="sw-mb-2 sw-typo-default">
                  {translate('quality_gates.conditions.overall_code', 'description')}
                </Note>
              )}
            </div>

            <ConditionsTable
              qualityGate={qualityGate}
              metrics={metrics}
              canEdit={canEdit}
              conditions={overallCodeConditions}
              scope="overall"
            />
          </div>
        )}
        {builtInOverallConditions.length > 0 && (
          <div>
            <div className="sw-flex sw-items-center sw-gap-2 sw-mb-2">
              <Heading as="h3" className="sw-mb-2">
                {translate('quality_gates.conditions.overall_code', 'long')}
              </Heading>
            </div>

            <HighlightedSection className="sw-p-0 sw-my-2 sw-w-3/4" id="ai-highlight">
              <ul
                className="sw-my-2"
                aria-label={translate('quality_gates.cayc.condition_simplification_list')}
              >
                {builtInOverallConditions.map((condition) => (
                  <AiCondition
                    key={condition.id}
                    condition={condition}
                    metric={metrics[condition.metric]}
                  />
                ))}
              </ul>
            </HighlightedSection>
          </div>
        )}
      </div>
      {caycStatus !== CaycStatus.NonCompliant && !editing && canEdit && (
        <div className="sw-mt-4 it__qg-unfollow-cayc">
          <div>
            <FormattedMessage
              id="quality_gates.cayc_unfollow.description"
              defaultMessage={translate('quality_gates.cayc_unfollow.description')}
              values={{
                cayc_link: <Link to={docUrl}>{translate('quality_gates.cayc')}</Link>,
              }}
            />
          </div>
          <Button className="sw-mt-2" onClick={() => setEditing(true)}>
            {translate('quality_gates.cayc.unlock_edit')}
          </Button>
        </div>
      )}
      {existingConditions.length === 0 && (
        <div className="sw-mt-4 sw-typo-default">
          <Text as="p">{translate('quality_gates.no_conditions')}</Text>
        </div>
      )}
    </Spinner>
  );
}
