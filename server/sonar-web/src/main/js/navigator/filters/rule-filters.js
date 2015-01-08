define([
  'navigator/filters/base-filters',
  'navigator/filters/ajax-select-filters'
], function (BaseFilters, AjaxSelectFilters) {

  var RuleSuggestions = AjaxSelectFilters.Suggestions.extend({

    url: function() {
      return baseUrl + '/api/rules/search';
    },


    fetch: function(options) {
      options.data.f = 'name,lang';
      return AjaxSelectFilters.Suggestions.prototype.fetch.call(this, options);
    },


    parse: function(r) {
      this.more = r.p * r.ps < r.total;
      return r.rules.map(function(rule) {
         return { id: rule.key, text: rule.name, category: rule.lang };
      });
    }

  });



  var RuleDetailsFilterView = AjaxSelectFilters.AjaxSelectDetailsFilterView.extend({
    searchKey: 'q'
  });


  return AjaxSelectFilters.AjaxSelectFilterView.extend({

    initialize: function() {
      AjaxSelectFilters.AjaxSelectFilterView.prototype.initialize.call(this, {
        detailsView: RuleDetailsFilterView
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
              category: r.rule.language,
              checked: true
            }));
          });
    }

  });

});
