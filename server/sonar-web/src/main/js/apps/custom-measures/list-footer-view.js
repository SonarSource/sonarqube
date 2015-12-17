import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from './templates/custom-measures-list-footer.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  collectionEvents: {
    'all': 'render'
  },

  events: {
    'click #custom-measures-fetch-more': 'onMoreClick'
  },

  onMoreClick: function (e) {
    e.preventDefault();
    this.fetchMore();
  },

  fetchMore: function () {
    this.collection.fetchMore();
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      total: this.collection.total,
      count: this.collection.length,
      more: this.collection.hasMore()
    });
  }
});


