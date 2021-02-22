[![maven-all](https://github.com/mboysan/fortumows/workflows/maven-all/badge.svg)](https://github.com/mboysan/fortumows/actions)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/b287c0ebc16344eb9b62471f2d4dad81)](https://www.codacy.com/gh/mboysan/fortumows/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=mboysan/fortumows&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/b287c0ebc16344eb9b62471f2d4dad81)](https://www.codacy.com/gh/mboysan/fortumows/dashboard?utm_source=github.com&utm_medium=referral&utm_content=mboysan/fortumows&utm_campaign=Badge_Coverage)


# Introduction

This is the implementation of the test task given by fortumo. Contact me for more details.
Note that this repo will be archived once fortumo reviews the solution.

# Running the Server

## Using Cargo Maven Plugin

Prerequisites:
- jdk 15
- maven (latest)

This is the easiest way to test and run the application. It uses the maven [cargo plugin](https://codehaus-cargo.github.io/cargo/Home.html).

To run the jetty application server and deploy the servlet use:
```
mvn clean package verify cargo:run
```

The default port is `8080`. You can change it using:
```
mvn -Dcargo.servlet.port=1337 clean package verify cargo:run
```
If you prefer tomcat as the application server, you can run it with:

```
mvn -Dcargo.containerId=tomcat10x clean package verify cargo:run
```

## Using Tomcat

Prerequisites:
- [Tomcat 10](https://tomcat.apache.org/download-10.cgi)
- maven (latest)
- jdk 15

This is a more involved process, but the servlet deployment can be summarized with the following instructions.

1. Download and extract the tomcat application server into some path on your local machine. We'll be using
   `/tmp/tomcat` as the download location.
2. Remove the `/tmp/tomcat/webapps/ROOT` directory. We don't need this since our servlet uses the POST path `/` as its
   base path.
3. Clone this repo to some directory, assuming `/tmp/fortumows` and change directory to this location.
4. Run `mvn clean package`.
5. Copy the war file generated in the target directory to tomcat webapps as ROOT.war:
   ```
   cp -v target/*.war /tmp/tomcat/webapps/ROOT.war
   ```
6. Start tomcat.
7. Finally, confirm that you can do POST requests to `http://localhost:8080`

# TODO:
- dockerize
- inspect log configuration
- add integration tests