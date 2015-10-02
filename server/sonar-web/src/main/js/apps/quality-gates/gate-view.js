import Marionette from 'backbone.marionette';
import Template from './templates/quality-gates-gate.hbs';

export default Marionette.ItemView.extend({
  tagName: 'a',
  className: 'list-group-item',
  template: Template,

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


