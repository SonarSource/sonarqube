// flow-typed signature: 2a37b992d69aa44091bbce792686c327
// flow-typed version: 3246e0e09e/underscore_v1.x.x/flow_>=v0.38.x

// @flow
/* eslint-disable */

// type definitions for (some of) underscore

type FnIteratee<T> = (t: T, index: number, array: Array<T>) => boolean;

declare module "underscore" {
  declare type UnaryFn<A, R> = (a: A) => R;
  declare type Compose =
    & (<A, B, C, D, E, F, G>(
      fg: UnaryFn<F, G>,
      ef: UnaryFn<E, F>,
      de: UnaryFn<D, E>,
      cd: UnaryFn<C, D>,
      bc: UnaryFn<B, C>,
      ab: UnaryFn<A, B>,
      ...rest: Array<void>
    ) => UnaryFn<A, G>)
    & (<A, B, C, D, E, F>(
      ef: UnaryFn<E, F>,
      de: UnaryFn<D, E>,
      cd: UnaryFn<C, D>,
      bc: UnaryFn<B, C>,
      ab: UnaryFn<A, B>,
      ...rest: Array<void>
    ) => UnaryFn<A, F>)
    & (<A, B, C, D, E>(
      de: UnaryFn<D, E>,
      cd: UnaryFn<C, D>,
      bc: UnaryFn<B, C>,
      ab: UnaryFn<A, B>,
      ...rest: Array<void>
    ) => UnaryFn<A, E>)
    & (<A, B, C, D>(cd: UnaryFn<C, D>, bc: UnaryFn<B, C>, ab: UnaryFn<A, B>, ...rest: Array<void>) => UnaryFn<A, D>)
    & (<A, B, C>(bc: UnaryFn<B, C>, ab: UnaryFn<A, B>, ...rest: Array<void>) => UnaryFn<A, C>)
    & (<A, B>(ab: UnaryFn<A, B>, ...rest: Array<void>) => UnaryFn<A, B>)

  declare function $underscore$Extend<A: {}>(a: A, ...rest: Array<void>): A;
  declare function $underscore$Extend<A: {}, B: {}>(a: A, b: B, ...rest: Array<void>): A & B;
  declare function $underscore$Extend<A: {}, B: {}, C: {}>(a: A, b: B, c: C, ...rest: Array<void>): A & B & C;
  declare function $underscore$Extend<A: {}, B: {}, C: {}, D: {}>(a: A, b: B, c: C, d: D, ...rest: Array<void>): A & B & C & D;
  declare function $underscore$Extend<A: {}, B: {}, C: {}, D: {}, E: {}>(a: A, b: B, c: C, d: D, e: E, ...rest: Array<void>): A & B & C & D & E;

  declare function $underscore$ExtendParameterized<A: {}>(...rest: Array<void>): A;
  declare function $underscore$ExtendParameterized<A: {}, B: {}>(b: B, ...rest: Array<void>): A & B;
  declare function $underscore$ExtendParameterized<A: {}, B: {}, C: {}>(b: B, c: C, ...rest: Array<void>): A & B & C;
  declare function $underscore$ExtendParameterized<A: {}, B: {}, C: {}, D: {}>(b: B, c: C, d: D, ...rest: Array<void>): A & B & C & D;
  declare function $underscore$ExtendParameterized<A: {}, B: {}, C: {}, D: {}, E: {}>(b: B, c: C, d: D, e: E, ...rest: Array<void>): A & B & C & D & E;


  declare var compose: Compose;

  // Handle underscore chainables things.
  declare class UnderscoreWrappedList<T> {
    // Chain
    chain(): UnderscoreChainedList<T>;

