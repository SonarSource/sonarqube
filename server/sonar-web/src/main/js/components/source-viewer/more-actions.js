import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Workspace from '../workspace/main';
import Template from './templates/source-viewer-more-actions.hbs';

export default Marionette.ItemView.extend({
  className: 'source-viewer-header-more-actions',
  template: Template,

  events: {
    'click .js-measures': 'showMeasures',
    'click .js-new-window': 'openNewWindow',
    'click .js-workspace': 'openInWorkspace',
    'click .js-raw-source': 'showRawSource'
  },

  onRender: function () {
    var that = this;
    $('body').on('click.component-viewer-more-actions', function () {
      $('body').off('click.component-viewer-more-actions');
      that.destroy();
    });
  },

  showMeasures: function () {
    this.options.parent.showMeasures();
  },

  openNewWindow: function () {
    this.options.parent.getPermalink();
  },

  openInWorkspace: function () {
    var uuid = this.options.parent.model.id;
    Workspace.openComponent({ uuid: uuid });
  },

  showRawSource: function () {
    this.options.parent.showRawSources();
  },

  serializeData: function () {
    var options = this.options.parent.options.viewer.options;
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      options: options
    });
  }
});


