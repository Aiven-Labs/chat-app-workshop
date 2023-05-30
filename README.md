# Pre-requisites 

## Aiven Account 

First of all you will need an Aiven account, you can create one for free with no credit card asked. You will have a free plan for MySQL, PostreSQL and Redis. You will alos have 300$ of free credit. 
This will be enough for you to run this workshop, get your account [here](https://console.aiven.io/signup).

## Aiven CLI (avn)

Ones you got your account, make sure to install the Aiven CLI tool, you will need it, you can it [here](https://github.com/aiven/aiven-client).

## Options for deploying the workloads

You will need at least to deploy one workload for this workshop. For the first workload we will give you 3 options : 

* Deploy on Kubernetes, this is the preferred way. You can get a free Kubenertes Cluster provided by Red Hat, called the OpenShift Sandbox, please get yours [here](https://developers.redhat.com/developer-sandbox). Make also sure to install the `kubectl` command line on your computer.
* Deploy on Docker, you will need to have Docker or Podman on your computer.
* Build and deploy from sources, you will need to have a JDK and Maven installed on your computer.

# Create Aiven services

## Postgres 

Use the Aiven Console to create a PostgreSQL service using the __free__ plan.

## Redis

Use the Aiven Console to create a Redis service using the __free__ plan.

## Kafka

Use the Aiven Console to create a Kafka service using the __startup__ plan and choose the cheapest region.

Once your service is created, looks for advanced configuration and enable : `kafka.auto_create_topics_enable`.

Also enable `Apache Kafka REST API (Karapace)`. 

## Flink 

Use the Aiven Console to create a Flink service using the __Business__ plan and choose the cheapest region.

# Deploying the `chat-app` 

The `chat-app` relies on 3 data infrastructure services : PostgreSQL, Redis and Kafka.

To connect to your Kafka service, you need to create a keystore and trustore, don't worry the `avn` cli tool can easily do that for you. 

Be sure to be at the root of your repository, connected to your Aiven account with a token and type (replace with your values) : 

```
avn service user-kafka-java-creds <your kafka service name>  --username avnadmin -d . --password safePassword123
```

## Configuration for Kubernetes and Docker

Open `manifests/secret.properties` and set all the correct values, the properties starting with `QUARKUS_DATASOURCE` are for PostgreSQL.

### Create Secrets for Kubernetes 

Be sure that your `kubectl` is connected to your Cluster. 

First let's create a secret that contains our `truststore` and `keystore` : 

```
kubectl create secret generic certs-secrets --from-file=keystore=client.keystore.p12 --from-file=trustore=client.truststore.jks
```
And then a secret for our configuration : 

```
kubectl create secret generic chat-app-secret --from-env-file=manifests/secrets.properties
```

## Configuration if running from sources : 

Copy all the entries from `client.properties` to `src/main/resources.application.properties` and prefix them with `kafka.`

# Deploying and running the app 

## With Kubernetes

Simply do a : `kubectl apply -f manifests/chat-app.yml` 

If you are using the OpenShift Sandbox you can also create the route : `kubectl apply -f manifests/route.yml` to access your app on port 80. 

## With Docker 

Simply do a :

```
docker run -v ./:/etc/certs --env-file=manifests/secrets.properties -p 8080:8080 sebi2706/chat-app

```

The Chat app will available on localhost:8080
## From sources 

simply do a : `./mvnw quarkus:dev`

The Chat app will available on localhost:8080

# Configure Debezium

## Create publication in PostgreSQL

Connect with psql to your PostgreSQL service and type this command : 

```
CREATE PUBLICATION message_publication FOR TABLE message;
```

## Create a Kafka Connect Service

From your Kafka service go to `connectors` and create a standalone Kafka Connect service. 

### Create Connector

Ones your Kafka Connect service is running go to the `connectors` tab and push the button `Create connector`.

Look for the source connector `Debezium - PostgreSQL`

You can try to set all the values yourself and use this configuration sample (replace the values for your database) : 

```

{
    "database.server.name": "workshop",
    "name": "workshop",
    "database.hostname": "<your pg host>",
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.port": <your pg port>,
    "database.user": "avnadmin",
    "database.password": "<your pg password>",
    "database.dbname": "defaultdb",
    "plugin.name": "pgoutput",
    "publication.name": "message_publication"
}

```
Create the connector, now go back to your chat-app, refresh the page, write some messages and go to your Kafka Service, browse to the tab `Topics` and look for a topic called : `workshop.public.message Messages` , press "Fetch messages` , you should see some entries there ! 
Congratulations you have setup a CDC flow ! 

# Configure Flink

## Integrate Kafka with Flink

On the home page of your Flink service click on the red button "Get started" and then choose your Kafka service. 

## Create a Flink Application

Go to the Applications tab and create a new application. You can call it `workshop`.

Press `Create first version` and then `Add source table`

Choose your integrated service and then for the content  : 

```
CREATE TABLE messages (
    after MAP<STRING, STRING>
) WITH (
    'connector' = 'kafka',
    'properties.bootstrap.servers' = '',
    'scan.startup.mode' = 'earliest-offset',
    'topic' = 'workshop.public.message',
    'value.format' = 'json',
    'value.json.ignore-parse-errors' = 'true'
)
```
and 'Add table` 

Now let's add a sink table : 

```
CREATE TABLE special_command (
    content STRING,
    username STRING
) WITH (
    'connector' = 'kafka',
    'properties.bootstrap.servers' = '',
    'scan.startup.mode' = 'earliest-offset',
    'topic' = 'special_command',
    'value.format' = 'json'
)
```
And finally create a statement : 

```

INSERT INTO special_command
SELECT 
    after['content'],
    after['username']
FROM messages
WHERE after['content']  LIKE '%/chatgpt%'

```

You can now create a deployment for your Flink Application. 

# Deploy workload for the special commands 

For now, only available through Kubernetes, simply type : `kubectl apply -f chatgpt-seb.yml`

Go back to your chat app and type a message starting with "\chatgpt" , i.e "\chatgpt What is Apache Flink ?" wait for a few seconds and you should get the reply from the openAI service. 

# Create your own Flink Applications

Now that you know how it works, you can create your own Flink Application being consumed by the worload that you want. 





