import Marionette from 'backbone.marionette';
import ListItemView from './list-item-view';

export default Marionette.CollectionView.extend({
  tagName: 'ul',
  childView: ListItemView,

  collectionEvents: {
    'request': 'showLoading',
    'sync': 'hideLoading'
  },

  showLoading: function () {
    this.$el.addClass('new-loading');
  },

  hideLoading: function () {
    this.$el.removeClass('new-loading');
  }
});


