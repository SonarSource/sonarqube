/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import Measure from '@sqcore/components/measure/Measure';
import DuplicationsRating from '@sqcore/components/ui/DuplicationsRating';
import { ProjectData } from '../../types';

interface Props extends Pick<ProjectData, 'measures'> {}

export default function MeasureDuplication({ measures }: Props) {
  return (
    <div className="project-card-measure">
      <div className="project-card-measure-value">
        {measures['duplicated_lines_density'] != null && (
          <span className="no-line-height little-spacer-right">
            <DuplicationsRating value={Number(measures['duplicated_lines_density'])} />
          </span>
        )}
        <Measure
          metricKey="duplicated_lines_density"
          metricType="PERCENT"
          value={measures['duplicated_lines_density']}
        />
      </div>
      <div className="project-card-measure-title">Duplications</div>
    </div>
  );
}
