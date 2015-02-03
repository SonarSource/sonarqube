define [
  'common/popup'
  'templates/issue'
], (
  PopupView
) ->

  $ = jQuery
  

  class extends PopupView
    className: 'bubble-popup issue-comment-bubble-popup'
    template: Templates['comment-form']


    ui:
      textarea: '.issue-comment-form-text textarea'
      cancelButton: '.js-issue-comment-cancel'
      submitButton: '.js-issue-comment-submit'


    events:
      'click': 'onClick'
      'keydown @ui.textarea': 'onKeydown'
      'keyup @ui.textarea': 'toggleSubmit'
      'click @ui.cancelButton': 'cancel'
      'click @ui.submitButton': 'submit'


    onRender: ->
      super
      setTimeout (=> @ui.textarea.focus()), 100


    toggleSubmit: ->
      @ui.submitButton.prop 'disabled', @ui.textarea.val().length == 0


    onClick: (e) ->
      # disable close by clicking inside
      e.stopPropagation()


    onKeydown: (e) ->
      @close() if e.keyCode == 27 # escape


    cancel: ->
      @options.detailView.updateAfterAction false


    submit: ->
      text = @ui.textarea.val()
      update = @model && @model.has('key')
      method = if update then 'edit_comment' else 'add_comment'
      url = "#{baseUrl}/api/issues/#{method}"
      data = text: text
      if update
        data.key = @model.get('key')
      else
        data.issue = @options.issue.id
      $.post url, data
      .done =>
        @options.detailView.updateAfterAction true
