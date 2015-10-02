import _ from 'underscore';
import Marionette from 'backbone.marionette';
import BaseView from './base-viewer-view';
import Template from '../templates/workspace-rule.hbs';

export default BaseView.extend({
  template: Template,

  onRender: function () {
    BaseView.prototype.onRender.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
  },

  onDestroy: function () {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  serializeData: function () {
    return _.extend(Marionette.LayoutView.prototype.serializeData.apply(this, arguments), {
      allTags: _.union(this.model.get('sysTags'), this.model.get('tags'))
    });
  }
});


