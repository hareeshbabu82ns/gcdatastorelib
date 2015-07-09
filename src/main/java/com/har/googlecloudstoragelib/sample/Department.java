package com.har.googlecloudstoragelib.sample;

import com.google.api.services.datastore.DatastoreV1.CommitRequest;
import com.google.api.services.datastore.DatastoreV1.Entity;
import com.google.api.services.datastore.DatastoreV1.Key;
import com.google.api.services.datastore.DatastoreV1.Mutation;
import com.google.api.services.datastore.client.Datastore;
import com.google.api.services.datastore.client.DatastoreException;

import java.util.List;

import static com.google.api.services.datastore.client.DatastoreHelper.makeKey;
import static com.google.api.services.datastore.client.DatastoreHelper.makeProperty;
import static com.google.api.services.datastore.client.DatastoreHelper.makeValue;

/**
 * Created by hareesh on 7/7/15.
 */
public class Department {
  public static final String TABLE_KIND = "department";
  public static final String COLUMN_NAME = "name";
  public static final String COLUMN_DESCRIPTION = "description";

  public String name;
  public String description;


  public static final List<Key> create(Datastore datastore, Department... depts)
      throws DatastoreException {
    final Mutation.Builder mutation = Mutation.newBuilder();
    for (Department dept : depts) {
      final Entity entity = Entity.newBuilder()
          .addProperty(makeProperty(COLUMN_NAME, makeValue(dept.name)))
          .addProperty(makeProperty(COLUMN_DESCRIPTION, makeValue(dept.description)))
          .setKey(makeKey(TABLE_KIND)).build();
      mutation.addInsertAutoId(entity);
    }
    final CommitRequest request = CommitRequest.newBuilder()
        .setMutation(mutation)
        .setMode(CommitRequest.Mode.NON_TRANSACTIONAL)
        .build();
    return datastore.commit(request).getMutationResult().getInsertAutoIdKeyList();
  }

  public static final Key create(Datastore datastore, Department dept)
      throws DatastoreException {
    return create(datastore, dept.name, dept.description);
  }

  public static final Key create(Datastore datastore, String name, String description)
      throws DatastoreException {
    final Entity entity = Entity.newBuilder().addProperty(makeProperty(COLUMN_NAME, makeValue(name)))
        .addProperty(makeProperty(COLUMN_DESCRIPTION, makeValue(description)))
        .setKey(makeKey(TABLE_KIND)).build();
    final CommitRequest request = CommitRequest.newBuilder()
        .setMutation(Mutation.newBuilder().addInsertAutoId(entity))
        .setMode(CommitRequest.Mode.NON_TRANSACTIONAL)
        .build();
    return datastore.commit(request).getMutationResult().getInsertAutoIdKey(0);
  }
}
