// flow-typed signature: ee6e939898c254f6578ac3485db3e223
// flow-typed version: 651c6d20b9/backbone_v1.x.x/flow_>=v0.25.x

declare module 'backbone' {
  declare var $: any; // @TODO this is no correct, but it is difficult to require another definition from here.
  declare var _: any; // @TODO this is no correct, but it is difficult to require another definition from here.
  declare var version: string;

  declare type eventCallback = (event: Event) => void | mixed;
  declare type Attrs = {[name: string]: mixed};
  declare type CRUDMethod = 'create' | 'read' | 'update' | 'delete';

  /**
   * Events Module - http://backbonejs.org/#Events
   */
  declare class Events {
    // Not sure the best way of adding these to the declaration files
    on(event: string, callback: eventCallback, context?: Object): void;
    bind(event: string, callback: eventCallback, context?: Object): void;
    off(event: ?string, callback?: ?eventCallback, context?: Object): void;
    unbind(event: ?string, callback?: ?eventCallback, context?: Object): void;
    trigger(event: string, ...args?: Array<mixed>): void;
    listenTo(other: Events, event: string, callback: eventCallback): void;
    stopListening(other: Events, callback?: ?eventCallback, context?: Object): void;
    static on(event: string, callback: eventCallback, context?: Object): void;
    static bind(event: string, callback: eventCallback, context?: Object): void;
    static off(event: ?string, callback?: ?eventCallback, context?: Object): void;
    static unbind(event: ?string, callback?: ?eventCallback, context?: Object): void;
    static trigger(event: string, ...args?: Array<mixed>): void;
    static listenTo(other: Events, event: string, callback: eventCallback): void;
    static stopListening(other: Events, callback?: ?eventCallback, context?: Object): void;
  }

  /**
   * Model Class - http://backbonejs.org/#Model
   */
  declare type ModelOpts = {
    collection?: Collection<*>,
    parse?: Function,
    [optionName: string]: mixed
  };

  declare class Model mixins Events {
    static extend<P, CP>(instanceProperies: P, classProperties?: CP): Class<Model & P> & CP;
    constructor(attributes?: Attrs, options?: ModelOpts): void;
    static initialize(attributes?: Attrs, options?: ModelOpts): void;
    idAttribute: string;
    id: string | number;
    attributes: Attrs;
    cid: string;
    cidPrefix: string;
    chagned: ?Object,
    validationError: ?Object;
    initialize(): void;
    toJSON(): Attrs;
    sync: sync;
    set(attrs: Attrs, options?: Object): this;
    set(attr: string, value: mixed, options?: Object): this;
    has(attr: string): boolean;
    unset(attr: string, options?: { unset?: boolean }): this;
    clear(options?: Object): this;
    escape(attr: string): mixed;
    previous(attr: string): mixed;
    previousAttributes(): Attrs;
    // @TODO should return a jQuery XHR, but I cannot define this without the dependency on jquery lib def.
    fetch(options?: Object): any;
    // Start Underscore methods
    // @TODO Underscore Methods should be defined by the library definition
    keys(): string[];
    values(): mixed[];
    pairs: Function;
    invert: Function;
    pick: Function;
    omit: Function;
    chain(): Function;
    isEmpty(): boolean;
    // End underscore methods
    isValid(): boolean;
    url(): string;
    urlRoot: string | () => string,
    clone: this;
    isNew: boolean;
    hasChanged(attribute?: string): boolean;
    chagnedAttributes(attributes?: {[attr: string]: mixed}): boolean;
    previous(attribute: string): mixed;
    previousAttirbutes(): Attrs;
  }

