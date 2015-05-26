define([
  './templates'
], function () {

  return Marionette.ItemView.extend({
    tagName: 'li',
    className: 'panel panel-vertical',
    template: Templates['computation-list-item'],

    onRender: function () {
      this.$el.attr('data-id', this.model.id);
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    },

    onClose: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    serializeData: function () {
      var dangerStatuses = ['CANCELLED', 'FAIL'];
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        duration: this.model.getDuration(),
        danger: dangerStatuses.indexOf(this.model.get('status')) !== -1
      });
    }
  });

});
