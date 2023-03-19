# Clear and Create dir
echo "Clear..."
rm -rf ./product
mkdir ./product
gradle clean

# Build by Gradle
echo "Building lightswallow-core.jar ..."
./gradlew lightswallow-core:jar
cp ./lightswallow-core/build/libs/lightswallow-core.jar ./product/lightswallow-core.jar

# Build by CMake
echo "Building lightswallow-lib.so ..."
cd ./lightswallow-native || exit
rm -rf ./build
mkdir build
cd ./build || exit
cmake ../
make
cd ../..
cp ./lightswallow-native/build/libLightSwallow-lib.so ./product/lightswallow-lib.so

echo "Build task finish"
