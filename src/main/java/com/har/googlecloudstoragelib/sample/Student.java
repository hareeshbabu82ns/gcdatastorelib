package com.har.googlecloudstoragelib.sample;

import com.google.api.services.datastore.DatastoreV1.CommitRequest;
import com.google.api.services.datastore.DatastoreV1.Entity;
import com.google.api.services.datastore.DatastoreV1.Key;
import com.google.api.services.datastore.DatastoreV1.Mutation;
import com.google.api.services.datastore.client.Datastore;
import com.google.api.services.datastore.client.DatastoreException;
import com.har.googlecloudstoragelib.DatastoreHandler;

import java.util.Date;

import static com.google.api.services.datastore.client.DatastoreHelper.makeKey;
import static com.google.api.services.datastore.client.DatastoreHelper.makeProperty;
import static com.google.api.services.datastore.client.DatastoreHelper.makeValue;

/**
 * Created by hareesh on 7/7/15.
 */
public class Student {
  public static final String TABLE_KIND = "student";
  public static final String COLUMN_NAME = "name";
  public static final String COLUMN_DATE_OF_BIRTH = "dob";
  public static final String COLUMN_DATE_OF_JOINING = "doj";
  public static final String COLUMN_DEPARTMENT = "dept";

  public static final Key create(Datastore datastore,
                                 String name, Date dob,
                                 Date doj, long deptID)
      throws DatastoreException {
    final Entity entity = Entity.newBuilder()
        .addProperty(makeProperty(COLUMN_NAME, makeValue(name)))
        .addProperty(makeProperty(COLUMN_DATE_OF_BIRTH, makeValue(dob)))
        .addProperty(makeProperty(COLUMN_DATE_OF_JOINING, makeValue(doj)))
        .addProperty(makeProperty(COLUMN_DEPARTMENT, makeValue(deptID)))
        .setKey(makeKey(TABLE_KIND)).build();
    final CommitRequest request = CommitRequest.newBuilder()
        .setMutation(Mutation.newBuilder().addInsertAutoId(entity))
        .setMode(CommitRequest.Mode.NON_TRANSACTIONAL)
        .build();
    return datastore.commit(request).getMutationResult().getInsertAutoIdKey(0);
  }

  public static final Entity findByID(long id)
      throws DatastoreException {
    return DatastoreHandler.lookup(makeKey(TABLE_KIND, id).build());
  }

}
