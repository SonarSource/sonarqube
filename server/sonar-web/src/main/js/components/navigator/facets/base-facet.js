define([
    'backbone.marionette'
], function (Marionette) {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    className: 'search-navigator-facet-box',

    modelEvents: function () {
      return {
        'change': 'render'
      };
    },

    events: function () {
      return {
        'click .js-facet-toggle': 'toggle',
        'click .js-facet': 'toggleFacet'
      };
    },

    onRender: function () {
      this.$el.toggleClass('search-navigator-facet-box-collapsed', !this.model.get('enabled'));
      var that = this,
          property = this.model.get('property'),
          value = this.options.app.state.get('query')[property];
      if (typeof value === 'string') {
        value.split(',').forEach(function (s) {
          var facet = that.$('.js-facet').filter('[data-value="' + s + '"]');
          if (facet.length > 0) {
            facet.addClass('active');
          }
        });
      }
    },

    toggle: function () {
      this.options.app.controller.toggleFacet(this.model.id);
    },

    getValue: function () {
      return this.$('.js-facet.active').map(function () {
        return $(this).data('value');
      }).get().join();
    },

    toggleFacet: function (e) {
      $(e.currentTarget).toggleClass('active');
      var property = this.model.get('property'),
          obj = {};
      obj[property] = this.getValue();
      this.options.app.state.updateFilter(obj);
    },

    disable: function () {
      var property = this.model.get('property'),
          obj = {};
      obj[property] = null;
      this.options.app.state.updateFilter(obj);
    },

    sortValues: function (values) {
      return _.sortBy(values, function (v) {
        return -v.count;
      });
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        values: this.sortValues(this.model.getValues())
      });
    }
  });

});
