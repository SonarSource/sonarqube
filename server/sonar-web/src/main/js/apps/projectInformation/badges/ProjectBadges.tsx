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

import { Spinner } from '@sonarsource/echoes-react';
import {
  BasicSeparator,
  ButtonSecondary,
  CodeSnippet,
  FlagMessage,
  FormField,
  IllustratedSelectionCard,
  InputSelect,
  SubTitle,
  ToggleButton,
} from 'design-system';
import { isEmpty } from 'lodash';
import * as React from 'react';
import { useState } from 'react';
import { Image } from '~sonar-aligned/components/common/Image';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { useAvailableFeatures } from '../../../app/components/available-features/withAvailableFeatures';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { localizeMetric } from '../../../helpers/measures';
import {
  DEPRECATED_METRIC_KEYS,
  useBadgeMetricsQuery,
  useBadgeTokenQuery,
  useRenewBagdeTokenMutation,
} from '../../../queries/badges';
import { BranchLike } from '../../../types/branch-like';
import { isProject } from '../../../types/component';
import { Feature } from '../../../types/features';
import { Component } from '../../../types/types';
import { BadgeFormats, BadgeOptions, BadgeType, getBadgeSnippet, getBadgeUrl } from './utils';

export interface ProjectBadgesProps {
  branchLike?: BranchLike;
  component: Component;
}

export default function ProjectBadges(props: ProjectBadgesProps) {
  const {
    branchLike,
    component: { key: project, qualifier, configuration },
  } = props;
  const [selectedType, setSelectedType] = useState(BadgeType.measure);
  const [selectedMetric, setSelectedMetric] = useState(MetricKey.alert_status);
  const [selectedFormat, setSelectedFormat] = useState<BadgeFormats>('md');
  const {
    data: token,
    isLoading: isLoadingToken,
    isFetching: isFetchingToken,
  } = useBadgeTokenQuery(project);
  const { data: metricOptions, isLoading: isLoadingMetrics } = useBadgeMetricsQuery();
  const { mutate: renewToken, isPending: isRenewing } = useRenewBagdeTokenMutation();
  const { hasFeature } = useAvailableFeatures();
  const isLoading = isLoadingMetrics || isLoadingToken || isRenewing;

  const handleSelectType = (selectedType: BadgeType) => {
    setSelectedType(selectedType);
  };

  const formatOptions = [
    {
      label: translate('overview.badges.options.formats.md'),
      value: 'md',
    },
    {
      label: translate('overview.badges.options.formats.url'),
      value: 'url',
    },
  ] as const;

  const fullBadgeOptions: BadgeOptions = {
    project,
    metric: selectedMetric,
    format: selectedFormat,
    ...getBranchLikeQuery(branchLike),
  };
  const canRenew = configuration?.showSettings;

  return (
    <div>
      <SubTitle>{translate('overview.badges.get_badge')}</SubTitle>
      <p className="sw-mb-4">{translate('overview.badges.description', qualifier)}</p>

      <Spinner isLoading={isLoading || isEmpty(token)}>
        <div className="sw-flex sw-space-x-4 sw-mb-4">
          <IllustratedSelectionCard
            className="sw-w-abs-300 it__badge-button"
            onClick={() => handleSelectType(BadgeType.measure)}
            selected={BadgeType.measure === selectedType}
            image={
              <Image
                alt={translate('overview.badges', BadgeType.measure, 'alt')}
                src={getBadgeUrl(BadgeType.measure, fullBadgeOptions, token, true)}
              />
            }
            description={translate('overview.badges', BadgeType.measure, 'description', qualifier)}
          />
          <IllustratedSelectionCard
            className="sw-w-abs-300 it__badge-button"
            onClick={() => handleSelectType(BadgeType.qualityGate)}
            selected={BadgeType.qualityGate === selectedType}
            image={
              <Image
                alt={translate('overview.badges', BadgeType.qualityGate, 'alt')}
                src={getBadgeUrl(BadgeType.qualityGate, fullBadgeOptions, token, true)}
                width="128px"
              />
            }
            description={translate(
              'overview.badges',
              BadgeType.qualityGate,
              'description',
              qualifier,
            )}
          />
          {hasFeature(Feature.AiCodeAssurance) && isProject(qualifier) && (
            <IllustratedSelectionCard
              className="sw-w-abs-300 it__badge-button"
              onClick={() => handleSelectType(BadgeType.aiCodeAssurance)}
              selected={BadgeType.aiCodeAssurance === selectedType}
              image={
                <Image
                  alt={translate('overview.badges', BadgeType.aiCodeAssurance, 'alt')}
                  src={getBadgeUrl(BadgeType.aiCodeAssurance, fullBadgeOptions, token, true)}
                />
              }
              description={translate(
                'overview.badges',
                BadgeType.aiCodeAssurance,
                'description',
                qualifier,
              )}
            />
          )}
        </div>
      </Spinner>

      {BadgeType.measure === selectedType && (
        <>
          <FormField htmlFor="badge-param-customize" label={translate('overview.badges.metric')}>
            <InputSelect
              className="sw-w-abs-300"
              inputId="badge-param-customize"
              options={metricOptions}
              onChange={(option) => {
                if (option) {
                  setSelectedMetric(option.value);
                }
              }}
              value={metricOptions.find((m) => m.value === selectedMetric)}
            />
          </FormField>

          {DEPRECATED_METRIC_KEYS.includes(selectedMetric) && (
            <FlagMessage className="sw-mb-4" variant="warning">
              {translateWithParameters(
                'overview.badges.deprecated_badge_x_y',
                localizeMetric(selectedMetric),
                translate('qualifier', qualifier),
              )}
            </FlagMessage>
          )}
        </>
      )}

      <BasicSeparator className="sw-mb-4" />

      <FormField label={translate('overview.badges.format')}>
        <div className="sw-flex ">
          <ToggleButton
            label={translate('overview.badges.format')}
            options={formatOptions}
            onChange={(value: BadgeFormats) => {
              if (value) {
                setSelectedFormat(value);
              }
            }}
            value={selectedFormat}
          />
        </div>
      </FormField>

      <Spinner className="sw-my-2" isLoading={isFetchingToken || isRenewing}>
        {!isLoading && (
          <CodeSnippet
            language="plaintext"
            className="sw-p-6 it__code-snippet"
            snippet={getBadgeSnippet(selectedType, fullBadgeOptions, token)}
            wrap
          />
        )}
      </Spinner>

      <FlagMessage className="sw-w-full" variant="warning">
        <p>
          {translate('overview.badges.leak_warning')}
          {canRenew && (
            <span className="sw-flex sw-flex-col">
              {translate('overview.badges.renew.description')}{' '}
              <ButtonSecondary
                disabled={isLoading}
                className="sw-mt-2 it__project-info-renew-badge sw-mr-auto"
                onClick={() => {
                  renewToken(project);
                }}
              >
                {translate('overview.badges.renew')}
              </ButtonSecondary>
            </span>
          )}
        </p>
      </FlagMessage>
    </div>
  );
}
