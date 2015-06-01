define([
  'components/navigator/workspace-header-view',
  './templates'
], function (WorkspaceHeaderView) {

  var $ = jQuery;

  return WorkspaceHeaderView.extend({
    template: Templates['issues-workspace-header'],

    events: function () {
      return _.extend(WorkspaceHeaderView.prototype.events.apply(this, arguments), {
        'click .js-back': 'returnToList',
        'click .js-new-search': 'newSearch'
      });
    },

    initialize: function () {
      var that = this;
      WorkspaceHeaderView.prototype.initialize.apply(this, arguments);
      this._onBulkIssues = window.onBulkIssues;
      window.onBulkIssues = function () {
        $('#modal').dialog('close');
        return that.options.app.controller.fetchList();
      };
    },

    onClose: function () {
      window.onBulkIssues = this._onBulkIssues;
    },

    returnToList: function () {
      this.options.app.controller.closeComponentViewer();
    },

    newSearch: function () {
      this.options.app.controller.newSearch();
    },

    bulkChange: function () {
      var query = this.options.app.controller.getQuery('&', true),
          url = baseUrl + '/issues/bulk_change_form?' + query;
      window.openModalWindow(url, {});
    }
  });

});
