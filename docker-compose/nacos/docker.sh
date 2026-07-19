docker pull nacos/nacos-server:v2.4.1

docker run -d -P m.daocloud.io/docker.io/library/nacos/nacos-server:v2.4.1
docker run -d --name nacos -p 8848:8848 -p 9848:9848 -e MODE=standalone -e NACOS_AUTH_ENABLE=true -e NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789 -e NACOS_AUTH_IDENTITY_KEY=server1 -e NACOS_AUTH_IDENTITY_VALUE=server1 nacos/nacos-server:v2.4.1