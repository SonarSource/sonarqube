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
import { translate } from 'sonar-ui-common/helpers/l10n';
import Measure from '../../../components/measure/Measure';
import ProjectCardRatingMeasure from './ProjectCardRatingMeasure';

interface Props {
  measures: T.Dict<string>;
}

export default function ProjectCardLeakMeasures({ measures }: Props) {
  return (
    <div className="project-card-leak-measures">
      <ProjectCardRatingMeasure
        iconLabel={translate('metric.bugs.name')}
        measures={measures}
        metricKey="new_bugs"
        metricRatingKey="new_reliability_rating"
        metricType="SHORT_INT"
      />

      <ProjectCardRatingMeasure
        iconLabel={translate('metric.vulnerabilities.name')}
        measures={measures}
        metricKey="new_vulnerabilities"
        metricRatingKey="new_security_rating"
        metricType="SHORT_INT"
      />

      <ProjectCardRatingMeasure
        iconKey="security_hotspots"
        iconLabel={translate('projects.security_hotspots_reviewed')}
        measures={measures}
        metricKey="new_security_hotspots_reviewed"
        metricRatingKey="new_security_review_rating"
        metricType="PERCENT"
      />

      <ProjectCardRatingMeasure
        iconLabel={translate('metric.code_smells.name')}
        measures={measures}
        metricKey="new_code_smells"
        metricRatingKey="new_maintainability_rating"
        metricType="SHORT_INT"
      />

      {measures['new_coverage'] != null && (
        <div className="project-card-measure" data-key="new_coverage">
          <div className="project-card-measure-inner">
            <div className="project-card-measure-number">
              <Measure
                metricKey="new_coverage"
                metricType="PERCENT"
                value={measures['new_coverage']}
              />
            </div>
            <div className="project-card-measure-label">{translate('metric.coverage.name')}</div>
          </div>
        </div>
      )}

      <div className="project-card-measure" data-key="new_duplicated_lines_density">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              metricKey="new_duplicated_lines_density"
              metricType="PERCENT"
              value={measures['new_duplicated_lines_density']}
            />
          </div>
          <div className="project-card-measure-label">
            {translate('metric.duplicated_lines_density.short_name')}
          </div>
        </div>
      </div>

      <div className="project-card-measure project-card-ncloc" data-key="new_lines">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure metricKey="new_lines" metricType="SHORT_INT" value={measures['new_lines']} />
          </div>
          <div className="project-card-measure-label">{translate('metric.lines.name')}</div>
        </div>
      </div>
    </div>
  );
}
