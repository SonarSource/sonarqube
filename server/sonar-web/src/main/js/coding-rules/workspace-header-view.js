define([
  'components/navigator/workspace-header-view',
  'coding-rules/bulk-change-popup-view',
  'coding-rules/rule/manual-rule-creation-view',
  'templates/coding-rules'
], function (WorkspaceHeaderView, BulkChangePopup, ManualRuleCreationView) {

  var $ = jQuery;

  return WorkspaceHeaderView.extend({
    template: Templates['coding-rules-workspace-header'],

    events: function () {
      return _.extend(WorkspaceHeaderView.prototype.events.apply(this, arguments), {
        'click .js-back': 'onBackClick',
        'click .js-bulk-change': 'onBulkChangeClick',
        'click .js-create-manual-rule': 'createManualRule'
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

    createManualRule: function() {
      new ManualRuleCreationView({
        app: this.options.app
      }).render();
    },

    serializeData: function () {
      return _.extend(WorkspaceHeaderView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite
      });
    }
  });

});
