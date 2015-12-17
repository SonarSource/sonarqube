import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from './templates/update-center-search.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'change [name="update-center-filter"]': 'onFilterChange',

    'submit #update-center-search-form': 'onFormSubmit',
    'search #update-center-search-query': 'onKeyUp',
    'keyup #update-center-search-query': 'onKeyUp',
    'change #update-center-search-query': 'onKeyUp'
  },

  collectionEvents: {
    'filter': 'onFilter'
  },

  initialize: function () {
    this._bufferedValue = null;
    this.search = _.debounce(this.search, 50);
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
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      state: this.options.state.toJSON()
    });
  }
});


