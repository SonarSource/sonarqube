import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  tagName: 'a',
  className: 'list-group-item',
  template: Templates['quality-profiles-profile'],

  modelEvents: {
    'change': 'render'
  },

  events: {
    'click': 'onClick'
  },

  onRender: function () {
    this.$el.toggleClass('active', this.options.highlighted);
    this.$el.attr('data-key', this.model.id);
    this.$el.attr('data-language', this.model.get('language'));
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
  },

  onDestroy: function () {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onClick: function (e) {
    e.preventDefault();
    this.model.trigger('select', this.model);
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      projectCountFormatted: window.formatMeasure(this.model.get('projectCount'), 'INT')
    });
  }
});


