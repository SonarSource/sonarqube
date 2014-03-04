define(['backbone', 'navigator/filters/base-filters', 'navigator/filters/ajax-select-filters'], function (Backbone, BaseFilters, AjaxSelectFilters) {

  var RuleSuggestions = AjaxSelectFilters.Suggestions.extend({

    url: function() {
      return baseUrl + '/api/rules/list';
    },


    parse: function(r) {
      this.more = r.more;
      return r.results.map(function(r) {
         return { id: r.key, text: r.name };
      });
    }

  });

  return AjaxSelectFilters.AjaxSelectFilterView.extend({

    initialize: function() {
      AjaxSelectFilters.AjaxSelectFilterView.prototype.initialize.call(this, {
        detailsView: AjaxSelectFilters.AjaxSelectDetailsFilterView
      });

      this.choices = new RuleSuggestions();
    },


    createRequest: function(v) {
      var that = this;
      return jQuery
          .ajax({
            url: baseUrl + '/api/rules/show',
            type: 'GET',
            data: { key: v }
          })
          .done(function (r) {
            that.choices.add(new Backbone.Model({
              id: r.rule.key,
              text: r.rule.name,
              checked: true
            }));
          });
    }

  });

});
