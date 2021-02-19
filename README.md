[![maven-all](https://github.com/mboysan/fortumows/workflows/maven-all/badge.svg)](https://github.com/mboysan/fortumows/actions)

TODO: add/polish readme

```
# jetty (default)
mvn clean package verify org.codehaus.cargo:cargo-maven3-plugin:run

# tomcat
mvn -Dcargo.containerId=tomcat10x clean package verify org.codehaus.cargo:cargo-maven3-plugin:run

# jetty (explicit)
mvn -Dcargo.containerId=jetty11x clean package verify org.codehaus.cargo:cargo-maven3-plugin:run
```