/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { formatMeasure, findMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';
import { MEASUREMENTS_MAP, MeasurementType } from '../utils';

interface Props {
  measures: T.Measure[];
  type: MeasurementType;
}

export default function AfterMergeEstimate({ measures, type }: Props) {
  const { afterMergeMetric } = MEASUREMENTS_MAP[type];

  const measure = findMeasure(measures, afterMergeMetric);

  if (!measure || measure.value === undefined) {
    return null;
  }

  return (
    <>
      <span className="huge">{formatMeasure(measure.value, 'PERCENT')}</span>
      <span className="label flex-1">
        {translate('component_measures.facet_category.overall_category.estimated')}
      </span>
    </>
  );
}
