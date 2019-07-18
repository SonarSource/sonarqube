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
import { connect } from 'react-redux';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getChildren } from '../../../api/components';
import { getMeasures } from '../../../api/measures';
import Measure from '../../../components/measure/Measure';
import { fetchMetrics } from '../../../store/rootActions';
import { getMetrics, Store } from '../../../store/rootReducer';
import '../styles.css';
import { SubComponent } from '../types';
import { convertMeasures, PORTFOLIO_METRICS, SUB_COMPONENTS_METRICS } from '../utils';
import MeasuresButtonLink from './MeasuresButtonLink';
import MetricBox from './MetricBox';
import Report from './Report';
import WorstProjects from './WorstProjects';

interface OwnProps {
  component: T.Component;
}

interface StateToProps {
  metrics: T.Dict<T.Metric>;
}

interface DispatchToProps {
  fetchMetrics: () => void;
}

type Props = StateToProps & DispatchToProps & OwnProps;

interface State {
  loading: boolean;
  measures?: T.Dict<string | undefined>;
  subComponents?: SubComponent[];
  totalSubComponents?: number;
}

export class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.props.fetchMetrics();
    this.fetchData();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.component !== this.props.component) {
      this.fetchData();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchData() {
    this.setState({ loading: true });
    Promise.all([
      getMeasures({ component: this.props.component.key, metricKeys: PORTFOLIO_METRICS.join() }),
      getChildren(this.props.component.key, SUB_COMPONENTS_METRICS, { ps: 20, s: 'qualifier' })
    ]).then(
      ([measures, subComponents]) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            measures: convertMeasures(measures),
            subComponents: subComponents.components.map((component: any) => ({
              ...component,
              measures: convertMeasures(component.measures)
            })),
            totalSubComponents: subComponents.paging.total
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  isEmpty = () => this.state.measures === undefined || this.state.measures['ncloc'] === undefined;

  isNotComputed = () =>
    this.state.measures && this.state.measures['reliability_rating'] === undefined;

  renderSpinner() {
    return (
      <div className="page page-limited">
        <div className="text-center">
          <i className="spinner spacer" />
        </div>
      </div>
    );
  }

  renderEmpty() {
    return (
      <div className="page page-limited">
        <div className="empty-search">
          <h3>
            {!this.state.measures || !this.state.measures['projects']
              ? translate('portfolio.empty')
              : translate('portfolio.no_lines_of_code')}
          </h3>
        </div>
      </div>
    );
  }

  renderWhenNotComputed() {
    return (
      <div className="page page-limited">
        <div className="empty-search">
          <h3>{translate('portfolio.not_computed')}</h3>
        </div>
      </div>
    );
  }

  render() {
    const { component } = this.props;
    const { loading, measures, subComponents, totalSubComponents } = this.state;

    if (loading) {
      return this.renderSpinner();
    }

    if (this.isEmpty()) {
      return this.renderEmpty();
    }

    if (this.isNotComputed()) {
      return this.renderWhenNotComputed();
    }

    return (
      <div className="page page-limited portfolio-overview">
        <div className="page-actions">
          <Report component={component} />
        </div>
        <h1>{translate('portfolio.health_factors')}</h1>
        <div className="portfolio-boxes">
          <MetricBox component={component.key} measures={measures!} metricKey="releasability" />
          <MetricBox component={component.key} measures={measures!} metricKey="reliability" />
          <MetricBox component={component.key} measures={measures!} metricKey="vulnerabilities" />
          <MetricBox component={component.key} measures={measures!} metricKey="security_hotspots" />
          <MetricBox component={component.key} measures={measures!} metricKey="maintainability" />
        </div>

        <h1>{translate('portfolio.breakdown')}</h1>
        <div className="portfolio-breakdown">
          <div className="portfolio-breakdown-box">
            <h2 className="text-muted small">{translate('portfolio.number_of_projects')}</h2>
            <div className="portfolio-breakdown-metric huge">
              <Measure
                metricKey="projects"
                metricType="SHORT_INT"
                value={(measures && measures.projects) || '0'}
              />
            </div>
            <div className="portfolio-breakdown-box-link">
              <div>
                <MeasuresButtonLink component={component.key} metric="projects" />
              </div>
            </div>
          </div>
          <div className="portfolio-breakdown-box">
            <h2 className="text-muted small">{translate('portfolio.number_of_lines')}</h2>
            <div className="portfolio-breakdown-metric huge">
              <Measure
                metricKey="ncloc"
                metricType="SHORT_INT"
                value={(measures && measures.ncloc) || '0'}
              />
            </div>
            <div className="portfolio-breakdown-box-link">
              <div>
                <MeasuresButtonLink
                  component={component.key}
                  label={translate('portfolio.language_breakdown_link')}
                  metric="ncloc"
                />
              </div>
            </div>
          </div>
        </div>

        {subComponents !== undefined && totalSubComponents !== undefined && (
          <WorstProjects
            component={component.key}
            subComponents={subComponents}
            total={totalSubComponents}
          />
        )}
      </div>
    );
  }
}

const mapDispatchToProps: DispatchToProps = { fetchMetrics };

const mapStateToProps = (state: Store): StateToProps => ({
  metrics: getMetrics(state)
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(App);
