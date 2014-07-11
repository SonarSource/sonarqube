$ = jQuery

METRICS =
  ncloc:
    name: 'Lines of code'
  coverage:
    name: 'Coverage'
    direction: -1

METRICS_PRIORITY = ['coverage', 'ncloc']

COMPONENTS = [
  { key: 'some-key-1', name: 'Some Name 1', longName: 'Some Long Name 1', qualifier: 'TRK', measures }
]



describe 'treemap widget suite', ->

  uid = 1

  render = (treemap, components = COMPONENTS, metrics = METRICS, metricsPriority = METRICS_PRIORITY) ->
    elementId = 'treemap-' + ++uid
    $("<div id='#{elementId}'></div>").appendTo 'body'
    treemap.metrics(metrics).metricsPriority(metricsPriority).components(components).options(
      heightInPercents: 55
      maxItems: 10
      maxItemsReachedMessage: ''
      baseUrl: ''
      noData: ''
    ).render('#' + elementId);


  close = ->
    elementId = 'treemap-' + uid
    $('#' + elementId).delete()


  beforeEach ->
    @treemap =  new window.SonarWidgets.Treemap()


  afterEach ->
    @treemap = undefined
    close()


  it 'renders', ->
    render @treemap
