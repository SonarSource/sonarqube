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
import { Card, CoverageIndicator, DuplicationsIndicator } from 'design-system';
import * as React from 'react';
import { duplicationRatingConverter } from '../../../components/measure/utils';
import { findMeasure } from '../../../helpers/measures';
import { Branch } from '../../../types/branch-like';
import { IssueType } from '../../../types/issues';
import { MetricKey } from '../../../types/metrics';
import { Component, MeasureEnhanced } from '../../../types/types';
import { MeasurementType } from '../utils';
import MeasuresPanelIssueMeasure from './MeasuresPanelIssueMeasure';
import MeasuresPanelPercentMeasure from './MeasuresPanelPercentMeasure';

export interface MeasuresPanelProps {
  branch?: Branch;
  component: Component;
  measures: MeasureEnhanced[];
  isNewCode: boolean;
}

export function MeasuresPanel(props: MeasuresPanelProps) {
  const { branch, component, measures, isNewCode } = props;

  return (
    <div className="sw-grid sw-grid-cols-2 sw-gap-4 sw-mt-4">
      {[IssueType.Bug, IssueType.CodeSmell, IssueType.Vulnerability, IssueType.SecurityHotspot].map(
        (type: IssueType) => (
          <Card key={type} className="sw-p-8">
            <MeasuresPanelIssueMeasure
              branchLike={branch}
              component={component}
              isNewCodeTab={isNewCode}
              measures={measures}
              type={type}
            />
          </Card>
        ),
      )}

      {(findMeasure(measures, MetricKey.coverage) ||
        findMeasure(measures, MetricKey.new_coverage)) && (
        <Card className="sw-p-8" data-test="overview__measures-coverage">
          <MeasuresPanelPercentMeasure
            branchLike={branch}
            component={component}
            measures={measures}
            ratingIcon={renderCoverageIcon}
            secondaryMetricKey={MetricKey.tests}
            type={MeasurementType.Coverage}
            useDiffMetric={isNewCode}
          />
        </Card>
      )}

      <Card className="sw-p-8">
        <MeasuresPanelPercentMeasure
          branchLike={branch}
          component={component}
          measures={measures}
          ratingIcon={renderDuplicationIcon}
          secondaryMetricKey={MetricKey.duplicated_blocks}
          type={MeasurementType.Duplication}
          useDiffMetric={isNewCode}
        />
      </Card>
    </div>
  );
}

export default React.memo(MeasuresPanel);

function renderCoverageIcon(value?: string) {
  return <CoverageIndicator value={value} size="md" />;
}

function renderDuplicationIcon(value?: string) {
  const rating = value !== undefined ? duplicationRatingConverter(Number(value)) : undefined;

  return <DuplicationsIndicator rating={rating} size="md" />;
}
