/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
//@flow
import React from 'react';
import Measure from '../../component-measures/components/Measure';
import BugIcon from '../../../components/icons-components/BugIcon';
import CodeSmellIcon from '../../../components/icons-components/CodeSmellIcon';
import Rating from '../../../components/ui/Rating';
import VulnerabilityIcon from '../../../components/icons-components/VulnerabilityIcon';
import { translate } from '../../../helpers/l10n';

type Props = {
  measures?: { [string]: string }
};

export default function ProjectCardLeakMeasures({ measures }: Props) {
  if (measures == null) {
    return null;
  }

  return (
    <div className="project-card-leak-measures">
      <div className="project-card-measure smaller-card" data-key="new_reliability_rating">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              className="spacer-right"
              measure={{ leak: measures['new_bugs'] }}
              metric={{ key: 'new_bugs', type: 'SHORT_INT' }}
            />
            <Rating value={measures['new_reliability_rating']} />
          </div>
          <div className="project-card-measure-label-with-icon">
            <BugIcon className="little-spacer-right vertical-bottom" />
            {translate('metric.bugs.name')}
          </div>
        </div>
      </div>

      <div className="project-card-measure" data-key="new_security_rating">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              className="spacer-right"
              measure={{ leak: measures['new_vulnerabilities'] }}
              metric={{ key: 'new_vulnerabilities', type: 'SHORT_INT' }}
            />
            <Rating value={measures['new_security_rating']} />
          </div>
          <div className="project-card-measure-label-with-icon">
            <VulnerabilityIcon className="little-spacer-right vertical-bottom" />
            {translate('metric.vulnerabilities.name')}
          </div>
        </div>
      </div>

      <div className="project-card-measure" data-key="new_maintainability_rating">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              className="spacer-right"
              measure={{ leak: measures['new_code_smells'] }}
              metric={{ key: 'new_code_smells', type: 'SHORT_INT' }}
            />
            <Rating value={measures['new_maintainability_rating']} />
          </div>
          <div className="project-card-measure-label-with-icon">
            <CodeSmellIcon className="little-spacer-right vertical-bottom" />
            {translate('metric.code_smells.name')}
          </div>
        </div>
      </div>

      <div className="project-card-measure" data-key="new_coverage">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              measure={{ leak: measures['new_coverage'] }}
              metric={{ key: 'new_coverage', type: 'PERCENT' }}
            />
          </div>
          <div className="project-card-measure-label">
            {translate('metric.coverage.name')}
          </div>
        </div>
      </div>

      <div className="project-card-measure" data-key="new_duplicated_lines_density">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              measure={{ leak: measures['new_duplicated_lines_density'] }}
              metric={{ key: 'new_duplicated_lines_density', type: 'PERCENT' }}
            />
          </div>
          <div className="project-card-measure-label">
            {translate('metric.duplicated_lines_density.short_name')}
          </div>
        </div>
      </div>

      <div className="project-card-measure smaller-card pull-right" data-key="new_lines">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              measure={{ leak: measures['new_lines'] }}
              metric={{ key: 'new_lines', type: 'SHORT_INT' }}
            />
          </div>
          <div className="project-card-measure-label">
            {translate('metric.lines.name')}
          </div>
        </div>
      </div>
    </div>
  );
}
