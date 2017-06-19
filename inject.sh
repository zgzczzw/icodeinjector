rm signed.apk
rm -rf log
mkdir log

echo "#1. Compile LogUtils.java"
javac -encoding UTF-8 -cp android.jar: LogUtils.java

echo "#2. Unzip src.apk"
rm -rf src
mkdir src
cp src.apk src
cd src
unzip src.apk > ../log/unzip.log
cd ..

echo "#3. Dex2Jar classed.dex -> src.jar"
cd dex2jar
./d2j-dex2jar.sh ../src/classes.dex -o ../src.jar --force
cd ..

echo "#4. Add LogUtils.class to src.jar"
rm -rf com
mkdir com
cd com
mkdir icodeinjector
cd ..
mv LogUtils.class com/icodeinjector
aapt r src.jar com/icodeinjector/LogUtils.class
aapt a src.jar com/icodeinjector/LogUtils.class


echo "#5. Jar2Dex src.jar -> classes.dex"
cd dex2jar
./d2j-jar2dex.sh ../src.jar -o ../classes.dex
cd ..

echo "#5. Replace the classes.dex of src.apk"
cp src.apk unsigned.apk
aapt r unsigned.apk classes.dex
aapt a unsigned.apk classes.dex

echo "#6. Resign the apk file"
for file in src/META-INF/*
do
    if test -f $file
    then
        aapt remove unsigned.apk META-INF/${file##*/}
    fi
done
jarsigner -verbose -keystore test.keystore -storepass 123456 -keypass 123456 -signedjar signed.apk unsigned.apk 'test.keystore' > log/sign.log

echo "#7. Install the apk file"
adb install -r signed.apk

echo "#8. Cleaning..."
rm -rf com
rm -rf src
rm src.jar
rm unsigned.apk
rm classes.dex
echo "#9. Finish"
