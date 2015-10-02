import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from './templates/quality-gate-detail-conditions-empty.hbs';

export default Marionette.ItemView.extend({
  tagName: 'tr',
  template: Template,

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canEdit: this.options.canEdit
    });
  }
});


