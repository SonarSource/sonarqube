(function() {
  requirejs.config({
    baseUrl: "" + baseUrl + "/javascripts",
    paths: {
      'backbone': 'third-party/backbone',
      'backbone.marionette': 'third-party/backbone.marionette',
      'handlebars': 'third-party/handlebars',
      'jquery.mockjax': 'third-party/jquery.mockjax'
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

  requirejs(['backbone', 'backbone.marionette', 'coding-rules/layout', 'coding-rules/router', 'coding-rules/views/header-view', 'coding-rules/views/actions-view', 'coding-rules/views/filter-bar-view', 'coding-rules/views/coding-rules-list-view', 'coding-rules/views/coding-rules-bulk-change-view', 'coding-rules/views/coding-rules-quality-profile-activation-view', 'navigator/filters/base-filters', 'navigator/filters/choice-filters', 'navigator/filters/string-filters', 'navigator/filters/date-filter-view', 'coding-rules/views/filters/quality-profile-filter-view', 'coding-rules/views/filters/inheritance-filter-view', 'coding-rules/mockjax'], function(Backbone, Marionette, CodingRulesLayout, CodingRulesRouter, CodingRulesHeaderView, CodingRulesActionsView, CodingRulesFilterBarView, CodingRulesListView, CodingRulesBulkChangeView, CodingRulesQualityProfileActivationView, BaseFilters, ChoiceFilters, StringFilterView, DateFilterView, QualityProfileFilterView, InheritanceFilterView) {
    var App, appXHR;
    jQuery.ajaxSetup({
      error: function(jqXHR) {
        var errorBox, text, _ref;
        text = jqXHR.responseText;
        errorBox = jQuery('.modal-error');
        if (((_ref = jqXHR.responseJSON) != null ? _ref.errors : void 0) != null) {
          text = _.pluck(jqXHR.responseJSON.errors, 'msg').join('. ');
        }
        if (errorBox.length > 0) {
          return errorBox.show().text(text);
        } else {
          return alert(text);
        }
      }
    });
    jQuery('html').addClass('navigator-page coding-rules-page');
    App = new Marionette.Application;
    App.getQuery = function() {
      return this.filterBarView.getQuery();
    };
    App.restoreSorting = function() {};
    App.storeQuery = function(query, sorting) {
      var queryString;
      if (sorting) {
        _.extend(query, {
          sort: sorting.sort,
          asc: '' + sorting.asc
        });
      }
      queryString = _.map(query, function(v, k) {
        return "" + k + "=" + (encodeURIComponent(v));
      });
      return this.router.navigate(queryString.join('|'), {
        replace: true
      });
    };
    App.fetchList = function(firstPage) {
      var fetchQuery, query;
      query = this.getQuery();
      fetchQuery = _.extend({
        pageIndex: this.pageIndex
      }, query);
      if (this.codingRules.sorting) {
        _.extend(fetchQuery, {
          sort: this.codingRules.sorting.sort,
          asc: this.codingRules.sorting.asc
        });
      }
      this.storeQuery(query, this.codingRules.sorting);
      this.layout.showSpinner('resultsRegion');
      return jQuery.ajax({
        url: "" + baseUrl + "/api/codingrules/search",
        data: fetchQuery
      }).done((function(_this) {
        return function(r) {
          if (firstPage) {
            _this.codingRules.reset(r.codingrules);
          } else {
            _this.codingRules.add(r.codingrules);
          }
          _this.codingRules.paging = r.paging;
          _this.codingRulesListView = new CodingRulesListView({
            app: _this,
            collection: _this.codingRules
          });
          _this.layout.resultsRegion.show(_this.codingRulesListView);
          return _this.codingRulesListView.selectFirst();
        };
      })(this));
    };
    App.fetchFirstPage = function() {
      this.pageIndex = 1;
      return App.fetchList(true);
    };
    App.fetchNextPage = function() {
      if (this.pageIndex < this.codingRules.paging.pages) {
        this.pageIndex++;
        return App.fetchList(false);
      }
    };
    App.getActiveQualityProfile = function() {
      var value;
      value = this.activeInFilter.get('value');
      if ((value != null) && value.length === 1) {
        return value[0];
      } else {
        return null;
      }
    };
    App.getInactiveQualityProfile = function() {
      var value;
      value = this.inactiveInFilter.get('value');
      if ((value != null) && value.length === 1) {
        return value[0];
      } else {
        return null;
      }
    };
    App.addInitializer(function() {
      this.layout = new CodingRulesLayout({
        app: this
      });
      return jQuery('body').append(this.layout.render().el);
    });
    App.addInitializer(function() {
      this.codingRulesHeaderView = new CodingRulesHeaderView({
        app: this
      });
      return this.layout.headerRegion.show(this.codingRulesHeaderView);
    });
    App.addInitializer(function() {
      this.codingRules = new Backbone.Collection;
      return this.codingRules.sorting = {
        sort: 'CREATION_DATE',
        asc: false
      };
    });
    App.addInitializer(function() {
      this.codingRulesActionsView = new CodingRulesActionsView({
        app: this,
        collection: this.codingRules
      });
      return this.layout.actionsRegion.show(this.codingRulesActionsView);
    });
    App.addInitializer(function() {
      return this.codingRulesBulkChangeView = new CodingRulesBulkChangeView({
        app: this
      });
    });
    App.addInitializer(function() {
      return this.codingRulesQualityProfileActivationView = new CodingRulesQualityProfileActivationView({
        app: this
      });
    });
    App.addInitializer(function() {
      this.filters = new BaseFilters.Filters;
      this.filters.add(new BaseFilters.Filter({
        name: t('coding_rules.filters.name'),
        property: 'name',
        type: StringFilterView
      }));
      this.filters.add(new BaseFilters.Filter({
        name: t('coding_rules.filters.language'),
        property: 'languages',
        type: ChoiceFilters.ChoiceFilterView,
        choices: this.languages
      }));
      this.filters.add(new BaseFilters.Filter({
        name: t('coding_rules.filters.severity'),
        property: 'severities',
        type: ChoiceFilters.ChoiceFilterView,
        choices: {
          'BLOCKER': t('severity.BLOCKER'),
          'CRITICAL': t('severity.CRITICAL'),
          'MAJOR': t('severity.MAJOR'),
          'MINOR': t('severity.MINOR'),
          'INFO': t('severity.INFO')
        },
        choiceIcons: {
          'BLOCKER': 'severity-blocker',
          'CRITICAL': 'severity-critical',
          'MAJOR': 'severity-major',
          'MINOR': 'severity-minor',
          'INFO': 'severity-info'
        }
      }));
      this.filters.add(new BaseFilters.Filter({
        name: t('coding_rules.filters.tag'),
        property: 'tags',
        type: ChoiceFilters.ChoiceFilterView,
        choices: this.tags
      }));
      this.activeInFilter = new BaseFilters.Filter({
        name: t('coding_rules.filters.in_quality_profile'),
        property: 'in_quality_profile',
        type: QualityProfileFilterView,
        multiple: false
      });
      this.filters.add(this.activeInFilter);
      this.filters.add(new BaseFilters.Filter({
        name: t('coding_rules.filters.key'),
        property: 'key',
        type: StringFilterView,
        enabled: false,
        optional: true
      }));
      this.filters.add(new BaseFilters.Filter({
        name: t('coding_rules.filters.description'),
        property: 'description',
        type: StringFilterView,
        enabled: false,
        optional: true
      }));
      this.filters.add(new BaseFilters.Filter({
        name: t('coding_rules.filters.repository'),
        property: 'repositories',
        type: ChoiceFilters.ChoiceFilterView,
        enabled: false,
        optional: true,
        choices: this.repositories
      }));
      this.filters.add(new BaseFilters.Filter({
        name: t('coding_rules.filters.status'),
        property: 'statuses',
        type: ChoiceFilters.ChoiceFilterView,
        enabled: false,
        optional: true,
        choices: this.statuses
      }));
      this.filters.add(new BaseFilters.Filter({
        name: t('coding_rules.filters.availableSince'),
        property: 'availableSince',
        type: DateFilterView,
        enabled: false,
        optional: true
      }));
      this.inactiveInFilter = new BaseFilters.Filter({
        name: t('coding_rules.filters.out_of_quality_profile'),
        property: 'out_of_quality_profile',
        type: QualityProfileFilterView,
        multiple: false,
        enabled: false,
        optional: true
      });
      this.filters.add(this.inactiveInFilter);
      this.filters.add(new BaseFilters.Filter({
        name: t('coding_rules.filters.inheritance'),
        property: 'inheritance',
        type: InheritanceFilterView,
        enabled: false,
        optional: true,
        multiple: false,
        qualityProfileFilter: this.activeInFilter,
        choices: {
          'any': t('coding_rules.filters.inheritance.any'),
          'not_inhertited': t('coding_rules.filters.inheritance.not_inherited'),
          'inhertited': t('coding_rules.filters.inheritance.inherited'),
          'overriden': t('coding_rules.filters.inheritance.overriden')
        }
      }));
      this.filterBarView = new CodingRulesFilterBarView({
        app: this,
        collection: this.filters,
        extra: {
          sort: '',
          asc: false
        }
      });
      return this.layout.filtersRegion.show(this.filterBarView);
    });
    App.addInitializer(function() {
      this.router = new CodingRulesRouter({
        app: this
      });
      return Backbone.history.start();
    });
    appXHR = jQuery.ajax({
      url: "" + baseUrl + "/api/codingrules/app"
    });
    return jQuery.when(appXHR).done(function(r) {
      App.appState = new Backbone.Model;
      App.state = new Backbone.Model;
      App.qualityProfiles = r.qualityprofiles;
      App.languages = r.languages;
      App.repositories = r.repositories;
      App.statuses = r.statuses;
      App.tags = r.tags;
      window.messages = r.messages;
      jQuery('#coding-rules-page-loader').remove();
      return App.start();
    });
  });

}).call(this);
