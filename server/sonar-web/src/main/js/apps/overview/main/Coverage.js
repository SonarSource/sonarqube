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
import React from 'react';
import enhance from './enhance';
import DrilldownLink from '../../../components/shared/DrilldownLink';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure, getPeriodValue } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';
import CoverageRating from '../../../components/ui/CoverageRating';

class Coverage extends React.PureComponent {
  getCoverage() {
    const { measures } = this.props;
    const { value } = measures.find(measure => measure.metric.key === 'coverage');
    return Number(value);
  }

  getNewCoverageMeasure() {
    const { measures } = this.props;
    return measures.find(measure => measure.metric.key === 'new_coverage');
  }

  getNewLinesToCover() {
    const { measures } = this.props;
    return measures.find(measure => measure.metric.key === 'new_lines_to_cover');
  }

  renderHeader() {
    return this.props.renderHeader('Coverage', translate('metric.coverage.name'));
  }

  renderTimeline(range) {
    return this.props.renderTimeline('coverage', range);
  }

  renderTests() {
    return this.props.renderMeasure('tests');
  }

  renderCoverage() {
    const { branch, component } = this.props;
    const metric = 'coverage';
    const coverage = this.getCoverage();

    return (
      <div className="overview-domain-measure">
        <div className="display-inline-block text-middle big-spacer-right">
          <CoverageRating value={coverage} size="big" />
        </div>

        <div className="display-inline-block text-middle">
          <div className="overview-domain-measure-value">
            <DrilldownLink branch={branch} component={component.key} metric={metric}>
              <span className="js-overview-main-coverage">
                {formatMeasure(coverage, 'PERCENT')}
              </span>
            </DrilldownLink>
          </div>

          <div className="overview-domain-measure-label">
            {getMetricName('coverage')}
            {this.props.renderHistoryLink('coverage')}
          </div>
        </div>
      </div>
    );
  }

  renderNewCoverage() {
    const { branch, component, leakPeriod } = this.props;
    const newCoverageMeasure = this.getNewCoverageMeasure();
    const newLinesToCover = this.getNewLinesToCover();

    const newCoverageValue = newCoverageMeasure
      ? getPeriodValue(newCoverageMeasure, leakPeriod.index)
      : null;
    const newLinesToCoverValue = newLinesToCover
      ? getPeriodValue(newLinesToCover, leakPeriod.index)
      : null;

    const formattedValue =
      newCoverageValue != null ? (
        <div>
          <DrilldownLink
            branch={branch}
            component={component.key}
            metric={newCoverageMeasure.metric.key}>
            <span className="js-overview-main-new-coverage">
              {formatMeasure(newCoverageValue, 'PERCENT')}
            </span>
          </DrilldownLink>
        </div>
      ) : (
        <span>—</span>
      );
    const label =
      newLinesToCoverValue != null && newLinesToCoverValue > 0 ? (
        <div className="overview-domain-measure-label">
          {translate('overview.coverage_on')}
          <br />
          <DrilldownLink
            branch={branch}
            className="spacer-right overview-domain-secondary-measure-value"
            component={component.key}
            metric={newLinesToCover.metric.key}>
            <span className="js-overview-main-new-coverage">
              {formatMeasure(newLinesToCoverValue, 'SHORT_INT')}
            </span>
          </DrilldownLink>
          {getMetricName('new_lines_to_cover')}
        </div>
      ) : (
        <div className="overview-domain-measure-label">{getMetricName('new_coverage')}</div>
      );
    return (
      <div className="overview-domain-measure">
        <div className="overview-domain-measure-value">{formattedValue}</div>
        {label}
      </div>
    );
  }

  renderNutshell() {
    return (
      <div className="overview-domain-nutshell">
        <div className="overview-domain-measures">
          {this.renderCoverage()}
          {this.renderTests()}
        </div>

        {this.renderTimeline('before')}
      </div>
    );
  }

  renderLeak() {
    const { leakPeriod } = this.props;
    if (leakPeriod == null) {
      return null;
    }
    return (
      <div className="overview-domain-leak">
        <div className="overview-domain-measures">{this.renderNewCoverage()}</div>

        {this.renderTimeline('after')}
      </div>
    );
  }

  render() {
    const { measures } = this.props;
    const coverageMeasure = measures.find(measure => measure.metric.key === 'coverage');
    if (coverageMeasure == null) {
      return null;
    }
    return (
      <div className="overview-card">
        {this.renderHeader()}

        <div className="overview-domain-panel">
          {this.renderNutshell()}
          {this.renderLeak()}
        </div>
      </div>
    );
  }
}

export default enhance(Coverage);
