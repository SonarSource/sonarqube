define([
  'backbone.marionette'
], function (Marionette) {

  return Marionette.Controller.extend({
    pageSize: 50,
    allFacets: [],
    facetsFromServer: [],
    transform: {},

    initialize: function (options) {
      this.app = options.app;
      this.listenTo(options.app.state, 'change:query', this.fetchList);
    },

    _allFacets: function () {
      return this.allFacets.map(function (facet) {
        return {property: facet};
      });
    },

    _enabledFacets: function () {
      var that = this,
          facets = this.options.app.state.get('facets'),
          criteria = Object.keys(this.options.app.state.get('query'));
      facets = facets.concat(criteria);
      facets = facets.map(function (facet) {
        return that.transform[facet] != null ? that.transform[facet] : facet;
      });
      return facets.filter(function (facet) {
        return that.allFacets.indexOf(facet) !== -1;
      });
    },

    _facetsFromServer: function () {
      var that = this,
          facets = this._enabledFacets();
      return facets.filter(function (facet) {
        return that.facetsFromServer.indexOf(facet) !== -1;
      });
    },

    fetchList: function () {

    },

    fetchNextPage: function () {
      this.options.app.state.nextPage();
      return this.fetchList(false);
    },

    enableFacet: function (id) {
      var facet = this.options.app.facets.get(id);
      if (facet.has('values') || this.facetsFromServer.indexOf(id) === -1) {
        facet.set({enabled: true});
      } else {
        var p = window.process.addBackgroundProcess();
        this.requestFacet(id)
            .done(function () {
              facet.set({enabled: true});
              window.process.finishBackgroundProcess(p);
            })
            .fail(function () {
              window.process.failBackgroundProcess(p);
            });
      }
    },

    disableFacet: function (id) {
      var facet = this.options.app.facets.get(id);
      facet.set({enabled: false});
      this.options.app.facetsView.children.findByModel(facet).disable();
    },

    toggleFacet: function (id) {
      var facet = this.options.app.facets.get(id);
      if (facet.get('enabled')) {
        this.disableFacet(id);
      } else {
        this.enableFacet(id);
      }
    },

    enableFacets: function (facets) {
      facets.forEach(this.enableFacet, this);
    },

    newSearch: function () {
      this.options.app.state.setQuery({});
    },

    parseQuery: function (query, separator) {
      separator = separator || '|';
      var q = {};
      (query || '').split(separator).forEach(function (t) {
        var tokens = t.split('=');
        if (tokens[0] && tokens[1] != null) {
          q[tokens[0]] = decodeURIComponent(tokens[1]);
        }
      });
      return q;
    },

    getQuery: function (separator) {
      separator = separator || '|';
      var filter = this.options.app.state.get('query'),
          route = [];
      _.map(filter, function (value, property) {
        route.push('' + property + '=' + encodeURIComponent(value));
      });
      return route.join(separator);
    },

    getRoute: function (separator) {
      separator = separator || '|';
      return this.getQuery(separator);
    },

    selectNext: function () {
      var index = this.options.app.state.get('selectedIndex') + 1;
      if (index < this.options.app.list.length) {
        this.options.app.state.set({ selectedIndex: index });
      } else {
        if (!this.options.app.state.get('maxResultsReached')) {
          var that = this;
          this.fetchNextPage().done(function () {
            that.options.app.state.set({ selectedIndex: index });
          });
        }
      }
    },

    selectPrev: function () {
      var index = this.options.app.state.get('selectedIndex') - 1;
      if (index >= 0) {
        this.options.app.state.set({ selectedIndex: index });
      }
    }

  });

});
