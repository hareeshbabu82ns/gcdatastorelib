package com.har.googlecloudstoragelib;

import android.test.AndroidTestCase;

import com.google.api.services.datastore.DatastoreV1.Entity;
import com.google.api.services.datastore.DatastoreV1.PropertyFilter;
import com.google.api.services.datastore.client.DatastoreException;
import com.har.googlecloudstoragelib.sample.Department;
import com.har.googlecloudstoragelib.sample.Student;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.List;

import static com.google.api.services.datastore.client.DatastoreHelper.makeValue;

/**
 * Created by hareesh on 7/7/15.
 */
public class TestCloudDataStore extends AndroidTestCase {
  public static final String DATASTORE_DATASET = "devotional-media";
  public static final String DATASTORE_PARTITION_ID = "media";
  public static final String DATASTORE_SERVICE_ACCOUNT = "232635178029-0ql70o67vtsk3j2ulljv2cr2pj12o5ok@developer.gserviceaccount.com";

  /* public void testLookupUpdate() {
     try {
 //      final Entity student = Student.findByID(5732568548769792l);

       final Entity student =
           DatastoreHandler.findBy(Student.TABLE_KIND, Student.COLUMN_NAME,
               PropertyFilter.Operator.EQUAL, makeValue("Karthik").build())
               .getBatch().getEntityResultList().get(0).getEntity();

       //prepare for modification of the entity
       final Entity.Builder builder = Entity.newBuilder(student);
       builder.clearProperty(); //clears all properties except Key
       for (Property property : student.getPropertyList()) {
         if (property.getName().equals(Student.COLUMN_NAME))
           builder.addProperty(makeProperty(Student.COLUMN_NAME, makeValue("Karthik Polla")));
         else
           builder.addProperty(property);
       }
       DatastoreHandler.newUpdateBuilder().addUpdate(builder).commit();
     } catch (DatastoreException e) {
       e.printStackTrace();
       assertTrue(false);
     }
   }
 */
  public void testQueryEntities() {

    try {
      final List<Entity> entities = DatastoreHandler.newQueryBuilder()
          .setKind(Department.TABLE_KIND)
          .setFilter(Department.COLUMN_NAME,
              PropertyFilter.Operator.EQUAL, makeValue("CSC").build())
          .runQueryGetEntities();

      assertTrue(entities.size() == 1);//only one department exist with CSC

      final Calendar cal = Calendar.getInstance();
      cal.set(1984, 01, 01);

      final List<Entity> students = DatastoreHandler.newQueryBuilder()
          .setKind(Student.TABLE_KIND)
          .setFilter(Student.COLUMN_DATE_OF_BIRTH,
              PropertyFilter.Operator.LESS_THAN_OR_EQUAL,
              makeValue(cal.getTime()).build())
//          .setFilter(Student.COLUMN_DEPARTMENT,
//              PropertyFilter.Operator.EQUAL,
//              makeValue(entities.get(0).getKey().getPathElement(0).getId()).build())
//          .addOrder(Student.COLUMN_DEPARTMENT, false)
          .addOrder(Student.COLUMN_DATE_OF_BIRTH, true)
          .addOrder(Student.COLUMN_NAME, true)
          .runQueryGetEntities();

      assertTrue(students.size() == 1);

    } catch (DatastoreException e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

/*
  public void testQueryEntityRelations() {
    //run Query for parent node (Department)
    Query.Builder queryBuilder = Query.newBuilder();
    queryBuilder.addKindBuilder().setName(Department.TABLE_KIND);
    queryBuilder.setFilter(makeFilter(Department.COLUMN_NAME,
        PropertyFilter.Operator.EQUAL, makeValue("MCA")));

    try {
      final List<Entity> entities = runQuery(queryBuilder.build());
      assertTrue(entities.size() == 1);//only one department exist with MCA

      //run Query for child node (Student with given Department)
      queryBuilder = Query.newBuilder();
      queryBuilder.addKindBuilder().setName(Student.TABLE_KIND);
      queryBuilder.setFilter(makeFilter(COLUMN_KEY,
          PropertyFilter.Operator.HAS_ANCESTOR,
          makeValue(entities.get(0).getKey())));
//      queryBuilder.addOrder(makeOrder(Student.COLUMN_DATE_OF_JOINING,
//          PropertyOrder.Direction.DESCENDING));

      final List<Entity> students = runQuery(queryBuilder.build());
      assertTrue(students.size() == 1);

    } catch (DatastoreException e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }
*/

  /* public void testCreateEntities() {
     try {
       final Key key = Department.create(DatastoreHandler.getDatastore(),
           "CSC", "Computer Sciences");
       final Calendar calendar = Calendar.getInstance();
       calendar.set(1985, 11, 8);
       Student.create(getDatastore(), "Karthik",
           calendar.getTime(),
           new Date(), key.getPathElement(0).getId());
     } catch (DatastoreException e) {
       e.printStackTrace();
       assertTrue(false);
     }
   }
 */
  public void testConnection() {
    try {
      DatastoreHandler.get(getContext(), DATASTORE_DATASET,
          DATASTORE_PARTITION_ID, DATASTORE_SERVICE_ACCOUNT,
          R.raw.devotional_media_p12);
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }
}
