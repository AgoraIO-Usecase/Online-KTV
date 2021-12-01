#! /bin/sh
set -e
export LC_APP_ID=
export LC_APP_KEY=
export LEANCLOUD_APP_ID=
export LEANCLOUD_APP_KEY=
export LEANCLOUD_APP_MASTER_KEY=
export LEANCLOUD_APP_PORT=3000
export LEANCLOUD_APP_ENV=development
java -jar ./target/spring-boot-getting-started-0.0.2-SNAPSHOT.jar
