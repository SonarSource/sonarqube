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
  ButtonSecondary,
  FlagMessage,
  HeadingDark,
  HelperHintIcon,
  HighlightedSection,
  LightLabel,
  LightPrimary,
  Link,
  Note,
  Spinner,
  SubHeading,
} from 'design-system';
import { uniqBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocHelpTooltip from '~sonar-aligned/components/controls/DocHelpTooltip';
import { useAvailableFeatures } from '../../../app/components/available-features/withAvailableFeatures';
import { useMetrics } from '../../../app/components/metrics/withMetricsContext';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { ModalProps } from '../../../components/controls/ModalButton';
import { DocLink } from '../../../helpers/doc-links';
import { useDocUrl } from '../../../helpers/docs';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { Feature } from '../../../types/features';
import { CaycStatus, Condition as ConditionType, QualityGate } from '../../../types/types';
import { groupAndSortByPriorityConditions, isQualityGateOptimized } from '../utils';
import AddConditionModal from './AddConditionModal';
import AIGeneratedIcon from './AIGeneratedIcon';
import CaycCompliantBanner from './CaycCompliantBanner';
import CaycCondition from './CaycCondition';
import CaYCConditionsSimplificationGuide from './CaYCConditionsSimplificationGuide';
import CaycFixOptimizeBanner from './CaycFixOptimizeBanner';
import CaycReviewUpdateConditionsModal from './ConditionReviewAndUpdateModal';
import ConditionsTable from './ConditionsTable';
import QGRecommendedIcon from './QGRecommendedIcon';

interface Props {
  organization: string
  isFetching?: boolean;
  qualityGate: QualityGate;
}

export default function Conditions({ organization, qualityGate, isFetching }: Readonly<Props>) {
  const { name, isBuiltIn, actions, conditions = [], caycStatus } = qualityGate;

  const [editing, setEditing] = React.useState<boolean>(caycStatus === CaycStatus.NonCompliant);
  const metrics = useMetrics();
  const { hasFeature } = useAvailableFeatures();

  const canEdit = Boolean(actions?.manageConditions);
  const existingConditions = conditions.filter((condition) => metrics[condition.metric]);
  const { overallCodeConditions, newCodeConditions, caycConditions } =
    groupAndSortByPriorityConditions(existingConditions, metrics, isBuiltIn);
  const isAICodeAssuranceQualityGate =
    hasFeature(Feature.AiCodeAssurance) && isBuiltIn && name === 'Sonar way';

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

  const renderCaycModal = React.useCallback(
    ({ onClose }: ModalProps) => {
      return (
        <CaycReviewUpdateConditionsModal
          organization={organization}
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

  return (
    <div>
      <CaYCConditionsSimplificationGuide qualityGate={qualityGate} />

      {isBuiltIn && (
        <div className="sw-flex sw-items-center">
          <QGRecommendedIcon className="sw-mr-1" />
          <LightLabel>
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
          </LightLabel>
        </div>
      )}

      {isAICodeAssuranceQualityGate && (
        <div className="sw-flex sw-items-center sw-mt-2">
          <AIGeneratedIcon className="sw-mr-1" />
          <LightLabel>
            <FormattedMessage
              defaultMessage="quality_gates.ai_generated.description"
              id="quality_gates.ai_generated.description"
              values={{
                link: (
                  <DocumentationLink to={DocLink.AiCodeAssurance}>
                    {translate('quality_gates.ai_generated.description.clean_ai_generated_code')}
                  </DocumentationLink>
                ),
              }}
            />
          </LightLabel>
        </div>
      )}

      {isCompliantCustomQualityGate && !isOptimizing && <CaycCompliantBanner />}
      {isCompliantCustomQualityGate && isOptimizing && canEdit && (
        <CaycFixOptimizeBanner renderCaycModal={renderCaycModal} isOptimizing />
      )}
      {caycStatus === CaycStatus.NonCompliant && canEdit && (
        <CaycFixOptimizeBanner renderCaycModal={renderCaycModal} />
      )}

      <header className="sw-flex sw-items-center sw-mt-9 sw-mb-4 sw-justify-between">
        <div className="sw-flex">
          <HeadingDark className="sw-typo-lg-semibold sw-m-0">
            {translate('quality_gates.conditions')}
          </HeadingDark>
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
              <HelperHintIcon />
            </DocHelpTooltip>
          )}
          <Spinner loading={isFetching} className="sw-ml-4 sw-mt-1" />
        </div>
        <div>
          {(caycStatus === CaycStatus.NonCompliant || editing) && canEdit && (
            <AddConditionModal organization={organization} qualityGate={qualityGate} />
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
        {caycConditions.length > 0 && (
          <div>
            <div className="sw-flex sw-items-center sw-gap-2 sw-mb-2">
              <HeadingDark as="h3">{translate('quality_gates.conditions.cayc')}</HeadingDark>
              <DocHelpTooltip
                content={translate('quality_gates.conditions.cayc.hint')}
                placement="right"
              >
                <HelperHintIcon />
              </DocHelpTooltip>
            </div>

            <HighlightedSection className="sw-p-0 sw-my-2 sw-w-3/4" id="cayc-highlight">
              <ul
                className="sw-my-2"
                aria-label={translate('quality_gates.cayc.condition_simplification_list')}
              >
                {caycConditions.map((condition) => (
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
            <HeadingDark as="h3" className="sw-mb-2">
              {translate('quality_gates.conditions.new_code', 'long')}
            </HeadingDark>

            <ConditionsTable
              organization={organization}
              qualityGate={qualityGate}
              metrics={metrics}
              canEdit={canEdit}
              conditions={newCodeConditions}
              showEdit={editing}
              scope="new"
            />

            {hasFeature(Feature.BranchSupport) && (
              <Note className="sw-mb-2 sw-typo-default">
                {translate('quality_gates.conditions.new_code', 'description')}
              </Note>
            )}
          </div>
        )}

        {overallCodeConditions.length > 0 && (
          <div className="sw-mt-5">
            <HeadingDark as="h3" className="sw-mb-2">
              {translate('quality_gates.conditions.overall_code', 'long')}
            </HeadingDark>

            <ConditionsTable
              organization={organization}
              qualityGate={qualityGate}
              metrics={metrics}
              canEdit={canEdit}
              conditions={overallCodeConditions}
              scope="overall"
            />

            {hasFeature(Feature.BranchSupport) && (
              <Note className="sw-mb-2 sw-typo-default">
                {translate('quality_gates.conditions.overall_code', 'description')}
              </Note>
            )}
          </div>
        )}
      </div>

      {caycStatus !== CaycStatus.NonCompliant && !editing && canEdit && (
        <div className="sw-mt-4 it__qg-unfollow-cayc">
          <SubHeading as="p" className="sw-mb-2 sw-typo-default">
            <FormattedMessage
              id="quality_gates.cayc_unfollow.description"
              defaultMessage={translate('quality_gates.cayc_unfollow.description')}
              values={{
                cayc_link: <Link to={docUrl}>{translate('quality_gates.cayc')}</Link>,
              }}
            />
          </SubHeading>
          <ButtonSecondary className="sw-mt-2" onClick={() => setEditing(true)}>
            {translate('quality_gates.cayc.unlock_edit')}
          </ButtonSecondary>
        </div>
      )}

      {existingConditions.length === 0 && (
        <div className="sw-mt-4 sw-typo-default">
          <LightPrimary as="p">{translate('quality_gates.no_conditions')}</LightPrimary>
        </div>
      )}
    </div>
  );
}
