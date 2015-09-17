import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['api-documentation-search'],

  ui: {
    input: '.search-box-input'
  },

  events: {
    'keyup @ui.input': 'onChange',
    'search @ui.input': 'onChange'
  },

  initialize: function () {
    this.query = '';
    this.debouncedFilter = _.debounce(this.filter, 250);
  },

  onChange: function () {
    var query = this.ui.input.val();
    if (query === this.query) {
      return;
    }
    this.query = this.ui.input.val();
    this.debouncedFilter(query);
  },

  filter: function (query) {
    this.options.state.set({ query: query });
  }
});


