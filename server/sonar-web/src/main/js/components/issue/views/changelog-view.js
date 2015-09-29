import _ from 'underscore';
import PopupView from 'components/common/popup';
import '../templates';

export default PopupView.extend({
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


