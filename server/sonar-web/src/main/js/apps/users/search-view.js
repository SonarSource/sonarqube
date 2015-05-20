define([
  './templates'
], function () {

  return Marionette.ItemView.extend({
    template: Templates['users-search'],

    events: {
      'submit #users-search-form': 'onFormSubmit',
      'click #users-search-cancel': 'onCancelClick'
    },

    onRender: function () {
      this.delegateEvents();
    },

    onFormSubmit: function (e) {
      e.preventDefault();
      var q = this.$('#users-search-query').val();
      this.filterList(q);
    },

    onCancelClick: function (e) {
      e.preventDefault();
      this.cancelSearch();
    },

    filterList: function (q) {
      this.collection.fetch({ data: { q: q } });
    },

    cancelSearch: function () {
      this.$('#users-search-query').val('');
      this.collection.fetch();
    }
  });

});
