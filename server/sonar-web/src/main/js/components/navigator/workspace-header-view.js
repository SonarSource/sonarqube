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
        'click .js-bulk-change': 'onBulkChangeClick',
        'click .js-reload': 'reload',
        'click .js-next': 'selectNext',
        'click .js-prev': 'selectPrev'
      };
    },

    initialize: function (options) {
      this.listenTo(options.app.state, 'change', this.render);
    },

    onRender: function () {
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    },

    onBeforeRender: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    onDestroy: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    onBulkChangeClick: function (e) {
      e.preventDefault();
      this.bulkChange();
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