    // Handle Collections functions
    each(iteratee: (element: T, index?: number, list?: Array<T>) => void): void;
    each(iteratee: (val: mixed, key: mixed, list?: Object) => void): void;
    map<U>(iteratee: (value: T, index: number, list: Array<T>) => U): Array<U>;
    reduce<U>(iteratee: (memo: Object, value: T, index: number) => U, init: Object): Object;
    reduce<U>(iteratee: (memo: Array<U>, value: T, index: number) => U, init: Array<U>): Array<U>;
    reduce<U>(iteratee: (memo: U, value: T, index: number) => U, init: U): U;
    reduceRight<U>(iteratee: (memo: Object, value: T, index: number) => U, init: Object): Object;
    reduceRight<U>(iteratee: (memo: Array<U>, value: T, index: number) => U, init: Array<U>): Array<U>;
    reduceRight<U>(iteratee: (memo: U, value: T, index: number) => U, init: U): U;
    find(predicate: (value: T) => boolean): T;
    filter(predicate: (value: ?T) => boolean): Array<T>;
    where(properties: Object): Array<T>;
    findWhere(properties: $Shape<T>): ?T;
    reject(predicate: (value: T) => boolean, context?: mixed): Array<T>;
    every(predicate: (value: T) => boolean, context?: mixed): boolean;
    some(predicate: (value: T) => boolean, context?: mixed): boolean;
    contains(value: T, fromIndex?: number): boolean;
    invoke<U>(methodName: string, ...args: Array<any>): Array<U>;
    pluck<U>(propertyName: string): Array<U>;
    max<U>(iteratee?: (value: T) => number, context?: mixed): U;
    min<U>(iteratee?: (value: T) => number, context?: mixed): U;
    sortBy<U>(iteratee: (value: T) => number): Array<U>;
    sortBy<U>(iteratee: string): Array<U>;
    // TODO: UnderscoreWrapTheseObjects
    groupBy<U>(iteratee: (value: T) => U, context?: mixed): { [key: U]: T };
    groupBy(iteratee: string, context?: mixed): { [key: any]: T };
    indexBy<U>(iteratee: (value: T) => U, context?: mixed): { [key: U]: T };
    indexBy(iteratee: string, context?: mixed): { [key: any]: T };
    countBy<U>(iteratee: (value: T) => U, context?: mixed): { [key: U]: T };
    shuffle(): Array<T>;
    sample(n?: number): Array<T>;
    toArray(): Array<T>;
    size(): number;
    partition(predicate: (value: T) => boolean): Array<Array<T>>;

    // Handle Array function
    first(): T;
    first(n: number): Array<T>;
    head(n?: number): Array<T>;
    take(n?: number): Array<T>;
    initial(n?: number): Array<T>;
    last(n?: number): Array<T>;
    rest(index?: number): Array<T>;
    tail(index?: number): Array<T>;
    drop(index?: number): Array<T>;
    compact(): Array<T>;
    flatten(shallow?: boolean): Array<T>;
    without(...values: Array<any>): Array<T>;
    union(...arrays: Array<any>): Array<T>;
    intersection(...arrays: Array<any>): Array<T>;
    difference(...others: Array<any>): Array<T>;
    uniq(): Array<T>;
    uniq(iteratee: FnIteratee<T>): Array<T>;
    uniq(isSorted: boolean, iteratee?: FnIteratee<T>): Array<T>;
    unique(): Array<T>;
    unique(iteratee: FnIteratee<T>): Array<T>;
    unique(isSorted: boolean, iteratee?: FnIteratee<T>): Array<T>;
    zip(...arrays: Array<any>): Array<T>;
    unzip(): Array<T>;
    object(values?: Array<any>): Object;
    indexOf(value: T, isSorted?: boolean): number;
    lastIndexOf(value: T, iteratee?: Function, context?: mixed): number;
    sortedIndex(value: T, iteratee?: Function, context?: mixed): number;
    findIndex(predicate: (value: T) => boolean, context?: mixed): number;
    findLastIndex(predicate: (value: T) => boolean, context?: mixed): number;
    range(stop: number, step?: number): Array<number>;
    range(): Array<number>;

    // TODO _.propertyOf
    // TODO _.matcher, _.matches
    isEqual(b: Object): boolean;
    isMatch(b: Object): boolean;
    isEmpty(): boolean;
    isElement(): boolean;
    isArray(): boolean;
    isObject(): boolean;
    isArguments(): boolean;
    isFunction(): boolean;
    isString(): boolean;
    isNumber(): boolean;
    isFinite(): boolean;
    isBoolean(): boolean;
    isDate(): boolean;
    isRegExp(): boolean;
    isError(): boolean;
    isNaN(): boolean;
    isNull(): boolean;
    isUndefined(): boolean;
  }


  // Handle underscore chainables things.
  declare class UnderscoreChainedList<T> {
    // End a Chain
    value(): Array<T>;

