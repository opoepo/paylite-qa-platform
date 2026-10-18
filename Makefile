.PHONY: help up test logs down clean

help:
	@echo "make up     - build and start the database and the service"
	@echo "make test   - start everything and run the API tests"
	@echo "make logs   - show the service log"
	@echo "make down   - stop and remove containers (data is kept)"
	@echo "make clean  - remove containers, volumes and test reports"
	>@echo "make compile - compile the tests without starting anything"

compile:
	docker compose run --rm --no-deps tests mvn -B -ntp test-compile

up:
	docker compose up -d --build --wait sut

test: compile up
	docker compose run --rm tests

logs:
	docker compose logs sut

down:
	docker compose down

clean:
	docker compose down -v
	rm -rf tests-java/target
