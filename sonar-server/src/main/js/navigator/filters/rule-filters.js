define([
  'backbone',
  'navigator/filters/base-filters',
  'navigator/filters/ajax-select-filters'
], function (Backbone, BaseFilters, AjaxSelectFilters) {

  var RuleSuggestions = AjaxSelectFilters.Suggestions.extend({

    url: function() {
      return baseUrl + '/api/rules/search';
    },


    parse: function(r) {
      this.more = r.p * r.ps < r.total;
      return r.rules.map(function(r) {
         return { id: r.key, text: r.name, category: r.lang };
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
