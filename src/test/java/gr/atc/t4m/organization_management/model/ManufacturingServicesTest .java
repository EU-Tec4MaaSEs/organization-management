package gr.atc.t4m.organization_management.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManufacturingServicesTest {

    @Test
    void testFromValue_WithValidInteger() {
        assertEquals(ManufacturingServices.AMS, ManufacturingServices.fromValue(1));
        assertEquals(ManufacturingServices.MACH, ManufacturingServices.fromValue(2));
        assertEquals(ManufacturingServices.PIMS, ManufacturingServices.fromValue(3));
        assertEquals(ManufacturingServices.EB, ManufacturingServices.fromValue(4));
        assertEquals(ManufacturingServices.FM, ManufacturingServices.fromValue(5));
    }

    @Test
    void testFromValue_WithValidString_CaseInsensitive() {
        assertEquals(ManufacturingServices.AMS, ManufacturingServices.fromValue("AMS"));
        assertEquals(ManufacturingServices.MACH, ManufacturingServices.fromValue("mach")); // lowercase
        assertEquals(ManufacturingServices.PIMS, ManufacturingServices.fromValue("PiMs")); // mixed case
    }

    @Test
    void testFromValue_WithInvalidInteger_ShouldThrowException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ManufacturingServices.fromValue(99));
        assertTrue(ex.getMessage().contains("Invalid ManufacturingServices value: 99"));
    }

    @Test
    void testFromValue_WithInvalidString_ShouldThrowException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ManufacturingServices.fromValue("INVALID"));
        assertTrue(ex.getMessage().contains("Invalid ManufacturingServices name: INVALID"));
    }

    @Test
    void testFromValue_WithInvalidType_ShouldThrowException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ManufacturingServices.fromValue(1.23));
        assertTrue(ex.getMessage().contains("Invalid ManufacturingServices input type"));
    }

    @Test
    void testGetName_ShouldReturnEnumName() {
        assertEquals("AMS", ManufacturingServices.AMS.getName());
        assertEquals("MACH", ManufacturingServices.MACH.getName());
    }
}
