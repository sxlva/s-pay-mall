CONTAINER_NAME=s-pay-mall-ddd-fcr
IMAGE_NAME=system/s-pay-mall-ddd-fcr-app:1.0-SNAPSHOT
PORT=8091
NETWORK=spay-network

echo "容器部署开始 ${CONTAINER_NAME}"

# 停止容器
docker stop ${CONTAINER_NAME}

# 删除容器
docker rm ${CONTAINER_NAME}

# 启动容器并连接到 spay-network
docker run --name ${CONTAINER_NAME} \
--network ${NETWORK} \
-p ${PORT}:${PORT} \
-e SPRING_DATASOURCE_PASSWORD=${MYSQL_ROOT_PASSWORD} \
-e SPRING_REDIS_PASSWORD=${REDIS_PASSWORD} \
-e ROCKETMQ_ACCESS_KEY=${ROCKETMQ_ACCESS_KEY} \
-e ROCKETMQ_SECRET_KEY=${ROCKETMQ_SECRET_KEY} \
-e ROCKETMQ_NAMESRV_ADDR=rocketmq-namesrv:9876 \
-d ${IMAGE_NAME}

echo "容器部署成功 ${CONTAINER_NAME}"

docker logs -f ${CONTAINER_NAME}