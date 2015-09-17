import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['issues-filters'],

  events: {
    'click .js-toggle-filters': 'toggleFilters',
    'click .js-filter': 'applyFilter',
    'click .js-filter-save-as': 'saveAs',
    'click .js-filter-save': 'save',
    'click .js-filter-copy': 'copy',
    'click .js-filter-edit': 'edit'
  },

  initialize: function (options) {
    var that = this;
    this.listenTo(options.app.state, 'change:filter', this.render);
    this.listenTo(options.app.state, 'change:changed', this.render);
    this.listenTo(options.app.filters, 'reset', this.render);
    window.onSaveAs = window.onCopy = window.onEdit = function (id) {
      $('#modal').dialog('close');
      return that.options.app.controller.fetchFilters().done(function () {
        var filter = that.collection.get(id);
        return filter.fetch().done(function () {
          return that.options.app.controller.applyFilter(filter);
        });
      });
    };
  },

  onRender: function () {
    this.$el.toggleClass('search-navigator-filters-selected', this.options.app.state.has('filter'));
  },

  toggleFilters: function (e) {
    var that = this;
    e.stopPropagation();
    this.$('.search-navigator-filters-list').toggle();
    return $('body').on('click.issues-filters', function () {
      $('body').off('click.issues-filters');
      return that.$('.search-navigator-filters-list').hide();
    });
  },

  applyFilter: function (e) {
    var that = this;
    var id = $(e.currentTarget).data('id'),
        filter = this.collection.get(id);
    return that.options.app.controller.applyFilter(filter);

  },

  saveAs: function () {
    var query = this.options.app.controller.getQuery('&'),
        url = baseUrl + '/issues/save_as_form?' + query;
    window.openModalWindow(url, {});
  },

  save: function () {
    var that = this;
    var query = this.options.app.controller.getQuery('&'),
        url = baseUrl + '/issues/save/' + (this.options.app.state.get('filter').id) + '?' + query;
    return $.post(url).done(function () {
      return that.options.app.state.set({ changed: false });
    });
  },

  copy: function () {
    var url = baseUrl + '/issues/copy_form/' + (this.options.app.state.get('filter').id);
    window.openModalWindow(url, {});
  },

  edit: function () {
    var url = baseUrl + '/issues/edit_form/' + (this.options.app.state.get('filter').id);
    window.openModalWindow(url, {});
  },

  serializeData: function () {
    var filter = this.options.app.state.get('filter');
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      state: this.options.app.state.toJSON(),
      filter: filter != null ? filter.toJSON() : null,
      currentUser: window.SS.user
    });
  }
});


