import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  tagName: 'a',
  className: 'list-group-item',
  template: Templates['quality-gates-gate'],

  modelEvents: {
    'change': 'render'
  },

  events: {
    'click': 'onClick'
  },

  onRender: function () {
    this.$el.toggleClass('active', this.options.highlighted);
    this.$el.attr('data-id', this.model.id);
  },

  onClick: function (e) {
    e.preventDefault();
    this.model.trigger('select', this.model);
  }
});


