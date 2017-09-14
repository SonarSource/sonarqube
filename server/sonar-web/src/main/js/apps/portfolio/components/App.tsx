/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import Summary from './Summary';
import Report from './Report';
import WorstProjects from './WorstProjects';
import ReleasabilityBox from './ReleasabilityBox';
import ReliabilityBox from './ReliabilityBox';
import SecurityBox from './SecurityBox';
import MaintainabilityBox from './MaintainabilityBox';
import { Component } from '../../../app/types';
import { getMeasures } from '../../../api/measures';
import { getChildren } from '../../../api/components';
import { SubComponent } from '../types';
import '../styles.css';

interface Props {
  component: Component;
}

interface State {
  loading: boolean;
  measures?: { [key: string]: string | undefined };
  subComponents?: SubComponent[];
  totalSubComponents?: number;
}

export default class App extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    const html = document.querySelector('html');
    if (html) {
      html.classList.add('dashboard-page');
    }
    this.fetchData();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.component !== this.props.component) {
      this.fetchData();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    const html = document.querySelector('html');
    if (html) {
      html.classList.remove('dashboard-page');
    }
  }

  fetchData() {
    this.setState({ loading: true });
    Promise.all([this.fetchMeasures(), this.fetchChildren()]).then(
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

  fetchMeasures() {
    const METRICS = [
      'projects',
      'ncloc',
      'ncloc_language_distribution',

      'releasability_rating',
      'releasability_effort',

      'sqale_rating',
      'maintainability_rating_effort',

      'reliability_rating',
      'reliability_rating_effort',

      'security_rating',
      'security_rating_effort',

      'last_change_on_releasability_rating',
      'last_change_on_maintainability_rating',
      'last_change_on_security_rating',
      'last_change_on_reliability_rating'
    ];
    return getMeasures(this.props.component.key, METRICS);
  }

  fetchChildren() {
    const METRICS = [
      'ncloc',
      'releasability_rating',
      'security_rating',
      'reliability_rating',
      'sqale_rating',
      'alert_status'
    ];
    return getChildren(this.props.component.key, METRICS, { ps: 20 });
  }

  renderSpinner() {
    return (
      <div className="page page-limited">
        <div className="text-center">
          <i className="spinner spinner-margin" />
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

    return (
      <div className="page page-limited">
        <div className="page-with-sidebar">
          <div className="page-main">
            {measures != undefined && (
              <div className="portfolio-boxes">
                <ReleasabilityBox component={component.key} measures={measures} />
                <ReliabilityBox component={component.key} measures={measures} />
                <SecurityBox component={component.key} measures={measures} />
                <MaintainabilityBox component={component.key} measures={measures} />
              </div>
            )}

            {subComponents != undefined &&
            totalSubComponents != undefined && (
              <WorstProjects
                component={component}
                subComponents={subComponents}
                total={totalSubComponents}
              />
            )}
          </div>

          <aside className="page-sidebar-fixed">
            {measures != undefined && <Summary component={component} measures={measures} />}
            <Report component={component} />
          </aside>
        </div>
      </div>
    );
  }
}

function convertMeasures(measures: Array<{ metric: string; value?: string }>) {
  const result: { [key: string]: string | undefined } = {};
  measures.forEach(measure => {
    result[measure.metric] = measure.value;
  });
  return result;
}
