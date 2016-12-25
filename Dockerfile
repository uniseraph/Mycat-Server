FROM java:8
MAINTAINER zhengtao.wuzt@gmail.com


RUN mkdir -p /opt/

ADD ./target/Mycat-server-1.6.1-RELEASE-*-linux.tar.gz /opt

RUN mkdir /opt/mycat/logs
WORKDIR /opt/mycat


CMD ["./bin/mycat","console"]