    // Handle Collections functions
    each(iteratee: (element: T, index: number, list: Array<T>) => void): Array<T>;
    each(iteratee: (val: mixed, key: mixed, list: Object) => void): Object;
    map<U>(iteratee: (value: T, index: number, list: Array<T>) => U): UnderscoreChainedList<U>;
    reduce<U>(iteratee: (memo: Object, value: T, index: number) => U, init: Object): UnderscoreChainedValue<Object>;
    reduce<U>(iteratee: (memo: Array<U>, value: T, index: number) => U, init: Array<U>): UnderscoreChainedList<U>;
    reduce<U>(iteratee: (memo: U, value: T, index: number) => U, init: U): UnderscoreChainedValue<U>;
    reduceRight<U>(iteratee: (memo: Object, value: T, index: number) => U, init: Object): UnderscoreChainedValue<U>;
    reduceRight<U>(iteratee: (memo: Array<U>, value: T, index: number) => U, init: Array<U>): UnderscoreChainedList<U>;
    reduceRight<U>(iteratee: (memo: U, value: T, index: number) => U, init: U): UnderscoreChainedValue<U>;
    find(predicate: (value: T) => boolean): UnderscoreChainedValue<T>;
    filter(predicate: (value: T) => boolean): UnderscoreChainedList<T>;
    where(properties: Object): UnderscoreChainedList<T>;
    findWhere(properties: $Shape<T>): ?UnderscoreChainedValue<T>;
    reject(predicate: (value: T) => boolean, context?: mixed): UnderscoreChainedList<T>;
    every(predicate: (value: T) => boolean, context?: mixed): UnderscoreChainedValue<boolean>;
    some(predicate: (value: T) => boolean, context?: mixed): UnderscoreChainedValue<boolean>;
    contains(value: T, fromIndex?: number): UnderscoreChainedValue<boolean>;
    invoke<U>(methodName: string, ...args: Array<any>): UnderscoreChainedList<U>;
    pluck<U>(propertyName: string): UnderscoreChainedList<U>;
    max<U>(iteratee?: (value: T) => number, context?: mixed): UnderscoreChainedValue<U>;
    min<U>(iteratee?: (value: T) => number, context?: mixed): UnderscoreChainedValue<U>;
    sortBy<U>(iteratee: (value: T) => number): UnderscoreChainedList<U>;
    sortBy<U>(iteratee: string): UnderscoreChainedList<U>;
    // TODO: UnderscoreWrapTheseObjects
    groupBy<U>(iteratee: (value: T) => U, context?: mixed): UnderscoreChainedValue<{ [key: U]: T }>;
    groupBy(iteratee: string, context?: mixed): UnderscoreChainedValue<{ [key: any]: T }>;
    indexBy<U>(iteratee: (value: T) => U, context?: mixed): UnderscoreChainedValue<{ [key: U]: T }>;
    indexBy(iteratee: string, context?: mixed): UnderscoreChainedValue<{ [key: any]: T }>;
    countBy<U>(iteratee: (value: T) => U, context?: mixed): UnderscoreChainedValue<{ [key: U]: T }>;
    shuffle(): UnderscoreChainedList<T>;
    sample(n?: number): UnderscoreChainedList<T>;
    toArray(): UnderscoreChainedList<T>;
    size(): UnderscoreChainedValue<number>;
    partition(predicate: (value: T) => boolean): UnderscoreChainedList<Array<T>>;

    // Handle Array function
    first(n: number): UnderscoreChainedList<T>;
    first(): UnderscoreChainedValue<T>;
    head(n?: number): UnderscoreChainedList<T>;
    take(n?: number): UnderscoreChainedList<T>;
    initial(n?: number): UnderscoreChainedList<T>;
    last(n?: number): UnderscoreChainedList<T>;
    rest(index?: number): UnderscoreChainedList<T>;
    tail(index?: number): UnderscoreChainedList<T>;
    drop(index?: number): UnderscoreChainedList<T>;
    compact(): UnderscoreChainedList<T>;
    flatten(shallow?: boolean): UnderscoreChainedList<T>;
    without(...values: Array<any>): UnderscoreChainedList<T>;
    union(...arrays: Array<any>): UnderscoreChainedList<T>;
    intersection(...arrays: Array<any>): UnderscoreChainedList<T>;
    difference(...others: Array<any>): UnderscoreChainedList<T>;
    uniq(): UnderscoreChainedList<T>;
    uniq(iteratee: FnIteratee<T>): UnderscoreChainedList<T>;
    uniq(isSorted: boolean, iteratee?: FnIteratee<T>): UnderscoreChainedList<T>;
    unique(): UnderscoreChainedList<T>;
    unique(iteratee: FnIteratee<T>): UnderscoreChainedList<T>;
    unique(isSorted: boolean, iteratee?: FnIteratee<T>): UnderscoreChainedList<T>;
    zip(...arrays: Array<any>): UnderscoreChainedList<T>;
    unzip(): UnderscoreChainedList<T>;
    object(values?: Array<any>): UnderscoreChainedValue<Object>;
    indexOf(value: T, isSorted?: boolean): UnderscoreChainedValue<number>;
    lastIndexOf(value: T, iteratee?: Function, context?: mixed): UnderscoreChainedValue<number>;
    sortedIndex(value: T, iteratee?: Function, context?: mixed): UnderscoreChainedValue<number>;
    findIndex(predicate: (value: T) => boolean, context?: mixed): UnderscoreChainedValue<number>;
    findLastIndex(predicate: (value: T) => boolean, context?: mixed): UnderscoreChainedValue<number>;
    range(stop: number, step?: number): UnderscoreChainedList<number>;
    range(): UnderscoreChainedList<number>;

