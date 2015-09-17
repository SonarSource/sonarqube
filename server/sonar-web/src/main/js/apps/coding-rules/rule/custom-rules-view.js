import _ from 'underscore';
import Marionette from 'backbone.marionette';
import CustomRuleView from './custom-rule-view';
import CustomRuleCreationView from './custom-rule-creation-view';
import '../templates';

export default Marionette.CompositeView.extend({
  template: Templates['coding-rules-custom-rules'],
  childView: CustomRuleView,
  childViewContainer: '#coding-rules-detail-custom-rules',

  childViewOptions: function () {
    return {
      app: this.options.app,
      templateRule: this.model
    };
  },

  modelEvents: {
    'change': 'render'
  },

  events: {
    'click .js-create-custom-rule': 'createCustomRule'
  },

  onRender: function () {
    this.$el.toggleClass('hidden', !this.model.get('isTemplate'));
  },

  createCustomRule: function () {
    new CustomRuleCreationView({
      app: this.options.app,
      templateRule: this.model
    }).render();
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canWrite: this.options.app.canWrite
    });
  }
});


