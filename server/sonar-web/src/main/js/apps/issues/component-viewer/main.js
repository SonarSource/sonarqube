import $ from 'jquery';
import _ from 'underscore';
import SourceViewer from 'components/source-viewer/main';
import IssueView from './issue-view';
import '../templates';

export default SourceViewer.extend({
  events: function () {
    return _.extend(SourceViewer.prototype.events.apply(this, arguments), {
      'click .js-close-component-viewer': 'closeComponentViewer',
      'click .code-issue': 'selectIssue'
    });
  },

  initialize: function (options) {
    SourceViewer.prototype.initialize.apply(this, arguments);
    return this.listenTo(options.app.state, 'change:selectedIndex', this.select);
  },

  onLoaded: function () {
    SourceViewer.prototype.onLoaded.apply(this, arguments);
    this.bindShortcuts();
    if (this.baseIssue != null) {
      this.baseIssue.trigger('locations', this.baseIssue);
      return this.scrollToLine(this.baseIssue.get('line'));
    }
  },

  bindShortcuts: function () {
    var that = this;
    var doAction = function (action) {
      var selectedIssueView = that.getSelectedIssueEl();
      if (!selectedIssueView) {
        return;
      }
      return selectedIssueView.find('.js-issue-' + action).click();
    };
    key('up', 'componentViewer', function () {
      that.options.app.controller.selectPrev();
      return false;
    });
    key('down', 'componentViewer', function () {
      that.options.app.controller.selectNext();
      return false;
    });
    key('left,backspace', 'componentViewer', function () {
      that.options.app.controller.closeComponentViewer();
      return false;
    });
    key('f', 'componentViewer', function () {
      return doAction('transition');
    });
    key('a', 'componentViewer', function () {
      return doAction('assign');
    });
    key('m', 'componentViewer', function () {
      return doAction('assign-to-me');
    });
    key('p', 'componentViewer', function () {
      return doAction('plan');
    });
    key('i', 'componentViewer', function () {
      return doAction('set-severity');
    });
    return key('c', 'componentViewer', function () {
      return doAction('comment');
    });
  },

  unbindShortcuts: function () {
    return key.deleteScope('componentViewer');
  },

  onDestroy: function () {
    SourceViewer.prototype.onDestroy.apply(this, arguments);
    this.unbindScrollEvents();
    return this.unbindShortcuts();
  },

  select: function () {
    var selected = this.options.app.state.get('selectedIndex'),
        selectedIssue = this.options.app.list.at(selected);
    if (selectedIssue.get('component') === this.model.get('key')) {
      selectedIssue.trigger('locations', selectedIssue);
      return this.scrollToIssue(selectedIssue.get('key'));
    } else {
      this.unbindShortcuts();
      return this.options.app.controller.showComponentViewer(selectedIssue);
    }
  },

  getSelectedIssueEl: function () {
    var selected = this.options.app.state.get('selectedIndex');
    if (selected == null) {
      return null;
    }
    var selectedIssue = this.options.app.list.at(selected);
    if (selectedIssue == null) {
      return null;
    }
    var selectedIssueView = this.$('#issue-' + (selectedIssue.get('key')));
    if (selectedIssueView.length > 0) {
      return selectedIssueView;
    } else {
      return null;
    }
  },

  selectIssue: function (e) {
    var key = $(e.currentTarget).data('issue-key'),
        issue = this.issues.find(function (model) {
          return model.get('key') === key;
        }),
        index = this.options.app.list.indexOf(issue);
    return this.options.app.state.set({ selectedIndex: index });
  },

  scrollToIssue: function (key) {
    var el = this.$('#issue-' + key);
    if (el.length > 0) {
      var line = el.closest('[data-line-number]').data('line-number');
      return this.scrollToLine(line);
    } else {
      this.unbindShortcuts();
      var selected = this.options.app.state.get('selectedIndex'),
          selectedIssue = this.options.app.list.at(selected);
      return this.options.app.controller.showComponentViewer(selectedIssue);
    }
  },

  openFileByIssue: function (issue) {
    this.baseIssue = issue;
    var componentKey = issue.get('component'),
        componentUuid = issue.get('componentUuid');
    return this.open(componentUuid, componentKey);
  },

  linesLimit: function () {
    var line = this.LINES_LIMIT / 2;
    if ((this.baseIssue != null) && this.baseIssue.has('line')) {
      line = Math.max(line, this.baseIssue.get('line'));
    }
    return {
      from: line - this.LINES_LIMIT / 2 + 1,
      to: line + this.LINES_LIMIT / 2
    };
  },

  limitIssues: function (issues) {
    var that = this;
    var index = this.ISSUES_LIMIT / 2;
    if ((this.baseIssue != null) && this.baseIssue.has('index')) {
      index = Math.max(index, this.baseIssue.get('index'));
    }
    return issues.filter(function (issue) {
      return Math.abs(issue.get('index') - index) <= that.ISSUES_LIMIT / 2;
    });
  },

  requestIssues: function () {
    var that = this;
    var r;
    if (this.options.app.list.last().get('component') === this.model.get('key')) {
      r = this.options.app.controller.fetchNextPage();
    } else {
      r = $.Deferred().resolve().promise();
    }
    return r.done(function () {
      that.issues.reset(that.options.app.list.filter(function (issue) {
        return issue.get('component') === that.model.key();
      }));
      that.issues.reset(that.limitIssues(that.issues));
      return that.addIssuesPerLineMeta(that.issues);
    });
  },

  renderIssues: function () {
    this.issues.forEach(this.renderIssue, this);
    return this.$('.source-line-issues').addClass('hidden');
  },

  renderIssue: function (issue) {
    var issueView = new IssueView({
      el: '#issue-' + issue.get('key'),
      model: issue,
      app: this.options.app
    });
    this.issueViews.push(issueView);
    return issueView.render();
  },

  scrollToLine: function (line) {
    var row = this.$('[data-line-number=' + line + ']'),
        goal = row.length > 0 ? row.offset().top - 200 : 0;
    return $(window).scrollTop(goal);
  },

  closeComponentViewer: function () {
    return this.options.app.controller.closeComponentViewer();
  }
});


