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
    var elOffset = view.el.getBoundingClientRect();
    if (elOffset != null) {
      var scrollTop = elOffset.top - 70;
      window.scrollTo(0, scrollTop);
    }
  }
});
