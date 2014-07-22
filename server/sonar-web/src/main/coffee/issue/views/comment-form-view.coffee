define [
  'backbone.marionette'
  'templates/issue'
], (
  Marionette
  Templates
) ->

  $ = jQuery
  

  class IssueDetailCommentFormView extends Marionette.ItemView
    template: Templates['comment-form']


    ui:
      textarea: '#issue-comment-text'
      cancelButton: '#issue-comment-cancel'
      submitButton: '#issue-comment-submit'


    events:
      'keyup #issue-comment-text': 'toggleSubmit'
      'click #issue-comment-cancel': 'cancel'
      'click #issue-comment-submit': 'submit'


    onDomRefresh: ->
      @ui.textarea.focus()


    toggleSubmit: ->
      @ui.submitButton.prop 'disabled', @ui.textarea.val().length == 0


    cancel: ->
      @options.detailView.updateAfterAction false


    submit: ->
      text = @ui.textarea.val()
      update = @model && @model.has('key')
      url = baseUrl + '/api/issues/' + (if update then 'edit_comment' else 'add_comment')
      data = text: text

      if update
        data.key = @model.get('key')
      else
        data.issue = @options.issue.get('key')

      @options.detailView.showActionSpinner()

      $.ajax
        type: 'POST'
        url: url
        data: data
      .done => @options.detailView.updateAfterAction true
      .fail (r) =>
        alert _.pluck(r.responseJSON.errors 'msg').join(' ')
        @options.detailView.hideActionSpinner()
