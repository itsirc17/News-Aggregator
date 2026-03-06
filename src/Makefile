JAVAC = javac
JAVA = java
SRC_DIR = .
BIN_DIR = .
LIB_DIR = ./lib
CLASSPATH = $(BIN_DIR):$(LIB_DIR)/*

SOURCES = $(wildcard $(SRC_DIR)/*.java)

.PHONY: build run clean

build:
	$(JAVAC) -cp "$(CLASSPATH)" $(SOURCES)

run: build
	$(JAVA) -cp "$(CLASSPATH)" Tema1 $(ARGS)

clean:
	rm -f $(SRC_DIR)/*.class