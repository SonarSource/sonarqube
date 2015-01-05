define([
    'coding-rules/facets/base-facet',
    'templates/coding-rules'
], function (BaseFacet, Templates) {

  return BaseFacet.extend({
    template: Templates['coding-rules-query-facet'],

    initialize: function () {
      this.applyFacetDebounced = _.debounce(this.applyFacet, 1000);
    },

    events: function () {
      return _.extend(BaseFacet.prototype.events.apply(this, arguments), {
        'change input': 'applyFacet',
        'keydown input': 'onKeydown',
        'keyup input': 'applyFacetDebounced'
      });
    },

    onRender: function () {
      this.$el.attr('data-property', this.model.get('property'));
      var value = this.options.app.state.get('query').q;
      if (value != null) {
        this.$('input').val(value);
      }
      var that = this;
      setTimeout(function () {
        that.$('input').focus();
      }, 100);
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
