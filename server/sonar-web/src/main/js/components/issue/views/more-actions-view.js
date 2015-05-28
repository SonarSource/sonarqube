define([
  'components/common/popup',
  '../templates'
], function (PopupView) {

  var $ = jQuery;

  return PopupView.extend({
    template: Templates['issue-more-actions'],

    events: function () {
      return {
        'click .js-issue-action': 'action'
      };
    },

    action: function (e) {
      var actionKey = $(e.currentTarget).data('action');
      return this.options.detailView.action(actionKey);
    }
  });

});
