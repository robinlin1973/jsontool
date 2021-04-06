SET xmibin="..\xmi\"
set xmibin=%xmibin:"=%
SET BIORelationbin="..\BIORel\"
set BIORelationbin=%BIORelationbin:"=%

java -Dfile.encoding=utf-8 -cp json2xmi.jar com.melax.json2xmi.XmiToRelationBIOMain  %xmibin% %BIORelationbin% problem,treatment