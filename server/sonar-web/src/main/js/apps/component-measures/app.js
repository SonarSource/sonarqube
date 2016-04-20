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
import { render } from 'react-dom';
import { Router, Route, IndexRoute, Redirect, IndexRedirect, useRouterHistory } from 'react-router';
import { createHistory } from 'history';
import { Provider } from 'react-redux';

import AppContainer from './app/AppContainer';
import HomeContainer from './home/HomeContainer';
import AllMeasuresContainer from './home/AllMeasuresContainer';
import DomainMeasuresContainer from './home/DomainMeasuresContainer';
import MeasureDetailsContainer from './details/MeasureDetailsContainer';
import ListViewContainer from './details/drilldown/ListViewContainer';
import TreeViewContainer from './details/drilldown/TreeViewContainer';
import MeasureHistoryContainer from './details/history/MeasureHistoryContainer';
import MeasureTreemapContainer from './details/treemap/MeasureTreemapContainer';

import configureStore from './store/configureStore';

import { checkHistoryExistence } from './hooks';

import './styles.css';

window.sonarqube.appStarted.then(options => {
  const el = document.querySelector(options.el);

  const history = useRouterHistory(createHistory)({
    basename: window.baseUrl + '/component_measures'
  });

  const store = configureStore({
    app: { component: options.component }
  });

  const handleRouteUpdate = () => {
    window.scrollTo(0, 0);
  };

  render((
      <Provider store={store}>
        <Router history={history} onUpdate={handleRouteUpdate}>
          <Redirect from="/index" to="/"/>

          <Route path="/" component={AppContainer}>
            <Route component={HomeContainer}>
              <IndexRoute component={AllMeasuresContainer}/>
              <Route path="domain/:domainName" component={DomainMeasuresContainer}/>
            </Route>

            <Route path="metric/:metricKey" component={MeasureDetailsContainer}>
              <IndexRedirect to="list"/>
              <Route path="list" component={ListViewContainer}/>
              <Route path="tree" component={TreeViewContainer}/>
              <Route path="history" component={MeasureHistoryContainer} onEnter={checkHistoryExistence}/>
              <Route path="treemap" component={MeasureTreemapContainer}/>
            </Route>
          </Route>

          <Redirect from="*" to="/"/>
        </Router>
      </Provider>
  ), el);
});
