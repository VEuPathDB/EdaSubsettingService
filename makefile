GEN_PACKAGE  := $(shell ./gradlew -q print-gen-package)
BIN_DIR      := .tools/bin

FETCH_EDA_COMMON_SCHEMA := $(shell ./gradlew -q "print-eda-common-schema-fetch")

C_BLUE := "\\033[94m"
C_NONE := "\\033[0m"
C_CYAN := "\\033[36m"

#
# Meta Targets
#

.PHONY: default
default:
	@echo "Please choose one of:"
	@echo ""
	@echo "$(C_BLUE)  make install-dev-env$(C_NONE)"
	@echo "    Ensures the current dev environment has the necessary "
	@echo "    installable tools to build this project."
	@echo ""
	@echo "$(C_BLUE)  make gen-jaxrs$(C_NONE)"
	@echo "    Generates Java classes representing API interfaces as "
	@echo "    defined in api.raml and child types."
	@echo ""
	@echo "$(C_BLUE)  make compile$(C_NONE)"
	@echo "    Compiles the existing code in 'src/'.  Regenerates files if the"
	@echo "    api spec has changed."
	@echo ""
	@echo "$(C_BLUE)  make test$(C_NONE)"
	@echo "    Compiles the existing code in 'src/' and runs unit tests."
	@echo "    Regenerates files if the api spec has changed."
	@echo ""
	@echo "$(C_BLUE)  make jar$(C_NONE)"
	@echo "    Compiles a 'fat jar' from this project and its dependencies."
	@echo ""
	@echo "$(C_BLUE)  make docker$(C_NONE)"
	@echo "    Builds a runnable docker image for this service"
	@echo ""
	@echo "$(C_BLUE)  make clean$(C_NONE)"
	@echo "    Remove files generated by other targets; put project back in its"
	@echo "    original state."
	@echo ""

.PHONY: compile
compile: install-dev-env gen-jaxrs gen-docs
	@./gradlew clean compileJava

.PHONY: test
test: install-dev-env gen-jaxrs gen-docs
	@./gradlew clean test

.PHONY: jar
jar: install-dev-env build/libs/service.jar

.PHONY: docker
docker:
	@./gradlew build-docker --stacktrace

.PHONY: install-dev-env
install-dev-env:
	@if [ ! -d .tools ]; then git clone https://github.com/VEuPathDB/lib-jaxrs-container-build-utils .tools; else cd .tools && git pull && cd ..; fi
	@./gradlew check-env install-raml-4-jax-rs
	@$(BIN_DIR)/install-raml-merge.sh
	@$(BIN_DIR)/install-npm.sh

.PHONY: gen-jaxrs
gen-jaxrs: api.raml merge-raml
	@$(BIN_DIR)/generate-jaxrs.sh $(GEN_PACKAGE)
	@$(BIN_DIR)/generate-jaxrs-streams.sh $(GEN_PACKAGE)
	@$(BIN_DIR)/generate-jaxrs-postgen-mods.sh $(GEN_PACKAGE)

.PHONY: clean
clean:
	@rm -rf .bin .gradle .tools build vendor

.PHONY: gen-docs
gen-docs: api.raml merge-raml
	@$(BIN_DIR)/generate-docs.sh

.PHONY: merge-raml
merge-raml:
	@echo "Downloading dependencies..."
	$(FETCH_EDA_COMMON_SCHEMA) > schema/url/eda-common-lib.raml
	$(BIN_DIR)/merge-raml schema > schema/library.raml
	rm schema/url/eda-common-lib.raml

.PHONY: api-test
api-test:
	@./gradlew regression-test

#
# File based targets
#

build/libs/service.jar: gen-jaxrs gen-docs build.gradle.kts
	@echo "$(C_BLUE)Building application jar$(C_NONE)"
	@./gradlew clean test shadowJar
