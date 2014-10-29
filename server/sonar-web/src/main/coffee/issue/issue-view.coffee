define [
  'backbone.marionette'
  'templates/issue'

  'issue/models/rule'
  'issue/views/rule-view'

  'issue/models/change-log'
  'issue/views/change-log-view'

  'issue/collections/action-plans'

  'issue/views/assign-form-view'
  'issue/views/comment-form-view'
  'issue/views/plan-form-view'
  'issue/views/set-severity-form-view'

], (
  Marionette
  Templates

  Rule
  RuleView

  ChangeLog
  ChangeLogView

  ActionPlans

  AssignFormView
  CommentFormView
  PlanFormView
  SetSeverityFormView

) ->

  $ = jQuery


  class IssueView extends Marionette.Layout
    template: Templates['issue']


    regions:
      formRegion: '.code-issue-form'
      ruleRegion: '#tab-issue-rule'
      changeLogRegion: '#tab-issue-changelog'


    modelEvents:
      'change': 'render'


    events:
      'click': 'setDetailScope',

      'click .code-issue-toggle': 'toggleCollapsed',

      'click [href=#tab-issue-rule]': 'showRuleTab',
      'click [href=#tab-issue-changelog]': 'showChangeLogTab',

      'click #issue-comment': 'comment',
      'click .issue-comment-edit': 'editComment',
      'click .issue-comment-delete': 'deleteComment',
      'click .issue-transition': 'transition',
      'click #issue-set-severity': 'setSeverity',
      'click #issue-assign': 'assign',
      'click #issue-assign-to-me': 'assignToMe',
      'click #issue-plan': 'plan',
      'click .issue-action': 'action'


    onRender: ->
      @rule = new Rule key: this.model.get('rule')
      @ruleRegion.show new RuleView model: @rule, issue: @model
      @changeLog = new ChangeLog()
      @changeLogRegion.show new ChangeLogView collection: @changeLog, issue: @model


    setDetailScope: ->
      key.setScope 'detail'


    setListScope: ->
      key.setScope 'list'


    onClose: ->
      @ruleRegion.reset() if @ruleRegion


    resetIssue: (options) ->
      @setListScope()
      key = @model.get 'key'
      @model.clear silent: true
      @model.set { key: key }, { silent: true }
      @model.fetch(options).done => @trigger 'reset'


    toggleCollapsed: ->
      @$('.code-issue').toggleClass 'code-issue-collapsed'
      unless @$('.code-issue').is '.code-issue-collapsed'
        @showRuleTab()


    hideTabs: ->
      @$('.js-tab-link').removeClass 'active-link'
      @$('.js-tab').hide()


    showTab: (tab) ->
      @hideTabs()
      s = "#tab-issue-#{tab}"
      @$(s).show()
      @$("[href=#{s}]").addClass 'active-link'


    showRuleTab: (e) ->
      e?.preventDefault()
      @showTab 'rule'
      unless @rule.has 'name'
        @$('#tab-issue-rule').addClass 'navigator-fetching'
        @rule.fetch
          success: => @$('#tab-issue-rule').removeClass 'navigator-fetching'


    showChangeLogTab: (e) ->
      e?.preventDefault()
      @showTab 'changelog'
      unless @changeLog.length > 0
        @$('#tab-issue-changeLog').addClass 'navigator-fetching'
        @changeLog.fetch
          data: issue: @model.get 'key'
          success: => @$('#tab-issue-changelog').removeClass 'navigator-fetching'


    showActionView: (view) ->
      @$('.code-issue-actions').hide()
      @$('.code-issue-form').show()
      @formRegion.show view


    showActionSpinner: ->
      @$('.code-issue-actions').addClass 'navigator-fetching'


    hideActionSpinner: ->
      @$('.code-issue-actions').removeClass 'navigator-fetching'


    updateAfterAction: (fetch) ->
      @formRegion.reset()
      @$('.code-issue-actions').show()
      @$('.code-issue-form').hide()
      @$('[data-comment-key]').show()

      if fetch
        $.when(@resetIssue()).done => @hideActionSpinner()


    comment: ->
      commentFormView = new CommentFormView
        issue: @model
        detailView: @
      @showActionView commentFormView


    editComment: (e) ->
      commentEl = $(e.target).closest '[data-comment-key]'
      commentKey = commentEl.data 'comment-key'
      comment = _.findWhere this.model.get('comments'), { key: commentKey }

      commentEl.hide();

      commentFormView = new CommentFormView
        model: new Backbone.Model comment
        issue: @model
        detailView: @
      @showActionView commentFormView


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
      @showActionSpinner();
      $.ajax
        type: 'POST',
        url: baseUrl + '/api/issues/do_transition',
        data:
          issue: @model.get('key')
          transition: $(e.target).data('transition')
      .done => @resetIssue()
      .fail (r) =>
        alert  _.pluck(r.responseJSON.errors, 'msg').join(' ')
        @hideActionSpinner()


    setSeverity: ->
      setSeverityFormView = new SetSeverityFormView
        issue: @model
        detailView: @
      @showActionView setSeverityFormView


    assign: ->
      assignFormView = new AssignFormView
        issue: @model
        detailView: this
      @showActionView assignFormView


    assignToMe: ->
      @showActionSpinner()
      $.ajax
        type: 'POST'
        url: baseUrl + '/api/issues/assign'
        data:
          issue: @model.get('key')
          me: true
      .done => @resetIssue()
      .fail (r) =>
        alert  _.pluck(r.responseJSON.errors, 'msg').join(' ')
        @hideActionSpinner()


    plan: ->
      actionPlans = new ActionPlans()
      planFormView = new PlanFormView
        collection: actionPlans
        issue: @model
        detailView: @
      @showActionSpinner()
      actionPlans.fetch
        reset: true
        data: project: @model.get('project')
        success: =>
          @hideActionSpinner()
          @showActionView planFormView


    action: (e) ->
      actionKey = $(e.target).data 'action'
      @showActionSpinner()
      $.ajax
        type: 'POST'
        url: baseUrl + '/api/issues/do_action'
        data:
          issue: @model.get('key')
          actionKey: actionKey
      .done => @resetIssue()
      .fail (r) =>
        alert  _.pluck(r.responseJSON.errors, 'msg').join(' ')
        @hideActionSpinner()


    serializeData: ->
      componentKey = encodeURIComponent @model.get 'component'
      issueKey = encodeURIComponent @model.get 'key'
      _.extend super,
        permalink: "#{baseUrl}/component/index#component=#{componentKey}&currentIssue=#{issueKey}"
