import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['provisioning-search'],

  collectionEvents: {
    'change:selected': 'onSelectedChange',
    'reset': 'onSelectedChange'
  },

  events: {
    'click .js-toggle-selection': 'onToggleSelectionClick',
    'submit #provisioning-search-form': 'onFormSubmit',
    'search #provisioning-search-query': 'debouncedOnKeyUp',
    'keyup #provisioning-search-query': 'debouncedOnKeyUp'
  },

  initialize: function () {
    this._bufferedValue = null;
    this.debouncedOnKeyUp = _.debounce(this.onKeyUp, 400);
  },

  onRender: function () {
    this.delegateEvents();
  },

  onFormSubmit: function (e) {
    e.preventDefault();
    this.debouncedOnKeyUp();
  },

  onKeyUp: function () {
    var q = this.getQuery();
    if (q === this._bufferedValue) {
      return;
    }
    this._bufferedValue = this.getQuery();
    if (this.searchRequest != null) {
      this.searchRequest.abort();
    }
    this.searchRequest = this.search(q);
  },

  onSelectedChange: function () {
    var projectsCount = this.collection.length,
        selectedCount = this.collection.where({ selected: true }).length,
        allSelected = projectsCount > 0 && projectsCount === selectedCount,
        someSelected = !allSelected && selectedCount > 0;
    this.$('.js-toggle-selection')
        .toggleClass('icon-checkbox-checked', allSelected || someSelected)
        .toggleClass('icon-checkbox-single', someSelected);
  },

  onToggleSelectionClick: function (e) {
    e.preventDefault();
    this.toggleSelection();
  },

  toggleSelection: function () {
    var selectedCount = this.collection.where({ selected: true }).length,
        someSelected = selectedCount > 0;
    return someSelected ? this.selectNone() : this.selectAll();
  },

  selectNone: function () {
    this.collection.where({ selected: true }).forEach(function (project) {
      project.set({ selected: false });
    });
  },

  selectAll: function () {
    this.collection.forEach(function (project) {
      project.set({ selected: true });
    });
  },

  getQuery: function () {
    return this.$('#provisioning-search-query').val();
  },

  search: function (q) {
    this.selectNone();
    return this.collection.fetch({ reset: true, data: { q: q } });
  },

  serializeData: function () {
    var projectsCount = this.collection.length,
        selectedCount = this.collection.where({ selected: true }).length,
        allSelected = projectsCount > 0 && projectsCount === selectedCount,
        someSelected = !allSelected && selectedCount > 0;
    return _.extend(this._super(), {
      selectedCount: selectedCount,
      allSelected: allSelected,
      someSelected: someSelected
    });
  }
});


