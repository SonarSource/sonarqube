define([
  'backbone.marionette',
  './templates'
], function (Marionette) {

  return Marionette.ItemView.extend({
    tagName: 'a',
    className: 'list-group-item',
    template: Templates['api-documentation-web-service'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click': 'onClick'
    },

    initialize: function () {
      this.listenTo(this.options.state, 'change:internal', this.toggleInternal);
    },

    onRender: function () {
      this.$el.attr('data-path', this.model.get('path'));
      this.$el.toggleClass('active', this.options.highlighted);
      this.toggleInternal();
    },

    onClick: function (e) {
      e.preventDefault();
      this.model.trigger('select', this.model);
    },

    toggleInternal: function () {
      var showInternal = this.options.state.get('internal'),
          hideMe = this.model.get('internal') && !showInternal;
      this.$el.toggleClass('hidden', hideMe);
    }
  });

});
