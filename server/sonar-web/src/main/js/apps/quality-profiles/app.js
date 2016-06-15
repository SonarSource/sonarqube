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
import {
    Router,
    Route,
    IndexRoute,
    Redirect,
    useRouterHistory
} from 'react-router';
import { createHistory } from 'history';
import App from './components/App';
import ProfileContainer from './components/ProfileContainer';
import HomeContainer from './home/HomeContainer';
import ProfileDetails from './details/ProfileDetails';
import ChangelogContainer from './changelog/ChangelogContainer';
import ComparisonContainer from './compare/ComparisonContainer';

window.sonarqube.appStarted.then(options => {
  const el = document.querySelector(options.el);

  const history = useRouterHistory(createHistory)({
    basename: window.baseUrl + '/profiles'
  });

  render((
      <Router history={history}>
        <Route path="/" component={App}>
          <Redirect from="/index" to="/"/>

          <IndexRoute component={HomeContainer}/>

          <Route component={ProfileContainer}>
            <Route path="show" component={ProfileDetails}/>
            <Route path="changelog" component={ChangelogContainer}/>
            <Route path="compare" component={ComparisonContainer}/>
          </Route>
        </Route>
      </Router>
  ), el);
});
