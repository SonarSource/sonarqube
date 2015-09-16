define([
  'components/navigator/workspace-list-view',
  './workspace-list-item-view',
  './workspace-list-empty-view',
  './templates'
], function (WorkspaceListView, WorkspaceListItemView, WorkspaceListEmptyView) {

  return WorkspaceListView.extend({
    template: Templates['coding-rules-workspace-list'],
    childView: WorkspaceListItemView,
    childViewContainer: '.js-list',
    emptyView: WorkspaceListEmptyView,

    bindShortcuts: function () {
      WorkspaceListView.prototype.bindShortcuts.apply(this, arguments);
      var that = this;
      key('right', 'list', function () {
        that.options.app.controller.showDetailsForSelected();
        return false;
      });
      key('a', function () {
        that.options.app.controller.activateCurrent();
        return false;
      });
      key('d', function () {
        that.options.app.controller.deactivateCurrent();
        return false;
      });
    }
  });

});
