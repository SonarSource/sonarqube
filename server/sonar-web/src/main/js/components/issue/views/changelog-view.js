define([
  'components/common/popup',
  '../templates'
], function (PopupView) {

  return PopupView.extend({
    template: Templates['issue-changelog'],

    collectionEvents: {
      'sync': 'render'
    },

    serializeData: function () {
      return _.extend(PopupView.prototype.serializeData.apply(this, arguments), {
        issue: this.options.issue.toJSON()
      });
    }
  });

});
