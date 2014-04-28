package org.sonar.server.cluster;

import java.io.Serializable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LocalNonBlockingWorkQueue implements WorkQueue{

  private final static int WORKQUEUE_INITIAL_CAPACITY = 20;

  private ConcurrentHashMap<String, Queue<Serializable>> index;
  private ConcurrentHashMap<String, Queue<Serializable>> update;
  private ConcurrentHashMap<String, Queue<Serializable>> delete;

  public LocalNonBlockingWorkQueue(){
    this.index = new ConcurrentHashMap<String, Queue<Serializable>>(WORKQUEUE_INITIAL_CAPACITY);
    this.update = new ConcurrentHashMap<String, Queue<Serializable>>(WORKQUEUE_INITIAL_CAPACITY);
    this.delete = new ConcurrentHashMap<String, Queue<Serializable>>(WORKQUEUE_INITIAL_CAPACITY);
  }

  private Integer enqueue(Map<String, Queue<Serializable>> map, String indexName, Serializable key){
    if(!map.containsKey(indexName)){
      map.put(indexName, new ConcurrentLinkedQueue<Serializable>());
    }
    map.get(indexName).offer(key);
    return 0;
  }

  private Object dequeue(Map<String, Queue<Serializable>> map, String indexName){
    return (map.containsKey(indexName))?
      map.get(indexName).poll():
        null;
  }

  @Override
  public Integer enqueInsert(String indexName, Serializable key) {
    return this.enqueue(index, indexName, key);
  }

  @Override
  public Integer enqueUpdate(String indexName, Serializable key) {
    return this.enqueue(update, indexName, key);
  }

  @Override
  public Integer enqueDelete(String indexName, Serializable key) {
    return this.enqueue(delete, indexName, key);
  }

  @Override
  public Object dequeInsert(String indexName) {
    return this.dequeue(index, indexName);
  }

  @Override
  public Object dequeUpdate(String indexName) {
    return this.dequeue(update, indexName);
  }

  @Override
  public Object dequeDelete(String indexName) {
    return this.dequeue(delete, indexName);
  }

  @Override
  public Status getStatus(Integer workId) {
    // TODO Auto-generated method stub
    return null;
  }

}
