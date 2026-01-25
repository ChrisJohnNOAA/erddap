package gov.noaa.pfel.erddap.dataset;

import gov.noaa.pfel.erddap.dataset.metadata.EDDInternationalString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testDataset.Initialization;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EDDInternationalStringTests {

    @BeforeAll
    static void init() throws Throwable {
        Initialization.edStatic();
    }

    @Test
    void testSingleArgumentConstructor() {
        EDDInternationalString eddIS = new EDDInternationalString("default string");
        assertEquals("default string", eddIS.toString());
    }

    @Test
    void testLocalized() {
        Map<Locale, String> localized = Map.of(Locale.FRENCH, "chaîne française");
        EDDInternationalString eddIS = new EDDInternationalString("default string", localized);

        // Test that toString() returns the default
        assertEquals("default string", eddIS.toString());

        // Test that toString(Locale) returns the localized string
        assertEquals("chaîne française", eddIS.toString(Locale.FRENCH));

        // Test that a non-existent locale falls back to the default
        assertEquals("default string", eddIS.toString(Locale.GERMAN));
    }
}
