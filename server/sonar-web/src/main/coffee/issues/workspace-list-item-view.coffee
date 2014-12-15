define [
  'issue/issue-view'
], (
  IssueView
) ->

  class extends IssueView

    ui:
      tagsChange: '.issue-tags-change'
      tagInput: '.issue-tag-input'
      tagsEdit: '.issue-tag-edit'
      tagsEditDone: '.issue-tag-edit-done'
      tagsEditCancel: '.issue-tag-edit-cancel'
      tagsList: '.issue-tag-list'

    events: ->
      _.extend super,
        'click': 'selectCurrent'
        'click .js-issue-navigate': 'openComponentViewer'
        'click @ui.tagsChange': 'changeTags'
        'click @ui.tagsEditDone': 'editDone'
        'click @ui.tagsEditCancel': 'cancelEdit'

    initialize: (options) ->
      @listenTo options.app.state, 'change:selectedIndex', @select


    onRender: ->
      super

      @ui.tagsEdit.hide()

      @$el.addClass 'issue-navigate-right'
      @select()


    select: ->
      selected = @model.get('index') == @options.app.state.get 'selectedIndex'
      @$el.toggleClass 'selected', selected


    selectCurrent: ->
      @options.app.state.set selectedIndex: @model.get('index')


    resetIssue: (options, p) ->
      key = @model.get 'key'
      index = @model.get 'index'
      @model.clear silent: true
      @model.set { key: key, index: index }, { silent: true }
      @model.fetch(options)
      .done =>
        @trigger 'reset'
        window.process.finishBackgroundProcess p if p?
      .fail ->
        window.process.failBackgroundProcess p if p?


    openComponentViewer: ->
      @options.app.state.set selectedIndex: @model.get('index')
      if @options.app.state.has 'component'
        @options.app.controller.closeComponentViewer()
      else
        @options.app.controller.showComponentViewer @model


    changeTags: ->
      jQuery.ajax
        url: "#{baseUrl}/api/issues/tags?ps=0"
      .done (r) =>
        if @ui.tagInput.select2
          # Prevent synchronization issue with navigation
          @ui.tagInput.select2
            tags: (_.difference r.tags, @model.get 'tags')
            width: '300px'
        if @ui.tagsEdit.show
          @ui.tagsEdit.show()
        if @ui.tagsList.hide
          @ui.tagsList.hide()
        @tagsBuffer = @ui.tagInput.select2 'val'
        key.setScope 'tags'
        key 'escape', 'tags', => @cancelEdit()


    cancelEdit: ->
      key.unbind 'escape', 'tags'
      if @ui.tagsList.show
        @ui.tagsList.show()
      if @ui.tagInput.select2
        @ui.tagInput.select2 'val', @tagsBuffer
        @ui.tagInput.select2 'close'
      if @ui.tagsEdit.hide
        @ui.tagsEdit.hide()


    editDone: ->
      @ui.tagsEdit.html '<i class="spinner"></i>'
      tags = @ui.tagInput.val()
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/issues/set_tags"
        data:
          key: @model.get 'key'
          tags: tags
      .done (r) =>
          @model.set 'tags', r.tags
          @cancelEdit()
      .always =>
        @render()


    serializeData: ->
      _.extend super,
        showComponent: true
