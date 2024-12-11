plugins {
    `java-library`
}

dependencies {
    api(libs.edc.spi.http)
    api(libs.edc.spi.data.plane)
    api(libs.edc.spi.data.plane.http)
    implementation(libs.edc.spi.util)
    implementation(libs.edc.spi.data.plane.util)
    implementation(libs.gson)
}
