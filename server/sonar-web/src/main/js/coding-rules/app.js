requirejs.config({
  baseUrl: baseUrl + '/js',

  paths: {
    'backbone': 'third-party/backbone',
    'backbone.marionette': 'third-party/backbone.marionette',
    'handlebars': 'third-party/handlebars'
  },

  shim: {
    'backbone.marionette': {
      deps: ['backbone'],
      exports: 'Marionette'
    },
    'backbone': {
      exports: 'Backbone'
    },
    'handlebars': {
      exports: 'Handlebars'
    }
  }
});


requirejs([
  'backbone',
  'backbone.marionette',

  'coding-rules/models/state',
  'coding-rules/layout',
  'coding-rules/models/rules',
  'components/navigator/models/facets',

  'coding-rules/controller',
  'components/navigator/router',

  'coding-rules/workspace-list-view',
  'coding-rules//workspace-header-view',

  'coding-rules/facets-view',
  'coding-rules/filters-view',

  'common/handlebars-extensions'
],
    function (Backbone,
              Marionette,
              State,
              Layout,
              Rules,
              Facets,
              Controller,
              Router,
              WorkspaceListView,
              WorkspaceHeaderView,
              FacetsView,
              FiltersView) {

      var $ = jQuery,
          App = new Marionette.Application(),
          p = window.process.addBackgroundProcess();

      App.addInitializer(function () {
        this.layout = new Layout();
        $('.coding-rules').empty().append(this.layout.render().el);
      });

      App.addInitializer(function () {
        this.state = new State();
        this.list = new Rules();
        this.facets = new Facets();
      });

      App.addInitializer(function () {
        this.controller = new Controller({
          app: this
        });
      });

      App.addInitializer(function () {
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
      });

      App.addInitializer(function () {
        key.setScope('list');
        this.router = new Router({
          app: this
        });
        Backbone.history.start();
        window.process.finishBackgroundProcess(p);
      });

      App.manualRepository = function () {
        return {
          key: 'manual',
          name: t('coding_rules.manual_rules'),
          language: 'none'
        };
      };

      App.getSubCharacteristicName = function (name) {
        return (App.characteristics[name] || '').replace(': ', ' > ');
      };

      var appXHR = $.get(baseUrl + '/api/rules/app').done(function(r) {
        App.canWrite = r.canWrite;
        App.qualityProfiles = _.sortBy(r.qualityprofiles, ['name', 'lang']);
        App.languages = _.extend(r.languages, {
          none: 'None'
        });
        _.map(App.qualityProfiles, function(profile) {
          profile.language = App.languages[profile.lang];
        });
        App.repositories = r.repositories;
        App.repositories.push(App.manualRepository());
        App.statuses = r.statuses;
        App.characteristics = r.characteristics;
      });

      $.when(window.requestMessages(), appXHR).done(function () {
        App.start();
      });

    });
