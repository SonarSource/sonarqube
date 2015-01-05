define([
  'components/navigator/workspace-list-view',
  'templates/coding-rules',
  'coding-rules/workspace-list-item-view',
  'coding-rules/workspace-list-empty-view'
], function (WorkspaceListView, Templates, WorkspaceListItemView, WorkspaceListEmptyView) {

  var $ = jQuery;

  return WorkspaceListView.extend({
    template: Templates['coding-rules-workspace-list'],
    itemView: WorkspaceListItemView,
    itemViewContainer: '.js-list',
    emptyView: WorkspaceListEmptyView,

    events: function () {
      return {
        'click .js-tag': 'onTagClick',
        'click .js-lang': 'onLangClick'
      };
    },

    bindShortcuts: function () {
      WorkspaceListView.prototype.bindShortcuts.apply(this, arguments);
      var that = this;
      key('right', 'list', function () {
        that.options.app.controller.showDetailsForSelected();
        return false;
      });
    },

    onTagClick: function (e) {
      var tag = $(e.currentTarget).data('tag');
      this.selectTag(tag);
    },

    onLangClick: function (e) {
      var lang = $(e.currentTarget).data('lang');
      this.selectLang(lang);
    },

    selectTag: function (tag) {
      this.options.app.state.setQuery({ tags: tag });
    },

    selectLang: function (lang) {
      this.options.app.state.setQuery({ languages: lang });
    }
  });

});
