import Marionette from 'backbone.marionette';
import ItemView from './item-view';

export default Marionette.CollectionView.extend({
  className: 'list-group',
  childView: ItemView,

  childViewOptions: function (model) {
    return {
      collectionView: this,
      highlighted: model.get('path') === this.highlighted,
      state: this.options.state
    };
  },

  highlight: function (path) {
    this.highlighted = path;
    this.render();
  }
});


