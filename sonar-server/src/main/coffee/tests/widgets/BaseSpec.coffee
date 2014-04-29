$ = jQuery

describe 'base widget suite', ->

  it 'exists', ->
    expect(window.SonarWidgets).toBeDefined()
    expect(window.SonarWidgets.BaseWidget).toBeDefined()


  it 'adds fields', ->
    widget = new window.SonarWidgets.BaseWidget()
    widget.addField 'fieldName', 1

    expect(typeof widget.fieldName).toBe 'function'
    expect(widget.fieldName()).toBe 1

    expect(widget.fieldName(2)).toBe widget
    expect(widget.fieldName()).toBe 2


  it 'adds metrics', ->
    widget = new window.SonarWidgets.BaseWidget()
    widget.addField 'metrics', 'metricA': { name: 'Metric A', someField: 2 }
    widget.addField 'metricsPriority', ['metricA']
    widget.addMetric 'myMetric', 0

    expect(widget.myMetric).toBeDefined()
    expect(widget.myMetric.key).toBe 'metricA'
    expect(widget.myMetric.name).toBe 'Metric A'
    expect(widget.myMetric.someField).toBe 2
    expect(typeof widget.myMetric.value).toBe 'function'
    expect(typeof widget.myMetric.formattedValue).toBe 'function'


  it 'has default properties', ->
    widget = new window.SonarWidgets.BaseWidget()

    expect(widget.components).toBeDefined()
    expect(widget.metrics).toBeDefined()
    expect(widget.metricsPriority).toBeDefined()
    expect(widget.options).toBeDefined()


  it 'created "translate" string', ->
    widget = new window.SonarWidgets.BaseWidget()

    expect(widget.trans(1, 2)).toBe 'translate(1,2)'