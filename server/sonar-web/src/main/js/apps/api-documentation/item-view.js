import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  tagName: 'a',
  className: 'list-group-item',
  template: Templates['api-documentation-web-service'],

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
          return that.options.state.match(test);
        });

    var showInternal = this.options.state.get('internal'),
        hideMe = this.model.get('internal') && !showInternal;
    return !match || hideMe;
  },

  onRender: function () {
    this.$el.attr('data-path', this.model.get('path'));
    this.$el.toggleClass('active', this.options.highlighted);
    this.toggleHidden();
  },

  onClick: function (e) {
    e.preventDefault();
    this.model.trigger('select', this.model);
  },

  toggleHidden: function () {
    this.$el.toggleClass('hidden', this.shouldBeHidden());
  }
});


