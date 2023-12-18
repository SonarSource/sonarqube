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

import styled from '@emotion/styled';
import classNames from 'classnames';
import {
  Card,
  HighImpactCircleIcon,
  LightLabel,
  PageTitle,
  SnoozeCircleIcon,
  Spinner,
  themeColor,
} from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { getLeakValue } from '../../../components/measure/utils';
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { findMeasure, formatMeasure } from '../../../helpers/measures';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import { Branch } from '../../../types/branch-like';
import { SoftwareImpactSeverity } from '../../../types/clean-code-taxonomy';
import { IssueStatus } from '../../../types/issues';
import { MetricKey, MetricType } from '../../../types/metrics';
import { Component, MeasureEnhanced } from '../../../types/types';
import { IssueMeasuresCardInner } from '../components/IssueMeasuresCardInner';

export interface AcceptedIssuesPanelProps {
  branch?: Branch;
  component: Component;
  measures?: MeasureEnhanced[];
  isNewCode: boolean;
  loading?: boolean;
}

function AcceptedIssuesPanel(props: Readonly<AcceptedIssuesPanelProps>) {
  const { branch, component, measures = [], isNewCode, loading } = props;
  const intl = useIntl();

  const acceptedIssuesUrl = getComponentIssuesUrl(component.key, {
    ...getBranchLikeQuery(branch),
    issueStatuses: IssueStatus.Accepted,
    ...(isNewCode ? { inNewCodePeriod: 'true' } : {}),
  });

  const acceptedIssuesWithHighImpactUrl = getComponentIssuesUrl(component.key, {
    ...getBranchLikeQuery(branch),
    ...DEFAULT_ISSUES_QUERY,
    issueStatuses: IssueStatus.Accepted,
    impactSeverities: SoftwareImpactSeverity.High,
  });

  const acceptedCount = isNewCode
    ? getLeakValue(findMeasure(measures, MetricKey.new_accepted_issues))
    : findMeasure(measures, MetricKey.accepted_issues)?.value;

  const acceptedWithHighImpactCount = isNewCode
    ? undefined
    : findMeasure(measures, MetricKey.high_impact_accepted_issues)?.value;

  return (
    <div className="sw-mt-8">
      <PageTitle as="h2" text={intl.formatMessage({ id: 'overview.accepted_issues' })} />
      <LightLabel as="div" className="sw-mt-1 sw-mb-4">
        {intl.formatMessage({ id: 'overview.accepted_issues.description' })}
      </LightLabel>
      <Spinner loading={loading}>
        <div
          className={classNames('sw-grid sw-gap-4', {
            'sw-grid-cols-2': isNewCode,
            'sw-grid-cols-1': !isNewCode,
          })}
        >
          <Card className="sw-flex sw-gap-4">
            <IssueMeasuresCardInner
              className={classNames({ 'sw-w-1/2': !isNewCode, 'sw-w-full': isNewCode })}
              metric={MetricKey.accepted_issues}
              value={formatMeasure(acceptedCount, MetricType.ShortInteger)}
              header={intl.formatMessage({
                id: isNewCode ? 'overview.accepted_issues' : 'overview.accepted_issues.total',
              })}
              url={acceptedIssuesUrl}
              icon={
                <SnoozeCircleIcon className="sw--translate-y-3" neutral={acceptedCount === '0'} />
              }
            />
            {!isNewCode && (
              <>
                <StyledCardSeparator />
                <IssueMeasuresCardInner
                  className="sw-w-1/2"
                  metric={MetricKey.high_impact_accepted_issues}
                  value={formatMeasure(acceptedWithHighImpactCount, MetricType.ShortInteger)}
                  header={intl.formatMessage({
                    id: `metric.${MetricKey.high_impact_accepted_issues}.name`,
                  })}
                  url={acceptedIssuesWithHighImpactUrl}
                  icon={<HighImpactCircleIcon className="sw--translate-y-3" />}
                />
              </>
            )}
          </Card>
        </div>
      </Spinner>
    </div>
  );
}

const StyledCardSeparator = styled.div`
  width: 1px;
  background-color: ${themeColor('projectCardBorder')};
`;

export default React.memo(AcceptedIssuesPanel);
