define([
  'components/navigator/workspace-header-view',
  'templates/coding-rules',
  'coding-rules/bulk-change-popup-view'
], function (WorkspaceHeaderView, Templates, BulkChangePopup) {

  var $ = jQuery;

  return WorkspaceHeaderView.extend({
    template: Templates['coding-rules-workspace-header'],

    events: function () {
      return _.extend(WorkspaceHeaderView.prototype.events.apply(this, arguments), {
        'click .js-back': 'onBackClick',
        'click .js-bulk-change': 'onBulkChangeClick'
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

    serializeData: function () {
      return _.extend(WorkspaceHeaderView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite
      });
    }
  });

});
