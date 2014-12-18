define([
    'backbone.marionette'
], function (Marionette) {

  return Marionette.ItemView.extend({

    collectionEvents: function () {
      return {
        'all': 'render'
      };
    },

    events: function () {
      return {
        'click .js-bulk-change': 'bulkChange',
        'click .js-reload': 'reload',
        'click .js-next': 'selectNext',
        'click .js-prev': 'selectPrev'
      };
    },

    initialize: function (options) {
      this.listenTo(options.app.state, 'change', this.render);
    },

    bulkChange: function () {

    },

    reload: function () {
      this.options.app.controller.fetchList();
    },

    selectNext: function () {
      this.options.app.controller.selectNext();
    },

    selectPrev: function () {
      this.options.app.controller.selectPrev();
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        state: this.options.app.state.toJSON()
      });
    }
  });

});