    isEqual(b: Object): UnderscoreChainedValue<boolean>;
    isMatch(b: Object): UnderscoreChainedValue<boolean>;
    isEmpty(): UnderscoreChainedValue<boolean>;
    isElement(): UnderscoreChainedValue<boolean>;
    isArray(): UnderscoreChainedValue<boolean>;
    isObject(): UnderscoreChainedValue<boolean>;
    isArguments(): UnderscoreChainedValue<boolean>;
    isFunction(): UnderscoreChainedValue<boolean>;
    isString(): UnderscoreChainedValue<boolean>;
    isNumber(): UnderscoreChainedValue<boolean>;
    isFinite(): UnderscoreChainedValue<boolean>;
    isBoolean(): UnderscoreChainedValue<boolean>;
    isDate(): UnderscoreChainedValue<boolean>;
    isRegExp(): UnderscoreChainedValue<boolean>;
    isError(): UnderscoreChainedValue<boolean>;
    isNaN(): UnderscoreChainedValue<boolean>;
    isNull(): UnderscoreChainedValue<boolean>;
    isUndefined(): UnderscoreChainedValue<boolean>;
  }

  declare class UnderscoreChainedValue<T> {
    value(): T;
  }

  declare class UnderscoreWrappedValue<T> {
    chain(): UnderscoreChainedValue<T>;

    escape(): string;
    // TODO: Probably move this to UnderscoreWrappedNumber or something
    range(): Array<number>;
    isEmpty(): boolean;
  }

  // Handle regular things.
  declare class UnderscoreList {
    // Handle chaining
    chain<T>(a: Array<T>): UnderscoreChainedList<T>;
    chain<T>(v: T): UnderscoreChainedValue<T>;

    // Handle Collections functions

    each<T>(o: {[key:string]: T}, iteratee: (val: T, key: string)=>void): void;
    each<T>(a: T[], iteratee: (val: T, key: string)=>void): void;
    each(list: Object, iteratee: (val: mixed, key: mixed, list: Object) => void): void;
    forEach<T>(o: {[key:string]: T}, iteratee: (val: T, key: string)=>void): void;
    forEach<T>(a: T[], iteratee: (val: T, key: string)=>void): void;

    map<T, U>(a: T[], iteratee: (val: T, n: number)=>U): U[];
    map<K, T, U>(a: {[key:K]: T}, iteratee: (val: T, k: K)=>U): U[];
    collect<T, U>(a: T[], iteratee: (val: T, n: number)=>U): U[];
    collect<K, T, U>(a: {[key:K]: T}, iteratee: (val: T, k: K)=>U): U[];

    reduce<T, MemoT>(a: Array<T>, iterator: (m: MemoT, o: T)=>MemoT, initialMemo?: MemoT): MemoT;
    inject<T, MemoT>(a: Array<T>, iterator: (m: MemoT, o: T)=>MemoT, initialMemo?: MemoT): MemoT;
    foldl<T, MemoT>(a: Array<T>, iterator: (m: MemoT, o: T)=>MemoT, initialMemo?: MemoT): MemoT;

    reduceRight<T, MemoT>(a: Array<T>, iterator: (m: MemoT, o: T)=>MemoT, initialMemo?: MemoT): MemoT;
    foldr<T, MemoT>(a: Array<T>, iterator: (m: MemoT, o: T)=>MemoT, initialMemo?: MemoT): MemoT;

    find<T>(list: T[], predicate: (val: T)=>boolean): ?T;
    detect<T>(list: T[], predicate: (val: T)=>boolean): ?T;

    filter<T>(o: {[key:string]: T}, pred: (val: T, k: string)=>boolean): T[];
    filter<T>(a: T[], pred: (val: T, k: string)=>boolean): T[];
    select<T>(o: {[key:string]: T}, pred: (val: T, k: string)=>boolean): T[];
    select<T>(a: T[], pred: (val: T, k: string)=>boolean): T[];
    where<T>(list: Array<T>, properties: Object): Array<T>;
    findWhere<T>(list: Array<T>, properties: {[key:string]: any}): ?T;

    reject<T>(o: {[key:string]: T}, pred: (val: T, k: string)=>boolean): T[];
    reject<T>(a: T[], pred: (val: T, k: string)=>boolean): T[];

    every<T>(a: Array<T>, pred?: (val: T)=>boolean): boolean;
    all<T>(a: Array<T>, pred?: (val: T)=>boolean): boolean;

    some<T>(a: Array<T>, pred?: (val: T)=>boolean): boolean;
    any<T>(a: Array<T>, pred?: (val: T)=>boolean): boolean;

    contains<T>(list: T[], val: T, fromIndex?: number): boolean;
    includes<T>(list: T[], val: T, fromIndex?: number): boolean;

    invoke(list: Array<any>, methodName: string, ...args?: Array<any>): any;

    pluck(a: Array<any>, propertyName: string): Array <any>;

    max<T>(a: Array<T>|{[key:any]: T}): T;
    min<T>(a: Array<T>|{[key:any]: T}): T;

