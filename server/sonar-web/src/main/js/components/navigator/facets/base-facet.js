define(function () {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    className: 'search-navigator-facet-box',
    forbiddenClassName: 'search-navigator-facet-box-forbidden',

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
      this.$el.attr('data-property', this.model.get('property'));
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
      if (!this.isForbidden()) {
        this.options.app.controller.toggleFacet(this.model.id);
      }
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

    forbid: function () {
      this.options.app.controller.disableFacet(this.model.id);
      this.$el.addClass(this.forbiddenClassName);
    },

    allow: function () {
      this.$el.removeClass(this.forbiddenClassName);
    },

    isForbidden: function () {
      return this.$el.hasClass(this.forbiddenClassName);
    },

    sortValues: function (values) {
      return values.slice().sort(function (left, right) {
        if (left.count !== right.count) {
          return right.count - left.count;
        }
        if (left.val !== right.val) {
          if (left.val > right.val) {
            return 1;
          }
          if (left.val < right.val) {
            return -1;
          }
        }
        return 0;
      });
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        values: this.sortValues(this.model.getValues())
      });
    }
  });

});
