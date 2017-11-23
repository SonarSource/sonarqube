/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { render } from 'react-dom';
import { Router, RouterState } from 'react-router';
import { Provider } from 'react-redux';
import routes from './routes';
import getStore from './getStore';
import getHistory from './getHistory';

export default function startReactApp() {
  const el = document.getElementById('content');

  const history = getHistory();
  const store = getStore();

  render(
    <Provider store={store}>
      <Router history={history} onUpdate={handleUpdate} routes={routes} />
    </Provider>,
    el
  );
}

function handleUpdate(this: { state: RouterState }) {
  const { action } = this.state.location;

  if (action === 'PUSH') {
    window.scrollTo(0, 0);
  }
}
