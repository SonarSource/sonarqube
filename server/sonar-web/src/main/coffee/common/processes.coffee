$ = jQuery

process = {}
process.queue = {}
process.timeout = 300
process.fadeTimeout = 100

_.extend process,

  addBackgroundProcess: ->
    uid = _.uniqueId 'process'
    @renderSpinner uid
    @queue[uid] = setTimeout (=> @showSpinner uid), @timeout
    uid


  isBackgroundProcessAlive: (uid) ->
    @queue[uid]?


  finishBackgroundProcess: (uid) ->
    if @isBackgroundProcessAlive uid
      clearInterval @queue[uid]
      delete @queue[uid]
      @removeSpinner uid


  failBackgroundProcess: (uid) ->
    if @isBackgroundProcessAlive uid
      clearInterval @queue[uid]
      delete @queue[uid]
      spinner = @getSpinner uid
      spinner.addClass 'process-spinner-failed'
      spinner.text t 'process.fail'
      close = $('<button></button>').html('<i class="icon-close"></i>').addClass 'process-spinner-close'
      close.appendTo spinner
      close.on 'click', => @removeSpinner uid


  renderSpinner: (uid) ->
    id = "spinner-#{uid}"
    spinner = $ '<div></div>'
    spinner.addClass 'process-spinner'
    spinner.prop 'id', id
    text = t 'process.still_working'
    text = 'Still Working...' if text == 'process.still_working'
    spinner.text text
    spinner.appendTo $('body')


  showSpinner: (uid) ->
    spinner = @getSpinner(uid)
    setTimeout (-> spinner.addClass 'shown'), @fadeTimeout


  removeSpinner: (uid) ->
    @getSpinner(uid).remove()


  getSpinner: (uid) ->
    id = "spinner-#{uid}"
    $('#' + id)


_.extend window, process: process


