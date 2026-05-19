
# 运行minio

linux
```bash
docker run -d \
--name minio \
-p 9000:9000 \
-p 9001:9001 \
-v /data/minio/data:/data \
-e "MINIO_ROOT_USER=minioadmin" \
-e "MINIO_ROOT_PASSWORD=minioadmin" \
minio/minio:RELEASE.2025-04-22T22-12-26Z \
server /data --console-address ":9001"
```

windows
```bash
docker run -d --name minio -p 9000:9000 -p 9001:9001 -v D:/minio/data:/data -e "MINIO_ROOT_USER=minioadmin" -e "MINIO_ROOT_PASSWORD=minioadmin" minio/minio server /data --console-address ":9001"
```