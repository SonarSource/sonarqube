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

import { FormattedMessage } from 'react-intl';
import { Note } from '~design-system';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { findMeasure } from '../../../helpers/measures';
import { MeasureEnhanced } from '../../../types/types';

interface Props {
  measures: MeasureEnhanced[];
  overallMetric: MetricKey;
}

export default function AfterMergeNote({ measures, overallMetric }: Readonly<Props>) {
  const afterMergeValue = findMeasure(measures, overallMetric)?.value;

  return afterMergeValue ? (
    <Note className="sw-mt-2 sw-typo-sm sw-inline-block">
      <strong className="sw-mr-1">{formatMeasure(afterMergeValue, MetricType.Percent)}</strong>
      <FormattedMessage id="component_measures.facet_category.overall_category.estimated" />
    </Note>
  ) : null;
}
