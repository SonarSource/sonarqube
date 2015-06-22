define([
  './templates'
], function () {

  return Marionette.ItemView.extend({
    template: Templates['update-center-search'],

    events: {
      'change [name="update-center-filter"]': 'onFilterChange',

      'submit #update-center-search-form': 'onFormSubmit',
      'search #update-center-search-query': 'debouncedOnKeyUp',
      'keyup #update-center-search-query': 'debouncedOnKeyUp'
    },

    initialize: function () {
      this._bufferedValue = null;
      this.debouncedOnKeyUp = _.debounce(this.onKeyUp, 50);
      this.listenTo(this.options.state, 'change', this.render);
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

    serializeData: function () {
      return _.extend(this._super(), { state: this.options.state.toJSON() });
    }
  });

});
