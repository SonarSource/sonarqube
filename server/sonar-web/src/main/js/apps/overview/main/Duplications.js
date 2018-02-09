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
import DuplicationsRating from '../../../components/ui/DuplicationsRating';

class Duplications extends React.PureComponent {
  renderHeader() {
    return this.props.renderHeader('Duplications', translate('overview.domain.duplications'));
  }

  renderTimeline(range) {
    return this.props.renderTimeline('duplicated_lines_density', range);
  }

  renderDuplicatedBlocks() {
    return this.props.renderMeasure('duplicated_blocks');
  }

  renderDuplications() {
    const { branch, component, measures } = this.props;
    const measure = measures.find(measure => measure.metric.key === 'duplicated_lines_density');
    const duplications = Number(measure.value);

    return (
      <div className="overview-domain-measure">
        <div className="display-inline-block text-middle big-spacer-right">
          <DuplicationsRating value={duplications} size="big" />
        </div>

        <div className="display-inline-block text-middle">
          <div className="overview-domain-measure-value">
            <DrilldownLink
              branch={branch}
              component={component.key}
              metric="duplicated_lines_density">
              {formatMeasure(duplications, 'PERCENT')}
            </DrilldownLink>
          </div>

          <div className="overview-domain-measure-label offset-left">
            {getMetricName('duplications')}
            {this.props.renderHistoryLink('duplicated_lines_density')}
          </div>
        </div>
      </div>
    );
  }

  renderNewDuplications() {
    const { branch, component, measures, leakPeriod } = this.props;
    const newDuplicationsMeasure = measures.find(
      measure => measure.metric.key === 'new_duplicated_lines_density'
    );
    const newLinesMeasure = measures.find(measure => measure.metric.key === 'new_lines');

    const newDuplicationsValue = newDuplicationsMeasure
      ? getPeriodValue(newDuplicationsMeasure, leakPeriod.index)
      : null;
    const newLinesValue = newLinesMeasure
      ? getPeriodValue(newLinesMeasure, leakPeriod.index)
      : null;

    const formattedValue =
      newDuplicationsValue != null ? (
        <div>
          <DrilldownLink
            branch={branch}
            component={component.key}
            metric={newDuplicationsMeasure.metric.key}>
            <span className="js-overview-main-new-duplications">
              {formatMeasure(newDuplicationsValue, 'PERCENT')}
            </span>
          </DrilldownLink>
        </div>
      ) : (
        <span>—</span>
      );
    const label =
      newLinesValue != null && newLinesValue > 0 ? (
        <div className="overview-domain-measure-label">
          {translate('overview.duplications_on')}
          <br />
          <DrilldownLink
            branch={branch}
            className="spacer-right overview-domain-secondary-measure-value"
            component={component.key}
            metric={newLinesMeasure.metric.key}>
            <span className="js-overview-main-new-lines">
              {formatMeasure(newLinesValue, 'SHORT_INT')}
            </span>
          </DrilldownLink>
          {getMetricName('new_lines')}
        </div>
      ) : (
        <div className="overview-domain-measure-label">{getMetricName('new_duplications')}</div>
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
          {this.renderDuplications()}
          {this.renderDuplicatedBlocks()}
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
        <div className="overview-domain-measures">{this.renderNewDuplications()}</div>

        {this.renderTimeline('after')}
      </div>
    );
  }

  render() {
    const { measures } = this.props;
    const duplications = measures.find(
      measure => measure.metric.key === 'duplicated_lines_density'
    );
    if (duplications == null) {
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

export default enhance(Duplications);
