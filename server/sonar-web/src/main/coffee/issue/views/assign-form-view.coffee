define [
  'backbone.marionette'
  'templates/issue'
], (
  Marionette
  Templates
) ->

  $ = jQuery
  ME = '#me#'


  class AssignFormView extends Marionette.ItemView
    template: Templates['assign-form']


    ui:
      select: '#issue-assignee-select'


    events:
      'click #issue-assign-cancel': 'cancel'
      'click #issue-assign-submit': 'submit'


    onRender: ->
      currentUser = window.SS.currentUser
      assignee = @options.issue.get('assignee')
      additionalChoices = []

      if !assignee || currentUser != assignee
        additionalChoices.push id: ME, text: t('assigned_to_me')

      if !!assignee
        additionalChoices.push id: '', text: t('unassigned')

      select2Options =
        allowClear: false
        width: '250px'
        formatNoMatches: -> t('select2.noMatches')
        formatSearching: -> t('select2.searching')
        formatInputTooShort: -> t('select2.tooShort')

      if additionalChoices.length > 0
        select2Options.minimumInputLength = 0
        select2Options.query = (query) ->
          if query.term.length == 0
            query.callback results: additionalChoices
          else if query.term.length >= 2
            $.ajax
              url: baseUrl + '/api/users/search?f=s2'
              data: s: query.term
              dataType: 'jsonp'
            .done (data) -> query.callback data
      else
        select2Options.minimumInputLength = 2
        select2Options.ajax =
          quietMillis: 300
          url: baseUrl + '/api/users/search?f=s2'
          data: (term, page) -> s: term, p: page
          results: (data) -> more: data.more, results: data.results

      @ui.select.select2 select2Options
      @ui.select.on 'change', => @$('[type=submit]').focus()
      @ui.select.select2 'open'


    cancel: ->
      @options.detailView.updateAfterAction false


    submit: ->
      @options.detailView.showActionSpinner()
      data = issue: @options.issue.get('key')
      if @ui.select.val() == ME then data.me = true else data.assignee = @ui.select.val()
      $.ajax
        type: 'POST'
        url: baseUrl + '/api/issues/assign'
        data: data
      .done => @options.detailView.updateAfterAction true
      .fail (r) =>
        alert _.pluck(r.responseJSON.errors, 'msg').join(' ')
        @options.detailView.hideActionSpinner()
