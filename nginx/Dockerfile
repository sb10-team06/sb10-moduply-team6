# 공식 Nginx Alpine 이미지를 사용해 이미지 크기와 런타임 오버헤드를 줄인다.
FROM nginx:1.27-alpine

# 프로젝트의 리버스 프록시 설정을 Nginx 기본 서버 설정 위치에 복사한다.
# 컨테이너가 시작되면 Nginx가 이 설정을 자동으로 읽는다.
COPY conf.d/default.conf /etc/nginx/conf.d/default.conf

# 컨테이너가 HTTP 요청을 수신하는 포트를 문서화한다.
# 실제 외부 공개 여부는 ECS Task Definition의 hostPort/containerPort 매핑으로 결정된다.
EXPOSE 80
