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
import IssueTypeIcon from 'sonar-ui-common/components/icons/IssueTypeIcon';
import Rating from 'sonar-ui-common/components/ui/Rating';
import Measure from '../../../components/measure/Measure';

export interface ProjectCardRatingMeasureProps {
  iconKey?: string;
  iconLabel: string;
  measures: T.Dict<string | undefined>;
  metricKey: string;
  metricRatingKey: string;
  metricType: string;
}

export default function ProjectCardRatingMeasure(props: ProjectCardRatingMeasureProps) {
  const { iconKey, iconLabel, measures, metricKey, metricRatingKey, metricType } = props;

  return (
    <div className="project-card-measure" data-key={metricRatingKey}>
      <div className="project-card-measure-inner">
        <div className="project-card-measure-number">
          <Measure
            className="spacer-right"
            metricKey={metricKey}
            metricType={metricType}
            value={measures[metricKey]}
          />

          <Rating value={measures[metricRatingKey]} />
        </div>

        <div className="project-card-measure-label-with-icon">
          <IssueTypeIcon className="little-spacer-right text-bottom" query={iconKey || metricKey} />

          {iconLabel}
        </div>
      </div>
    </div>
  );
}
