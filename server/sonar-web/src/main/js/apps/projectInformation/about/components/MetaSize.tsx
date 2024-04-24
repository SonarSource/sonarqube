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
import { DrilldownLink, Note, SizeIndicator, SubHeading } from 'design-system';
import * as React from 'react';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { localizeMetric } from '../../../../helpers/measures';
import { getComponentDrilldownUrl } from '../../../../helpers/urls';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey, MetricType } from '../../../../types/metrics';
import { Component, Measure } from '../../../../types/types';

interface MetaSizeProps {
  component: Component;
  measures: Measure[];
}

export default function MetaSize({ component, measures }: MetaSizeProps) {
  const isApp = component.qualifier === ComponentQualifier.Application;
  const ncloc = measures.find((measure) => measure.metric === MetricKey.ncloc);
  const projects = isApp
    ? measures.find((measure) => measure.metric === MetricKey.projects)
    : undefined;
  const url = getComponentDrilldownUrl({
    componentKey: component.key,
    metric: MetricKey.ncloc,
    listView: true,
  });

  return (
    <>
      <div className="sw-flex sw-items-baseline">
        <SubHeading>{localizeMetric(MetricKey.ncloc)}</SubHeading>
        <span className="sw-ml-1">({translate('project.info.main_branch')})</span>
      </div>
      <div className="sw-flex sw-items-center">
        {ncloc && ncloc.value ? (
          <>
            <DrilldownLink to={url}>
              <span
                aria-label={translateWithParameters(
                  'project.info.see_more_info_on_x_locs',
                  ncloc.value,
                )}
              >
                {formatMeasure(ncloc.value, MetricType.ShortInteger)}
              </span>
            </DrilldownLink>

            <span className="sw-ml-2">
              <SizeIndicator value={Number(ncloc.value)} size="xs" />
            </span>
          </>
        ) : (
          <span>0</span>
        )}

        {isApp && (
          <span className="sw-inline-flex sw-items-center sw-ml-10">
            {projects ? (
              <DrilldownLink to={url}>
                <span>{formatMeasure(projects.value, MetricType.ShortInteger)}</span>
              </DrilldownLink>
            ) : (
              <span>0</span>
            )}
            <Note className="sw-ml-1">{translate('metric.projects.name')}</Note>
          </span>
        )}
      </div>
    </>
  );
}
