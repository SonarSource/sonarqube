import Marionette from 'backbone.marionette';
import ItemView from './gate-view';
import Template from './templates/quality-gates-gates.hbs';

export default Marionette.CompositeView.extend({
  className: 'list-group',
  template: Template,
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


