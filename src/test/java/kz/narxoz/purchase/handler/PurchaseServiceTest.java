package kz.narxoz.purchase.handler;

import kz.narxoz.purchase.config.Application;
import kz.narxoz.purchase.domain.PurchaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = Application.class)
class PurchaseServiceTest {
    @Autowired
    private PurchaseService service;

    @Test
    void springInjectsRuleAndPermitsApprovedPurchaseToBeOrdered() {
        assertEquals(PurchaseStatus.ORDERED,
                service.move(PurchaseStatus.APPROVED, PurchaseStatus.ORDERED));
    }

    @Test
    void springWiredRuleStopsUnapprovedOrder() {
        assertThrows(IllegalStateException.class,
                () -> service.move(PurchaseStatus.DRAFT, PurchaseStatus.ORDERED));
    }
}
