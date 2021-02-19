[![maven-all](https://github.com/mboysan/fortumows/workflows/maven-all/badge.svg)](https://github.com/mboysan/fortumows/actions)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/b287c0ebc16344eb9b62471f2d4dad81)](https://www.codacy.com/gh/mboysan/fortumows/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=mboysan/fortumows&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/b287c0ebc16344eb9b62471f2d4dad81)](https://www.codacy.com/gh/mboysan/fortumows/dashboard?utm_source=github.com&utm_medium=referral&utm_content=mboysan/fortumows&utm_campaign=Badge_Coverage)


TODO: add/polish readme

```
# jetty (default)
mvn clean package verify org.codehaus.cargo:cargo-maven3-plugin:run

# tomcat
mvn -Dcargo.containerId=tomcat10x clean package verify org.codehaus.cargo:cargo-maven3-plugin:run

# jetty (explicit)
mvn -Dcargo.containerId=jetty11x clean package verify org.codehaus.cargo:cargo-maven3-plugin:run
```