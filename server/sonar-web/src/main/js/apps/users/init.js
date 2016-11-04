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
import Marionette from 'backbone.marionette';

import Layout from './layout';
import Users from './users';
import HeaderView from './header-view';
import SearchView from './search-view';
import ListView from './list-view';
import ListFooterView from './list-footer-view';
import { getIdentityProviders } from '../../api/users';

const App = new Marionette.Application();

const init = function (el, providers) {
  // Layout
  this.layout = new Layout({ el });
  this.layout.render();

  // Collection
  this.users = new Users();

  // Header View
  this.headerView = new HeaderView({ collection: this.users });
  this.layout.headerRegion.show(this.headerView);

  // Search View
  this.searchView = new SearchView({ collection: this.users });
  this.layout.searchRegion.show(this.searchView);

  // List View
  this.listView = new ListView({ collection: this.users, providers });
  this.layout.listRegion.show(this.listView);

  // List Footer View
  this.listFooterView = new ListFooterView({ collection: this.users });
  this.layout.listFooterRegion.show(this.listFooterView);

  // Go!
  this.users.fetch();
};

App.on('start', function (el) {
  getIdentityProviders().then(r => init.call(App, el, r.identityProviders));
});

export default function (el) {
  App.start(el);
}
