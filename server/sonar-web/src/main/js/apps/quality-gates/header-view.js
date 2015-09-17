import _ from 'underscore';
import Marionette from 'backbone.marionette';
import RenameView from './rename-view';
import CopyView from './copy-view';
import DeleteView from './delete-view';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['quality-gate-detail-header'],

  modelEvents: {
    'change': 'render'
  },

  events: {
    'click #quality-gate-rename': 'renameQualityGate',
    'click #quality-gate-copy': 'copyQualityGate',
    'click #quality-gate-delete': 'deleteQualityGate',
    'click #quality-gate-toggle-default': 'toggleDefault'
  },

  renameQualityGate: function () {
    new RenameView({
      model: this.model
    }).render();
  },

  copyQualityGate: function () {
    new CopyView({
      model: this.model,
      collection: this.model.collection
    }).render();
  },

  deleteQualityGate: function () {
    new DeleteView({
      model: this.model
    }).render();
  },

  toggleDefault: function () {
    this.model.toggleDefault();
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canEdit: this.options.canEdit
    });
  }
});


