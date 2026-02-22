# Backend TODO Roadmap (Community Focus)

## P0 (High Impact)

### 1) 좋아요/조회수 동시성 제어
- Problem: 동시 요청 시 카운트 오차 위험
- Plan:
  - DB 원자 연산 기반 증감 쿼리 적용
  - 좋아요 멱등성 강화(중복 요청 방지 키)
  - 필요 시 분산락(REDIS) 비교 실험
- Evidence:
  - 동시성 테스트(100~1000 동시 요청)에서 오차 0건
  - p95 응답시간 및 실패율 측정

### 2) 검색 최적화
- Problem: LIKE 기반 검색은 데이터 증가 시 느려짐
- Plan:
  - 1차: MySQL FULLTEXT 인덱스 실험
  - 2차: Elasticsearch/OpenSearch 도입 검토
- Evidence:
  - 검색 p95, QPS, DB CPU 비교 Before/After

## P1 (Scalability)

### 3) 추천순 캐시(랭킹) Redis ZSET
- Problem: 추천순 조회 시 DB 정렬 비용 증가
- Plan:
  - Redis ZSET에 post score 저장
  - score = likeCount * w1 + recency * w2
  - 주기적 재계산 또는 이벤트 기반 갱신
- Evidence:
  - 추천순 목록 API p95 개선률
  - DB 쿼리 수/CPU 감소율

### 4) 게시글 상세/목록 캐시 + 무효화
- Problem: 동일 조회 트래픽이 DB에 집중
- Plan:
  - 상세/목록 read-through 캐시
  - 댓글/좋아요/수정 이벤트 시 선택적 무효화
- Evidence:
  - cache hit ratio, 응답시간 개선

## P2 (Reliability & Portfolio Quality)

### 5) 관측성(Observability)
- Plan:
  - Micrometer + Prometheus + Grafana
  - 핵심 API 대시보드(에러율, p95, TPS)
  - slow query 로그 시각화

### 6) 테스트 고도화
- Plan:
  - Testcontainers(MySQL, Redis)
  - 부하테스트(k6) 스크립트 추가
  - 경쟁조건 테스트(좋아요/조회수/댓글)
