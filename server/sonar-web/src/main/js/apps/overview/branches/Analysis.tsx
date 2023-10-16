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
import { QualityGateIndicator } from 'design-system';
import { sortBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { translate } from '../../../helpers/l10n';
import { ComponentQualifier } from '../../../types/component';
import {
  AnalysisMeasuresVariations,
  ProjectAnalysisEventCategory,
  Analysis as TypeAnalysis,
} from '../../../types/project-activity';
import { Status } from '../../../types/types';
import { AnalysisVariations } from './AnalysisVariations';
import Event from './Event';

export interface AnalysisProps {
  analysis: TypeAnalysis;
  isFirstAnalysis?: boolean;
  qualifier: string;
  qualityGateStatus?: string;
  variations?: AnalysisMeasuresVariations;
}

export function Analysis(props: Readonly<AnalysisProps>) {
  const { analysis, isFirstAnalysis, qualifier, qualityGateStatus, variations } = props;

  const sortedEvents = sortBy(
    analysis.events.filter((event) => event.category !== ProjectAnalysisEventCategory.QualityGate),
    (event) => {
      switch (event.category) {
        case ProjectAnalysisEventCategory.Version:
          // versions first
          return 0;
        case ProjectAnalysisEventCategory.SqUpgrade:
          // SQ Upgrade second
          return 1;
        default:
          // then the rest sorted by category
          return 2;
      }
    },
    'category',
  );

  // use `TRK` for all components but applications
  const displayedQualifier =
    qualifier === ComponentQualifier.Application
      ? ComponentQualifier.Application
      : ComponentQualifier.Project;

  return (
    <div className="sw-body-sm">
      <div className="sw-flex sw-justify-between sw-mb-1">
        <div className="sw-body-sm-highlight">
          <DateTimeFormatter date={analysis.date} />
        </div>
        {qualityGateStatus !== undefined && (
          <div className="sw-flex sw-items-center">
            <FormattedMessage
              id="overview.quality_gate_x"
              values={{
                0: (
                  <QualityGateIndicator
                    className="sw-mx-2"
                    size="sm"
                    status={qualityGateStatus as Status}
                  />
                ),
              }}
            />
            <span className="sw-body-sm-highlight">
              <FormattedMessage id={`metric.level.${qualityGateStatus}`} />
            </span>
          </div>
        )}
      </div>

      {sortedEvents.length > 0
        ? sortedEvents.map((event) => <Event event={event} key={event.key} />)
        : translate('project_activity.analyzed', displayedQualifier)}

      {qualifier === ComponentQualifier.Project && variations !== undefined && (
        <AnalysisVariations isFirstAnalysis={isFirstAnalysis} variations={variations} />
      )}
    </div>
  );
}

export default React.memo(Analysis);
