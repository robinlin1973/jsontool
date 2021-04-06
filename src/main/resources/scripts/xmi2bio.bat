SET xmibin="..\xmi\"
set xmibin=%xmibin:"=%
SET BIOEntitybin="..\BIO\"
set BIOEntitybin=%BIOEntitybin:"=%

java -Dfile.encoding=utf-8 -cp json2xmi.jar com.melax.json2xmi.XmiToEntityBIOMain  %xmibin% %BIOEntitybin% 