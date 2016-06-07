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
import { Router, Route, IndexRoute, Redirect, useRouterHistory } from 'react-router';
import { createHistory } from 'history';
import AccountApp from './components/AccountApp';
import Home from './components/Home';
import NotificationsContainer from './components/NotificationsContainer';
import Security from './components/Security';
import Issues from './components/Issues';
import ProjectsContainer from './projects/ProjectsContainer';

window.sonarqube.appStarted.then(options => {
  const el = document.querySelector(options.el);

  const history = useRouterHistory(createHistory)({
    basename: window.baseUrl + '/account'
  });

  render((
      <Router history={history}>
        <Route path="/" component={AccountApp}>
          <IndexRoute component={Home}/>
          <Route path="issues" component={Issues}/>
          <Route path="notifications" component={NotificationsContainer}/>
          <Route path="security" component={Security}/>
          <Route path="projects" component={ProjectsContainer}/>

          <Redirect from="/index" to="/"/>
        </Route>
      </Router>
  ), el);
});
