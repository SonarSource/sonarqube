define([
  'components/common/popup',
  './templates'
], function (Popup) {

  return Popup.extend({
    template: Templates['update-center-plugin-changelog'],

    onRender: function () {
      this._super();
      this.$('.bubble-popup-container').isolatedScroll();
    }
  });

});
