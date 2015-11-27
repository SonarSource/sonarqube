import Marionette from 'backbone.marionette';

import ListItemView from './list-item-view';
import Template from './templates/users-list.hbs';


export default Marionette.CompositeView.extend({
  template: Template,
  childView: ListItemView,
  childViewContainer: 'tbody',

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


