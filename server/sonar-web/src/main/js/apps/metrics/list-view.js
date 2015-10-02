import Marionette from 'backbone.marionette';
import ListItemView from './list-item-view';

export default Marionette.CollectionView.extend({
  tagName: 'ul',
  childView: ListItemView,

  childViewOptions: function () {
    return {
      types: this.options.types,
      domains: this.options.domains
    };
  }
});


