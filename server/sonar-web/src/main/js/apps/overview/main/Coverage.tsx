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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import DocTooltip from '../../../components/docs/DocTooltip';
import DrilldownLink from '../../../components/shared/DrilldownLink';
import CoverageRating from '../../../components/ui/CoverageRating';
import { getPeriodValue } from '../../../helpers/measures';
import { getMetricName } from '../utils';
import enhance, { ComposedProps } from './enhance';

export class Coverage extends React.PureComponent<ComposedProps> {
  getCoverage() {
    const measure = this.props.measures.find(measure => measure.metric.key === 'coverage');
    return Number(measure ? measure.value : undefined);
  }

  renderHeader() {
    return this.props.renderHeader('Coverage', translate('metric.coverage.name'));
  }

  renderTimeline(range: string) {
    return this.props.renderTimeline('coverage', range);
  }

  renderTests() {
    return this.props.renderMeasure(
      'tests',
      <DocTooltip
        className="little-spacer-left"
        doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/metrics/unit-tests.md')}
      />
    );
  }

  renderCoverage() {
    const { branchLike, component } = this.props;
    const metric = 'coverage';
    const coverage = this.getCoverage();

    return (
      <div className="overview-domain-measure">
        <div className="display-inline-block text-middle big-spacer-right neg-offset-left">
          <CoverageRating size="big" value={coverage} />
        </div>

        <div className="display-inline-block text-middle">
          <div className="overview-domain-measure-value">
            <DrilldownLink branchLike={branchLike} component={component.key} metric={metric}>
              <span className="js-overview-main-coverage">
                {formatMeasure(coverage, 'PERCENT')}
              </span>
            </DrilldownLink>
          </div>

          <div className="overview-domain-measure-label display-flex-center display-flex-justify-center">
            {getMetricName('coverage')}
            <DocTooltip
              className="little-spacer-left"
              doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/metrics/coverage.md')}
            />
          </div>
          {this.props.renderHistoryLink('coverage')}
        </div>
        {this.props.renderHistoryLink('coverage')}
      </div>
    );
  }

  renderNewCoverage() {
    const { branchLike, component, leakPeriod, measures } = this.props;
    if (!leakPeriod) {
      return null;
    }

    const newCoverageMeasure = measures.find(measure => measure.metric.key === 'new_coverage');
    const newCoverageValue =
      newCoverageMeasure && getPeriodValue(newCoverageMeasure, leakPeriod.index);
    const formattedValue =
      newCoverageMeasure && newCoverageValue !== undefined ? (
        <div>
          <DrilldownLink
            branchLike={branchLike}
            component={component.key}
            metric={newCoverageMeasure.metric.key}>
            <span className="js-overview-main-new-coverage">
              {formatMeasure(newCoverageValue, 'PERCENT')}
            </span>
          </DrilldownLink>
        </div>
      ) : (
        <span className="big">—</span>
      );

    const newLinesToCover = measures.find(measure => measure.metric.key === 'new_lines_to_cover');
    const newLinesToCoverValue =
      newLinesToCover && getPeriodValue(newLinesToCover, leakPeriod.index);
    const label =
      newLinesToCover && newLinesToCoverValue !== undefined && Number(newLinesToCoverValue) > 0 ? (
        <div className="overview-domain-measure-label">
          {translate('overview.coverage_on')}
          <br />
          <DrilldownLink
            branchLike={branchLike}
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
        <div className="overview-domain-measure-label display-flex-center display-flex-justify-center">
          {getMetricName('new_coverage')}
        </div>
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
        <div className="overview-domain-measures overview-domain-measures-big">
          {this.renderCoverage()}
          {this.renderTests()}
        </div>

        {this.renderTimeline('before')}
      </div>
    );
  }

  renderLeak() {
    const { leakPeriod } = this.props;
    if (!leakPeriod) {
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
    if (!coverageMeasure) {
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
