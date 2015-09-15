define([
  './models/changelog',
  './views/changelog-view',
  './collections/action-plans',
  './views/issue-popup',
  './views/transitions-form-view',
  './views/assign-form-view',
  './views/comment-form-view',
  './views/plan-form-view',
  './views/set-severity-form-view',
  './views/more-actions-view',
  './views/tags-form-view',
  'components/workspace/main',
  './templates'
], function (ChangeLog, ChangeLogView, ActionPlans, IssuePopup, TransitionsFormView, AssignFormView, CommentFormView,
             PlanFormView, SetSeverityFormView, MoreActionsView, TagsFormView, Workspace) {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    className: 'issue',
    template: Templates.issue,

    modelEvents: {
      'change': 'render',
      'transition': 'onTransition'
    },

    events: function () {
      return {
        'click .js-issue-comment': 'onComment',
        'click .js-issue-comment-edit': 'editComment',
        'click .js-issue-comment-delete': 'deleteComment',
        'click .js-issue-transition': 'transition',
        'click .js-issue-set-severity': 'setSeverity',
        'click .js-issue-assign': 'assign',
        'click .js-issue-assign-to-me': 'assignToMe',
        'click .js-issue-plan': 'plan',
        'click .js-issue-show-changelog': 'showChangeLog',
        'click .js-issue-rule': 'showRule',
        'click .js-issue-edit-tags': 'editTags',
        'click .js-issue-locations': 'showLocations'
      };
    },

    onRender: function () {
      this.$el.attr('data-key', this.model.get('key'));
    },

    disableControls: function () {
      this.$(':input').prop('disabled', true);
    },

    enableControls: function () {
      this.$(':input').prop('disabled', false);
    },

    resetIssue: function (options) {
      var that = this;
      var key = this.model.get('key'),
          componentUuid = this.model.get('componentUuid');
      this.model.reset({ key: key, componentUuid: componentUuid }, { silent: true });
      return this.model.fetch(options).done(function () {
        return that.trigger('reset');
      });
    },

    showChangeLog: function (e) {
      var that = this;
      var t = $(e.currentTarget),
          changeLog = new ChangeLog();
      return changeLog.fetch({
        data: { issue: this.model.get('key') }
      }).done(function () {
        if (that.popup) {
          that.popup.destroy();
        }
        that.popup = new ChangeLogView({
          triggerEl: t,
          bottomRight: true,
          collection: changeLog,
          issue: that.model
        });
        that.popup.render();
      });
    },

    updateAfterAction: function (fetch) {
      if (this.popup) {
        this.popup.destroy();
      }
      if (fetch) {
        this.resetIssue();
      }
    },

    onComment: function (e) {
      e.stopPropagation();
      this.comment();
    },

    comment: function (options) {
      $('body').click();
      this.popup = new CommentFormView({
        triggerEl: this.$('.js-issue-comment'),
        bottom: true,
        issue: this.model,
        detailView: this,
        additionalOptions: options
      });
      this.popup.render();
    },

    editComment: function (e) {
      e.stopPropagation();
      $('body').click();
      var commentEl = $(e.currentTarget).closest('.issue-comment'),
          commentKey = commentEl.data('comment-key'),
          comment = _.findWhere(this.model.get('comments'), { key: commentKey });
      this.popup = new CommentFormView({
        triggerEl: $(e.currentTarget),
        bottomRight: true,
        model: new Backbone.Model(comment),
        issue: this.model,
        detailView: this
      });
      this.popup.render();
    },

    deleteComment: function (e) {
      var that = this;
      var commentKey = $(e.target).closest('[data-comment-key]').data('comment-key'),
          confirmMsg = $(e.target).data('confirm-msg');
      if (confirm(confirmMsg)) {
        this.disableControls();
        return $.ajax({
          type: 'POST',
          url: baseUrl + '/api/issues/delete_comment?key=' + commentKey
        }).done(function () {
          that.updateAfterAction(true);
        });
      }
    },

    transition: function (e) {
      e.stopPropagation();
      $('body').click();
      this.popup = new TransitionsFormView({
        triggerEl: $(e.currentTarget),
        bottom: true,
        model: this.model,
        view: this
      });
      this.popup.render();
    },

    setSeverity: function (e) {
      e.stopPropagation();
      $('body').click();
      this.popup = new SetSeverityFormView({
        triggerEl: $(e.currentTarget),
        bottom: true,
        model: this.model
      });
      this.popup.render();
    },

    assign: function (e) {
      e.stopPropagation();
      $('body').click();
      this.popup = new AssignFormView({
        triggerEl: $(e.currentTarget),
        bottom: true,
        model: this.model
      });
      this.popup.render();
    },

    assignToMe: function () {
      var view = new AssignFormView({
        model: this.model,
        triggerEl: $('body')
      });
      view.submit(window.SS.user, window.SS.userName);
      view.destroy();
    },

    plan: function (e) {
      var that = this;
      var t = $(e.currentTarget),
          actionPlans = new ActionPlans();
      return actionPlans.fetch({
        reset: true,
        data: { project: this.model.get('project') }
      }).done(function () {
        e.stopPropagation();
        $('body').click();
        that.popup = new PlanFormView({
          triggerEl: t,
          bottom: true,
          model: that.model,
          collection: actionPlans
        });
        that.popup.render();
      });
    },

    showRule: function () {
      if (Workspace == null) {
        Workspace = require('components/workspace/main');
      }
      var ruleKey = this.model.get('rule');
      Workspace.openRule({ key: ruleKey });
    },

    editTags: function (e) {
      e.stopPropagation();
      $('body').click();
      this.popup = new TagsFormView({
        triggerEl: $(e.currentTarget),
        bottomRight: true,
        model: this.model
      });
      this.popup.render();
    },

    showLocations: function () {
      this.model.trigger('locations', this.model);
    },

    onTransition: function (transition) {
      if (transition === 'falsepositive' || transition === 'wontfix') {
        this.comment({ fromTransition: true });
      }
    },

    serializeData: function () {
      var issueKey = encodeURIComponent(this.model.get('key'));
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        permalink: baseUrl + '/issues/search#issues=' + issueKey,
        hasSecondaryLocations: this.model.get('flows').length
      });
    }
  });

});