    sortBy<T>(a: T[], property: any): T[];
    sortBy<T>(a: T[], iteratee: (val: T)=>any): T[];
    groupBy<T>(a: Array<T>, iteratee: string | (val: T, index: number)=>any): {[key:string]: T[]};
    indexBy<T>(a: Array<T>, iteratee: string | (val: T, index: number)=>any): {[key:string]: T[]};
    countBy<T>(a: Array<T>, iteratee: (val: T, index: number)=>any): {[key:string]: T[]};
    shuffle<T>(list: ?Array<T>): Array<T>;
    sample<T>(a: T[]): T;

    toArray<T>(a: Iterable<T>|{[key:any]: T}): Array<T>;

    size(o: Object): number;
    size(o: Array<any>): number;

    partition<T>(o: {[key:string]: T}, pred: (val: T, k: string)=>boolean): [T[], T[]];
    partition<T>(o: Array<T>, pred: (val: T)=>boolean): [T[], T[]];

    // Handle Array function

    first<T>(a: Array<T>, n: number): Array<T>;
    first<T>(a: Array<T>): T;
    head<T>(a: Array<T>, n: number): Array<T>;
    head<T>(a: Array<T>): T;
    take<T>(a: Array<T>, n: number): Array<T>;
    take<T>(a: Array<T>): T;

    initial<T>(a: Array<T>, n?: number): Array<T>;

    last<T>(a: Array<T>, n: number): Array<T>;
    last<T>(a: Array<T>): T;

    rest<T>(a: Array<T>, index?: number): Array<T>;
    tail<T>(a: Array<T>, index?: number): Array<T>;
    drop<T>(a: Array<T>, index?: number): Array<T>;

    compact<T>(a: Array<?T>): T[];

    flatten<S>(a: S[][]): S[];

    without<T>(a: T[], ...values: T[]): T[];

    union<T>(...arrays: Array<Array<T>>): Array<T>;
    intersection<T>(...arrays: Array<Array<T>>): Array<T>;

    difference<T>(array: Array<T>, ...others: Array<Array<T>>): Array<T>;

    uniq<T>(a: T[]): T[];
    uniq<T>(list: Array<T>, iteratee: Function): Array<T>;
    uniq<T>(list: Array<T>, isSorted: boolean, iteratee?: Function): Array<T>;
    unique<T>(a: T[]): T[];
    unique<T>(list: Array<T>, iteratee: Function): Array<T>;
    unique<T>(list: Array<T>, isSorted: boolean, iteratee?: Function): Array<T>;

    zip<S, T>(a1: S[], a2: T[]): Array<[S, T]>;
    unzip(array: Array<Array<any>>): Array<Array<any>>;

    object<T>(a: Array<[string, T]>): {[key:string]: T};
    object<T>(list: Array<string>, values?: Array<T>): {[key: string]: T};

    indexOf<T>(list: T[], val: T, isSorted?: boolean): number;
    lastIndexOf<T>(array: Array<T>, value: T, fromIndex?: number): number;
    sortedIndex<T>(list: Array<T>, value: T, iteratee?: (value: T) => mixed, context?: any): number;
    findIndex<T>(list: T[], predicate: (val: T)=>boolean): number;
    findLastIndex<T>(array: Array<T>, predicate: any, context?: any): number;
    range(a: number, b: number): Array<number>;

    isEqual(object: Object, b: Object): boolean;
    isMatch(object: Object, b: Object): boolean;
    isEmpty(object: Object): boolean;
    isElement(object: any): boolean;
    isArray(value: any): boolean;
    isObject(value: any): boolean;
    isArguments(object: any): boolean;
    isFunction(object: any): boolean;
    isString(object: any): boolean;
    isNumber(object: any): boolean;
    isFinite(object: any): boolean;
    isBoolean(object: any): boolean;
    isDate(object: any): boolean;
    isRegExp(object: any): boolean;
    isError(object: any): boolean;
    isNaN(object: any): boolean;
    isNull(object: any): boolean;
    isUndefined(object: any): boolean;
  }

  declare class UnderscoreFunctions {
    bind(func: Function, object: Object, ...arguments: Array<any>): Function;
    bindAll(object: ?Object, ...methodNames: Array<string | [string]>): Object;
    partial(func: Function, ...arguments: Array<any>): Function;
    memoize(func: Function, hashFunction?: Function): Function;
    delay(func: Function, wait: number, ...arguments: Array<any>): void;
    defer(func: Function, ...arguments: Array<any>): void;
    throttle(func: Function, wait: number, options?: {leading: boolean, trailing: boolean}): Function;
    debounce(func: Function, wait: number, immediate?: boolean): Function;
    once(func: Function): Function;
    after(count: number, func: Function): Function;
    before(count: number, func: Function): Function;
    wrap(func: Function, wrapper: Function): Function;
    negate(predicate: (...args: any) => boolean): Function;
    compose: Compose;

