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
import BugIcon from 'sonar-ui-common/components/icons/BugIcon';
import CodeSmellIcon from 'sonar-ui-common/components/icons/CodeSmellIcon';
import VulnerabilityIcon from 'sonar-ui-common/components/icons/VulnerabilityIcon';
import Rating from 'sonar-ui-common/components/ui/Rating';
import { translate } from 'sonar-ui-common/helpers/l10n';
import Measure from '../../../components/measure/Measure';

interface Props {
  measures: T.Dict<string>;
}

export default function ProjectCardLeakMeasures({ measures }: Props) {
  return (
    <div className="project-card-leak-measures">
      <div className="project-card-measure" data-key="new_reliability_rating">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              className="spacer-right"
              metricKey="new_bugs"
              metricType="SHORT_INT"
              value={measures['new_bugs']}
            />
            <Rating value={measures['new_reliability_rating']} />
          </div>
          <div className="project-card-measure-label-with-icon">
            <BugIcon className="little-spacer-right text-bottom" />
            {translate('metric.bugs.name')}
          </div>
        </div>
      </div>

      <div className="project-card-measure" data-key="new_security_rating">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              className="spacer-right"
              metricKey="new_vulnerabilities"
              metricType="SHORT_INT"
              value={measures['new_vulnerabilities']}
            />
            <Rating value={measures['new_security_rating']} />
          </div>
          <div className="project-card-measure-label-with-icon">
            <VulnerabilityIcon className="little-spacer-right text-bottom" />
            {translate('metric.vulnerabilities.name')}
          </div>
        </div>
      </div>

      <div className="project-card-measure" data-key="new_maintainability_rating">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              className="spacer-right"
              metricKey="new_code_smells"
              metricType="SHORT_INT"
              value={measures['new_code_smells']}
            />
            <Rating value={measures['new_maintainability_rating']} />
          </div>
          <div className="project-card-measure-label-with-icon">
            <CodeSmellIcon className="little-spacer-right text-bottom" />
            {translate('metric.code_smells.name')}
          </div>
        </div>
      </div>

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
