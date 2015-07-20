package com.har.googlecloudstoragelib;

import com.google.api.services.datastore.DatastoreV1;
import com.google.api.services.datastore.client.DatastoreException;

/**
 * Created by hareesh on 20/07/15.
 */
public interface IDatastoreEntity {

  void delete() throws DatastoreException;

  void update() throws DatastoreException;

  DatastoreV1.Key create() throws DatastoreException;

  DatastoreV1.Entity.Builder buildEntity();

  DatastoreV1.Key.Builder buildKey();

  void setValue(String name, DatastoreV1.Value value);

  DatastoreV1.Value getValue(String name);
}
