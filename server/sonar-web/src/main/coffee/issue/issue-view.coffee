define [
  'backbone.marionette'
  'templates/issue'

  'issue/models/changelog'
  'issue/views/changelog-view'

  'issue/collections/action-plans'

  'issue/views/issue-popup'

  'issue/views/assign-form-view'
  'issue/views/comment-form-view'
  'issue/views/plan-form-view'
  'issue/views/set-severity-form-view'
  'issue/views/more-actions-view'
  'issue/views/rule-overlay'

], (
  Marionette
  Templates

  ChangeLog
  ChangeLogView

  ActionPlans

  IssuePopup

  AssignFormView
  CommentFormView
  PlanFormView
  SetSeverityFormView
  MoreActionsView
  RuleOverlay

) ->

  $ = jQuery


  class extends Marionette.ItemView
    className: 'issue'
    template: Templates['issue']


    modelEvents:
      'change': 'render'


    events: ->
      'click .js-issue-comment': 'comment'
      'click .js-issue-comment-edit': 'editComment'
      'click .js-issue-comment-delete': 'deleteComment'

      'click .js-issue-transition': 'transition'
      'click .js-issue-set-severity': 'setSeverity'
      'click .js-issue-assign': 'assign'
      'click .js-issue-assign-to-me': 'assignToMe'
      'click .js-issue-plan': 'plan'
      'click .js-issue-show-changelog': 'showChangeLog'
      'click .js-issue-more': 'showMoreActions'
      'click .js-issue-rule': 'showRule'


    onRender: ->


    resetIssue: (options, p) ->
      key = @model.get 'key'
      @model.clear silent: true
      @model.set { key: key }, { silent: true }
      @model.fetch(options)
      .done =>
        @trigger 'reset'
        window.process.finishBackgroundProcess p if p?
      .fail ->
        window.process.failBackgroundProcess p if p?


    showChangeLog: (e) ->
      t = $(e.currentTarget)
      changeLog = new ChangeLog()
      changeLog.fetch
        data: issue: @model.get 'key'
      .done =>
        @popup.close() if @popup
        @popup = new ChangeLogView
          triggerEl: t
          bottomRight: true
          collection: changeLog
          issue: @model
        @popup.render()


    showActionView: (view, el, position) ->
      if el
        @popup.close() if @popup
        options =
          view: view
          triggerEl: el
        if position?
          options[position] = true
        else
          options['bottom'] = true
        @popup = new IssuePopup options
        @popup.render()


    showActionSpinner: ->
      @$('.code-issue-actions').addClass 'navigator-fetching'


    hideActionSpinner: ->
      @$('.code-issue-actions').removeClass 'navigator-fetching'


    updateAfterAction: (fetch) ->
      @popup.close() if @popup
      if fetch
        p = window.process.addBackgroundProcess()
        $.when(@resetIssue()).done =>
          window.process.finishBackgroundProcess p


    comment: (e) ->
      e.stopPropagation()
      $('body').click()
      @popup = new CommentFormView
        triggerEl: $(e.currentTarget)
        bottomRight: true
        issue: @model
        detailView: @
      @popup.render()


    editComment: (e) ->
      e.stopPropagation()
      $('body').click()
      commentEl = $(e.currentTarget).closest '.issue-comment'
      commentKey = commentEl.data 'comment-key'
      comment = _.findWhere @model.get('comments'), { key: commentKey }
      @popup = new CommentFormView
        triggerEl: $(e.currentTarget)
        bottomRight: true
        model: new Backbone.Model comment
        issue: @model
        detailView: @
      @popup.render()


    deleteComment: (e) ->
      commentKey = $(e.target).closest('[data-comment-key]').data 'comment-key'
      confirmMsg = $(e.target).data 'confirm-msg'

      if confirm(confirmMsg)
        @showActionSpinner()
        $.ajax
          type: "POST"
          url: baseUrl + "/issue/delete_comment?id=" + commentKey
        .done => @updateAfterAction true
        .fail (r) =>
          alert  _.pluck(r.responseJSON.errors, 'msg').join(' ')
          @hideActionSpinner()


    transition: (e) ->
      p = window.process.addBackgroundProcess()
      $.ajax
        type: 'POST',
        url: baseUrl + '/api/issues/do_transition',
        data:
          issue: @model.get('key')
          transition: $(e.currentTarget).data 'transition'
      .done =>
        @resetIssue {}, p
      .fail =>
        window.process.failBackgroundProcess p


    setSeverity: (e) ->
      e.stopPropagation()
      $('body').click()
      @popup = new SetSeverityFormView
        triggerEl: $(e.currentTarget)
        bottom: true
        model: @model
      @popup.render()


    assign: (e) ->
      e.stopPropagation()
      $('body').click()
      @popup = new AssignFormView
        triggerEl: $(e.currentTarget)
        bottom: true
        model: @model
      @popup.render()


    assignToMe: ->
      view = new AssignFormView model: @model
      view.submit window.SS.user, window.SS.userName
      view.close()


    plan: (e) ->
      p = window.process.addBackgroundProcess()
      t = $(e.currentTarget)
      actionPlans = new ActionPlans()
      actionPlans.fetch
        reset: true
        data: project: @model.get('project')
      .done =>
        e.stopPropagation()
        $('body').click()
        @popup = new PlanFormView
          triggerEl: t
          bottom: true
          model: @model
          collection: actionPlans
        @popup.render()
        window.process.finishBackgroundProcess p
      .fail =>
        window.process.failBackgroundProcess p


    showMoreActions: (e) ->
      e.stopPropagation()
      $('body').click()
      @popup = new MoreActionsView
        triggerEl: $(e.currentTarget)
        bottomRight: true
        model: @model
        detailView: @
      @popup.render()


    action: (action) ->
      p = window.process.addBackgroundProcess()
      $.post "#{baseUrl}/api/issues/do_action", issue: @model.id, actionKey: action
      .done =>
        window.process.finishBackgroundProcess p
        @resetIssue()
      .fail =>
        window.process.failBackgroundProcess p


    showRule: ->
      ruleKey = @model.get 'rule'
      $.get "#{baseUrl}/api/rules/show", key: ruleKey, (r) =>
        ruleOverlay = new RuleOverlay
          model: new Backbone.Model r.rule
        ruleOverlay.render()


    serializeData: ->
      componentKey = encodeURIComponent @model.get 'component'
      issueKey = encodeURIComponent @model.get 'key'
      _.extend super,
        permalink: "#{baseUrl}/component/index#component=#{componentKey}&currentIssue=#{issueKey}"
