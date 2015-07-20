package com.har.googlecloudstoragelib;

import android.content.Context;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.datastore.DatastoreV1.CommitRequest;
import com.google.api.services.datastore.DatastoreV1.CommitResponse;
import com.google.api.services.datastore.DatastoreV1.Entity;
import com.google.api.services.datastore.DatastoreV1.EntityResult;
import com.google.api.services.datastore.DatastoreV1.Key;
import com.google.api.services.datastore.DatastoreV1.LookupRequest;
import com.google.api.services.datastore.DatastoreV1.LookupResponse;
import com.google.api.services.datastore.DatastoreV1.Mutation;
import com.google.api.services.datastore.DatastoreV1.PartitionId;
import com.google.api.services.datastore.DatastoreV1.PropertyExpression;
import com.google.api.services.datastore.DatastoreV1.PropertyFilter;
import com.google.api.services.datastore.DatastoreV1.PropertyOrder;
import com.google.api.services.datastore.DatastoreV1.PropertyReference;
import com.google.api.services.datastore.DatastoreV1.Query;
import com.google.api.services.datastore.DatastoreV1.QueryResultBatch;
import com.google.api.services.datastore.DatastoreV1.RunQueryRequest;
import com.google.api.services.datastore.DatastoreV1.RunQueryResponse;
import com.google.api.services.datastore.DatastoreV1.Value;
import com.google.api.services.datastore.client.Datastore;
import com.google.api.services.datastore.client.DatastoreException;
import com.google.api.services.datastore.client.DatastoreFactory;
import com.google.api.services.datastore.client.DatastoreHelper;
import com.google.api.services.datastore.client.DatastoreOptions;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import static com.google.api.services.datastore.client.DatastoreHelper.makeFilter;
import static com.google.api.services.datastore.client.DatastoreHelper.makeKey;
import static com.google.api.services.datastore.client.DatastoreHelper.makeOrder;

/**
 * Created by hareesh on 7/7/15.
 */
public class DatastoreHandler {
  public static final String COLUMN_KEY = "__key__";
  private static DatastoreHandler dsHandler;
  private Datastore datastore = null;
  private String mHost;
  private String mDataSetID;
  private String mPartition;
  private PartitionId mPartitionID;
  private String mServiceAccount;

  private DatastoreHandler() {

  }

  public static Datastore getDatastore() {
    if (dsHandler != null && dsHandler.datastore != null)
      return dsHandler.datastore;
    else
      throw new IllegalStateException("Datastore Not initialized");
  }

  public static DatastoreHandler get() {
    if (dsHandler != null)
      return dsHandler;
    else
      throw new IllegalStateException("DatastoreHandler Not initialized");
  }

  public static DatastoreHandler get(Context context,
                                     String host, String dataset,
                                     String partition, String account,
                                     int p12Key)
      throws GeneralSecurityException, IOException {
    dsHandler = new DatastoreHandler();
    dsHandler.mHost = host;
    dsHandler.mServiceAccount = account;
    if (partition != null) {
      dsHandler.mPartition = partition;
      dsHandler.mPartitionID = PartitionId.newBuilder().setNamespace(partition).build();
    }
    dsHandler.mDataSetID = dataset;
    dsHandler.datastore = DatastoreFactory.get().create(buildOptions(context, p12Key));
    return dsHandler;
  }

  public static DatastoreHandler get(Context context, String dataset,
                                     String partition, String account,
                                     int p12Key)
      throws GeneralSecurityException, IOException {
    return get(context, null, dataset, partition, account, p12Key);
  }

  public static Key.Builder makeKeyWithPartition(Object... elements) {
    final Key.Builder builder = makeKey(elements);
    if (dsHandler.mPartitionID != null)
      builder.setPartitionId(dsHandler.mPartitionID);
    return builder;
  }

  public static RunQueryResponse runQueryWithPartition(RunQueryRequest.Builder builder)
      throws DatastoreException {
    if (dsHandler.mPartitionID != null && !builder.hasPartitionId()) {
      builder.setPartitionId(dsHandler.mPartitionID);
    }
    return getDatastore().runQuery(builder.build());
  }

//  public static RunQueryRequest.Builder newRunQueryRequestBuilder() {
//    return DatastoreV1.RunQueryRequest.newBuilder()
//        .setPartitionId(dsHandler.mPartitionID);
//  }

  static DatastoreOptions buildOptions(Context context, int p12Key) throws GeneralSecurityException,
      IOException {
    DatastoreOptions.Builder options = new DatastoreOptions.Builder();
    options.dataset(dsHandler.mDataSetID);
    if (dsHandler.mHost != null)
      options.host(dsHandler.mHost);
    GoogleCredential credential = getServiceAccountCredential(
        dsHandler.mServiceAccount, context.getResources().openRawResource(p12Key));
    options.credential(credential);
    return options.build();
  }

  public static GoogleCredential getServiceAccountCredential(
      String datastoreServiceAccount, InputStream datastore_private_key_file)
      throws GeneralSecurityException, IOException {
    HttpTransport transport = AndroidHttp.newCompatibleTransport();
    JacksonFactory jsonFactory = new JacksonFactory();
    PrivateKey privateKey = SecurityUtils.loadPrivateKeyFromKeyStore(
        SecurityUtils.getPkcs12KeyStore(), datastore_private_key_file, "notasecret",
        "privatekey", "notasecret");
    return new GoogleCredential.Builder()
        .setTransport(transport)
        .setJsonFactory(jsonFactory)
        .setServiceAccountId(datastoreServiceAccount)
        .setServiceAccountScopes(DatastoreOptions.SCOPES)
        .setServiceAccountPrivateKey(privateKey)
        .build();
  }

