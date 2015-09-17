import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['users-search'],

  events: {
    'submit #users-search-form': 'onFormSubmit',
    'search #users-search-query': 'debouncedOnKeyUp',
    'keyup #users-search-query': 'debouncedOnKeyUp'
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

  getQuery: function () {
    return this.$('#users-search-query').val();
  },

  search: function (q) {
    return this.collection.fetch({ reset: true, data: { q: q } });
  }
});


