define [
  'backbone.marionette'
  'templates/issues'
], (
  Marionette
  Templates
) ->

  $ = jQuery
  API_ISSUE = "#{baseUrl}/api/issues/show"
  API_ADD_MANUAL_ISSUE = "#{baseUrl}/api/issues/create"


  class extends Marionette.ItemView
    template: Templates['manual-issue']


    events:
      'submit .js-manual-issue-form': 'formSubmit'
      'click .js-cancel': 'cancel'


    onRender: ->
      @delegateEvents()
      @$('[name=rule]').select2
        width: '250px'
        minimumResultsForSearch: 10


    showSpinner: ->
      @$('.js-submit').hide()
      @$('.js-spinner').show()


    hideSpinner: ->
      @$('.js-submit').show()
      @$('.js-spinner').hide()


    validateFields: ->
      message = @$('[name=message]')
      unless message.val()
        message.addClass('invalid').focus()
        return false
      return true


    formSubmit: (e) ->
      e.preventDefault()
      return unless @validateFields()
      @showSpinner()
      data = $(e.currentTarget).serialize()
      $.post API_ADD_MANUAL_ISSUE, data
        .done (r) =>
          @addIssue r.issue.key
        .fail (r) =>
          @hideSpinner()
          if r.responseJSON?.errors?
            @showError _.pluck(r.responseJSON.errors, 'msg').join '. '


    addIssue: (key) ->
      $.get API_ISSUE, key: key, (r) =>
        @trigger 'add', r.issue
        @close()


    showError: (msg) ->
      @$('.code-issue-errors').removeClass('hidden').text msg


    cancel: ->
      @close()


    serializeData: ->
      _.extend super,
        line: @options.line
        component: @options.component
        rules: _.sortBy @options.rules, 'name'
