package drivers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrowserstackOsVersionTest {

    @Test
    void s22UltraDoesNotKeepAndroid13() {
        assertEquals("12.0", BrowserstackDriver.resolveAndroidOsVersion(
                "Samsung Galaxy S22 Ultra", "13.0"));
    }

    @Test
    void s22UltraAlreadyOn12Stays() {
        assertEquals("12.0", BrowserstackDriver.resolveAndroidOsVersion(
                "Samsung Galaxy S22 Ultra", "12.0"));
    }

    @Test
    void s23KeepsAndroid13() {
        assertEquals("13.0", BrowserstackDriver.resolveAndroidOsVersion(
                "Samsung Galaxy S23", "13.0"));
    }

    @Test
    void otherDevicesAreNotRewritten() {
        assertEquals("14.0", BrowserstackDriver.resolveAndroidOsVersion(
                "Google Pixel 9", "14.0"));
    }
}
