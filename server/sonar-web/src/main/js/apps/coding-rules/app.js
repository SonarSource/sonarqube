import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import State from './models/state';
import Layout from './layout';
import Rules from './models/rules';
import Facets from 'components/navigator/models/facets';
import Controller from './controller';
import Router from 'components/navigator/router';
import WorkspaceListView from './workspace-list-view';
import WorkspaceHeaderView from './workspace-header-view';
import FacetsView from './facets-view';
import FiltersView from './filters-view';

var App = new Marionette.Application(),
    init = function (options) {
      this.layout = new Layout({ el: options.el });
      this.layout.render();
      $('#footer').addClass('search-navigator-footer');

      this.state = new State();
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

      this.filtersView = new FiltersView({
        app: this
      });
      this.layout.filtersRegion.show(this.filtersView);

      key.setScope('list');
      this.router = new Router({
        app: this
      });
      Backbone.history.start();
    };

App.manualRepository = function () {
  return {
    key: 'manual',
    name: t('coding_rules.manual_rule'),
    language: 'none'
  };
};

App.getSubCharacteristicName = function (key) {
  if (key != null) {
    var ch = _.findWhere(App.characteristics, { key: key }),
        parent = _.findWhere(App.characteristics, { key: ch.parent });
    return [parent.name, ch.name].join(' > ');
  } else {
    return null;
  }
};

var appXHR = $.get(baseUrl + '/api/rules/app').done(function (r) {
  App.canWrite = r.canWrite;
  App.qualityProfiles = _.sortBy(r.qualityprofiles, ['name', 'lang']);
  App.languages = _.extend(r.languages, {
    none: 'None'
  });
  _.map(App.qualityProfiles, function (profile) {
    profile.language = App.languages[profile.lang];
  });
  App.repositories = r.repositories;
  App.repositories.push(App.manualRepository());
  App.statuses = r.statuses;
  App.characteristics = r.characteristics.map(function (item, index) {
    return _.extend(item, { index: index });
  });
});

App.on('start', function (options) {
  $.when(window.requestMessages(), appXHR).done(function () {
    init.call(App, options);
  });
});

export default App;
