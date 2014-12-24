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

  'coding-rules/models/rule',
  'coding-rules/rule-details-view',

  'common/handlebars-extensions'
],
    function (Backbone,
              Marionette,
              Rule,
              RuleDetailsView) {

      var $ = jQuery,
          App = new Marionette.Application(),
          p = window.process.addBackgroundProcess();

      App.addInitializer(function () {
        var url = baseUrl + '/api/rules/show',
            key = decodeURIComponent(window.location.search.substr(5)),
            options = {
              key: key,
              actives: true
            };
        $.get(url, options).done(function (data) {
          this.ruleDetailsView = new RuleDetailsView({
            app: App,
            model: new Rule(data.rule),
            actives: data.actives
          });
          this.ruleDetailsView.render().$el.appendTo($('.page'));
          window.process.finishBackgroundProcess(p);
        }).fail(function () {
          window.process.failBackgroundProcess(p);
        });
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
