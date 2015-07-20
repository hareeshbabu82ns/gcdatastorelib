package com.har.googlecloudstoragelib;

import android.text.TextUtils;

import com.google.api.client.util.Lists;
import com.google.api.services.datastore.DatastoreV1;
import com.google.api.services.datastore.DatastoreV1.*;
import com.google.api.services.datastore.client.DatastoreException;
import com.har.googlecloudstoragelib.annotations.DatastoreEntity;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static com.google.api.services.datastore.client.DatastoreHelper.makeFilter;
import static com.google.api.services.datastore.client.DatastoreHelper.makeKey;
import static com.google.api.services.datastore.client.DatastoreHelper.makeProperty;
import static com.google.api.services.datastore.client.DatastoreHelper.makeValue;

/**
 * Created by hareesh on 17/07/15.
 */
public abstract class BaseDatastoreEntity implements IDatastoreEntity, Serializable {

  public static final String COLUMN_KEY = "__key__";
  public static final String COLUMN_PARENT_KEY = "__p_key__";

  public String mTableKind = null;
  public String mParentTableKind = null;

  public long mID;
  public long mParentID;


  /**
   * New Entity with Parent Hierarchy
   *
   * @param parentID
   */
  public BaseDatastoreEntity(long id, long parentID) {

    fetchKinds();
    if (TextUtils.isEmpty(mTableKind))
      throw new IllegalArgumentException("TableKind can't be empty");
    if (TextUtils.isEmpty(mParentTableKind))
      throw new IllegalArgumentException("Parent TableKind can't be empty");

    mID = id;
    mParentID = parentID;
  }

  /*
   * Existing Entity with given ID
   *
   * @param tableKind
   * @param id
   */
  public BaseDatastoreEntity(long id) {
    fetchKinds();
    if (TextUtils.isEmpty(mTableKind))
      throw new IllegalArgumentException("TableKind can't be empty");
    if (id <= 0)
      throw new IllegalArgumentException("ID can't be empty");

    mID = id;
  }

  /**
   * New Entity with No Parent
   *
   * @param
   */
  public BaseDatastoreEntity() {
    fetchKinds();
    if (TextUtils.isEmpty(mTableKind))
      throw new IllegalArgumentException("TableKind can't be empty");
  }

