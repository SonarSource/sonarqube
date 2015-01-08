define([
  'components/navigator/workspace-list-item-view',
  'templates/coding-rules'
], function (WorkspaceListItemView) {

  return WorkspaceListItemView.extend({
    className: 'coding-rule',
    template: Templates['coding-rules-workspace-list-item'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click': 'selectCurrent',
      'click .js-rule': 'openRule'
    },

    selectCurrent: function () {
      this.options.app.state.set({ selectedIndex: this.model.get('index') });
    },

    openRule: function () {
      this.options.app.controller.showDetails(this.model);
    },

    serializeData: function () {
      return _.extend(WorkspaceListItemView.prototype.serializeData.apply(this, arguments), {
        tags: _.union(this.model.get('sysTags'), this.model.get('tags'))
      });
    }
  });

});
