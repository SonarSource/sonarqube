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

import ComponentMeasuresApp from './components/ComponentMeasuresApp';
import AllMeasuresList from './components/AllMeasures';
import MeasureDetails from './components/MeasureDetails';
import MeasureDrilldownTree from './components/MeasureDrilldownTree';
import MeasureDrilldownList from './components/MeasureDrilldownList';
import MeasureHistory from './components/MeasureHistory';

import { checkHistoryExistence } from './hooks';

import './styles.css';

window.sonarqube.appStarted.then(options => {
  const el = document.querySelector(options.el);

  const history = useRouterHistory(createHistory)({
    basename: '/component_measures'
  });

  const Container = (props) => (
      <ComponentMeasuresApp {...props} component={options.component}/>
  );

  const handleRouteUpdate = () => {
    window.scrollTo(0, 0);
  };

  render((
      <Router history={history} onUpdate={handleRouteUpdate}>
        <Redirect from="/index" to="/"/>

        <Route path="/" component={Container}>
          <IndexRoute component={AllMeasuresList}/>
          <Route path=":metricKey" component={MeasureDetails}>
            <IndexRedirect to="tree"/>
            <Route path="tree" component={MeasureDrilldownTree}/>
            <Route path="list" component={MeasureDrilldownList}/>
            <Route path="history" component={MeasureHistory} onEnter={checkHistoryExistence}/>
          </Route>
        </Route>

        <Redirect from="*" to="/"/>
      </Router>
  ), el);
});
