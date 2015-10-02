import _ from 'underscore';
import PopupView from '../../common/popup';
import Template from '../templates/issue-changelog.hbs';

export default PopupView.extend({
  template: Template,

  collectionEvents: {
    'sync': 'render'
  },

  serializeData: function () {
    return _.extend(PopupView.prototype.serializeData.apply(this, arguments), {
      issue: this.options.issue.toJSON()
    });
  }
});


