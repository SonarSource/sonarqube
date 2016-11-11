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
import { Router, Route, useRouterHistory } from 'react-router';
import { createHistory } from 'history';
import { Provider } from 'react-redux';
import App from './components/App';
import aboutRoutes from '../apps/about/routes';
import accountRoutes from '../apps/account/routes';
import projectsRoutes from '../apps/projects/routes';
import qualityGatesRoutes from '../apps/quality-gates/routes';
import qualityProfilesRoutes from '../apps/quality-profiles/routes';
import configureStore from '../components/store/configureStore';
import rootReducer from './store/rootReducer';
import './styles/index';

window.sonarqube.appStarted.then(options => {
  const el = document.querySelector(options.el);

  const history = useRouterHistory(createHistory)({
    basename: window.baseUrl + '/'
  });

  const store = configureStore(rootReducer);

  render((
      <Provider store={store}>
        <Router history={history}>
          <Route path="/" component={App}>
            <Route path="about">{aboutRoutes}</Route>
            <Route path="account">{accountRoutes}</Route>
            {projectsRoutes}
            {qualityGatesRoutes}
            {qualityProfilesRoutes}
          </Route>
        </Router>
      </Provider>
  ), el);
});
