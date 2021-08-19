/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { Location } from 'history';
import * as React from 'react';
import { connect } from 'react-redux';
import { InjectedRouter } from 'react-router';
import { getChildren } from '../../../api/components';
import { getMeasures } from '../../../api/measures';
import MeasuresLink from '../../../components/common/MeasuresLink';
import ComponentReportActions from '../../../components/controls/ComponentReportActions';
import { withCurrentUser } from '../../../components/hoc/withCurrentUser';
import Measure from '../../../components/measure/Measure';
import handleRequiredAuthentication from '../../../helpers/handleRequiredAuthentication';
import { translate } from '../../../helpers/l10n';
import { isLoggedIn } from '../../../helpers/users';
import { fetchMetrics } from '../../../store/rootActions';
import { getMetrics, Store } from '../../../store/rootReducer';
import '../styles.css';
import { SubComponent } from '../types';
import { convertMeasures, PORTFOLIO_METRICS, SUB_COMPONENTS_METRICS } from '../utils';
import MetricBox from './MetricBox';
import UnsubscribeEmailModal from './UnsubscribeEmailModal';
import WorstProjects from './WorstProjects';

interface OwnProps {
  component: T.Component;
  currentUser: T.CurrentUser;
  location: Location;
  router: InjectedRouter;
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
  showUnsubscribeModal: boolean;
}

export class App extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      loading: true,
      showUnsubscribeModal:
        Boolean(props.location.query.unsubscribe) && isLoggedIn(props.currentUser)
    };
  }

  componentDidMount() {
    this.mounted = true;

    if (Boolean(this.props.location.query.unsubscribe) && !isLoggedIn(this.props.currentUser)) {
      handleRequiredAuthentication();
    }

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

  handleCloseUnsubscribeEmailModal = () => {
    const { location, router } = this.props;
    this.setState({ showUnsubscribeModal: false });
    router.replace({ ...location, query: { ...location.query, unsubscribe: undefined } });
  };

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
    const {
      loading,
      measures,
      subComponents,
      totalSubComponents,
      showUnsubscribeModal
    } = this.state;

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
          <ComponentReportActions component={component} />
        </div>

        {component.description && (
          <div className="portfolio-description display-inline-block big-spacer-bottom">
            {component.description}
          </div>
        )}
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
                <MeasuresLink component={component.key} metric="projects" />
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
                <MeasuresLink
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

        {showUnsubscribeModal && (
          <UnsubscribeEmailModal
            component={component}
            onClose={this.handleCloseUnsubscribeEmailModal}
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

export default connect(mapStateToProps, mapDispatchToProps)(withCurrentUser(App));
