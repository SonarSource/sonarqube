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
import ProjectCardLanguages from './ProjectCardLanguages';
import Measure from '../../component-measures/components/Measure';
import Rating from '../../../components/ui/Rating';
import CoverageRating from '../../../components/ui/CoverageRating';
import DuplicationsRating from '../../../components/ui/DuplicationsRating';
import SizeRating from '../../../components/ui/SizeRating';
import { translate } from '../../../helpers/l10n';

type Props = {
  measures?: { [string]: string }
};

export default function ProjectCardOverallMeasures({ measures }: Props) {
  if (measures == null) {
    return null;
  }

  return (
    <div className="project-card-measures">
      <div className="project-card-measure smaller-card" data-key="reliability_rating">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Rating value={measures['reliability_rating']} />
          </div>
          <div className="project-card-measure-label">
            {translate('metric_domain.Reliability')}
          </div>
        </div>
      </div>

      <div className="project-card-measure smaller-card" data-key="security_rating">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Rating value={measures['security_rating']} />
          </div>
          <div className="project-card-measure-label">
            {translate('metric_domain.Security')}
          </div>
        </div>
      </div>

      <div className="project-card-measure" data-key="sqale_rating">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            <Rating value={measures['sqale_rating']} />
          </div>
          <div className="project-card-measure-label">
            {translate('metric_domain.Maintainability')}
          </div>
        </div>
      </div>

      <div className="project-card-measure" data-key="coverage">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            {measures['coverage'] != null &&
              <span className="spacer-right">
                <CoverageRating value={measures['coverage']} />
              </span>}
            <Measure
              measure={{ value: measures['coverage'] }}
              metric={{ key: 'coverage', type: 'PERCENT' }}
            />
          </div>
          <div className="project-card-measure-label">
            {translate('metric.coverage.name')}
          </div>
        </div>
      </div>

      <div className="project-card-measure" data-key="duplicated_lines_density">
        <div className="project-card-measure-inner">
          <div className="project-card-measure-number">
            {measures['duplicated_lines_density'] != null &&
              <span className="spacer-right">
                <DuplicationsRating value={Number(measures['duplicated_lines_density'])} />
              </span>}
            <Measure
              measure={{ value: measures['duplicated_lines_density'] }}
              metric={{ key: 'duplicated_lines_density', type: 'PERCENT' }}
            />
          </div>
          <div className="project-card-measure-label">
            {translate('metric.duplicated_lines_density.short_name')}
          </div>
        </div>
      </div>

      {measures['ncloc'] != null &&
        <div className="project-card-measure pull-right" data-key="ncloc">
          <div className="project-card-measure-inner pull-right">
            <div className="project-card-measure-number">
              <span className="spacer-right">
                <SizeRating value={Number(measures['ncloc'])} />
              </span>
              <Measure
                measure={{ value: measures['ncloc'] }}
                metric={{ key: 'ncloc', type: 'SHORT_INT' }}
              />
            </div>
            <div className="project-card-measure-label">
              <ProjectCardLanguages distribution={measures['ncloc_language_distribution']} />
            </div>
          </div>
        </div>}
    </div>
  );
}
