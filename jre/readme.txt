Verifying the used modules (from unzipped distribution):
C:\jdk\jdk-12\bin\jdeps --list-deps -cp lib\*.jar lib\naaccr-geocoder-review.jar

   java.base
   java.datatransfer
   java.desktop
   java.sql
   java.xml

Creating runtime image (from jre folder):
C:\jdk\jdk-12\bin\jlink --module-path C:\Users\Fabian\dev\jdk\jdk-12\jmods --add-modules java.datatransfer,java.desktop,java.sql,java.xml --output jre-12 --no-header-files --no-man-pages --strip-debug --compress=2

The "--no-header-files" and "no-man-pages" are very safe to use.
The "--strip-debug" option is safe, but keep in mind that it might prevent debugging via the IDE.
The "--compress=2" is a bit obscure, hopefully that won't have any bad side effects.

When a new version of Java is available, re-create the runtime image (the java version appears three times in the command!), test it and push the changes.