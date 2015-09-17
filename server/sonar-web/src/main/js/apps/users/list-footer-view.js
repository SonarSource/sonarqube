import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['users-list-footer'],

  collectionEvents: {
    'all': 'render'
  },

  events: {
    'click #users-fetch-more': 'onMoreClick'
  },

  onMoreClick: function (e) {
    e.preventDefault();
    this.fetchMore();
  },

  fetchMore: function () {
    this.collection.fetchMore();
  },

  serializeData: function () {
    return _.extend(this._super(), {
      total: this.collection.total,
      count: this.collection.length,
      more: this.collection.hasMore()
    });
  }
});