    isEqual(object: any, b: any): boolean;
    isMatch(object: Object, b: Object): boolean;
    isEmpty(object: Object): boolean;
    isElement(object: Object): boolean;
    isArray(value: any): boolean;
    isObject(value: any): boolean;
    isArguments(object: any): boolean;
    isFunction(object: any): boolean;
    isString(object: any): boolean;
    isNumber(object: any): boolean;
    isFinite(object: any): boolean;
    isBoolean(object: any): boolean;
    isDate(object: any): boolean;
    isRegExp(object: any): boolean;
    isError(object: any): boolean;
    isNaN(object: any): boolean;
    isNull(object: any): boolean;
    isUndefined(object: any): boolean;
  }

  declare class UnderscoreObject {
    keys<K, V>(object: {[keys: K]: V}): Array<K>;
    allKeys<K, V>(object: {[keys: K]: V}): Array<K>;
    values<K, V>(object: {[keys: K]: V}): Array<V>;
    mapObject(
      object: Object,
      iteratee: (val: any, key: string) => Object,
      context?: mixed
    ): Object;
    pairs<K, V>(object: {[keys: K]: V}): Array<[K, V]>;
    invert<K, V>(object: {[keys: K]: V}): {[keys: V]: K};
    // TODO: _.create
    functions(object: Object): Array<string>;
    findKey(object: Object, predicate: (...args: Array<any>) => boolean, context?: mixed): ?string;
    extend: typeof $underscore$Extend;
    extendOwn: typeof $underscore$Extend;
    pick<K, V>(object: {[keys: K]: V}, predicate?: K): {[keys: K]: V};
    omit<K, V>(object: {[keys: K]: V}, predicate?: K): {[keys: K]: V};
    defaults<K, V>(defaults: {[keys: K]: V}, more: {[keys: K]: V}): {[keys: K]: V};
    clone<O: {}>(object: O): O;
    tap<O>(object: O): O;
    has(object: Object, key: string): boolean;
    property<K: string, V>(key: K): (obj: { [key: K]: V }) => V;
    // TODO _.propertyOf
    // TODO _.matcher, _.matches
    isEqual(object: Object, b: Object): boolean;
    isMatch(object: Object, b: Object): boolean;
    isEmpty(object: Object): boolean;
    isElement(object: any): boolean;
    isArray(value: any): boolean;
    isObject(value: any): boolean;
    isArguments(object: any): boolean;
    isFunction(object: any): boolean;
    isString(object: any): boolean;
    isNumber(object: any): boolean;
    isFinite(object: any): boolean;
    isBoolean(object: any): boolean;
    isDate(object: any): boolean;
    isRegExp(object: any): boolean;
    isError(object: any): boolean;
    isNaN(object: any): boolean;
    isNull(object: any): boolean;
    isUndefined(object: any): boolean;
  }

  declare class UnderscoreChainedObject<WrappedObj: {}> {
    value(): WrappedObj;
    keys(): UnderscoreChainedList<$Keys<WrappedObj>>;
    allKeys(): UnderscoreChainedList<$Keys<WrappedObj>>;
    // This call necessarily loses precision since we treat chained lists generics of a single type.
    values(): UnderscoreChainedList<any>;

    mapObject<Ret, NewObj: $ObjMap<WrappedObj, <V>(v: V) => Ret>>(
      iteratee: Function,
      context?: mixed
    ): UnderscoreChainedObject<NewObj>;
    map<T>(mapFn: (v: any, k: $Keys<WrappedObj>, obj: WrappedObj) => T): UnderscoreChainedList<T>;
    pairs(): UnderscoreChainedList<[any, any]>;
    invert(): UnderscoreChainedObject<WrappedObj>;
    // TODO: _.create
    functions(): UnderscoreChainedList<string>;
    findKey(predicate: (...args: Array<any>) => boolean, context?: mixed): UnderscoreChainedValue<?string>;
    // TODO: Reimplement these when you can get them to return UnderscoreChainedObject
    // extend: ExtendParameterized<{[key: K]: V}>;
    // extendOwn: ExtendParameterized<{[key: K]: V}>>;
    pick(...rest: Array<string | Array<string>>): UnderscoreChainedObject<WrappedObj>;
    pick(predicate: (v: any, k: any, object: WrappedObj) => boolean): UnderscoreChainedObject<WrappedObj>;
    omit(...rest: Array<string | Array<string>>): UnderscoreChainedObject<WrappedObj>;
    omit(predicate: (v: any, k: any, object: WrappedObj) => boolean): UnderscoreChainedObject<WrappedObj>;
    defaults(more: $Shape<WrappedObj>): UnderscoreChainedObject<WrappedObj>;
    clone(): UnderscoreChainedObject<WrappedObj>;
    tap(): UnderscoreChainedObject<WrappedObj>;
    has(key: string): UnderscoreChainedValue<boolean>;
    // TODO _.propertyOf
    // TODO _.matcher, _.matches
    isEqual(b: Object): UnderscoreChainedValue<boolean>;
    isMatch(b: Object): UnderscoreChainedValue<boolean>;
    isEmpty(): UnderscoreChainedValue<boolean>;
    isElement(): UnderscoreChainedValue<boolean>;
    isArray(): UnderscoreChainedValue<boolean>;
    isObject(): UnderscoreChainedValue<boolean>;
    isArguments(): UnderscoreChainedValue<boolean>;
    isFunction(): UnderscoreChainedValue<boolean>;
    isString(): UnderscoreChainedValue<boolean>;
    isNumber(): UnderscoreChainedValue<boolean>;
    isFinite(): UnderscoreChainedValue<boolean>;
    isBoolean(): UnderscoreChainedValue<boolean>;
    isDate(): UnderscoreChainedValue<boolean>;
    isRegExp(): UnderscoreChainedValue<boolean>;
    isError(): UnderscoreChainedValue<boolean>;
    isNaN(): UnderscoreChainedValue<boolean>;
    isNull(): UnderscoreChainedValue<boolean>;
    isUndefined(): UnderscoreChainedValue<boolean>;
  }

