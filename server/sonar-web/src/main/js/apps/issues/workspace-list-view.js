define([
  'components/navigator/workspace-list-view',
  './workspace-list-item-view',
  './workspace-list-empty-view',
  './templates'
], function (WorkspaceListView, IssueView, EmptyView) {

  var $ = jQuery,
      COMPONENT_HEIGHT = 29,
      BOTTOM_OFFSET = 10;

  return WorkspaceListView.extend({
    template: Templates['issues-workspace-list'],
    componentTemplate: Templates['issues-workspace-list-component'],
    itemView: IssueView,
    itemViewContainer: '.js-list',
    emptyView: EmptyView,

    bindShortcuts: function () {
      var that = this;
      var doAction = function (action) {
        var selectedIssue = that.collection.at(that.options.app.state.get('selectedIndex'));
        if (selectedIssue == null) {
          return;
        }
        var selectedIssueView = that.children.findByModel(selectedIssue);
        selectedIssueView.$('.js-issue-' + action).click();
      };
      WorkspaceListView.prototype.bindShortcuts.apply(this, arguments);
      key('right', 'list', function () {
        var selectedIssue = that.collection.at(that.options.app.state.get('selectedIndex'));
        that.options.app.controller.showComponentViewer(selectedIssue);
        return false;
      });
      key('f', 'list', function () {
        return doAction('transition');
      });
      key('a', 'list', function () {
        return doAction('assign');
      });
      key('m', 'list', function () {
        return doAction('assign-to-me');
      });
      key('p', 'list', function () {
        return doAction('plan');
      });
      key('i', 'list', function () {
        return doAction('set-severity');
      });
      key('c', 'list', function () {
        return doAction('comment');
      });
      return key('t', 'list', function () {
        return doAction('edit-tags');
      });
    },

    scrollTo: function () {
      var selectedIssue = this.collection.at(this.options.app.state.get('selectedIndex'));
      if (selectedIssue == null) {
        return;
      }
      var selectedIssueView = this.children.findByModel(selectedIssue),
          parentTopOffset = this.$el.offset().top,
          viewTop = selectedIssueView.$el.offset().top - parentTopOffset;
      if (selectedIssueView.$el.prev().is('.issues-workspace-list-component')) {
        viewTop -= COMPONENT_HEIGHT;
      }
      var viewBottom = selectedIssueView.$el.offset().top + selectedIssueView.$el.outerHeight() + BOTTOM_OFFSET,
          windowTop = $(window).scrollTop(),
          windowBottom = windowTop + $(window).height();
      if (viewTop < windowTop) {
        $(window).scrollTop(viewTop);
      }
      if (viewBottom > windowBottom) {
        $(window).scrollTop($(window).scrollTop() - windowBottom + viewBottom);
      }
    },

    appendHtml: function (compositeView, itemView, index) {
      var $container = this.getItemViewContainer(compositeView),
          model = this.collection.at(index);
      if (model != null) {
        var prev = this.collection.at(index - 1),
            putComponent = prev == null;
        if (prev != null) {
          var fullComponent = [model.get('project'), model.get('component')].join(' '),
              fullPrevComponent = [prev.get('project'), prev.get('component')].join(' ');
          if (fullComponent !== fullPrevComponent) {
            putComponent = true;
          }
        }
        if (putComponent) {
          $container.append(this.componentTemplate(model.toJSON()));
        }
      }
      $container.append(itemView.el);
    },

    closeChildren: function () {
      WorkspaceListView.prototype.closeChildren.apply(this, arguments);
      this.$('.issues-workspace-list-component').remove();
    }
  });

});
