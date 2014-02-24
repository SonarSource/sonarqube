define(['backbone', 'navigator/filters/base-filters', 'navigator/filters/ajax-select-filters'], function (Backbone, BaseFilters, AjaxSelectFilters) {

  var RuleSuggestions = Backbone.Collection.extend({

    url: function() {
      return baseUrl + '/api/rules';
    },


    parse: function(r) {
      return _
          .filter(r, function(item) {
            return item.title;
          })
          .map(function(item) {
            return { id: item.key, text: item.title };
          });
    },


    fetch: function(options) {
      var data = {};
      if (options.data && options.data.s) {
        data.searchtext = options.data.s;
      }

      var settings = _.extend({}, options, { data: data });
      Backbone.Collection.prototype.fetch.call(this, settings);
    },


    fetchNextPage: function() {}

  });

  return AjaxSelectFilters.AjaxSelectFilterView.extend({

    initialize: function() {
      AjaxSelectFilters.AjaxSelectFilterView.prototype.initialize.call(this, {
        detailsView: AjaxSelectFilters.AjaxSelectDetailsFilterView
      });

      this.choices = new RuleSuggestions();
    },


    createRequest: function() {

    }

  });

});
