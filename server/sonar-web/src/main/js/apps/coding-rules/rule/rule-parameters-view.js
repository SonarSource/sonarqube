import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from '../templates/rule/coding-rules-rule-parameters.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  modelEvents: {
    'change': 'render'
  },

  onRender: function () {
    this.$el.toggleClass('hidden', _.isEmpty(this.model.get('params')));
  },

  serializeData: function () {
    var isEditable = this.options.app.canWrite && (this.model.get('isManual') || this.model.get('isCustom'));

    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      isEditable: isEditable,
      canWrite: this.options.app.canWrite
    });
  }
});


