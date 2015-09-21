define(function () {

  var $ = jQuery,
      BOTTOM_OFFSET = 60;

  return Marionette.CompositeView.extend({

    ui: {
      loadMore: '.js-more',
      lastElementReached: '.js-last-element-reached'
    },

    childViewOptions: function () {
      return {
        app: this.options.app
      };
    },

    collectionEvents: {
      'reset': 'scrollToTop'
    },

    initialize: function (options) {
      this.loadMoreThrottled = _.throttle(this.loadMore, 1000, {trailing: false});
      this.listenTo(options.app.state, 'change:maxResultsReached', this.toggleLoadMore);
      this.listenTo(options.app.state, 'change:selectedIndex', this.scrollTo);
      this.bindShortcuts();
    },

    onDestroy: function () {
      this.unbindScrollEvents();
      this.unbindShortcuts();
    },

    onRender: function () {
      this.toggleLoadMore();
    },

    toggleLoadMore: function () {
      var maxResultsReached = this.options.app.state.get('maxResultsReached');
      this.ui.loadMore.toggle(!maxResultsReached);
      this.ui.lastElementReached.toggle(maxResultsReached);
    },

    bindScrollEvents: function () {
      var that = this;
      $(window).on('scroll.workspace-list-view', function () {
        that.onScroll();
      });
    },

    unbindScrollEvents: function () {
      $(window).off('scroll.workspace-list-view');
    },

    bindShortcuts: function () {
      var that = this;
      key('up', 'list', function () {
        that.options.app.controller.selectPrev();
        return false;
      });

      key('down', 'list', function () {
        that.options.app.controller.selectNext();
        return false;
      });
    },

    loadMore: function () {
      if (!this.options.app.state.get('maxResultsReached')) {
        var that = this;
        this.unbindScrollEvents();
        this.options.app.controller.fetchNextPage().done(function () {
          that.bindScrollEvents();
        });
      }
    },

    onScroll: function () {
      if ($(window).scrollTop() + $(window).height() >= this.ui.loadMore.offset().top) {
        this.loadMoreThrottled();
      }
    },

    scrollToTop: function () {
      this.$el.scrollParent().scrollTop(0);
    },

    scrollTo: function () {
      var selected = this.collection.at(this.options.app.state.get('selectedIndex'));
      if (selected == null) {
        return;
      }
      var selectedView = this.children.findByModel(selected),
          parentTopOffset = this.$el.offset().top,
          viewTop = selectedView.$el.offset().top - parentTopOffset,
          viewBottom = selectedView.$el.offset().top + selectedView.$el.outerHeight() + BOTTOM_OFFSET,
          windowTop = $(window).scrollTop(),
          windowBottom = windowTop + $(window).height();
      if (viewTop < windowTop) {
        $(window).scrollTop(viewTop);
      }
      if (viewBottom > windowBottom) {
        $(window).scrollTop($(window).scrollTop() - windowBottom + viewBottom);
      }
    }

  });

});
