define([
    'coding-rules/facets/base-facet',
    'templates/coding-rules'
], function (BaseFacet) {

  return BaseFacet.extend({
    template: Templates['coding-rules-query-facet'],

    events: function () {
      return _.extend(BaseFacet.prototype.events.apply(this, arguments), {
        'change input': 'applyFacet',
        'keydown input': 'onKeydown'
      });
    },

    onRender: function () {
      this.$el.attr('data-property', this.model.get('property'));
      var query = this.options.app.state.get('query'),
          value = query.q;
      if (value != null) {
        this.$('input').val(value);
      }
    },

    applyFacet: function() {
      var obj = {},
          property = this.model.get('property');
      obj[property] = this.$('input').val();
      this.options.app.state.updateFilter(obj);
    },

    onKeydown: function (e) {
      // escape
      if (e.keyCode === 27) {
        this.$('input').blur();
      }
    }
  });

});
