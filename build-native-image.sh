# Set GRAALVM_HOME to /path/to/graalvm
export PATH=$GRAALVM_HOME/bin:$PATH
export JAVA_HOME=$GRAALVM_HOME

native-image -jar target/scaffold.jar --no-fallback --no-server target/scaffold
