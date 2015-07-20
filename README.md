# GoogleCloudStorateLib

This is a support library to work with Google Cloud DataStore Java APIs.

# Setup Instructions

1. Activate **Google Cloud Datastore** service
1. Create Project and activate Google Cloud Datastore API ( **project-id or dataset:
devotional-media** )
1. Create Entities in Default Namespace or give name for new namespace
1. If used specific namespace, remember to **setPartitionID(namespace)** with every API request
1. Create Public & Private Keys in [Google Console](https://console.developers.google.com)
    1. select appropriate project
    1. navigate to menu **APIs & auth** --> **Credentials**
    1. click on **Create new Clien ID**
    1. keep safe backup of **Private Key** file
    1. take note of email address which is later reffed as **Service Account ID**
    1. click on **Generate new P12 key**
    1. save this **Public Key** file into RAW folder in your Android project
1. Add **Protobuf API library** for Java in Gradle

```
  compile 'com.google.apis:google-api-services-datastore:v1beta2-rev25-1.20.0'
  compile 'com.google.apis:google-api-services-datastore-protobuf:v1beta2-rev1-2.1.0'
  compile 'com.google.http-client:google-http-client-android:1.20.0'
```

## Usage

**DatastoreHandler.java** provides useful helper methods to work with Cloud Datastore

### Establish Connection
```java
  DatastoreHandler.get(getContext(), DATASTORE_DATASET,
      DATASTORE_PARTITION_ID, DATASTORE_SERVICE_ACCOUNT,
      R.raw.devotional_media_p12);
```
### Building Keys
```java
  //for single Department key - for Auto Insert ID - creation
  Key departmentKey = makeKey(Department.TABLE_KIND);
  //for single Department Key - for specifying KEY
  Key departmentKey = makeKey(Department.TABLE_KIND, "CSC");

  //for Dtudent key with ancestor as Department
  Key studentKey = makeKey(Department.TABLE_KIND,deptID,Student.TABLE_KIND);
```
### Create Entities
```java
  final Key key = Department.create(DatastoreHandler.getDatastore(),
     "DeptID", "Dept Desc");
  Student.create(getDatastore(), "StudentName",
     dateOfBirth,new Date(), key.getPathElement(0).getId());
```

### Query Entities
```java
  final List<Entity> entities = DatastoreHandler.newQueryBuilder()
      .setKind(Department.TABLE_KIND)
      .setFilter(Department.COLUMN_NAME,
          PropertyFilter.Operator.EQUAL, makeValue("DeptID").build())
      .runQueryGetEntities();

  final List<Entity> students = DatastoreHandler.newQueryBuilder()
      .setKind(Student.TABLE_KIND)
      .setFilter(Student.COLUMN_DATE_OF_BIRTH,
          PropertyFilter.Operator.LESS_THAN_OR_EQUAL,
          makeValue(cal.getTime()).build())
      .addOrder(Student.COLUMN_DATE_OF_BIRTH, true)
      .runQueryGetEntities();

  //for Parent-Child Relations add following
  .setFilter(makeFilter(COLUMN_KEY,
            PropertyFilter.Operator.HAS_ANCESTOR,
            makeValue(entities.get(0).getKey()));
```

### Lookup
```java
  final Entity student =
     DatastoreHandler.findBy(Student.TABLE_KIND, Student.COLUMN_NAME,
         PropertyFilter.Operator.EQUAL, makeValue("StudentName").build())
         .getBatch().getEntityResultList().get(0).getEntity();
```

### Update
```java
  final Entity.Builder builder = Entity.newBuilder(studentEntityFromLookupOrQuery);
  builder.clearProperty(); //clears all properties except Key
  for (Property property : student.getPropertyList()) {
   if (property.getName().equals(Student.COLUMN_NAME))
     builder.addProperty(makeProperty(Student.COLUMN_NAME, makeValue("AlteredName")));
   else
     builder.addProperty(property);
  }
  DatastoreHandler.newUpdateBuilder().addUpdate(builder).commit();
```

## Setting Local Datastore Server
# Set Environment
```bash
  export DATASTORE_HOST=http://localhost:8080
  export DATASTORE_DATASET=<dataset_id>
```
# Create local dataset
```bash
  gcd-v1beta2-rev1-2.1.1/gcd.sh create [options] <dataset-directory>
```
# Start Dev Server
```bash
  gcd-v1beta2-rev1-2.1.1/gcd.sh start [options] <dataset-directory>
```
*. [options]
  *. --port=...
  *. --host=...

# Datastore Connection code
```java
  import com.google.api.services.datastore.client.Datastore;
  import com.google.api.services.datastore.client.DatastoreHelper;

  // uses the DATASTORE_HOST and DATASTORE_DATASET env variables
  Datastore datastore = DatastoreHelper.getDatastoreFromEnv();
```
# Datastore connection from Emulator
* on computer => http://localhost:<port>
* on standard emulator => http//10.0.2.2:<port>
* on genymotion emulator => http//10.0.3.2:<port>

# Local Dev Admin Console
```bash
  http://localhost:8080/_ah/admin
```
## Helpful Links

* [API Help](https://cloud.google.com/datastore/docs/concepts/overview)
* [API Explorer](https://developers.google.com/apis-explorer)
* [Cloud Datastore Sample](https://github.com/GoogleCloudPlatform/google-cloud-datastore)