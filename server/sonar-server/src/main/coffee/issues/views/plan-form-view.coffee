define [
  'backbone.marionette'
  'templates/issues'
], (
  Marionette
  Templates
) ->

  $ = jQuery


  class PlanFormView extends Marionette.ItemView
    template: Templates['plan-form']


    collectionEvents:
      'reset': 'render'


    ui:
      select: '#issue-detail-plan-select'


    events:
      'click #issue-plan-cancel': 'cancel'
      'click #issue-plan-submit': 'submit'


    onRender: ->
      @ui.select.select2
        width: '250px'
        minimumResultsForSearch: 100

      @$('.error a').prop('href', baseUrl + '/action_plans/index/' + this.options.issue.get('project'))


    cancel: ->
      @options.detailView.updateAfterAction(false);


    submit: ->
      plan = @ui.select.val()
      @options.detailView.showActionSpinner()

      $.ajax
        type: 'POST'
        url: baseUrl + '/api/issues/plan'
        data:
          issue: this.options.issue.get('key'),
          plan: if plan == '#unplan' then '' else plan
      .done => @options.detailView.updateAfterAction true
      .fail (r) =>
        alert _.pluck(r.responseJSON.errors, 'msg').join(' ')
        @options.detailView.hideActionSpinner()


    serializeData: ->
      items: @collection.toJSON()
      issue: @options.issue.toJSON()