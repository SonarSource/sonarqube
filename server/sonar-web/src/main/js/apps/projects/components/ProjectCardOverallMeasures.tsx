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
import ProjectCardLanguagesContainer from './ProjectCardLanguagesContainer';
import Measure from '../../../components/measure/Measure';
import Rating from '../../../components/ui/Rating';
import CoverageRating from '../../../components/ui/CoverageRating';
import DuplicationsRating from '../../../components/ui/DuplicationsRating';
import SizeRating from '../../../components/ui/SizeRating';
import { translate } from '../../../helpers/l10n';
import BugIcon from '../../../components/icons-components/BugIcon';
import CodeSmellIcon from '../../../components/icons-components/CodeSmellIcon';
import VulnerabilityIcon from '../../../components/icons-components/VulnerabilityIcon';

interface Props {
  measures: { [key: string]: string | undefined };
}

export default function ProjectCardOverallMeasures({ measures }: Props) {
  if (measures === undefined) {
    return null;
  }

  return (
    <div className="project-card-measures">
      <div className="project-card-measure smaller-card" data-key="reliability_rating">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              className="spacer-right"
              metricKey="bugs"
              metricType="SHORT_INT"
              value={measures['bugs']}
            />
            <Rating value={measures['reliability_rating']} />
          </div>
          <div className="project-card-measure-label-with-icon">
            <BugIcon className="little-spacer-right vertical-bottom" />
            {translate('metric.bugs.name')}
          </div>
        </div>
      </div>

      <div className="project-card-measure" data-key="security_rating">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              className="spacer-right"
              metricKey="vulnerabilities"
              metricType="SHORT_INT"
              value={measures['vulnerabilities']}
            />
            <Rating value={measures['security_rating']} />
          </div>
          <div className="project-card-measure-label-with-icon">
            <VulnerabilityIcon className="little-spacer-right vertical-bottom" />
            {translate('metric.vulnerabilities.name')}
          </div>
        </div>
      </div>

      <div className="project-card-measure" data-key="sqale_rating">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Measure
              className="spacer-right"
              metricKey="code_smells"
              metricType="SHORT_INT"
              value={measures['code_smells']}
            />
            <Rating value={measures['sqale_rating']} />
          </div>
          <div className="project-card-measure-label-with-icon">
            <CodeSmellIcon className="little-spacer-right vertical-bottom" />
            {translate('metric.code_smells.name')}
          </div>
        </div>
      </div>

      <div className="project-card-measure" data-key="coverage">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            {measures['coverage'] != null && (
              <span className="spacer-right">
                <CoverageRating value={measures['coverage']} />
              </span>
            )}
            <Measure metricKey="coverage" metricType="PERCENT" value={measures['coverage']} />
          </div>
          <div className="project-card-measure-label">{translate('metric.coverage.name')}</div>
        </div>
      </div>

      <div className="project-card-measure" data-key="duplicated_lines_density">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            {measures['duplicated_lines_density'] != null && (
              <span className="spacer-right">
                <DuplicationsRating value={Number(measures['duplicated_lines_density'])} />
              </span>
            )}
            <Measure
              metricKey="duplicated_lines_density"
              metricType="PERCENT"
              value={measures['duplicated_lines_density']}
            />
          </div>
          <div className="project-card-measure-label">
            {translate('metric.duplicated_lines_density.short_name')}
          </div>
        </div>
      </div>

      {measures['ncloc'] != null && (
        <div className="project-card-measure project-card-ncloc" data-key="ncloc">
          <div className="project-card-measure-inner pull-right">
            <div className="project-card-measure-number">
              <Measure metricKey="ncloc" metricType="SHORT_INT" value={measures['ncloc']} />
              <span className="spacer-left">
                <SizeRating value={Number(measures['ncloc'])} />
              </span>
            </div>
            <div className="project-card-measure-label">
              <ProjectCardLanguagesContainer
                distribution={measures['ncloc_language_distribution']}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
