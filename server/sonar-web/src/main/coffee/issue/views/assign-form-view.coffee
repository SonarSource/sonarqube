define [
  'templates/issue'
  'issue/views/action-options-view'
], (
  Templates
  ActionOptionsView
) ->

  $ = jQuery


  class extends ActionOptionsView
    template: Templates['issue-assign-form']
    optionTemplate: Templates['issue-assign-form-option']


    events: ->
      _.extend super,
        'click input': 'onInputClick'
        'keydown input': 'onInputKeydown'
        'keyup input': 'onInputKeyup'


    initialize: ->
      super
      @assignees = []
      @debouncedSearch = _.debounce @search, 250


    getAssignee: ->
      @model.get 'assignee'


    getAssigneeName: ->
      @model.get 'assigneeName'


    onRender: ->
      super
      @renderAssignees()
      setTimeout (=> @$('input').focus()), 100


    renderAssignees: ->
      @$('.issue-action-option').remove()
      @getAssignees().forEach @renderAssignee, @
      @selectInitialOption()


    renderAssignee: (assignee) ->
      html = @optionTemplate assignee
      @$('.issue-action-options').append html


    selectOption: (e) ->
      assignee = $(e.currentTarget).data 'value'
      assigneeName = $(e.currentTarget).data 'text'
      @submit assignee, assigneeName
      super


    submit: (assignee, assigneeName) ->
      _assignee = @getAssignee()
      _assigneeName = @getAssigneeName()
      return if assignee == _assignee
      p = window.process.addBackgroundProcess()
      if assignee == ''
        @model.set assignee: null, assigneeName: null
      else
        @model.set assignee: assignee, assigneeName: assigneeName
      $.ajax
        type: 'POST'
        url: "#{baseUrl}/api/issues/assign"
        data:
          issue: @model.id
          assignee: assignee
      .done =>
        window.process.finishBackgroundProcess p
      .fail =>
        @model.set assignee: _assignee, assigneeName: _assigneeName
        window.process.failBackgroundProcess p


    onInputClick: (e) ->
      e.stopPropagation()


    onInputKeydown: (e) ->
      @query = @$('input').val()
      return @selectPreviousOption() if e.keyCode == 38 # up
      return @selectNextOption() if e.keyCode == 40 # down
      return @selectActiveOption() if e.keyCode == 13 # return
      return false if e.keyCode == 9 # tab
      @close() if e.keyCode == 27 # escape


    onInputKeyup: ->
      query = @$('input').val()
      if query != @query
        query = '' if query.length < 2
        @query = query
        @debouncedSearch query


    search: (query) ->
      if query.length > 1
        p = window.process.addBackgroundProcess()
        $.get "#{baseUrl}/api/users/search", s: query
        .done (data) =>
          @resetAssignees data.users
          window.process.finishBackgroundProcess p
        .fail =>
          window.process.failBackgroundProcess p
      else
        @resetAssignees []


    resetAssignees: (users) ->
      @assignees = users.map (user) ->
        id: user.login
        text: user.name
      @renderAssignees()


    getAssignees: ->
      return @assignees if @assignees.length > 0
      assignees = [{ id: '', text: t('unassigned') }]
      currentUser = window.SS.user
      currentUserName = window.SS.userName
      currentAssignee = @getAssignee()
      if !currentAssignee || currentUser != currentAssignee
        assignees.push id: currentUser, text: currentUserName
      @makeUnique assignees


    makeUnique: (assignees) ->
      _.uniq assignees, false, (assignee) -> assignee.id
