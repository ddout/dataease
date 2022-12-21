FROM registry.cn-qingdao.aliyuncs.com/dataease/fabric8-java-alpine-openjdk8-jre:edge-chromium

ARG IMAGE_TAG

RUN mkdir -p /opt/apps /opt/dataease/data/feature/full /opt/dataease/drivers /opt/dataease/plugins /opt/dataease/default

ADD mapFiles/* /opt/dataease/data/feature/full/

ADD drivers/* /opt/dataease/drivers/

ADD plugs/* /opt/dataease/plugins/default/

ADD backend/target/backend-$IMAGE_TAG.jar /opt/apps

ENV JAVA_APP_JAR=/opt/apps/backend-$IMAGE_TAG.jar

ENV AB_OFF=true

ENV JAVA_OPTIONS=-Dfile.encoding=utf-8

HEALTHCHECK --interval=15s --timeout=5s --retries=20 --start-period=30s CMD curl -f 127.0.0.1:8081

CMD ["/deployments/run-java.sh"]
#1: yarn or npm frontend : yarn build:prodbuild:prod
#2: mvn clean package -Pwhole  for backend
#3: docker build to image
#docker build --build-arg IMAGE_TAG=1.16.0.001 -t dataease:v1.16.0.001 .
#docker save -o dataease.v1.16.0.001.tar  dataease:v1.16.0.001
#docker load < dataease.v1.16.0.001.tar
