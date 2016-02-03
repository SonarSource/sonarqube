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
import React from 'react';

import Gate from './gate/gate';
import GeneralMain from './main/main';
import Meta from './meta';
import { StructureMain } from './domains/structure-domain';
import { DuplicationsMain } from './domains/duplications-domain';
import { CoverageMain } from './domains/coverage-domain';
import { DebtMain } from './domains/debt-domain';

import { getMetrics } from '../../api/metrics';
import { RouterMixin } from '../../components/router/router';
import { translate } from '../../helpers/l10n';


export const Overview = React.createClass({
  mixins: [RouterMixin],

  getInitialState () {
    return { ready: false };
  },

  componentDidMount () {
    this.requestMetrics();
  },

  requestMetrics () {
    return getMetrics().then(metrics => this.setState({ ready: true, metrics }));
  },

  renderLoading () {
    return <div className="text-center">
      <i className="spinner spinner-margin"/>
    </div>;
  },

  renderMain() {
    return <div className="overview">
      <div className="overview-main">
        <Gate component={this.props.component} gate={this.props.gate}/>
        <GeneralMain {...this.props} {...this.state} navigate={this.navigate}/>
      </div>
      <Meta component={this.props.component}/>
    </div>;
  },

  renderSize () {
    return <div className="overview">
      <StructureMain {...this.props} {...this.state}/>
    </div>;
  },

  renderDuplications () {
    return <div className="overview">
      <DuplicationsMain {...this.props} {...this.state}/>
    </div>;
  },

  renderTests () {
    return <div className="overview">
      <CoverageMain {...this.props} {...this.state}/>
    </div>;
  },

  renderIssues () {
    return <div className="overview">
      <DebtMain {...this.props} {...this.state}/>
    </div>;
  },

  render () {
    if (!this.state.ready) {
      return this.renderLoading();
    }
    switch (this.state.route) {
      case '':
        return this.renderMain();
      case '/structure':
        return this.renderSize();
      case '/duplications':
        return this.renderDuplications();
      case '/coverage':
        return this.renderTests();
      case '/debt':
        return this.renderIssues();
      default:
        throw new Error('Unknown route: ' + this.state.route);
    }
  }
});


export const EmptyOverview = React.createClass({
  render() {
    return (
        <div className="page">
          <div className="alert alert-warning">
            {translate('provisioning.no_analysis')}
          </div>
          <div className="big-spacer-top">
            <h4>{translate('key')}</h4>
            <code>{this.props.component.key}</code>
          </div>
        </div>
    );
  }
});
