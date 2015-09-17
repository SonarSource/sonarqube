import _ from 'underscore';
import IssueView from '../workspace-list-item-view';

export default IssueView.extend({
  onRender: function () {
    IssueView.prototype.onRender.apply(this, arguments);
    this.$el.removeClass('issue-navigate-right issue-with-checkbox');
  },

  serializeData: function () {
    return _.extend(IssueView.prototype.serializeData.apply(this, arguments), {
      showComponent: false
    });
  }
});


