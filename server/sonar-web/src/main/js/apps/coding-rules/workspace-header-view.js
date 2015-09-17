import $ from 'jquery';
import _ from 'underscore';
import WorkspaceHeaderView from 'components/navigator/workspace-header-view';
import BulkChangePopup from './bulk-change-popup-view';
import './templates';

export default WorkspaceHeaderView.extend({
  template: Templates['coding-rules-workspace-header'],

  events: function () {
    return _.extend(WorkspaceHeaderView.prototype.events.apply(this, arguments), {
      'click .js-back': 'onBackClick',
      'click .js-bulk-change': 'onBulkChangeClick',
      'click .js-reload': 'reload',
      'click .js-new-search': 'newSearch'
    });
  },

  onBackClick: function () {
    this.options.app.controller.hideDetails();
  },

  onBulkChangeClick: function (e) {
    e.stopPropagation();
    $('body').click();
    new BulkChangePopup({
      app: this.options.app,
      triggerEl: $(e.currentTarget),
      bottomRight: true
    }).render();
  },

  reload: function () {
    this.options.app.controller.fetchList(true);
  },

  newSearch: function () {
    this.options.app.controller.newSearch();
  },

  serializeData: function () {
    return _.extend(WorkspaceHeaderView.prototype.serializeData.apply(this, arguments), {
      canWrite: this.options.app.canWrite
    });
  }
});


