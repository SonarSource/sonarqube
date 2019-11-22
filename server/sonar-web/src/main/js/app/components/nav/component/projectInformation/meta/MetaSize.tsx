/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as React from 'react';
import SizeRating from 'sonar-ui-common/components/ui/SizeRating';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure, localizeMetric } from 'sonar-ui-common/helpers/measures';
import DrilldownLink from '../../../../../../components/shared/DrilldownLink';
import { ComponentQualifier } from '../../../../../../types/component';
import { MetricKey } from '../../../../../../types/metrics';

export interface MetaSizeProps {
  component: T.Component;
  measures: T.Measure[];
}

export default function MetaSize({ component, measures }: MetaSizeProps) {
  const isApp = component.qualifier === ComponentQualifier.Application;
  const ncloc = measures.find(measure => measure.metric === MetricKey.ncloc);
  const projects = isApp
    ? measures.find(measure => measure.metric === MetricKey.projects)
    : undefined;

  return (
    <>
      <h3>{localizeMetric(MetricKey.ncloc)}</h3>
      <div className="display-flex-center">
        {ncloc ? (
          <>
            <DrilldownLink className="huge" component={component.key} metric={MetricKey.ncloc}>
              {formatMeasure(ncloc.value, 'SHORT_INT')}
            </DrilldownLink>

            <span className="spacer-left">
              <SizeRating value={Number(ncloc.value)} />
            </span>
          </>
        ) : (
          <span>0</span>
        )}

        {isApp && (
          <span className="huge-spacer-left display-inline-flex-center">
            {projects ? (
              <DrilldownLink component={component.key} metric={MetricKey.projects}>
                <span className="big">{formatMeasure(projects.value, 'SHORT_INT')}</span>
              </DrilldownLink>
            ) : (
              <span className="big">0</span>
            )}
            <span className="little-spacer-left text-muted">
              {translate('metric.projects.name')}
            </span>
          </span>
        )}
      </div>
    </>
  );
}
