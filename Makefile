# Makefile for PtoP overlay
all: compile
	echo 'Done'
	
clean:
	echo -e 'Cleaning up...'
	rm -rf ./cs455/**/**/*.class

compile:
	echo -e 'Compiling the Source...'
	javac -d . ./src/cs455/**/**/*.java