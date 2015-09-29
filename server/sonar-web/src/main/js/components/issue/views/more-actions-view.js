import $ from 'jquery';
import PopupView from 'components/common/popup';
import '../templates';

export default PopupView.extend({
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


