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
import { Link } from 'react-router';
import { keyBy, sortBy, groupBy } from 'lodash';
import MeasuresOverlayMeasure from './MeasuresOverlayMeasure';
import MeasuresOverlayTestCases from './MeasuresOverlayTestCases';
import { Button } from '../../../components/ui/buttons';
import { getFacets } from '../../../api/issues';
import { getMeasures } from '../../../api/measures';
import { getAllMetrics } from '../../../api/metrics';
import { FacetValue, SourceViewerFile } from '../../../app/types';
import Modal from '../../controls/Modal';
import Measure from '../../measure/Measure';
import QualifierIcon from '../../shared/QualifierIcon';
import SeverityHelper from '../../shared/SeverityHelper';
import CoverageRating from '../../ui/CoverageRating';
import DuplicationsRating from '../../ui/DuplicationsRating';
import IssueTypeIcon from '../../ui/IssueTypeIcon';
import { SEVERITIES, TYPES } from '../../../helpers/constants';
import { translate, getLocalizedMetricName } from '../../../helpers/l10n';
import {
  formatMeasure,
  MeasureEnhanced,
  getDisplayMetrics,
  enhanceMeasuresWithMetrics
} from '../../../helpers/measures';
import { getProjectUrl } from '../../../helpers/urls';

interface Props {
  branch: string | undefined;
  onClose: () => void;
  sourceViewerFile: SourceViewerFile;
}

interface Measures {
  [metricKey: string]: MeasureEnhanced;
}

interface State {
  loading: boolean;
  measures: Measures;
  severitiesFacet?: FacetValue[];
  showAllMeasures: boolean;
  tagsFacet?: FacetValue[];
  typesFacet?: FacetValue[];
}

