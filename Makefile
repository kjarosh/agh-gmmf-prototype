
_all: docker

docker:
	docker build gmmf-zone-simulator -t kjarosh/agh-gmmf-prototype:latest
	docker build . -f tests-k8s/Dockerfile.tests -t kjarosh/agh-gmmf-prototype-tester:latest

push: docker
	docker push kjarosh/agh-gmmf-prototype:latest
	docker push kjarosh/agh-gmmf-prototype-tester:latest

.PHONY: _all docker push
