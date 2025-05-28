# Docker Compose를 이용한 쿠폰 서비스 실행 가이드

이 문서는 Docker Compose를 사용하여 MySQL, Redis, Adminer를 실행하고 애플리케이션을 실행하는 방법을 설명합니다.

## 사전 요구사항

- Docker Desktop (Windows/Mac) 또는 Docker Engine (Linux) 설치
- Docker Compose 설치 (Docker Desktop에는 기본 포함)
- Git (선택사항)

## 1. 환경 설정

### 1.1 환경 변수 설정

`.env` 파일을 생성하고 필요한 환경 변수를 설정합니다. 기본값이 이미 설정되어 있으니 필요시 수정하세요.

```bash
# .env 파일이 없는 경우 복사하여 생성
cp .env.example .env
```

### 1.2 데이터베이스 초기화 스크립트

`init/init.sql` 파일에 데이터베이스 초기화 스크립트가 포함되어 있습니다. 필요에 따라 수정하세요.

## 2. Docker Compose를 사용한 서비스 실행

### 2.1 서비스 시작

다음 명령어로 모든 서비스를 시작합니다:

```bash
docker-compose up -d
```

### 2.2 서비스 상태 확인

실행 중인 컨테이너 상태를 확인합니다:

```bash
docker-compose ps
```

### 2.3 서비스 로그 확인

특정 서비스의 로그를 확인합니다:

```bash
# MySQL 로그
docker-compose logs -f mysql

# Redis 로그
docker-compose logs -f redis
```

## 3. 서비스 접속 정보

### 3.1 MySQL

- 호스트: `localhost`
- 포트: `3306`
- 데이터베이스: `coupon_db`
- 사용자: `coupon_user`
- 비밀번호: `coupon_pass` (기본값)

### 3.2 Redis

- 호스트: `localhost`
- 포트: `6379`
- 비밀번호: 설정된 경우 `.env` 파일 참조

### 3.3 Adminer (웹 기반 데이터베이스 관리 도구)

- URL: http://localhost:8080
- 시스템: MySQL
- 서버: `mysql`
- 사용자: `coupon_user`
- 비밀번호: `coupon_pass` (기본값)
- 데이터베이스: `coupon_db`

## 4. 애플리케이션 실행

### 4.1 로컬에서 실행 (Docker 없이)

1. JDK 17 이상 설치
2. MySQL 및 Redis 서버가 실행 중인지 확인
3. 애플리케이션 실행:
   ```bash
   ./gradlew bootRun
   ```

### 4.2 Docker를 통한 실행 (선택사항)

`docker-compose.yml` 파일의 주석을 해제하여 애플리케이션을 Docker 컨테이너로 실행할 수 있습니다.

## 5. 서비스 중지 및 정리

### 5.1 서비스 중지 (컨테이너 유지)

```bash
docker-compose stop
```

### 5.2 서비스 중지 및 컨테이너 제거

```bash
docker-compose down
```

### 5.3 볼륨을 포함한 모든 리소스 제거

```bash
docker-compose down -v
```

## 6. 문제 해결

### 6.1 포트 충돌

- 3306, 6379, 8080 포트가 이미 사용 중인 경우 `docker-compose.yml` 파일에서 포트 매핑을 수정하세요.

### 6.2 데이터베이스 연결 문제

- MySQL이 완전히 시작되기 전에 애플리케이션이 연결을 시도할 수 있습니다. `depends_on`과 `healthcheck`를 사용하여 해결할 수 있습니다.

### 6.3 볼륨 문제

데이터가 예상대로 유지되지 않는 경우, 볼륨이 제대로 마운트되었는지 확인하세요.

## 7. 백업 및 복원

### 7.1 데이터베이스 백업

```bash
docker exec -t coupon-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} coupon_db > backup.sql
```

### 7.2 데이터베이스 복원

```bash
cat backup.sql | docker exec -i coupon-mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} coupon_db
```

## 8. 모니터링

### 8.1 컨테이너 리소스 사용량 확인

```bash
docker stats
```

### 8.2 Redis 모니터링

```bash
docker exec -it coupon-redis redis-cli
> INFO
> MONITOR
```

## 9. 추가 리소스

- [Docker 문서](https://docs.docker.com/)
- [Docker Compose 문서](https://docs.docker.com/compose/)
- [MySQL Docker 이미지](https://hub.docker.com/_/mysql)
- [Redis Docker 이미지](https://hub.docker.com/_/redis)
