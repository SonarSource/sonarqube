import _ from 'underscore';
import Marionette from 'backbone.marionette';
import ManualRuleCreationView from './rule/manual-rule-creation-view';
import Template from './templates/coding-rules-filters.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'click .js-create-manual-rule': 'createManualRule'
  },

  createManualRule: function () {
    new ManualRuleCreationView({
      app: this.options.app
    }).render();
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canWrite: this.options.app.canWrite
    });
  }
});
