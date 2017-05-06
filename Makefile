check:
	./gradlew clean check bintrayUpload

publish: check
	./gradlew releng
	./gradlew -PdryRun=false --info annotations:bintrayUpload || echo 'Failure!'
	./gradlew -PdryRun=false --info runtime:bintrayUpload || echo 'Failure!'
	./gradlew -PdryRun=false --info plugin:bintrayUpload || echo 'Failure!'
