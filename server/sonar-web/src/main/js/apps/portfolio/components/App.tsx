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
import { connect } from 'react-redux';
import Summary from './Summary';
import Report from './Report';
import WorstProjects from './WorstProjects';
import ReleasabilityBox from './ReleasabilityBox';
import ReliabilityBox from './ReliabilityBox';
import SecurityBox from './SecurityBox';
import MaintainabilityBox from './MaintainabilityBox';
import Activity from './Activity';
import { SubComponent } from '../types';
import { PORTFOLIO_METRICS, SUB_COMPONENTS_METRICS, convertMeasures } from '../utils';
import { getMeasures } from '../../../api/measures';
import { getChildren } from '../../../api/components';
import { translate } from '../../../helpers/l10n';
import { fetchMetrics } from '../../../store/rootActions';
import { getMetrics } from '../../../store/rootReducer';
import { Metric } from '../../../app/types';
import '../styles.css';

interface OwnProps {
  component: { key: string; name: string };
}

interface StateToProps {
  metrics: { [key: string]: Metric };
}

interface DispatchToProps {
  fetchMetrics: () => void;
}

type Props = StateToProps & DispatchToProps & OwnProps;

interface State {
  loading: boolean;
  measures?: { [key: string]: string | undefined };
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
      getMeasures(this.props.component.key, PORTFOLIO_METRICS),
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
          <i className="spinner spinner-margin" />
        </div>
      </div>
    );
  }

  renderEmpty() {
    return (
      <div className="empty-search">
        <h3>{translate('portfolio.empty')}</h3>
        <p>{translate('portfolio.empty.hint')}</p>
      </div>
    );
  }

  renderWhenNotComputed() {
    return (
      <div className="empty-search">
        <h3>{translate('portfolio.not_computed')}</h3>
      </div>
    );
  }

  renderMain() {
    const { component } = this.props;
    const { measures, subComponents, totalSubComponents } = this.state;

    if (this.isEmpty()) {
      return this.renderEmpty();
    }

    if (this.isNotComputed()) {
      return this.renderWhenNotComputed();
    }

    return (
      <div>
        <div className="portfolio-boxes">
          <ReleasabilityBox component={component.key} measures={measures!} />
          <ReliabilityBox component={component.key} measures={measures!} />
          <SecurityBox component={component.key} measures={measures!} />
          <MaintainabilityBox component={component.key} measures={measures!} />
        </div>

        {subComponents !== undefined &&
          totalSubComponents !== undefined && (
            <WorstProjects
              component={component.key}
              subComponents={subComponents}
              total={totalSubComponents}
            />
          )}
      </div>
    );
  }

  render() {
    const { component } = this.props;
    const { loading, measures } = this.state;

    if (loading) {
      return this.renderSpinner();
    }

    return (
      <div className="page page-limited">
        <div className="page-with-sidebar">
          <div className="page-main">{this.renderMain()}</div>

          <aside className="page-sidebar-fixed">
            {!this.isEmpty() &&
              !this.isNotComputed() && <Summary component={component} measures={measures!} />}
            <Activity component={component.key} metrics={this.props.metrics} />
            <Report component={component} />
          </aside>
        </div>
      </div>
    );
  }
}

const mapDispatchToProps: DispatchToProps = { fetchMetrics };

const mapStateToProps = (state: any): StateToProps => ({
  metrics: getMetrics(state)
});

export default connect<StateToProps, DispatchToProps, Props>(mapStateToProps, mapDispatchToProps)(
  App
);
