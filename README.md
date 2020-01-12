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

<!--
### Replaying a schedule in Cassandra:

Configure the following parameters in ```explorer-server/test-server/explorer.conf``` file:

- The paths of directories/binaries
- The schedule file to reproduce


Run the exploration server. The server automatically starts the Cassandra nodes and sends query workloads to be processed.

```
java -jar target/test-server-jar-with-dependencies.jar 
```

Some example schedule files can be found in ```explorer-server/test-server/schedules``` folder.
-->


### Writing and running  a test scenario

You can write your own test scenario using the test API in ```explorer-server/test-server/src/testAPI``` folder. The test driver automatically starts the Cassandra nodes,  sends query workloads to be processed and injects failures as specified using the API.

Here is an example test file, reproducing the Cassandra 6023 bug.


```
    public void onlineFailuresForBuggyScenario() {
    	String query1 = "UPDATE tests SET value_1 = 'A' WHERE name = 'testing' IF owner = 'user_1'";
    	String query2 = "UPDATE tests SET value_1 = 'B', value_2 = 'B' WHERE name = 'testing' IF  value_1 = 'A'";
    	String query3 = "UPDATE tests SET value_3 = 'C' WHERE name = 'testing' IF owner = 'user_1'";

	OnlineTestDriver test = new OnlineTestDriver();
	test.startCluster();
        test.sleep(4000);

        // send workload
        test.submitQuery(0, query1);

        test.runForRounds(4); 
        test.failNode(2);

        // send workload
        test.submitQuery(1, query2);

        test.runForRounds(2); 
        test.resumeNode(2);

	test.runForRounds(2); 
        test.failNode(2);

        test.runForRounds(2); 
        test.failNode(0);

        // send workload
        test.submitQuery(2, query3);

	test.runForRounds(2); 
        test.resumeNode(2);
        test.failNode(0);
        test.failNode(1);

        test.runForRounds(2); 
        test.resumeNode(1);

        test.runToCompletion();

	test.waitUntilCompletion();  // returns control here when the execution is completed 

        new CassVerifier().verify();

        test.stopCluster();
        test.tearDown();
    }
```

To run the scenario, configure the following parameters in ```explorer-server/test-server/explorer.conf``` file:

- (Necessary) The paths of directories and binaries
- (Optional) Timeouts, output files, etc.


Run the sample test in ```explorer-server/test-server/src/TestMain.java``` file:

```
java -jar target/test-server-jar-with-dependencies.jar 
```




