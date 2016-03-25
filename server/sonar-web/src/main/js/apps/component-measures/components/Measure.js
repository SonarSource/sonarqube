/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';

import { Rating } from '../../../components/shared/rating';
import Level from '../../../components/shared/Level';
import { formatMeasure } from '../../../helpers/measures';
import { formatLeak, isDiffMetric } from '../utils';

const Measure = ({ measure, metric }) => {
  const finalMetric = metric || measure.metric;

  if (finalMetric.type === 'RATING') {
    return <Rating value={measure.value}/>;
  }

  if (finalMetric.type === 'LEVEL') {
    return <Level level={measure.value}/>;
  }

  const formattedValue = isDiffMetric(finalMetric) ?
      formatLeak(measure.leak, finalMetric) :
      formatMeasure(measure.value, finalMetric.type);

  return (
      <span>
        {formattedValue != null ? formattedValue : '–'}
      </span>
  );
};

export default Measure;
