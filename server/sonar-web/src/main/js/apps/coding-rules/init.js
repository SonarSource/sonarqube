/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import $ from 'jquery';
import { sortBy } from 'lodash';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import key from 'keymaster';
import State from './models/state';
import Layout from './layout';
import Rules from './models/rules';
import Facets from '../../components/navigator/models/facets';
import Controller from './controller';
import Router from '../../components/navigator/router';
import WorkspaceListView from './workspace-list-view';
import WorkspaceHeaderView from './workspace-header-view';
import FacetsView from './facets-view';
import { searchQualityProfiles } from '../../api/quality-profiles';
import { getRulesApp } from '../../api/rules';
import { areThereCustomOrganizations } from '../../store/organizations/utils';

const App = new Marionette.Application();

App.on('start', function(
  options /*: {
  el: HTMLElement,
  organization: ?string,
  isDefaultOrganization: boolean
} */
) {
  App.organization = options.organization;
  const data = options.organization ? { organization: options.organization } : {};
  Promise.all([getRulesApp(data), searchQualityProfiles(data)])
    .then(([appResponse, profilesResponse]) => {
      App.customRules = !areThereCustomOrganizations();
      App.canWrite = appResponse.canWrite;
      App.organization = options.organization;
      App.qualityProfiles = sortBy(profilesResponse.profiles, ['name', 'lang']);
      App.languages = { ...appResponse.languages, none: 'None' };
      App.repositories = appResponse.repositories;
      App.statuses = appResponse.statuses;

      this.layout = new Layout({ el: options.el });
      this.layout.render();
      $('#footer').addClass('page-footer-with-sidebar');

      const allFacets = [
        'q',
        'rule_key',
        'languages',
        'types',
        'tags',
        'repositories',
        'severities',
        'statuses',
        'available_since',
        App.customRules ? 'is_template' : null,
        'qprofile',
        'inheritance',
        'active_severities'
      ].filter(f => f != null);

      this.state = new State({ allFacets });
      this.list = new Rules();
      this.facets = new Facets();

      this.controller = new Controller({ app: this });

      this.workspaceListView = new WorkspaceListView({
        app: this,
        collection: this.list
      });
      this.layout.workspaceListRegion.show(this.workspaceListView);
      this.workspaceListView.bindScrollEvents();

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

      key.setScope('list');
      this.router = new Router({
        app: this
      });
      Backbone.history.start();
    })
    .catch(() => {
      // do nothing in case of WS error
    });
});

export default function(
  el /*: HTMLElement */,
  organization /*: ?string */,
  isDefaultOrganization /*: boolean */
) {
  App.start({ el, organization, isDefaultOrganization });

  return () => {
    // $FlowFixMe
    Backbone.history.stop();
    App.layout.destroy();
    $('#footer').removeClass('page-footer-with-sidebar');
  };
}