  declare class UnderscoreWrappedObject<WrappedObj: {}> {
    chain(): UnderscoreChainedObject<WrappedObj>;

    map<R>(fn: (v: any, k: $Keys<WrappedObj>) => R): Array<R>;
    filter(fn: (v: any, k: $Keys<WrappedObj>, obj: any) => boolean): Array<any>;
    keys(): Array<$Keys<WrappedObj>>;
    allKeys(): Array<$Keys<WrappedObj>>;
    values(): Array<any>;
    mapObject<Ret, NewObj: $ObjMap<WrappedObj, <V>(v: V) => Ret>>(
      iteratee: Function,
      context?: mixed
    ): NewObj;
    pairs<K, V>(): Array<[K, V]>;
    invert<K, V>(): {[keys: V]: K};
    // TODO: _.create
    functions(): Array<string>;
    find(predicate: (v: any, k: $Keys<WrappedObj>, obj: WrappedObj) => boolean): ?any;
    findKey(predicate: (...args: Array<any>) => boolean, context?: mixed): ?string;
    extend: typeof $underscore$ExtendParameterized;
    extendOwn: typeof $underscore$ExtendParameterized;
    // TODO make these actually remove properties
    pick(...rest: Array<string | Array<string>>): WrappedObj;
    pick(predicate: (v: any, k: $Keys<WrappedObj>, object: WrappedObj) => boolean): WrappedObj;
    omit(...rest: Array<string | Array<string>>): WrappedObj;
    omit(predicate: (v: any, k: $Keys<WrappedObj>, object: WrappedObj) => boolean): WrappedObj;
    defaults(more: $Shape<WrappedObj>): WrappedObj;
    clone(): WrappedObj;
    tap(): WrappedObj;
    has(key: string): boolean;
    // TODO _.propertyOf
    // TODO _.matcher, _.matches
    isEqual(b: Object): boolean;
    isMatch(b: Object): boolean;
    isEmpty(): boolean;
    isElement(): boolean;
    isArray(): boolean;
    isObject(): boolean;
    isArguments(): boolean;
    isFunction(): boolean;
    isString(): boolean;
    isNumber(): boolean;
    isFinite(): boolean;
    isBoolean(): boolean;
    isDate(): boolean;
    isRegExp(): boolean;
    isError(): boolean;
    isNaN(): boolean;
    isNull(): boolean;
    isUndefined(): boolean;
  }

  declare class UnderscoreUtility {
    noConflict(): Underscore;
    identity<U>(value: U): U;
    constant<U>(value: U): () => U;
    noop(): void;
    times(n: number, iteratee: Function, context?: mixed): void;
    random(min: number, max: number): number;
    // TODO: Is this right?
    mixin(object: Object): Underscore & Object;
    // TODO: _.iteratee
    uniqueId(prefix?: string): string;
    escape(string: string): string;
    unescape(string: string): string;
    // TODO: _.result
    now(): number;
    template(templateText: string): (values: {[key: string]: string}) => string;
  }

  declare class UnderscoreWrappedList<T> {
    chain(): UnderscoreChainedList<T>;

