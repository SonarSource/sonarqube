import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from './templates/api-documentation-web-service.hbs';

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

  initialize: function () {
    this.listenTo(this.options.state, 'change:query', this.toggleHidden);
    this.listenTo(this.options.state, 'change:internal', this.toggleHidden);
  },

  shouldBeHidden: function () {
    var that = this;
    var match = this.options.state.match(this.model.get('path')) ||
        _.some(this.model.get('actions'), function (action) {
          var test = action.path + '/' + action.key;
          return that.options.state.match(test, action.internal);
        });

    var showInternal = this.options.state.get('internal'),
        hideMe = this.model.get('internal') && !showInternal;
    return !match || hideMe;
  },

  onRender: function () {
    this.$el.attr('data-path', this.model.get('path'));
    this.$el.toggleClass('active', this.options.highlighted);
    this.toggleHidden();
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'right' });
  },

  onClick: function (e) {
    e.preventDefault();
    this.model.trigger('select', this.model);
  },

  toggleHidden: function () {
    this.$el.toggleClass('hidden', this.shouldBeHidden());
  }
});
