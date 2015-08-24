define([
  'components/common/popup',
  '../../components/common/jquery-isolated-scroll',
  './templates'
], function (Popup) {

  return Popup.extend({
    template: Templates['update-center-plugin-changelog'],

    onRender: function () {
      Popup.prototype.onRender.apply(this, arguments);
      this.$('.bubble-popup-container').isolatedScroll();
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    },

    onClose: function () {
      Popup.prototype.onClose.apply(this, arguments);
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    serializeData: function () {
      return _.extend(Popup.prototype.serializeData.apply(this, arguments), {
        // if there is no status, this is a new plugin
        // => force COMPATIBLE status
        status: this.model.get('status') || 'COMPATIBLE'
      });
    }
  });

});
