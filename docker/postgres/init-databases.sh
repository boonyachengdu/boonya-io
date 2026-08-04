#!/bin/bash
set -e

# 创建所有需要的数据库
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE iot_auth;
    CREATE DATABASE iot_ota;
EOSQL
