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
import $ from 'jquery';
import { difference } from 'lodash';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import State from '../issues/models/state';
import Layout from '../issues/layout';
import Issues from '../issues/models/issues';
import Facets from '../../components/navigator/models/facets';
import Controller from '../issues/controller';
import Router from '../issues/router';
import WorkspaceListView from '../issues/workspace-list-view';
import WorkspaceHeaderView from '../issues/workspace-header-view';
import FacetsView from './../issues/facets-view';
import HeaderView from './../issues/HeaderView';

const App = new Marionette.Application();
const init = function({ el, component, currentUser }) {
  this.config = {
    resource: component.id,
    resourceName: component.name,
    resourceQualifier: component.qualifier
  };
  this.state = new State({
    canBulkChange: currentUser.isLoggedIn,
    isContext: true,
    contextQuery: { componentUuids: this.config.resource },
    contextComponentUuid: this.config.resource,
    contextComponentName: this.config.resourceName,
    contextComponentQualifier: this.config.resourceQualifier
  });
  this.updateContextFacets();
  this.list = new Issues();
  this.facets = new Facets();

  this.layout = new Layout({ app: this, el });
  this.layout.render();
  $('#footer').addClass('search-navigator-footer');

  this.controller = new Controller({ app: this });

  this.issuesView = new WorkspaceListView({
    app: this,
    collection: this.list
  });
  this.layout.workspaceListRegion.show(this.issuesView);
  this.issuesView.bindScrollEvents();

  this.workspaceHeaderView = new WorkspaceHeaderView({
    app: this,
    collection: this.list
  });
  this.layout.workspaceHeaderRegion.show(this.workspaceHeaderView);

  this.facetsView = new FacetsView({
    app: this,
    collection: this.facets
  });
  this.layout.facetsRegion.show(this.facetsView);

  this.headerView = new HeaderView({
    app: this
  });
  this.layout.filtersRegion.show(this.headerView);

  key.setScope('list');
  App.router = new Router({ app: App });
  Backbone.history.start();
};

App.getContextQuery = function() {
  return { componentUuids: this.config.resource };
};

App.getRestrictedFacets = function() {
  return {
    TRK: ['projectUuids'],
    BRC: ['projectUuids'],
    DIR: ['projectUuids', 'moduleUuids', 'directories'],
    DEV: ['authors'],
    DEV_PRJ: ['projectUuids', 'authors']
  };
};

App.updateContextFacets = function() {
  const facets = this.state.get('facets');
  const allFacets = this.state.get('allFacets');
  const facetsFromServer = this.state.get('facetsFromServer');
  return this.state.set({
    facets,
    allFacets: difference(allFacets, this.getRestrictedFacets()[this.config.resourceQualifier]),
    facetsFromServer: difference(
      facetsFromServer,
      this.getRestrictedFacets()[this.config.resourceQualifier]
    )
  });
};

App.on('start', options => {
  init.call(App, options);
});

export default function(el, component, currentUser) {
  App.start({ el, component, currentUser });

  return () => {
    Backbone.history.stop();
    App.layout.destroy();
    $('#footer').removeClass('search-navigator-footer');
  };
}
