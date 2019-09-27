## Schedule Explorer for Distributed Systems

This repo contains code for the enforcement of certain schedules in the execution of [Cassandra distributed system](https://gitlab.mpi-sws.org/burcu/cas-6023). 


### Requirements:

- Jenv with Java 7 (for compiling Cassandra 2.0.0) and Java 8 (for compiling the server code) installations
- Ant 1.9.14 (for compiling Cassandra 2.0.0)

### Contents of the repo:
- ```test-server:``` Keeps the test server code which communicates the Cassandra nodes to intercept and enforce the processing of certain messages
- ```cassandra:``` Keeps the configuration, data and query files to be used by Cassandra system

### Installation of the test server 

Go to the server directory and build the explorer jar file:
 
```
cd test-server
./mvnw clean install
```

### Installation of Cassandra 2.0.0
Clone the source code of Cassandra-2.0.0 which is instrumented to communicate to the test server: 

```
git clone https://gitlab.mpi-sws.org/burcu/cas-6023.git
```

Place the folder into ```explorer-server``` and compile the systems:

```
cd explorer-server/cassandra-6023
ant
```


### Replaying a schedule in Cassandra:

Configure the following parameters in ```explorer-server/test-server/explorer.conf``` file:

- The paths of directories/binaries
- The schedule file to reproduce


Run the exploration server. The server automatically starts the Cssandra nodes and sends query workloads to be processed.

```
java -jar target/test-server-jar-with-dependencies.jar 
```

Some example schedule files can be found in ```explorer-server/test-server/schedules``` folder.