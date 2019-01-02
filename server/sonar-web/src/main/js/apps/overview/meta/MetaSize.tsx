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
import * as classNames from 'classnames';
import DrilldownLink from '../../../components/shared/DrilldownLink';
import LanguageDistributionContainer from '../../../components/charts/LanguageDistributionContainer';
import SizeRating from '../../../components/ui/SizeRating';
import { formatMeasure } from '../../../helpers/measures';
import { getMetricName } from '../utils';
import { translate } from '../../../helpers/l10n';

interface Props {
  branchLike?: T.BranchLike;
  component: T.LightComponent;
  measures: T.MeasureEnhanced[];
}

export default class MetaSize extends React.PureComponent<Props> {
  renderLoC = (ncloc?: T.MeasureEnhanced) => (
    <div
      className={classNames('overview-meta-size-ncloc', {
        'is-half-width': this.props.component.qualifier === 'APP'
      })}
      id="overview-ncloc">
      {ncloc && (
        <span className="spacer-right">
          <SizeRating value={Number(ncloc.value)} />
        </span>
      )}
      {ncloc ? (
        <DrilldownLink
          branchLike={this.props.branchLike}
          component={this.props.component.key}
          metric="ncloc">
          {formatMeasure(ncloc.value, 'SHORT_INT')}
        </DrilldownLink>
      ) : (
        <span>0</span>
      )}
      <div className="spacer-top text-muted">{getMetricName('ncloc')}</div>
    </div>
  );

  renderLoCDistribution = () => {
    const languageDistribution = this.props.measures.find(
      measure => measure.metric.key === 'ncloc_language_distribution'
    );

    const className =
      this.props.component.qualifier === 'TRK' ? 'overview-meta-size-lang-dist' : 'big-spacer-top';

    return languageDistribution && languageDistribution.value !== undefined ? (
      <div className={className} id="overview-language-distribution">
        <LanguageDistributionContainer distribution={languageDistribution.value} width={160} />
      </div>
    ) : null;
  };

  renderProjects = () => {
    const projects = this.props.measures.find(measure => measure.metric.key === 'projects');
    return (
      <div className="overview-meta-size-ncloc is-half-width" id="overview-projects">
        {projects ? (
          <DrilldownLink
            branchLike={this.props.branchLike}
            component={this.props.component.key}
            metric="projects">
            {formatMeasure(projects.value, 'SHORT_INT')}
          </DrilldownLink>
        ) : (
          <span>0</span>
        )}
        <div className="spacer-top text-muted">{translate('metric.projects.name')}</div>
      </div>
    );
  };

  render() {
    const ncloc = this.props.measures.find(measure => measure.metric.key === 'ncloc');

    if (ncloc == null && this.props.component.qualifier !== 'APP') {
      return null;
    }

    return (
      <div className="big-spacer-top" id="overview-size">
        {this.props.component.qualifier === 'APP' && this.renderProjects()}
        {this.renderLoC(ncloc)}
        {this.renderLoCDistribution()}
      </div>
    );
  }
}
