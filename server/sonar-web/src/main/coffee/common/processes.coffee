$ = jQuery

process = {}
process.queue = {}
process.timeout = 300

_.extend process,

  addBackgroundProcess: ->
    uid = _.uniqueId 'process'
    @queue[uid] = new Date()
    setTimeout (=> @showSpinner uid if @isBackgroundProcessAlive uid), @timeout
    uid


  isBackgroundProcessAlive: (uid) ->
    @queue[uid]?


  finishBackgroundProcess: (uid) ->
    delete @queue[uid]
    @removeSpinner uid


  showSpinner: (uid) ->
    id = "spinner-#{uid}"
    spinner = $ '<div></div>'
    spinner.addClass 'process-spinner'
    spinner.prop 'id', id
    spinner.text 'still working... ' + uid
    spinner.appendTo $('body')
    setTimeout (-> spinner.addClass 'shown'), 100


  removeSpinner: (uid) ->
    id = "spinner-#{uid}"
    $('#' + id).remove()


_.extend window, process: process


