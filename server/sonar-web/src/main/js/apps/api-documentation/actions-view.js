import $ from 'jquery';
import Marionette from 'backbone.marionette';
import ActionView from './action-view';

export default Marionette.CollectionView.extend({
  childView: ActionView,

  childViewOptions: function () {
    return {
      state: this.options.state
    };
  },

  scrollToTop: function () {
    var parent = this.$el.scrollParent();
    if (parent.is(document)) {
      parent = $(window);
    }
    parent.scrollTop(0);
  },

  scrollToAction: function (action) {
    var model = this.collection.findWhere({ key: action });
    if (model != null) {
      var view = this.children.findByModel(model);
      if (view != null) {
        this.scrollToView(view);
      }
    }
  },

  scrollToView: function (view) {
    var el = view.$el,
        parent = el.scrollParent();
    var elOffset = el.offset(),
        parentOffset = parent.offset();
    if (parent.is(document)) {
      parentOffset = { top: 0 };
    }
    if (elOffset != null && parentOffset != null) {
      var scrollTop = elOffset.top - parentOffset.top - 70;
      parent.scrollTop(scrollTop);
    }
  }
});
