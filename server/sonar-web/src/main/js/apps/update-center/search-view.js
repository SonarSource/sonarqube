import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['update-center-search'],

  events: {
    'change [name="update-center-filter"]': 'onFilterChange',

    'submit #update-center-search-form': 'onFormSubmit',
    'search #update-center-search-query': 'debouncedOnKeyUp',
    'keyup #update-center-search-query': 'debouncedOnKeyUp',
    'change #update-center-search-query': 'debouncedOnKeyUp'
  },

  collectionEvents: {
    'filter': 'onFilter'
  },

  initialize: function () {
    this._bufferedValue = null;
    this.debouncedOnKeyUp = _.debounce(this.onKeyUp, 50);
    this.listenTo(this.options.state, 'change', this.render);
  },

  onRender: function () {
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy: function () {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFilterChange: function () {
    var value = this.$('[name="update-center-filter"]:checked').val();
    this.filter(value);
  },

  filter: function (value) {
    this.options.router.navigate(value, { trigger: true });
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
    this.search(q);
  },

  getQuery: function () {
    return this.$('#update-center-search-query').val();
  },

  search: function (q) {
    this.collection.search(q);
  },

  focusSearch: function () {
    var that = this;
    setTimeout(function () {
      that.$('#update-center-search-query').focus();
    }, 0);
  },

  onFilter: function (model) {
    var q = model.get('category');
    this.$('#update-center-search-query').val(q);
    this.search(q);
  },

  serializeData: function () {
    return _.extend(this._super(), { state: this.options.state.toJSON() });
  }
});


