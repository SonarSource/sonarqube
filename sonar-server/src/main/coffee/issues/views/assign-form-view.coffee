define [
  'backbone.marionette'
  'templates/issues'
], (
  Marionette
  Templates
) ->

  $ = jQuery


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
        additionalChoices.push id: currentUser, text: translate('assignedToMe')

      if !!assignee
        additionalChoices.push id: '', text: translate('unassigned')

      select2Options =
        allowClear: false
        width: '250px'
        formatNoMatches: -> translate('select2.noMatches')
        formatSearching: -> translate('select2.searching')
        formatInputTooShort: -> translate('select2.tooShort')

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

      @ui.select.select2(select2Options).select2 'open'


    cancel: ->
      @options.detailView.updateAfterAction false


    submit: ->
      @options.detailView.showActionSpinner()

      $.ajax
        type: 'POST'
        url: baseUrl + '/api/issues/assign'
        data:
          issue: @options.issue.get('key')
          assignee: @ui.select.val()
      .done => @options.detailView.updateAfterAction true
      .fail (r) =>
        alert _.pluck(r.responseJSON.errors, 'msg').join(' ')
        @options.detailView.hideActionSpinner()