  public static IDatastoreEntity buildFromEntity(Entity entity,
                                                 Class<? extends BaseDatastoreEntity> clazz) {
    IDatastoreEntity datastoreEntity = null;
    try {
      datastoreEntity = clazz.getDeclaredConstructor().newInstance();
      extractKey(entity.getKey(), datastoreEntity);
      for (DatastoreV1.Property property : entity.getPropertyList()) {
        datastoreEntity.setValue(property.getName(), property.getValue());
      }
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
    return datastoreEntity;
  }

  public static void delete(BaseDatastoreEntity... entities) throws DatastoreException {
    final DatastoreHandler.UpdateBuilder updateBuilder = DatastoreHandler.newUpdateBuilder();
    for (BaseDatastoreEntity entity : entities) {
      updateBuilder.addDelete(entity.buildKey().build());
    }
    updateBuilder.commit();
  }

  public void delete() throws DatastoreException {
    DatastoreHandler.newUpdateBuilder().addDelete(buildKey().build()).commit();
  }

  public static void update(BaseDatastoreEntity... entities) throws DatastoreException {
    final DatastoreHandler.UpdateBuilder updateBuilder = DatastoreHandler.newUpdateBuilder();
    for (BaseDatastoreEntity entity : entities) {
      updateBuilder.addUpdate(entity.buildEntity());
    }
    updateBuilder.commit();
  }

  public void update() throws DatastoreException {
    final Entity.Builder builder = buildEntity();
    DatastoreHandler.newUpdateBuilder().addUpdate(builder).commit();
  }

  public Key create() throws DatastoreException {
    final Entity.Builder builder = buildEntity();
    final CommitRequest request = CommitRequest.newBuilder()
        .setMutation(Mutation.newBuilder().addInsertAutoId(builder))
        .setMode(CommitRequest.Mode.NON_TRANSACTIONAL)
        .build();
    final Key key = DatastoreHandler.getDatastore().commit(request)
        .getMutationResult().getInsertAutoIdKey(0);
    extractKey(key, this);
    return key;
  }

  public static List<Key> create(BaseDatastoreEntity... entities) throws DatastoreException {
    final Mutation.Builder mutation = Mutation.newBuilder();
    for (BaseDatastoreEntity entity : entities) {
      final Entity.Builder builder = entity.buildEntity();
      mutation.addInsertAutoId(builder);
    }
    final CommitRequest request = CommitRequest.newBuilder()
        .setMutation(mutation)
        .setMode(CommitRequest.Mode.NON_TRANSACTIONAL)
        .build();
    return DatastoreHandler.getDatastore().commit(request)
        .getMutationResult().getInsertAutoIdKeyList();
  }

  /**
   * Build Entity along with Key and Properties
   *
   * @return
   */
  public Entity.Builder buildEntity() {
    final Entity.Builder builder = Entity.newBuilder();

    //build properties from inherited entities
    buildProperties(builder);

    builder.setKey(buildKey());
    return builder;
  }

  /**
   * Add all the properties of the Object to be saved into datastore
   *
   * @param builder
   */
  protected abstract void buildProperties(Entity.Builder builder);

  public Key.Builder buildKey() {
    if (!TextUtils.isEmpty(mParentTableKind)) { //parent hierarchy exist
      if (mID <= 0) //new entity with parent
        return makeKey(makeKey(mParentTableKind, mParentID).build(), mTableKind);
      else //existing entity with parent
        return makeKey(makeKey(mParentTableKind, mParentID).build(), mTableKind, mID);
    } else {
      if (mID <= 0) //new entity
        return makeKey(mTableKind);
      else //existing entity
        return makeKey(mTableKind, mID);
    }
  }

  /**
   * Extract details from the Key into ID, ParentID etc..
   *
   * @param key
   */
  public static void extractKey(Key key, IDatastoreEntity iEntity) {
    BaseDatastoreEntity entity = (BaseDatastoreEntity) iEntity;
    for (Key.PathElement path : key.getPathElementList()) {
      if (path.getKind().equals(entity.mTableKind))
        entity.mID = path.getId();
      else if (path.getKind().equals(entity.mParentTableKind))
        entity.mParentID = path.getId();
    }
  }

  /**
   * Set Property of the object with a given value
   *
   * @param name
   * @param value
   */
  public abstract void setValue(String name, Value value);

  public Value getValue(String name) {
    switch (name) {
      case COLUMN_KEY:
        return makeValue(mID).build();
      case COLUMN_PARENT_KEY:
        return makeValue(mParentID).build();
    }
    return null;
  }


  public static final List<IDatastoreEntity> getEntities(
      Class<? extends BaseDatastoreEntity> clazz, Key parentKey)
      throws DatastoreException, NoSuchFieldException {
    final String tableKind, tableKindParent;
    if (clazz.isAnnotationPresent(DatastoreEntity.class)) {
      DatastoreEntity dsEntity = clazz.getAnnotation(DatastoreEntity.class);
      tableKind = dsEntity.kind();
      tableKindParent = dsEntity.kindParent();
    } else {
      throw new IllegalArgumentException(clazz.getSimpleName() +
          " must use Annotation DSEntity and mention TABLE_KIND");
    }

    final DatastoreHandler.RunQueryBuilder runQueryBuilder =
        DatastoreHandler.newQueryBuilder()
            .setKind(tableKind);

    if (parentKey != null)
      runQueryBuilder.setFilter(COLUMN_KEY, PropertyFilter.Operator.HAS_ANCESTOR,
          makeValue(parentKey).build());

    final List<DatastoreV1.Entity> entities = runQueryBuilder.runQueryGetEntities();

    final List<IDatastoreEntity> datastoreEntities = Lists.newArrayList();
    for (DatastoreV1.Entity entity : entities) {
      final IDatastoreEntity datastoreEntity = BaseDatastoreEntity.buildFromEntity(entity, clazz);
      if (datastoreEntity != null)
        datastoreEntities.add(datastoreEntity);
    }
    return datastoreEntities;

  }

  public static final List<IDatastoreEntity> getEntities(
      Class<? extends BaseDatastoreEntity> clazz)
      throws DatastoreException, NoSuchFieldException {

    return getEntities(clazz, null);

/*    final String tableKind;
    if (clazz.isAnnotationPresent(DatastoreEntity.class)) {
      DatastoreEntity dsEntity = clazz.getAnnotation(DatastoreEntity.class);
      tableKind = dsEntity.kind();
    } else {
      throw new IllegalArgumentException(clazz.getSimpleName() +
          " must use Annotation DSEntity and mention TABLE_KIND");
    }

    final List<DatastoreV1.Entity> entities = DatastoreHandler.newQueryBuilder()
        .setKind(tableKind)
        .runQueryGetEntities();
    final List<IDatastoreEntity> datastoreEntities = Lists.newArrayList();
    for (DatastoreV1.Entity entity : entities) {
      final IDatastoreEntity datastoreEntity = BaseDatastoreEntity.buildFromEntity(entity, clazz);
      if (datastoreEntity != null)
        datastoreEntities.add(datastoreEntity);
    }
    return (List<? extends BaseDatastoreEntity>) datastoreEntities;*/
  }

  private void fetchKinds() {
    Class<?> clazz = getClass();
    if (clazz.isAnnotationPresent(DatastoreEntity.class)) {
      DatastoreEntity dsEntity = clazz.getAnnotation(DatastoreEntity.class);
      mTableKind = dsEntity.kind();
      mParentTableKind = dsEntity.kindParent();
    } else {
      throw new IllegalArgumentException(clazz.getSimpleName() +
          " must use Annotation DSEntity and mention TABLE_KIND");
    }

  }
}