  /**
   * Collection Class - http://backbonejs.org/#Collection
   */
  declare class Collection<TModel> mixins Events {
    static extend<P, CP>(instanceProperies: P, classProperties?: CP): Class<Collection<*> & P> & CP;
    constructor(models?: TModel, options?: Object): this;
    initialize(models?: TModel, options?: Object): this;
    model: TModel;
    modelId(attributes: TModel): string;
    models: TModel[];
    toJSON(options?: Object): TModel[];
    sync: sync;
    // Underscore Methods
    // @TODO should be defined by the underscore library defintion and not as generic functions.
    forEach: Function; //(each)
    map: Function; //(collect)
    reduce: Function; // (foldl, inject)
    reduceRight: Function; //(foldr)
    find: Function; // (detect)
    findIndex: Function;
    findLastIndex: Function;
    filter: Function; //(select)
    reject: Function;
    every: Function; //(all)
    some: Function; //(any)
    contains: Function; //(includes)
    invoke: Function;
    max: Function;
    min: Function;
    sortBy: Function;
    groupBy: Function;
    shuffle: Function;
    toArray: Function;
    size: Function;
    first: Function; //(head, take)
    initial: Function;
    rest: Function; //(tail, drop)
    last: Function;
    without: Function;
    indexOf: Function;
    lastIndexOf: Function;
    isEmpty: Function;
    chain: Function;
    difference: Function;
    sample: Function;
    partition: Function;
    countBy: Function;
    indexBy: Function;
    // end underscore methods
    add(models: Array<TModel>, options?: Object): void;
    remove(models: Array<TModel>, options?: Object): void;
    reset(models?: Array<TModel>, options?: Object): void;
    set(models: Array<TModel>, options?: Object): void;
    get(id: string): ?TModel;
    at(index: number): ?TModel;
    push(model: TModel, options?: Object): void;
    pop(otions?: Object): void;
    unshift(model: TModel, options?: Object): void;
    unshift(model: TModel, options?: Object): void;
    shift(options?: Object): TModel;
    slice(begin: number, end: number): Array<TModel>;
    length: number;
    sort(options?: Object): Array<TModel>;
    pluck(attribute: string): Array<TModel>;
    where(attributes: {[attributeName: string]: mixed}): Array<TModel>;
    findWhere(attributes: {[attributeName: string]: mixed}): TModel;
    url: () => string | string;
    parse(response: Object, options: Object): Object;
    clone(): this;
    fetch(options?: Object): void;
    create(attributes: Object, options?: Object): void;
  }


  /**
   * Router Class http://backbonejs.org/#Router
   */
  declare class Router mixins Events {
      static extend<P, CP>(instanceProperies: P, classProperties?: CP): Class<Router & P> & CP;
      routes: {
        [route: string]: string | ((e: Event) => mixed | void);
      };
      constructor(options?: Object): this;
      initialize(options?: Object): this;
      route(route: string, name: string, callback?: (e: Event) => mixed | void): this;
      navigate(fragment: string, options?: { trigger?: boolean, replace?:  boolean}): this;
      execute(callback: Function, args: Array<mixed>, name: string): void | mixed;
  }

  /**
   * History - http://backbonejs.org/#History
   */
  declare class History mixins Events {
    static extend<P, CP>(instanceProperies: P, classProperties?: CP): Class<History & P> & CP;
    static started: boolean;
    constructor(options?: Object): this;
    initialize(options?: Object): this;
    start(options?: { pushState?: boolean, hashChange?: boolean, root?: string}): this;
  }
  declare var history: History;

  /**
   * Sync - http://backbonejs.org/#Sync
   */
  declare function sync(method: CRUDMethod, model: Model, options?: Object):  any; // Should really be a jQuery XHR.
  declare function ajax(request: Object): any;
  declare var emulateHTTP: boolean;
  declare var emulateJSON: boolean;

  /**
   * View -
   */
  declare type AttributesHasMap = {
      [attribute: string]: mixed
  };
  declare type EventsHash = {
      [event: string]: string | Function
  };
  declare class View mixins Events {
    static extend<P, CP>(instanceProperies: P, classProperties?: CP): Class<View & P> & CP;
    constructor(): this;
    initialize(options?: Object): this;
    el: HTMLElement | string;
    $el: any;
    setElement(el: HTMLElement): this;
    attributes: AttributesHasMap | () => AttributesHasMap;
    $: typeof $;
    template(data: Object): string;
    render(): this | mixed;
    remove(): this;
    events: EventsHash | () => EventsHash;
    delegateEvents(events?: EventsHash): this;
    undelegateEvents(): this;
  }

  /**
   * Declaring the export for backbone as well.
   */
  declare class Backbone {
    Events: typeof Events;
    Model: typeof Model;
    Collection: typeof Collection;
    Router: typeof Router;
    History: typeof History;
    history: typeof history;
    View: typeof View;

    // Sync
    sync: typeof sync;
    ajax: typeof ajax;
    emulateHTTP: typeof emulateHTTP;
    emulateJSON: typeof emulateJSON;


    // Utilty
    $: typeof $; // @TODO this is no correct, but it is difficult to require another definition from here.
    _: typeof _; // @TODO this is no correct, but it is difficult to require another definition from here.
    version: typeof version;
    noConflict(): this;
  }

  declare var exports: Backbone;
}
