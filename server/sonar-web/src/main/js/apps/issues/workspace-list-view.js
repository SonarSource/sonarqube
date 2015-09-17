import $ from 'jquery';
import WorkspaceListView from 'components/navigator/workspace-list-view';
import IssueView from './workspace-list-item-view';
import EmptyView from './workspace-list-empty-view';
import './templates';

var COMPONENT_HEIGHT = 29,
    BOTTOM_OFFSET = 10;

export default WorkspaceListView.extend({
  template: Templates['issues-workspace-list'],
  componentTemplate: Templates['issues-workspace-list-component'],
  childView: IssueView,
  childViewContainer: '.js-list',
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
    key('space', 'list', function () {
      var selectedIssue = that.collection.at(that.options.app.state.get('selectedIndex'));
      selectedIssue.set({ selected: !selectedIssue.get('selected') });
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

  attachHtml: function (compositeView, childView, index) {
    var $container = this.getChildViewContainer(compositeView),
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
    $container.append(childView.el);
  },

  destroyChildren: function () {
    WorkspaceListView.prototype.destroyChildren.apply(this, arguments);
    this.$('.issues-workspace-list-component').remove();
  }
});


