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

To build the CLI zip on Ubuntu:
```bash
sudo apt update
sudo apt install openjdk-11-jre-headless
sudo apt install maven
git clone https://github.com/Enterprise-Neurosystem/ai-signal-processing.git
cd ai-signal-processing
mvn -DskipTests clean install # Add -P cuda10.2 to build the GPU version
ls aisp-core/aisp-core-main/target/*.zip  # This is the zip file containing the CLI
```
To enable on Linux, you can install the CLI tree anywhere, but we'll put it in your home directory here.
```bash
cd ~
unzip YOUR_GIT_PARENT_DIR/ai-signal-processing/aisp-core/aisp-core-main/target/aisp-core-main-*.zip
export AISP_HOME=$HOME/aisp
export PATH=$AISP_HOME/bin:$PATH
setup-aisp # add -gpu option if you built the GPU version above.
```
You will probably want to add the two `export` commands above to your ~/.bashrc file so you have the CLI available in all your shells.

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



