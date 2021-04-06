SET jsonbin="F:\LINBIN\Melax\SEMA4\JSONXMI\Release\json\"
set jsonbin=%jsonbin:"=%
SET bratbin="F:\LINBIN\Melax\SEMA4\JSONXMI\Release\brat\"
set bratbin=%bratbin:"=%
SET xmibin="F:\LINBIN\Melax\SEMA4\JSONXMI\Release\xmi\"
set xmibin=%xmibin:"=%

python generator.py -i %jsonbin% -o %bratbin%
java -Dfile.encoding=utf-8 -jar json2xmi.jar %bratbin% %xmibin%
