import _ from 'underscore';
import Marionette from 'backbone.marionette';
import ManualRuleCreationView from './rule/manual-rule-creation-view';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['coding-rules-filters'],

  events: {
    'click .js-create-manual-rule': 'createManualRule'
  },

  createManualRule: function () {
    new ManualRuleCreationView({
      app: this.options.app
    }).render();
  },

  serializeData: function () {
    return _.extend(this._super(), { canWrite: this.options.app.canWrite });
  }
});


