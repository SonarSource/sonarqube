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
import { ContentCell, NumericalCell, RatingCell } from 'design-system';
import * as React from 'react';
import { isPortfolioLike } from '~sonar-aligned/helpers/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import {
  CCT_SOFTWARE_QUALITY_METRICS,
  OLD_TO_NEW_TAXONOMY_METRICS_MAP,
} from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import { useStandardExperienceMode } from '../../../queries/settings';
import { ComponentMeasure } from '../../../types/types';

interface ComponentsHeaderProps {
  baseComponent?: ComponentMeasure;
  canBePinned?: boolean;
  metrics: string[];
  rootComponent: ComponentMeasure;
  showAnalysisDate?: boolean;
}

const SHORT_NAME_METRICS = [
  ...CCT_SOFTWARE_QUALITY_METRICS,
  MetricKey.duplicated_lines_density,
  MetricKey.new_lines,
  MetricKey.new_coverage,
  MetricKey.new_duplicated_lines_density,
];

export default function ComponentsHeader(props: ComponentsHeaderProps) {
  const { baseComponent, canBePinned = true, metrics, rootComponent, showAnalysisDate } = props;
  const { data: isStandardMode = false } = useStandardExperienceMode();
  const isPortfolio = isPortfolioLike(rootComponent.qualifier);
  let columns: string[] = [];
  let Cell: typeof NumericalCell;
  if (isPortfolio) {
    columns = [
      translate('metric_domain.Releasability'),
      translate('portfolio.metric_domain.security'),
      translate('metric_domain.Reliability'),
      translate('metric_domain.Maintainability'),
      translate('portfolio.metric_domain.security_review'),
      translate('metric.ncloc.name'),
    ];

    if (showAnalysisDate) {
      columns.push(translate('code.last_analysis_date'));
    }

    Cell = RatingCell;
  } else {
    columns = metrics.map((m: MetricKey) => {
      const metric = isStandardMode ? m : (OLD_TO_NEW_TAXONOMY_METRICS_MAP[m] ?? m);

      return translate(
        'metric',
        metric,
        SHORT_NAME_METRICS.includes(metric as MetricKey) ? 'short_name' : 'name',
      );
    });

    Cell = NumericalCell;
  }

  return (
    <>
      {canBePinned && <ContentCell aria-label={translate('code.pin')} />}
      <ContentCell aria-label={translate('code.name')} />
      {baseComponent &&
        columns.map((column) => (
          <Cell className="sw-whitespace-nowrap" key={column}>
            {column}
          </Cell>
        ))}
    </>
  );
}