  public static UpdateBuilder newUpdateBuilder() {
    return new UpdateBuilder();
  }

  public static RunQueryBuilder newQueryBuilder() {
    return new RunQueryBuilder();
  }

  public static final RunQueryResponse findBy(String kind, String column,
                                              PropertyFilter.Operator operator,
                                              Value value) throws DatastoreException {
    final Query.Builder builder = Query.newBuilder();
    builder.addKindBuilder().setName(kind);
    builder.setFilter(makeFilter(column, operator, value));

    RunQueryRequest.Builder request = RunQueryRequest.newBuilder();
    request.setQuery(builder.build());

    return getDatastore().runQuery(request.build());
  }

  /**
   * @param key Key of the object to Lookup
   * @return Entity if found, null if not
   * @throws DatastoreException
   */
  public static final Entity lookup(Key key)
      throws DatastoreException {
    LookupRequest request = LookupRequest.newBuilder().addKey(key).build();
    LookupResponse response = getDatastore().lookup(request);
    if (response.getMissingCount() == 1) {
      return null; //entity not found
    }
    return response.getFound(0).getEntity();
  }

  public static final List<Entity> runQuery(Query query) throws DatastoreException {
    RunQueryRequest.Builder request = RunQueryRequest.newBuilder();
    request.setQuery(query);
    RunQueryResponse response = getDatastore().runQuery(request.build());

    if (response.getBatch().getMoreResults() == QueryResultBatch.MoreResultsType.NOT_FINISHED) {
      System.err.println("WARNING: partial results\n");
    }
    List<EntityResult> results = response.getBatch().getEntityResultList();
    List<Entity> entities = new ArrayList<>(results.size());
    for (EntityResult result : results) {
      entities.add(result.getEntity());
    }
    return entities;
  }

  public static long getID(Key key) {
    long id = 0;
    for (Key.PathElement element : key.getPathElementList()) {
      if (element.hasId()) {
        id = element.getId();
        break;
      }
    }
    return id;
  }

  public static class UpdateBuilder {

    Mutation.Builder mutation = null;

    public UpdateBuilder() {
      mutation = Mutation.newBuilder();
    }

    public UpdateBuilder addUpdate(Entity.Builder entity) {
      mutation.addUpdate(entity);
      return this;
    }

    public UpdateBuilder addUpsert(Entity.Builder entity) {
      mutation.addUpsert(entity);
      return this;
    }

    public UpdateBuilder addDelete(Key key) {
      mutation.addDelete(key);
      return this;
    }

    public CommitResponse commit() throws DatastoreException {
      final CommitRequest request = CommitRequest.newBuilder()
          .setMode(CommitRequest.Mode.NON_TRANSACTIONAL)
          .setMutation(mutation)
          .build();
      return getDatastore().commit(request);
    }
  }

  public static class RunQueryBuilder {
    final Query.Builder queryBuilder;
    RunQueryResponse response = null;

    public RunQueryBuilder() {
      queryBuilder = Query.newBuilder();
    }

    public RunQueryBuilder setKind(String kind) {
      queryBuilder.addKindBuilder().setName(kind);
      return this;
    }

    public Query.Builder getBuilder() {
      return queryBuilder;
    }

    public RunQueryBuilder limit(int limit) {
      queryBuilder.setLimit(limit);
      return this;
    }

    public RunQueryBuilder setFilter(String column, PropertyFilter.Operator operator,
                                     Value value) {
      queryBuilder.setFilter(makeFilter(column, operator, value));
      return this;
    }

    public RunQueryBuilder addProjection(String column) {
      queryBuilder.addProjection(PropertyExpression.newBuilder().setProperty(
          PropertyReference.newBuilder().setName(column)));
      return this;
    }

    public RunQueryBuilder addGroupBy(String column) {
      queryBuilder.addGroupBy(PropertyReference.newBuilder().setName(column));
      return this;
    }

    public RunQueryBuilder addOrder(String column, boolean ascending) {
      queryBuilder.addOrder(makeOrder(column,
          (ascending) ? PropertyOrder.Direction.ASCENDING :
              PropertyOrder.Direction.DESCENDING));
      return this;
    }

    public RunQueryResponse runQuery() throws DatastoreException {
      if (response != null) {
        if (response.getBatch().getMoreResults() ==
            QueryResultBatch.MoreResultsType.NOT_FINISHED) {
          //re-running for next set of results
          queryBuilder.setEndCursor(response.getBatch().getEndCursor());
        } else
          return null;
      }
      final RunQueryRequest.Builder request = RunQueryRequest.newBuilder();
      request.setQuery(queryBuilder.build());

      response = getDatastore().runQuery(request.build());
      return response;
    }

    public List<Entity> runQueryGetEntities() throws DatastoreException {
      if (runQuery() == null)
        return null;

      final List<EntityResult> results = response.getBatch().getEntityResultList();
      final List<Entity> entities = new ArrayList<>(results.size());
      for (EntityResult result : results) {
        entities.add(result.getEntity());
      }
      return entities;
    }

    public boolean isPartialResults() {
      return (response != null && response.getBatch().getMoreResults() ==
          QueryResultBatch.MoreResultsType.NOT_FINISHED);
    }

    public RunQueryResponse getResponse() {
      return response;
    }
  }
}
