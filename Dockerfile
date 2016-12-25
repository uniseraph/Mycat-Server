FROM java:8



MAINTAINER zhengtao.wuzt@gmail.com


RUN mkdir -p /opt

ADD ./target/Mycat-server-1.6-RELEASE-*-linux.tar.gz /opt

WORKDIR /opt/mycat


CMD ["./bin/mycat","console"]