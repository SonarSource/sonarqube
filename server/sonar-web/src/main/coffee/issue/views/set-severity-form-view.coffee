define [
  'backbone.marionette'
  'templates/issue'
], (
  Marionette
  Templates
) ->

  $ = jQuery
  

  class SetSeverityFormView extends Marionette.ItemView
    template: Templates['set-severity-form']


    ui:
      select: '#issue-set-severity-select'


    events:
      'click #issue-set-severity-cancel': 'cancel'
      'click #issue-set-severity-submit': 'submit'


    onRender: ->
     format = (state) ->
       return state.text unless state.id
       '<i class="icon-severity-' + state.id.toLowerCase() + '"></i> ' + state.text

     @ui.select.select2
       minimumResultsForSearch: 100
       formatResult: format
       formatSelection: format
       escapeMarkup: (m) -> m


    cancel: ->
      @options.detailView.updateAfterAction false


    submit: ->
      @options.detailView.showActionSpinner()
      $.ajax
        type: 'POST'
        url: baseUrl + '/api/issues/set_severity'
        data:
          issue: @options.issue.get('key')
          severity: @ui.select.val()
      .done => @options.detailView.updateAfterAction true
      .fail (r) =>
        alert _.pluck(r.responseJSON.errors, 'msg').join(' ')
        @options.detailView.hideActionSpinner()
