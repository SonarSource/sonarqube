$ = jQuery

describe 'WORK_DUR suite', ->

  beforeEach ->
    window.SS = {}
    window.SS.phrases =
      'work_duration':
        'x_days': '{0}d', 'x_hours': '{0}h', 'x_minutes': '{0}min'

    @input = $('<input type="text">')
    @input.appendTo $('body')
    @input.data 'type', 'WORK_DUR'


  it 'converts', ->
    @input.originalVal '2d 7h 13min'
    expect(@input.val()).toBe 1393

  it 'converts only days', ->
    @input.originalVal '1d'
    expect(@input.val()).toBe 480

  it 'converts hours with minutes', ->
    @input.originalVal '2h 30min'
    expect(@input.val()).toBe 150

  it 'converts zero', ->
    @input.originalVal '0'
    expect(@input.val()).toBe 0


  it 'restores', ->
    @input.val 1393
    expect(@input.originalVal()).toBe '2d 7h 13min'

  it 'restores zero', ->
    @input.val '0'
    expect(@input.originalVal()).toBe '0'


  it 'returns initially incorrect value', ->
    @input.val 'something'
    expect(@input.val()).toBe 'something'


describe 'RATING suite', ->

  beforeEach ->
    @input = $('<input type="text">')
    @input.appendTo $('body')
    @input.data 'type', 'RATING'


  it 'converts A', ->
    @input.originalVal 'A'
    expect(@input.val()).toBe 1


  it 'converts B', ->
    @input.originalVal 'B'
    expect(@input.val()).toBe 2


  it 'converts E', ->
    @input.originalVal 'E'
    expect(@input.val()).toBe 5


  it 'does not convert F', ->
    @input.originalVal 'F'
    expect(@input.val()).toBe 'F'


  it 'restores A', ->
    @input.val 1
    expect(@input.originalVal()).toBe 'A'


  it 'restores E', ->
    @input.val 5
    expect(@input.originalVal()).toBe 'E'


  it 'returns initially incorrect value', ->
    @input.val 'something'
    expect(@input.val()).toBe 'something'
