
_all: docker

docker:
	docker build . -t kjarosh/ms-graph-simulator:latest
	docker build . -f tests/Dockerfile.tests -t kjarosh/ms-graph-simulator-tester:latest

push: docker
	docker push kjarosh/ms-graph-simulator:latest
	docker push kjarosh/ms-graph-simulator-tester:latest

.PHONY: _all docker push
