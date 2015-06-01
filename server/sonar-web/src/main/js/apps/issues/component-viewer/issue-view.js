define([
  '../workspace-list-item-view'
], function (IssueView) {

  return IssueView.extend({
    onRender: function () {
      IssueView.prototype.onRender.apply(this, arguments);
      this.$el.removeClass('issue-navigate-right');
    },

    serializeData: function () {
      return _.extend(IssueView.prototype.serializeData.apply(this, arguments), {
        showComponent: false
      });
    }
  });

});
