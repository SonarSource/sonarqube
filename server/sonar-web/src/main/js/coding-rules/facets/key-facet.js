define([
    'coding-rules/facets/base-facet',
    'templates/coding-rules'
], function (BaseFacet) {

  return BaseFacet.extend({
    template: Templates['coding-rules-key-facet'],

    onRender: function () {
      this.$el.toggleClass('hidden', !this.options.app.state.get('query').rule_key);
    },

    disable: function () {
      this.options.app.state.updateFilter({ rule_key: null });
    },

    serializeData: function () {
      return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
        key: this.options.app.state.get('query').rule_key
      });
    }
  });

});
