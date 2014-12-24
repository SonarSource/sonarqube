define([
    'coding-rules/facets/base-facet',
    'templates/coding-rules'
], function (BaseFacet, Templates) {

  var $ = jQuery;

  return BaseFacet.extend({
    template: Templates['coding-rules-severity-facet'],
    severities: ['BLOCKER', 'MINOR', 'CRITICAL', 'INFO', 'MAJOR'],

    initialize: function (options) {
      this.listenTo(options.app.state, 'change:query', this.onQueryChange);
    },

    onQueryChange: function () {
      var query = this.options.app.state.get('query'),
          isProfileSelected = query.qprofile != null;
      if (!isProfileSelected) {
        this.forbid();
      }
    },

    onRender: function () {
      BaseFacet.prototype.onRender.apply(this, arguments);
      this.onQueryChange();
    },

    forbid: function () {
      BaseFacet.prototype.forbid.apply(this, arguments);
      this.$el.prop('title', t('coding_rules.filters.active_severity.inactive'));
    },

    allow: function () {
      BaseFacet.prototype.allow.apply(this, arguments);
      this.$el.prop('title', null);
    },

    getValues: function () {
      return this.severities.map(function (s) {
        return { val: s };
      });
    },

    sortValues: function (values) {
      var order = this.severities;
      return _.sortBy(values, function (v) {
        return order.indexOf(v.val);
      });
    },

    serializeData: function () {
      return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
        values: this.sortValues(this.getValues())
      });
    }
  });

});
