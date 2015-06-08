define([
  './templates'
], function () {

  return Marionette.ItemView.extend({
    tagName: 'li',
    className: 'panel',
    template: Templates['computation-list-item'],

    onRender: function () {
      this.$el.attr('data-id', this.model.id);
      this.$el.toggleClass('panel-danger', this.model.isDanger());
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    },

    onClose: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        duration: this.model.getDuration()
      });
    }
  });

});
