# ai-signal-processing
Framework, Tools and Solutions for processing high-rate signal data such as acoustics, vibrations, etc. See the <a href="https://github.com/Enterprise-Neurosystem/ai-signal-processing/wiki">wiki</a> for documentation.

Please subscribe to the Community mailer to participate in asynchronous discussions. https://lists.enterpriseneurosystem.org/admin/lists/community.lists.enterpriseneurosystem.org/

Contact David Wood (dawood@us.ibm.com) for questions/comments/contributions.

## Build/Install
1. Install Java 1.8 or later (up to Java 14)
1. Install maven 3.6.3 or later
1. git pull this repo
1. CPU builds
   1. mvn -DskipTests=true clean install
   1. cd SOMEWHERE; unzip .../ai-signal-processing/aisp-core/aisp-core-main/target/aisp-core-main-0.0.1-SNAPSHOT-cpu.zip
1. GPU builds
   1. mvn -DskipTests=true -P cuda10.2 clean install
   1. cd SOMEWHERE; unzip .../ai-signal-processing/aisp-core/aisp-core-main/target/aisp-core-main-0.0.1-SNAPSHOT-cuda10.2.zip   
1. See the wiki to enable and use the CLI

## Dependency
Artifacts are not currently published to any public maven repositories, but if you build locally you can use
the following dependency in your projects.
```xml
<dependency>
   <groupId>org.eng.aisp</groupId>
   <artifactId>aisp-core-main</artifactId>
   <version>0.0.1-SNAPSHOT</version>
</dependency>
```