export default class MeasuresOverlay extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true, measures: {}, showAllMeasures: false };

  componentDidMount() {
    this.mounted = true;
    this.fetchData();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchData = () => {
    Promise.all([this.fetchMeasures(), this.fetchIssues()]).then(
      ([measures, facets]) => {
        if (this.mounted) {
          this.setState({ loading: false, measures, ...facets });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  fetchMeasures = () => {
    return getAllMetrics().then(metrics => {
      const metricKeys = getDisplayMetrics(metrics).map(metric => metric.key);

      // eslint-disable-next-line promise/no-nesting
      return getMeasures(this.props.sourceViewerFile.key, metricKeys, this.props.branch).then(
        measures => {
          const withMetrics = enhanceMeasuresWithMetrics(measures, metrics).filter(
            measure => measure.metric
          );
          return keyBy(withMetrics, measure => measure.metric.key);
        }
      );
    });
  };

  fetchIssues = () => {
    return getFacets(
      {
        branch: this.props.branch,
        componentKeys: this.props.sourceViewerFile.key,
        resolved: 'false'
      },
      ['types', 'severities', 'tags']
    ).then(({ facets }) => {
      const severitiesFacet = facets.find(f => f.property === 'severities');
      const tagsFacet = facets.find(f => f.property === 'tags');
      const typesFacet = facets.find(f => f.property === 'types');
      return {
        severitiesFacet: severitiesFacet && severitiesFacet.values,
        tagsFacet: tagsFacet && tagsFacet.values,
        typesFacet: typesFacet && typesFacet.values
      };
    });
  };

  handleAllMeasuresClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ showAllMeasures: true });
  };

  renderMeasure = (measure: MeasureEnhanced | undefined) => {
    return measure ? <MeasuresOverlayMeasure key={measure.metric.key} measure={measure} /> : null;
  };

  renderLines = () => {
    const { measures } = this.state;

    return (
      <div className="source-viewer-measures-section">
        <div className="source-viewer-measures-card">
          <div className="measures">
            <div className="measures-list">
              {this.renderMeasure(measures.lines)}
              {this.renderMeasure(measures.ncloc)}
              {this.renderMeasure(measures.comment_lines)}
              {this.renderMeasure(measures.comment_lines_density)}
            </div>
          </div>

          <div className="measures">
            <div className="measures-list">
              {this.renderMeasure(measures.cognitive_complexity)}
              {this.renderMeasure(measures.complexity)}
              {this.renderMeasure(measures.function_complexity)}
            </div>
          </div>
        </div>
      </div>
    );
  };

  renderBigMeasure = (measure: MeasureEnhanced | undefined) => {
    return measure ? (
      <div className="measure measure-big" data-metric={measure.metric.key}>
        <span className="measure-value">
          <Measure
            metricKey={measure.metric.key}
            metricType={measure.metric.type}
            value={measure.value}
          />
        </span>
        <span className="measure-name">{getLocalizedMetricName(measure.metric, true)}</span>
      </div>
    ) : null;
  };

  renderIssues = () => {
    const { measures, severitiesFacet, tagsFacet, typesFacet } = this.state;
    return (
      <div className="source-viewer-measures-section">
        <div className="source-viewer-measures-card">
          <div className="measures">
            {this.renderBigMeasure(measures.violations)}
            {this.renderBigMeasure(measures.sqale_index)}
          </div>
          {measures.violations &&
            !measures.violations.value && (
              <>
                {typesFacet && (
                  <div className="measures">
                    <div className="measures-list">
                      {sortBy(typesFacet, f => TYPES.indexOf(f.val)).map(f => (
                        <div className="measure measure-one-line" key={f.val}>
                          <span className="measure-name">
                            <IssueTypeIcon className="little-spacer-right" query={f.val} />
                            {translate('issue.type', f.val)}
                          </span>
                          <span className="measure-value">
                            {formatMeasure(f.count, 'SHORT_INT')}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                {severitiesFacet && (
                  <div className="measures">
                    <div className="measures-list">
                      {sortBy(severitiesFacet, f => SEVERITIES.indexOf(f.val)).map(f => (
                        <div className="measure measure-one-line" key={f.val}>
                          <span className="measure-name">
                            <SeverityHelper severity={f.val} />
                          </span>
                          <span className="measure-value">
                            {formatMeasure(f.count, 'SHORT_INT')}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                {tagsFacet && (
                  <div className="measures">
                    <div className="measures-list">
                      {tagsFacet.map(f => (
                        <div className="measure measure-one-line" key={f.val}>
                          <span className="measure-name">
                            <i className="icon-tags little-spacer-right" />
                            {f.val}
                          </span>
                          <span className="measure-value">
                            {formatMeasure(f.count, 'SHORT_INT')}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </>
            )}
        </div>
      </div>
    );
  };

  renderCoverage = () => {
    const { coverage } = this.state.measures;
    if (!coverage) {
      return null;
    }
    return (
      <div className="source-viewer-measures-section">
        <div className="source-viewer-measures-card">
          <div className="measures">
            <div className="measures-chart">
              <CoverageRating size="big" value={coverage.value} />
            </div>
            <div className="measure measure-big" data-metric={coverage.metric.key}>
              <span className="measure-value">
                <Measure
                  metricKey={coverage.metric.key}
                  metricType={coverage.metric.type}
                  value={coverage.value}
                />
              </span>
              <span className="measure-name">{getLocalizedMetricName(coverage.metric)}</span>
            </div>
          </div>

          <div className="measures">
            <div className="measures-list">
              {this.renderMeasure(this.state.measures.uncovered_lines)}
              {this.renderMeasure(this.state.measures.lines_to_cover)}
              {this.renderMeasure(this.state.measures.uncovered_conditions)}
              {this.renderMeasure(this.state.measures.conditions_to_cover)}
            </div>
          </div>
        </div>
      </div>
    );
  };

  renderDuplications = () => {
    const { duplicated_lines_density: duplications } = this.state.measures;
    if (!duplications) {
      return null;
    }
    return (
      <div className="source-viewer-measures-section">
        <div className="source-viewer-measures-card">
          <div className="measures">
            <div className="measures-chart">
              <DuplicationsRating
                muted={duplications.value === undefined}
                size="big"
                value={Number(duplications.value || 0)}
              />
            </div>
            <div className="measure measure-big" data-metric={duplications.metric.key}>
              <span className="measure-value">
                <Measure
                  metricKey={duplications.metric.key}
                  metricType={duplications.metric.type}
                  value={duplications.value}
                />
              </span>
              <span className="measure-name">
                {getLocalizedMetricName(duplications.metric, true)}
              </span>
            </div>
          </div>

          <div className="measures">
            <div className="measures-list">
              {this.renderMeasure(this.state.measures.duplicated_blocks)}
              {this.renderMeasure(this.state.measures.duplicated_lines)}
            </div>
          </div>
        </div>
      </div>
    );
  };

  renderTests = () => {
    const { measures } = this.state;
    return (
      <div className="source-viewer-measures">
        <div className="source-viewer-measures-section">
          <div className="source-viewer-measures-card">
            <div className="measures">
              <div className="measures-list">
                {this.renderMeasure(measures.tests)}
                {this.renderMeasure(measures.test_success_density)}
                {this.renderMeasure(measures.test_failures)}
                {this.renderMeasure(measures.test_errors)}
                {this.renderMeasure(measures.skipped_tests)}
                {this.renderMeasure(measures.test_execution_time)}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  };

  renderDomain = (domain: string, measures: MeasureEnhanced[]) => {
    return (
      <div className="source-viewer-measures-card" key={domain}>
        <div className="measures">
          <div className="measures-list">
            <div className="measure measure-one-line measure-big">
              <span className="measure-name">{domain}</span>
            </div>
            {sortBy(measures.filter(measure => measure.value !== undefined), measure =>
              getLocalizedMetricName(measure.metric)
            ).map(measure => this.renderMeasure(measure))}
          </div>
        </div>
      </div>
    );
  };

  renderAllMeasures = () => {
    const domains = groupBy(Object.values(this.state.measures), measure => measure.metric.domain);
    const domainKeys = Object.keys(domains);
    const odd = domainKeys.filter((_, index) => index % 2 === 1);
    const even = domainKeys.filter((_, index) => index % 2 === 0);
    return (
      <div className="source-viewer-measures source-viewer-measures-secondary js-all-measures">
        <div className="source-viewer-measures-section source-viewer-measures-section-big">
          {odd.map(domain => this.renderDomain(domain, domains[domain]))}
        </div>
        <div className="source-viewer-measures-section source-viewer-measures-section-big">
          {even.map(domain => this.renderDomain(domain, domains[domain]))}
        </div>
      </div>
    );
  };

  render() {
    const { branch, sourceViewerFile } = this.props;
    const { loading } = this.state;

    return (
      <Modal contentLabel="" large={true} onRequestClose={this.props.onClose}>
        <div className="modal-container source-viewer-measures-modal">
          <div className="source-viewer-header-component source-viewer-measures-component">
            <div className="source-viewer-header-component-project">
              <QualifierIcon className="little-spacer-right" qualifier="TRK" />
              <Link to={getProjectUrl(sourceViewerFile.project, branch)}>
                {sourceViewerFile.projectName}
              </Link>

              {sourceViewerFile.subProject && (
                <>
                  <QualifierIcon className="big-spacer-left little-spacer-right" qualifier="BRC" />
                  <Link to={getProjectUrl(sourceViewerFile.subProject, branch)}>
                    {sourceViewerFile.subProjectName}
                  </Link>
                </>
              )}
            </div>

            <div className="source-viewer-header-component-name">
              <QualifierIcon className="little-spacer-right" qualifier={sourceViewerFile.q} />
              {sourceViewerFile.path}
            </div>
          </div>

          {loading ? (
            <i className="spinner" />
          ) : (
            <>
              {sourceViewerFile.q === 'UTS' ? (
                <>
                  {this.renderTests()}
                  <MeasuresOverlayTestCases branch={branch} componentKey={sourceViewerFile.key} />
                </>
              ) : (
                <div className="source-viewer-measures">
                  {this.renderLines()}
                  {this.renderIssues()}
                  {this.renderCoverage()}
                  {this.renderDuplications()}
                </div>
              )}
            </>
          )}

          <div className="spacer-top">
            {this.state.showAllMeasures ? (
              this.renderAllMeasures()
            ) : (
              <a className="js-show-all-measures" href="#" onClick={this.handleAllMeasuresClick}>
                {translate('component_viewer.show_all_measures')}
              </a>
            )}
          </div>
        </div>

        <footer className="modal-foot">
          <Button className="button-link" onClick={this.props.onClose}>
            {translate('close')}
          </Button>
        </footer>
      </Modal>
    );
  }
}
