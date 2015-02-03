define [
  'issue/views/action-options-view'
  'templates/issue'
], (
  ActionOptionsView
) ->

  $ = jQuery


  class extends ActionOptionsView
    template: Templates['issue-tags-form']
    optionTemplate: Templates['issue-tags-form-option']


    modelEvents:
      'change:tags': 'renderTags'


    events: ->
      _.extend super,
        'click input': 'onInputClick'
        'keydown input': 'onInputKeydown'
        'keyup input': 'onInputKeyup'


    initialize: ->
      super
      @query = ''
      @tags = []
      @selected = 0
      @debouncedSearch = _.debounce @search, 250
      @requestTags()


    requestTags: ->
      $.get "#{baseUrl}/api/issues/tags", ps: 0
      .done (data) =>
        @tags = data.tags
        @renderTags()


    onRender: ->
      super
      @renderTags()
      setTimeout (=> @$('input').focus()), 100


    selectInitialOption: ->
      @selected = Math.max Math.min(@selected, @getOptions().length - 1), 0
      @makeActive @getOptions().eq @selected


    filterTags: (tags) ->
      _.filter tags, (tag) => tag.indexOf(@query) != -1


    renderTags: ->
      @$('.issue-action-option').remove()
      @filterTags(@getTags()).forEach @renderSelectedTag, @
      @filterTags(_.difference(@tags, @getTags())).forEach @renderTag, @
      if @query.length > 0 && @tags.indexOf(@query) == -1 && @getTags().indexOf(@query) == -1
        @renderCustomTag @query
      @selectInitialOption()


    renderSelectedTag: (tag) ->
      html = @optionTemplate { tag: tag, selected: true, custom: false }
      @$('.issue-action-options').append html


    renderTag: (tag) ->
      html = @optionTemplate { tag: tag, selected: false, custom: false }
      @$('.issue-action-options').append html


    renderCustomTag: (tag) ->
      html = @optionTemplate { tag: tag, selected: false, custom: true }
      @$('.issue-action-options').append html


    selectOption: (e) ->
      e.preventDefault()
      e.stopPropagation()
      tags = @getTags().slice()
      tag = $(e.currentTarget).data 'value'
      if $(e.currentTarget).data('selected')?
        tags = _.without tags, tag
      else
        tags.push tag
      @selected = @getOptions().index $(e.currentTarget)
      @submit tags


    submit: (tags) ->
      _tags = @getTags()
      @model.set tags: tags
      $.ajax
        type: 'POST'
        url: "#{baseUrl}/api/issues/set_tags"
        data:
          key: @model.id
          tags: tags.join()
      .fail =>
        @model.set tags: _tags


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
        @query = query
        @debouncedSearch query


    search: (query) ->
      @query = query
      @renderTags()


    resetAssignees: (users) ->
      @assignees = users.map (user) ->
        id: user.login
        text: user.name
      @renderTags()


    getTags: ->
      @model.get('tags') || []
