
set -e # Exit early if any commands fail

(
  cd "$(dirname "$0")" # Ensure compile steps are run within the repository directory
  mvn -B package -Ddir=/tmp/serverBuild
)

exec java -jar /tmp/serverBuild/java_http.jar "$@" &

  SERVER_PID=$!
  sleep 5

  echo "Server started with PID: $SERVER_PID"

  BASE_URL="http://localhost:4221"

  echo "Testing the base URL"
  curl -i $BASE_URL/

  echo "Stopping the server with PID: $SERVER_PID"
  kill $SERVER_PID
