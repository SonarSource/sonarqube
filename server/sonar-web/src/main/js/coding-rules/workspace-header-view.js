define([
  'components/navigator/workspace-header-view',
  'templates/coding-rules'
], function (WorkspaceHeaderView, Templates) {

  return WorkspaceHeaderView.extend({
    template: Templates['coding-rules-workspace-header'],

    events: function () {
      return _.extend(WorkspaceHeaderView.prototype.events.apply(this, arguments), {
        'click .js-back': 'onBackClick'
      });
    },

    onBackClick: function () {
      this.options.app.controller.hideDetails();
    }
  });

});
