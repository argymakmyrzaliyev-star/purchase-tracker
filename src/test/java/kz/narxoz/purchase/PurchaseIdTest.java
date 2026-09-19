package kz.narxoz.purchase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseIdTest {
    @Test
    void keepsAValidId() {
        assertEquals("PUR-001", new PurchaseId("PUR-001").value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n", " \t\n "})
    void rejectsNullOrBlankIds(String value) {
        assertThrows(IllegalArgumentException.class, () -> new PurchaseId(value));
    }
}