    // Handle Collections functions
    each(iteratee: (element: T, index: number, list: Array<T>) => void): Array<T>;
    each(iteratee: (val: mixed, key: mixed, list: Object) => void): Object;
    map<U>(iteratee: (value: T, index: number, list: Array<T>) => U): Array<U>;
    reduce<U>(iteratee: (memo: Object, value: T, index?: number) => U, init: Object): Object;
    reduce<U>(iteratee: (memo: Array<U>, value: T, index?: number) => U, init: Array<U>): Array<U>;
    reduce<U>(iteratee: (memo: U, value: T, index?: number) => U, init: U): U;
    reduceRight<U>(iteratee: (memo: Object, value: T, index?: number) => U, init: Object): U;
    reduceRight<U>(iteratee: (memo: Array<U>, value: T, index?: number) => U, init: Array<U>): Array<U>;
    reduceRight<U>(iteratee: (memo: U, value: T, index?: number) => U, init: U): U;
    find(predicate: (value: T) => boolean): ?T;
    filter(predicate: (value: T) => boolean): Array<T>;
    where(properties: Object): Array<T>;
    findWhere(properties: $Shape<T>): ?T;
    reject(predicate: (value: T) => boolean, context?: mixed): Array<T>;
    every(predicate: (value: T) => boolean, context?: mixed): boolean;
    some(predicate: (value: T) => boolean, context?: mixed): boolean;
    contains(value: T, fromIndex?: number): boolean;
    invoke<U>(methodName: string, ...args: Array<any>): Array<U>;
    pluck<U>(propertyName: string): Array<U>;
    max<U>(iteratee?: (value: T) => number, context?: mixed): U;
    min<U>(iteratee?: (value: T) => number, context?: mixed): U;
    sortBy<U>(iteratee: (value: T) => number): Array<U>;
    sortBy<U>(iteratee: string): Array<U>;
    // TODO: UnderscoreWrapTheseObjects
    groupBy<U>(iteratee: (value: T) => U, context?: mixed): { [key: U]: T };
    groupBy(iteratee: string, context?: mixed): { [key: any]: T };
    indexBy<U>(iteratee: (value: T) => U, context?: mixed): { [key: U]: T };
    indexBy(iteratee: string, context?: mixed): { [key: any]: T };
    countBy<U>(iteratee: (value: T) => U, context?: mixed): { [key: U]: T };
    shuffle(): Array<T>;
    sample(n?: number): Array<T>;
    toArray(): Array<T>;
    size(): number;
    partition(predicate: (value: T) => boolean): Array<Array<T>>;

    // Handle Array function
    first(): T;
    first(n: number): Array<T>;
    head(n?: number): Array<T>;
    take(n?: number): Array<T>;
    initial(n?: number): Array<T>;
    last(n?: number): Array<T>;
    rest(index?: number): Array<T>;
    tail(index?: number): Array<T>;
    drop(index?: number): Array<T>;
    compact(): Array<T>;
    flatten(shallow?: boolean): Array<T>;
    without(...values: Array<any>): Array<T>;
    union(...arrays: Array<any>): Array<T>;
    intersection(...arrays: Array<any>): Array<T>;
    difference(...others: Array<any>): Array<T>;
    uniq(): Array<T>;
    uniq(iteratee: Function): Array<T>;
    uniq(isSorted: boolean, iteratee?: Function): Array<T>;
    unique(): Array<T>;
    unique(iteratee: Function): Array<T>;
    unique(isSorted: boolean, iteratee?: Function): Array<T>;
    zip(...arrays: Array<any>): Array<T>;
    unzip(): Array<T>;
    object(values?: Array<any>): Object;
    indexOf(value: T, isSorted?: boolean): number;
    lastIndexOf(value: T, iteratee?: Function, context?: mixed): number;
    sortedIndex(value: T, iteratee?: Function, context?: mixed): number;
    findIndex(predicate: (value: T) => boolean, context?: mixed): number;
    findLastIndex(predicate: (value: T) => boolean, context?: mixed): number;
    range(stop: number, step?: number): Array<number>;
    range(): Array<number>;
    isEmpty(): boolean;
    isEqual(other: Array<T>): boolean;
  }

  // Have to use a type with $call instead of function type because otherwise this will cause us to lose type
  // information. see: https://github.com/facebook/flow/issues/3781
  declare type WrappedExports = {
    $call:
      // A type that can be an object or an array (usually 'any') should have both return types.
      & (<AnyType: {} & []>(arg: AnyType) => UnderscoreWrappedObject<AnyType> & UnderscoreWrappedList<AnyType>)
      // It's important that UnderscoreWrappedObject, UnderscoreWrappedList takes precedence over UnderscoreWrappedValue
      & (<WrappedObj: {}>(arg: WrappedObj) => UnderscoreWrappedObject<WrappedObj>)
      & (<T>(arg: Array<T>) => UnderscoreWrappedList<T>)
      & (<T>(arg: [T]) => UnderscoreWrappedList<T>)
      & (<T>(arg: T) => UnderscoreWrappedValue<T>)
  }

  declare type Underscore =
    & UnderscoreList
    & UnderscoreFunctions
    & UnderscoreObject
    & UnderscoreUtility
    & WrappedExports;

  declare module.exports: Underscore;
}
