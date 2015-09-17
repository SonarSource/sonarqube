import Marionette from 'backbone.marionette';
import ItemView from './gate-view';
import './templates';

export default Marionette.CompositeView.extend({
  className: 'list-group',
  template: Templates['quality-gates-gates'],
  childView: ItemView,
  childViewContainer: '.js-list',

  childViewOptions: function (model) {
    return {
      collectionView: this,
      highlighted: model.id === this.highlighted
    };
  },

  highlight: function (id) {
    this.highlighted = id;
    this.render();
  }
});


