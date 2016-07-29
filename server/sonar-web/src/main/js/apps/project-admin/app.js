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
import { Provider } from 'react-redux';
import { Router, Route, useRouterHistory } from 'react-router';
import { createHistory } from 'history';
import Deletion from './deletion/Deletion';
import QualityProfiles from './quality-profiles/QualityProfiles';
import QualityGate from './quality-gate/QualityGate';
import Links from './links/Links';
import rootReducer from './store/rootReducer';
import configureStore from '../../components/store/configureStore';

window.sonarqube.appStarted.then(options => {
  const el = document.querySelector(options.el);

  const history = useRouterHistory(createHistory)({
    basename: window.baseUrl + '/project'
  });

  const store = configureStore(rootReducer);

  const withComponent = ComposedComponent => props =>
      <ComposedComponent {...props} component={options.component}/>;

  render((
      <Provider store={store}>
        <Router history={history}>
          <Route
              path="/deletion"
              component={withComponent(Deletion)}/>
          <Route
              path="/quality_profiles"
              component={withComponent(QualityProfiles)}/>
          <Route
              path="/quality_gate"
              component={withComponent(QualityGate)}/>
          <Route
              path="/links"
              component={withComponent(Links)}/>
        </Router>
      </Provider>
  ), el);
});
