import $ from 'jquery';
import _ from 'underscore';
import WorkspaceHeaderView from 'components/navigator/workspace-header-view';
import './templates';

export default WorkspaceHeaderView.extend({
  template: Templates['issues-workspace-header'],

  events: function () {
    return _.extend(WorkspaceHeaderView.prototype.events.apply(this, arguments), {
      'click .js-selection': 'onSelectionClick',
      'click .js-back': 'returnToList',
      'click .js-new-search': 'newSearch',
      'click .js-bulk-change-selected': 'onBulkChangeSelectedClick'
    });
  },

  initialize: function () {
    WorkspaceHeaderView.prototype.initialize.apply(this, arguments);
    this._onBulkIssues = window.onBulkIssues;
    window.onBulkIssues = _.bind(this.afterBulkChange, this);
  },

  onDestroy: function () {
    this._super();
    window.onBulkIssues = this._onBulkIssues;
  },

  onSelectionClick: function (e) {
    e.preventDefault();
    this.toggleSelection();
  },

  onBulkChangeSelectedClick: function (e) {
    e.preventDefault();
    this.bulkChangeSelected();
  },

  afterBulkChange: function () {
    var that = this;
    $('#modal').dialog('close');
    var selectedIndex = this.options.app.state.get('selectedIndex');
    var selectedKeys = _.pluck(this.options.app.list.where({ selected: true }), 'id');
    this.options.app.controller.fetchList().done(function () {
      that.options.app.state.set({ selectedIndex: selectedIndex });
      that.options.app.list.selectByKeys(selectedKeys);
    });
  },

  render: function () {
    if (!this._suppressUpdate) {
      this._super();
    }
  },

  toggleSelection: function () {
    this._suppressUpdate = true;
    var selectedCount = this.options.app.list.where({ selected: true }).length,
        someSelected = selectedCount > 0;
    return someSelected ? this.selectNone() : this.selectAll();
  },

  selectNone: function () {
    this.options.app.list.where({ selected: true }).forEach(function (issue) {
      issue.set({ selected: false });
    });
    this._suppressUpdate = false;
    this.render();
  },

  selectAll: function () {
    this.options.app.list.forEach(function (issue) {
      issue.set({ selected: true });
    });
    this._suppressUpdate = false;
    this.render();
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
  },

  bulkChangeSelected: function () {
    var selected = this.options.app.list.where({ selected: true }),
        selectedKeys = _.first(_.pluck(selected, 'id'), 200),
        query = 'issues=' + selectedKeys.join(),
        url = baseUrl + '/issues/bulk_change_form?' + query;
    window.openModalWindow(url, {});
  },

  serializeData: function () {
    var issuesCount = this.options.app.list.length,
        selectedCount = this.options.app.list.where({ selected: true }).length,
        allSelected = issuesCount > 0 && issuesCount === selectedCount,
        someSelected = !allSelected && selectedCount > 0;
    return _.extend(this._super(), {
      selectedCount: selectedCount,
      allSelected: allSelected,
      someSelected: someSelected
    });
  }
});


